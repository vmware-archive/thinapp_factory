# VMware ThinApp Factory
# Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import codecs
import collections
import ConfigParser
import errno
import eventlet
import inject
import json
import logging
import ntpath
import os
import png
import random
import tempfile
import threading
import time
import traceback
import urllib2
import weakref

# Only necessary in standalone case
eventlet.monkey_patch()

from eventlet.green import subprocess
from contextlib import closing, contextmanager
from path import path
from sqlalchemy.orm.exc import NoResultFound

from converter import defs
from converter.lib import registry
from converter.server.storage import DataStores
from converter.server import model, util
from converter.server.storage import DatastoreException

log = logging.getLogger(__name__)

Image = collections.namedtuple('Image', 'width height size path')

@inject.appscope
class Projects(object):
   """ Service for managing created projects """
   STOP = object()

   # TODO: Any more?
   RESTRICTED_PROJECT_FILENAMES = [
      '##Attributes.ini',
   ]

   # XXX: Should user be able to edit build.bat?
   RESTRICTED_ROOT_FILENAMES = [
      'Package.ini',
      'build.bat',
      'HKEY_CLASSES_ROOT.txt',
      'HKEY_CURRENT_USER.txt',
      'HKEY_LOCAL_MACHINE.txt',
      'HKEY_USERS.txt',
      'HKEY_CURRENT_CONFIG.txt',
   ]

   # These paths will never be registered and tracked.
   DONT_REGISTER_PATHS = [
      'bin',
      'Support',
   ]

   db = inject.attr('db')

   def __init__(self):
      self.deleteQueue = eventlet.Queue()
      self.deleteStopped = False
      self.rebuildQueue = eventlet.Queue()
      self.rebuildStopped = False

      # Dictionary of weak references to Locks that govern file locking
      # of ThinAppFile objects. The mapping is fileId -> Lock. If no entry
      # is present, the file is assumed to be unlocked.
      self.fileLocks = weakref.WeakValueDictionary()

      # It's locks all the way down.
      self.lockMutex = threading.Lock()

      # By default this lets 1000 requests run so we may want to limit
      # it to a lower number.
      self.pool = eventlet.GreenPool()

   def Stop(self):
      self.deleteQueue.put(self.STOP)
      self.rebuildQueue.put(self.STOP)

   def GenericLoop(self, loopMethod):
      def method():
         while True:
            try:
               loopMethod()
            except Exception:
               # NOTE: This will trap AssertionErrors (raised from
               # `assert foo`).  Unclear if we should trap them or not.

               # Delay in restarting so that we don't DOS ourselves in
               # case an exceptions keeps on getting thrown.
               log.exception('Project loop encountered an exception but is restarting in 5 seconds.')
               eventlet.sleep(5)

      return method

   @staticmethod
   def GetFiles(projectRoot):
      """
      Return a list of tuple (relativePath, size) for all packages in the
      given project

      @param projectRoot absolute path to project root directory
      """
      deliverables = projectRoot / 'bin'

      try:
         return [(projectRoot.relpathto(f), f.size) for f in deliverables.walkfiles()]
      except OSError, e:
         # The job directory may have been removed on failure or
         # cancellation.
         if e.errno != errno.ENOENT:
            raise

      return []

   @staticmethod
   def ReadRegistry(sandbox):
      # Scan the registry files for this project and add them to the
      # database. Note that for the root key, isolation and parent are
      # both null, unlike other keys.
      rootKey = model.RegistryKey()
      rootKey.path = ''

      REGISTRY_HIVES = ('HKEY_CLASSES_ROOT',
                        'HKEY_CURRENT_USER',
                        'HKEY_LOCAL_MACHINE',
                        'HKEY_USERS',
                        'HKEY_CURRENT_CONFIG')

      for hive in REGISTRY_HIVES:
         hivePath = sandbox / hive + '.txt'
         if hivePath.exists():
            # XXX: File is not locked at this time; should think of a way to
            # use OpenProjectFile
            with codecs.open(hivePath, 'r', 'utf-16') as f:
               top = registry.ImportRegistry(f)
               # Ignore the 'root' of each hive.
               walkNodes = [(rootKey, v) for v in top.subkeys.itervalues()]

               for parent, child in walkNodes:
                  # Register the current key and its values.
                  newKey = Projects.MakeRegistryKey(child)
                  parent.subkeys.append(newKey)

                  for item in child.values.values():
                     newValue = Projects.MakeRegistryValue(item)
                     newKey.values.append(newValue)

                  # Add its children to the walk list, with a pointer
                  # to the parent key.
                  walkNodes += [(newKey, v) for v in child.subkeys.itervalues()]

      return rootKey

   # Needs to use OpenProjectFile - so cannot be @staticmethod
   def WriteRegistry(self, project, makeDirty=True):
      """
         Turn the model.RegistryKey hierarchy in the database back into a real
         RegistryKey hive for export.

         project is treated as read-only (detached) in this method.
      """
      sandbox = path(project.datastore.localPath) / project.subdir

      def walkSubkey(modelKey):
         key = registry.RegistryKey(modelKey.path,
                                    modelKey.isolation,
                                    modelKey.intermediate)

         for modelVal in modelKey.values:
            val = registry.RegistryValue(modelVal.name,
                                         modelVal.data,
                                         modelVal.regType,
                                         modelVal.nameExpand,
                                         modelVal.dataExpand)

            key.values[modelVal.name] = val

         for modelSub in modelKey.subkeys:
            key.subkeys[ntpath.basename(modelSub.path)] = walkSubkey(modelSub)

         return key

      for subkey in project.registry.subkeys:
         txtPath = sandbox / subkey.path + '.txt'

         # Create a fake root for each one.
         subRoot = registry.RegistryKey('', 'full', True)
         subRoot.subkeys[subkey.path] = walkSubkey(subkey)

         with self.OpenProjectFile(project.id, subkey.path + '.txt', 'w', 'utf-16', internal=True, makeDirty=makeDirty) as (tvrFile, fileId):
            registry.ExportRegistry(subRoot, tvrFile)
            tvrFile.flush()

   @staticmethod
   def IsRestrictedPath(p):
      dir, base = path(p).splitpath()

      return base in Projects.RESTRICTED_PROJECT_FILENAMES or \
             (dir == '' and base in Projects.RESTRICTED_ROOT_FILENAMES)

   def Wait(self):
      self.pool.waitall()

   def Fsck(self):
      # TODO: This could contain more consistency checks in the future.
      log.debug('Performing consistency check.')

      # Go through ALL projects and fix in-flight states.
      STATE_CLEANUP_MAP = {
         'deleting': 'deleted',
         'rebuilding': 'dirty',
      }

      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         dirty = sess.query(model.Project).filter(model.Project.state.in_(STATE_CLEANUP_MAP))
         for proj in dirty:
            newState = STATE_CLEANUP_MAP[proj.state]
            log.info('Set state to %s for project %d', newState, proj.id)
            proj.state = newState

   def DeleteLoop(self):
      log.debug('Projects delete loop started.')

      sess = self.db.CreateSession()

      # XXX: If the datastore the files are on is currently offline
      # the caller won't know that we fail, it'll just go to deleted.
      # Need more status cases.

      while True:
         request = self.deleteQueue.get()

         if request is self.STOP:
            self.deleteStopped = True
            break

         with closing(sess):
            project = sess.query(model.Project).get(request)

            if not project:
               # Just drop the request on the floor and keep going.
               log.error('No such project %d' % request)
            else:
               assert project.state == defs.Projects.DELETING
               self.pool.spawn(self._processDelete, project.id)

      log.debug('Project delete loop exited.')

   def RebuildLoop(self):
      log.debug('Project rebuild loop started.')

      sess = self.db.CreateSession()

      # XXX: If the datastore the files are on is currently offline
      # the caller won't know that we fail, it'll just go to deleted.
      # Need more status cases.

      while True:
         request = self.rebuildQueue.get()

         if request is self.STOP:
            self.rebuildStopped = True
            break

         with closing(sess):
            project = sess.query(model.Project).get(request)

            # We don't check here
            if not project:
               # Just drop the request on the floor and keep going.
               log.error('No such project %d' % request)
            else:
               assert project.state == defs.Projects.REBUILDING
               self.pool.spawn(self._processRebuild, project.id)

      log.debug('Project rebuild loop exited.')

   def Update(self, project):
      sess = self.db.CreateSession()
      with model.AutoSession(sess):
         sess.merge(project)

   @inject.param('datastores', DataStores)
   def Create(self, targetDatastoreId, runtimeId, datastores):
      sess = self.db.CreateSession()

      project = model.Project()

      with model.AutoSession(sess):
         project.datastore = datastores.GetDatastore(sess, targetDatastoreId)
         project.runtime_id = runtimeId
         project.state = defs.Projects.CREATED
         # subdir isn't nullable so set to a dummy value until we have
         # the primary key after flushing below.
         project.subdir = ''

         sess.add(project)
         sess.flush() # So that we have the id.

         project.subdir = 'project-%d' % project.id
         # Have to flush again for project.subdir to get set.
         sess.flush()

         lease = None

         # Create the base directory for the project.
         try:
            ds = project.datastore
            lease = datastores.Acquire(ds.id)
            # Make sure that the datastore is up-to-date since it
            # could have gone into the online state between looking
            # the project up and acquiring the datastore lease.
            sess.refresh(ds)
            supportPath = path(ds.localPath) / project.subdir / 'Support'
            logPath = supportPath / 'taf.log'
            try:
               supportPath.makedirs()
               # to ensure the Support/appfactory.log is
               # writeable by tomcat and readable by user
               logPath.touch()
               logPath.chmod(0664)
            except OSError, e:
               if e.errno != errno.EEXIST:
                  raise
         finally:
            if lease:
               datastores.Release(lease)

         # project has been expired since it was just committed.
         # Therefore the next time one of its attributes is accessed
         # it will need to hit the database.  So go ahead and refresh
         # it so that its values are already loaded for the caller to
         # access.
         sess.refresh(project)

         # Remove it from the session since we just need to read
         # values.
         sess.expunge(project)

      return project

   @inject.param('datastores', DataStores)
   def Import(self, targetDatastoreId, runtimeId, datastores):
      newProjects = {}

      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         ds = datastores.GetDatastore(sess, targetDatastoreId)
         if ds.status != 'online':
            raise DatastoreException('Datastore [%s] must be online to import projects!', ds.name);

         result = util.ScanProjectDirs(ds.localPath)
         dirs = result['Valid_Dirs']

         if len(dirs) == 0:
            log.info('No ThinApp project directories found in %s', ds.localPath)
            return { 'newProjects' : newProjects, 'errors' : result['Invalid_Dirs_Map'] }

         for dir in dirs:
            project = model.Project()
            project.datastore = ds
            project.state = defs.Projects.CREATED
            project.subdir = dir
            project.runtime_id = runtimeId
            sess.add(project)
            sess.flush()
            newProjects[project.id] = dir

      return { 'newProjects' : newProjects, 'errors' : result['Invalid_Dirs_Map'] }

   def SetState(self, projectId, state):
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         project = sess.query(model.Project).get(projectId)
         project.state = state

   @staticmethod
   def _iniToJson(stream):
      parser = ConfigParser.ConfigParser()
      parser.optionxform = str
      parser.readfp(stream)

      result = {}

      for section in parser.sections():
         contents = dict()

         for option in parser.options(section):
            contents[option] = parser.get(section, option)

         result[section] = contents

      return result

   def _extractIcons(self, iconFile, index):
      """
      @return tuple of (temporary directory, icon directory).  Caller
      is responsible for removing the temporary directory (which will
      include the icon directory.)
      """
      tmpDir = path(tempfile.mkdtemp())

      try:
         cmd = ['wrestool', '-x', '-t14', '-o', tmpDir, iconFile]
         log.info('Running command: %s.', ' '.join(cmd))
         stdout, stderr = subprocess.Popen(cmd, stderr=subprocess.PIPE,
                                           stdout=subprocess.PIPE).communicate()
         stderr.strip() and log.error(stderr)
         stdout.strip() and log.info(stdout)
         # XXX: Not really sure this is the right index to use or not,
         # just going off filesystem name ordering.
         log.debug('Extracted resource contents: %s', tmpDir.files())
         files = tmpDir.files()
         # Sort file list so index maps correctly.
         files.sort()
         icon = files[index]

         extractedIcons = tmpDir / 'icons'
         extractedIcons.mkdir()
         # Note: icotool always produces .png output.
         cmd = ['icotool', '-x', '-o', extractedIcons, icon]
         stdout, stderr = subprocess.Popen(cmd, stderr=subprocess.PIPE,
                                           stdout=subprocess.PIPE).communicate()
         log.info('Running command: %s.', ' '.join(cmd))
         log.debug('Extracted icon contents: %s', extractedIcons.files())
         stderr.strip() and log.error(stderr)
         stdout.strip() and log.info(stdout)

         return tmpDir, extractedIcons
      except:
         tmpDir.rmtree()
         raise

   @staticmethod
   def GetRegistryKeyFromPath(project, path):
      """
      Given a registry key path like HKLM\Software\Microsoft return
      the corresponding registry key for the project.
      """
      def scanLeft(func, state, items):
         for i in items:
            state = func(state, i)
            yield state

      pathElems = path.split('\\')
      # Given the input HKLM\Software\Microsoft this would generate
      # [HKLM, HKLM\Software, HKLM\Software\Microsoft] so that we can
      # iterate over it looking for the corresponding key.
      fullPath = scanLeft(lambda x, y: x + '\\' + y, '', pathElems)
      # Strip off leading \
      fullPath = map(lambda x: x.lstrip('\\'), fullPath)

      # Start search at project registry root.
      regKey = project.registry

      for p in fullPath:
         for i in regKey.subkeys:
            if i.path == p:
               # Continue search by rebasing root onto the new key.
               regKey = i
               break
         else:
            # The next subkey was not found.
            return None

      # Check for the full path at the end to ensure we terminated at
      # the right place.
      if regKey.path == path:
         return regKey
      else:
         return None

   @classmethod
   def _guessIconFromRegistry(cls, project):
      key = cls.GetRegistryKeyFromPath(project,
         'HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall')
      if len(key.subkeys) > 0:
         entry = key.subkeys[0]
         for i in entry.values:
            if i.name == 'DisplayIcon':
               return registry.InterpretData(i.regType, i.data, i.dataExpand)

   @staticmethod
   def getIconImages(iconDir):
      for f in iconDir.walk('*.png'):
         img = png.Reader(filename=f)
         width, height, _, _ = img.read()
         yield Image(width, height, f.size, f)

   @classmethod
   def _getBestIcon(cls, iconDir):
      """
      Try to find the most suitable icon.
      @return icon Image if found, otherwise None
      """
      PREFERABLE_SIZES = (64, 48, 32)
      images = list(cls.getIconImages(iconDir))

      # Only consider square icons.
      squared = filter(lambda x: x.width == x.height, images)

      # Sort descending by dimensions and file size.  The comparison
      # is done on the tuple of the (dimension, file size) so within
      # the sorted dimensions the file sizes will also be sorted.
      # This is so that we get the highest possible quality for the
      # given dimension.  It's possible this could result in a lower
      # quality file if, for example, the size 64 icon was present but
      # had lower quality than the size 48 one.
      #
      # This is just a rough heuristic for now.  Ideally we would make
      # a better tradeoff in terms of size vs. image quality
      # available.
      sortedSquares = sorted(squared, cmp=lambda x, y: cmp((x.width, x.size),
                                                           (y.width, y.size)),
                                                           reverse=True)
      for i in sortedSquares:
         if i.width in PREFERABLE_SIZES:
            return i

      # Couldn't find anything we like so just pick the first largest square.
      if len(sortedSquares) > 0:
         return sortedSquares[0]
      else:
         return None

   def _registerIcon(self, project, projectRoot):
      with self.OpenProjectFile(project.id, 'Package.ini', 'r', 'utf-16') as (packageIni, fileId):
         ini = self._iniToJson(packageIni)
         buildOptions = ini['BuildOptions']
         if 'InventoryIcon' in buildOptions:
            icon = buildOptions['InventoryIcon']
         else:
            log.debug('No InventoryIcon was preset in project %d. Making a guess.',
                      project.id)
            icon = self._guessIconFromRegistry(project)

         if icon is None:
            log.info('No icon is available.')
            return

         log.info('Using icon: %s.', icon)

         # Convert to forward slashes and remove any leading path
         # separator to avoid path joining problems.
         icon = icon.replace('\\', '/').lstrip('/')

         # Icon path may optionally have an index separated by a
         # comma indicating which icon within the resource to use.
         try:
            iconFile, iconIndex = icon.split(',')
         except ValueError:
            iconFile, iconIndex = icon, '0'

         iconIndex = int(iconIndex)

         iconFile = path(projectRoot) / iconFile
         log.info('Using icon %s[%d] for project %d.', iconFile, iconIndex, project.id)
         tmpDir, iconDir = self._extractIcons(iconFile, iconIndex)
         try:
            icon = self._getBestIcon(iconDir)

            if icon is not None:
               log.debug('Using icon %s.', icon)
               iconData = icon.path.bytes()
               project.icon = iconData
            else:
               log.debug('Unable to find a suitable icon.')
         finally:
            tmpDir.rmtree()

   @inject.param('datastores', DataStores)
   def Refresh(self, projectId, datastores):
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         project = sess.query(model.Project).get(projectId)

         datastore = datastores.GetDatastore(sess, project.datastore.id)
         projectRoot = path(datastore.localPath) / project.subdir

         lease = datastores.Acquire(datastore.id)

         try:
            # Always completely repopulate files since entry points
            # may have been removed.
            del project.files[:]
            files = self.GetFiles(projectRoot)
            self.RegisterFiles(project, files, sess)
         finally:
            datastores.Release(lease)

      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         # Have to do this in a new session otherwise registryIcon
         # will fail because it's looking for some state that isn't
         # flushed when it tries to read Package.ini settings.
         project = sess.query(model.Project).get(projectId)
         try:
            self._registerIcon(project, projectRoot)
         except Exception:
            log.exception('Unable to register an icon for the project.  Continuing.')
         project.state = defs.Projects.AVAILABLE

   def Delete(self, projectId):
      if self.deleteStopped:
         # Don't queue requests that will not get processed.
         raise RuntimeError('The project delete queue is not running.')
      else:
         self.deleteQueue.put(projectId)

   def Rebuild(self, projectId):
      if self.rebuildStopped:
         # Don't queue requests that will not get processed.
         raise RuntimeError('The project rebuild queue is not running.')
      else:
         log.debug('Got rebuild request for %d', projectId)
         self.rebuildQueue.put(projectId)

   def GetProjectFileByPath(self, projectId, findPath, directory=False, session=None):
      detached = False
      if session is None:
         session = self.db.CreateSession()
         detached = True

      project = session.query(model.Project).get(projectId)

      query = session.query(model.ThinAppFile) \
             .filter(model.ThinAppFile.root == project.directory) \
             .filter(model.ThinAppFile.path == findPath) \
             .filter(model.ThinAppFile.isDirectory == directory)

      # Might throw NotResultFound
      res = query.one()

      if detached:
         session.refresh(res)
         session.expunge(res)

      return res

   def CreateDirectory(self, projectId, createPath):
      # Verify that parent exists in DB
      dir, base = path(createPath).splitpath()
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         project = sess.query(model.Project).get(projectId)
         root = path(project.datastore.localPath) / project.subdir
         sysPath = root / createPath

         if sysPath.isdir():
            raise Exception('Directory %s already exists for project' % createPath)
         elif '..' in sysPath:
            raise Exception('Relative paths not allowed: %s' % createPath)

         # Create the directory before touching the DB. That way, the second
         # the database is flushed, there is no race to create the directory.
         sysPath.mkdir()

         try:
            # We want to raise the exception directly if the parent is not
            # found for whatever reason.
            parent = self.GetProjectFileByPath(projectId, dir, directory=True, session=sess)

            for child in parent.children:
               # Verify that a name with a different (or equal) case does not
               # already exist. Windows is case insensitive.
               if path(child.path).name == base:
                  raise Exception('Directory %s already exists for project' % createPath)

            node = model.ThinAppFile()
            node.isDirectory = True
            node.root = parent.root
            node.path = createPath

            parent.children.append(node)

            sess.add(node)
            sess.flush()
            sess.refresh(node)

            return node.id
         except:
            sysPath.rmdir()
            sess.rollback()
            raise

   def DeleteFile(self, projectId, fileId, internal=False):
      sess = self.db.CreateSession()

      # We want to close the session only on exceptions.
      project = sess.query(model.Project).get(projectId)
      fileObj = sess.query(model.ThinAppFile).get(fileId)

      if project is None or fileObj is None:
         sess.close()
         raise Exception('Project or file object not found')

      root = path(project.datastore.localPath) / project.subdir
      dir, base = path(fileObj.path).splitpath()

      if not internal and self.IsRestrictedPath(fileObj.path):
         raise Exception('Cannot delete restricted file %s' % base)
      else:
         pathSave = fileObj.path

         if fileObj.isDirectory:
            # If the directory has more than one file in it, it is obviously
            # not empty. If it only has one file in it, and that file is not
            # ##Attributes.ini, also consider it not empty.
            if len(fileObj.children) > 1:
               raise Exception('Cannot delete nonempty directory %s' % fileObj.path)
            elif len(fileObj.children) == 1:
               # Only automatically delete ##Attributes.ini. Not anything else
               child = fileObj.children[0]

               if path(child.path).name != '##Attributes.ini':
                  raise Exception('Cannot delete nonempty directory %s' % fileObj.path)

               self.DeleteFile(projectId, child.id, internal=True)

         # Delete the DB object before the file object to ensure consistency
         with model.AutoSession(sess):
            # XXX: fileObj seems to get expired in certain cases here.
            # Ask for it again. TODO: why?
            fileObj = sess.query(model.ThinAppFile).get(fileId)

            try:
               with self.LockProjectFile(fileObj.id):
                  sess.delete(fileObj)
            except NoResultFound:
               log.warning('File deleted before it could be locked. Returning.')
               return

         # Now actually delete the file/directory
         try:
            if fileObj.isDirectory:
               (root / pathSave).rmdir()
            else:
               (root / pathSave).unlink()
         except OSError, e:
            if e.errno != errno.ENOENT:
               raise
            log.warning('File %s already deleted. Continuing anyway.', pathSave)

   def DeleteProjectData(self, recordType, recordId, projectId=None):
      PROJECT_DATA_TYPE_MAP = {
         defs.Projects.TYPE_REGKEY: model.RegistryKey,
         defs.Projects.TYPE_REGVALUE: model.RegistryValue,
         defs.Projects.TYPE_FILE: model.ThinAppFile,
      }

      if recordType not in PROJECT_DATA_TYPE_MAP:
         raise RuntimeError('Unrecognized type name %s' % recordType)

      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         chosenType = PROJECT_DATA_TYPE_MAP[recordType]
         obj = sess.query(chosenType).get(recordId)
         sess.delete(obj)

         # If supplied, mark project as dirty
         if projectId:
            project = sess.query(model.Project).get(projectId)
            project.state = defs.Projects.DIRTY

   def UpdateRegistryKey(self, updateKey, newValues, projectId=None):
      """
         updateKey is meant to be a detached object returned by GetRegKey.
         The caller will modify this object and send it back.
      """
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         # Bring this into the session
         updateKey = sess.merge(updateKey)

         # Mark the key as no longer intermediate since it has
         # values bound to it.
         updateKey.intermediate = False

         newValueDict = dict([(v.name, v) for v in newValues])
         oldValueDict = dict([(v.name, v) for v in updateKey.values])

         newValueSet = set([v.name for v in newValues])
         oldValueSet = set([v.name for v in updateKey.values])

         finalValues = []

         # Start by populating totally new values, the easiest case
         for newValueName in newValueSet - oldValueSet:
            newValue = newValueDict[newValueName]
            finalValues.append(self.MakeRegistryValue(newValue))

         # For the rest, must check the data carefully
         for existingValueName in newValueSet & oldValueSet:
            updateValue = newValueDict[existingValueName]
            oldValue = oldValueDict[existingValueName]

            # XXX: We unnecessarily delete and recreate here
            if updateValue.regType != oldValue.regType or \
               updateValue.data != oldValue.data or \
               updateValue.nameExpand != oldValue.nameExpand or \
               updateValue.dataExpand != oldValue.dataExpand:
               finalValues.append(self.MakeRegistryValue(updateValue))
            else:
               # If neither registry type nor data have changed, consider
               # it a no-op for now.
               finalValues.append(oldValue)

         # Deleted values are automatically handled by SQLAlchemy
         updateKey.values = finalValues

         # If supplied, mark project as dirty
         if projectId:
            project = sess.query(model.Project).get(projectId)
            project.state = defs.Projects.DIRTY

   @contextmanager
   # TODO: Allow caller to pass in a fileId or file object instead of having to pass
   # in the path of the object.
   def OpenProjectFile(self, projectId, _filePath, mode, charset=None, internal=False, makeDirty=True):
      sess = self.db.CreateSession()

      try:
         project = sess.query(model.Project).get(projectId)
      except NoResultFound:
         sess.close()
         raise

      root = path(project.datastore.localPath) / project.subdir
      filePath = path(_filePath)
      fileDir, fileBase = filePath.splitpath()

      # Some sanity checks.
      if not (root / filePath).abspath().startswith(root):
         sess.close()
         raise Exception('Directory traversal outside project root not allowed')

      if 'r' in mode:
         # Do not allow opening of a file that is not backed by a ThinAppFile.
         # Note that reading locked files is OK.
         dbFile = self.GetProjectFileByPath(project.id, filePath, session=sess)

         # XXX: Is this necessary? If you yield a file, does its enter/exit get
         # automatically called as if you just did with open(...)?
         try:
            fobj = codecs.open(root / filePath, mode, encoding=charset)
            yield fobj, dbFile.id
         except:
            sess.close()
            raise
         finally:
            fobj.close()
      elif 'w' in mode:
         # Only internal consumers may write to restricted paths.
         if not internal and self.IsRestrictedPath(filePath):
            raise Exception('Cannot write to restricted filename %s' % fileBase)

         # Write to a randomly generated file first. Return a wrapped file
         # that upon successful closing will rename it to the target, and
         # upon exception will delete the temp file.
         # XXX: Better way to generate temp file names.
         tempPath = root / filePath + '.%d.tmp' % time.time()

         # NB: codecs.open always opens in binary mode even if no charset
         # is specified
         fobj = codecs.open(tempPath, mode, encoding=charset)

         try:
            fileId = self.GetProjectFileByPath(projectId, filePath, session=sess).id
            existingFile = True
         except NoResultFound:
            log.debug('File %s not found -- creating a new one', filePath)
            existingFile = False

            # Verify that a differently cased version of the same name does not
            # already exist.
            listing = (root / fileDir).listdir()
            fileLower = fileBase.lower()

            for f in listing:
               if f.name.lower() == fileLower:
                  raise Exception('Conflicting filename, tried to create %s but %s already exists' % (fileBase, f.name))

            # New file - create a new entry and lock and hide it.
            parentObj = self.GetProjectFileByPath(project.id,
                                                  fileDir,
                                                  directory=True,
                                                  session=sess)

            dbFile = model.ThinAppFile()
            dbFile.path = filePath
            dbFile.isDirectory = False
            dbFile.hidden = True
            dbFile.root = project.directory

            # implies sess.add(dbFile)
            with model.AutoSession(sess):
               parentObj.children.append(dbFile)

               sess.flush()
               sess.refresh(dbFile)
               fileId = dbFile.id
         except: # Catch-all
            sess.close()
            raise

         # At this point, we have no need for the session, close it if
         # not already closed by an AutoSession.
         sess.close()

         # Now lock the file for writing and pass control to the caller.
         with self.LockProjectFile(fileId):
            try:
               # Pass the file ID on as well (XXX: Better alternative?)
               yield fobj, fileId
            except:
               # Catch-all - exception is reraised after nuking temp file
               tempPath.unlink()
               raise

            log.debug('Rename file from %s to %s', tempPath, root / filePath)
            tempPath.rename(root / filePath)

         # File is created hidden by default. If it's not a system file, then
         # unhide it now.
         with model.AutoSession(sess):
            if not existingFile and not self.IsRestrictedPath(filePath) and \
               fileBase not in self.RESTRICTED_PROJECT_FILENAMES:
               dbFile = sess.query(model.ThinAppFile).get(fileId)
               dbFile.hidden = False

            # Any write operation to a project file causes the project to
            # become dirty.
            if makeDirty:
               project = sess.query(model.Project).get(projectId)
               project.state = defs.Projects.DIRTY
      else:
         sess.close()
         raise Exception('Malformed open mode %s' % mode)

   @contextmanager
   def LockProjectFile(self, fileId):
      # XXX: Better way to atomically unlock lockMutex while beginning to
      # lock self.fileLocks[fileId]?
      success = False
      lock = None

      if fileId not in self.fileLocks:
         self.lockMutex.acquire()
         lock = threading.Lock()
         self.fileLocks[fileId] = lock
         self.lockMutex.release()
      else:
         lock = self.fileLocks[fileId]

      lock.acquire()

      # Now that we have the lock, make sure the object still exists.
      sess = self.db.CreateSession()

      try:
         sess.query(model.ThinAppFile).get(fileId)
      except NoResultFound:
         del self.fileLocks[fileId]
         raise

      yield

      lock.release()

   @staticmethod
   def MakeRegistryValue(value):
      """
         Makes the model.RegistryValue for a given RegistryValue
         This should be the only point of churn if the underlying
         RegistryValue objects change.
      """
      newValue = model.RegistryValue()

      newValue.name = value.name
      newValue.data = value.data
      newValue.regType = value.regType

      newValue.nameExpand = value.nameExpand
      newValue.dataExpand = value.dataExpand

      return newValue

   @staticmethod
   def MakeRegistryKey(key):
      """
         Merely makes the model.RegistryKey object for a RegistryKey.
         This should be the only point of churn if the underlying
         RegistryKey objects change.
      """
      newKey = model.RegistryKey()

      newKey.path = key.path
      newKey.isolation = key.isolation
      newKey.intermediate = key.intermediate

      return newKey

   def CreateRegistryKey(self, parentId, key, values, projectId=None):
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         newKey = self.MakeRegistryKey(key)
         parent = sess.query(model.RegistryKey).get(parentId)

         parent.subkeys.append(newKey)

         for v in values:
            newKey.values.append(self.MakeRegistryValue(v))

         sess.add(newKey)
         sess.flush()

         sess.refresh(newKey)
         newId = newKey.id

         # If supplied, mark project as dirty
         if projectId:
            project = sess.query(model.Project).get(projectId)
            project.state = defs.Projects.DIRTY

         return newId

   @inject.param('datastores', DataStores)
   def _processDelete(self, projectId, datastores):
      sess = self.db.CreateSession()

      with closing(sess):
         lease = None

         try:
            project = sess.query(model.Project).get(projectId)

            # Ensure it won't be unmounted from underneath us.
            lease = datastores.Acquire(project.datastore.id)
            projectDir = path(project.subdir)

            assert not projectDir.isabs()

            fullPath = path(lease.share.localPath) / projectDir
            log.info('Deleting project %d from %s.', projectId, fullPath)

            log.info('Removing read-only bits from project.')

            # path.walk traverses symlinks which we don't want to do.
            eventlet.tpool.execute(util.FixPermissions, fullPath)

            log.info('Read-only bits removed from all files and directories.')

            # XXX: Who knows what errors could happen here but we ignore
            # them and just set the project state to deleted anyway.  The
            # client will have no idea something went wrong.  Maybe it needs
            # to go to a failure state so the client can retry?
            try:
               # XXX: If this fails the client will think they got
               # deleted when they really didn't.  Network might have
               # gone out, etc.

               # Also note that leading directories
               # $localPath/path/to/project will not get cleaned up
               # when they become empty.

               # Must run in threadpool to prevent blocking greenthreads.
               eventlet.tpool.execute(fullPath.rmtree)
               log.info('Project %d deleted.', projectId)
            except Exception:
               log.exception('There was an error removing project %d.', project.id)

            with model.AutoSession(sess):
               # NULL-out icon to avoid database from growing too
               # large.
               project.icon = None
               project.state = defs.Projects.DELETED

         finally:
            if lease:
               datastores.Release(lease)

   @inject.param('datastores', DataStores)
   @inject.param('config')
   def _processRebuild(self, projectId, datastores, config):
      sess = self.db.CreateSession()

      with closing(sess):
         lease = None

         try:
            project = sess.query(model.Project).get(projectId)

            # Ensure it won't be unmounted from underneath us.
            lease = datastores.Acquire(project.datastore.id)
            projectDir = path(project.subdir)

            assert not projectDir.isabs()

            # Dump the registry from the database back out to a file.
            # (But don't affect the REBUILDING state.)
            self.WriteRegistry(project, makeDirty=False)

            fullPath = path(lease.share.localPath) / projectDir
            binPath = fullPath / 'bin'

            # Usually build.bat clears this out but when running under
            # wine it doesn't.
            try:
               binPath.rmtree()
            except OSError, e:
               if e.errno == errno.ENOENT:
                  log.debug('%s did not exist.', binPath)
               else:
                  raise

            buildBat = fullPath / 'build.bat'
            if not buildBat.exists():
               raise RuntimeError('Expected %s to exist but it does not' % buildBat)

            runtimesUrl = config['runtimes.url']
            runtimes = json.load(urllib2.urlopen(runtimesUrl))

            for runtime in runtimes:
               if runtime['id'] == project.runtime_id:
                  log.debug('Using runtime: %s.', runtime)
                  break
            else:
               raise Exception('Unable to locate runtime version %s' % project.runtime_id)

            taRoot = path(runtime['path'])

            # Unfortunately the build.bat files don't exit 1 when the
            # ThinApp runtime is not found. So check for it ourselves first
            # as a sanity check so that we are a little more sure that exit
            # status 0 means success.
            files = ('vregtool.exe', 'vftool.exe', 'tlink.exe')
            for f in files:
               p = taRoot / f
               if not p.exists():
                  raise RuntimeError('ThinApp runtime file unavailable: %s' % p)

            rebuildProc = subprocess.Popen(['wine', 'cmd.exe', '/c', 'build.bat'],
                                              env={'THINSTALL_BIN': taRoot},
                                              cwd=str(fullPath),
                                              stderr=subprocess.PIPE,
                                              stdout=subprocess.PIPE)
            stdout, stderr = rebuildProc.communicate()
            stderr.strip() and log.error(stderr)
            stdout.strip() and log.info(stdout)
            ret = rebuildProc.returncode

            # Check if build.bat left behind package.vo.tvr* files. This
            # indicates a build failure.
            pkgInvalid = self._validateBuildGeneratedFiles(binPath)

            # For now, if a rebuild fails for any reason, the project becomes
            # dirty. Even if you didn't make any changes prior to the rebuild,
            # this rebuild could have created partial output and we don't
            # want people hitting any of the bin/ files until a rebuild does
            # succeed.
            if ret != 0 or pkgInvalid:
               log.info('Rebuild for project %d has failed, status %d', projectId, ret)
               with model.AutoSession(sess):
                  project.state = defs.Projects.DIRTY
               return

            # Else, if successful, let Refresh set up the AVAILABLE
            # state after it scans everything.
            log.info('Rebuild for project %d succeeded.', projectId)
            self.Refresh(projectId)
         finally:
            if lease:
               datastores.Release(lease)

   def _validateBuildGeneratedFiles(self, binPath):
      """ Should be invoked from within _processRebuild """
      dirFiles = os.listdir(binPath)
      for eachFile in dirFiles:
         if eachFile.find('package.ro.tvr') != -1:
            log.info('Rebuild failed, as it left behind package.ro.tvr* files.')
            return True
      return False

   def RegisterFiles(self, project, files, sess):
      """ Should be called from an AutoSession block """

      # Register deliverables
      for (filePath, size) in files:
         log.info('Registering binary %s (%d bytes).', filePath, size)

         f = model.File(path(project.subdir) / filePath, size)

         # Only add files that do not already exist in the case of
         # multiple refreshes.  Note: this does not take into account if
         # the file already exists but the file size has changed!
         if f not in project.files:
            project.files.append(f)

      # Delete old file hierarchies
      deleteDirectory = None

      if project.directory_id:
         deleteDirectory = project.directory

      sandbox = path(project.datastore.localPath) / project.subdir

      # Walk the filesystem hierarchy and add files and directories to
      # the database.
      def addEntry(rootPath, subPath, rootNode, parentNode, isDirectory):
         # This complicated snippet is just to strip the sandbox prefix
         # from this node's path.
         inProjectPath = subPath.partition(rootPath + os.sep)[2]

         node = model.ThinAppFile()

         # Don't register files that we expose through other APIs / are not
         # part of the project.
         if inProjectPath in self.DONT_REGISTER_PATHS:
            return None
         if self.IsRestrictedPath(inProjectPath):
            node.hidden = True

         node.path = inProjectPath
         node.isDirectory = isDirectory
         node.root = rootNode

         if parentNode:
            parentNode.children.append(node)

         return node

      def walker(rootPath, subPath, rootNode, parentNode=None):
         files = subPath.listdir()
         node = addEntry(rootPath, subPath, rootNode, parentNode, True)

         if rootNode is None:
            rootNode = node

         for f in files:
            isDir = f.isdir()
            if isDir:
               walker(rootPath, f, rootNode, node)
            else:
               addEntry(rootPath, f, rootNode, node, isDir)

         return node

      rootDir = walker(sandbox, sandbox, None)

      # Always refresh the directory hierarchy.
      # TODO: Incremental update (removing files that don't exist, adding
      # new ones, keeping the rest.)
      project.directory = rootDir

      # Only import the registry once.
      if project.registry_id is None:
         project.registry = self.ReadRegistry(sandbox)

      if deleteDirectory:
         sess.delete(deleteDirectory)
