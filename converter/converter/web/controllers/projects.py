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


import base64
import ConfigParser
import codecs
import eventlet
import httplib
import inject
import json
import logging
import ntpath
import os
import shutil
import urllib2

from converter.web.lib.base import *
from path import path
from pylons import config, url

from converter import defs
from converter.server.storage import DataStores
from converter.server.projects import Projects
from converter.lib.registry import InterpretData, SerializeData, RegistryValue, RegistryKey

import syscfg

log = logging.getLogger(__name__)

class ProjectsController(BaseController):
   datastores = inject.attr('datastores', DataStores)
   projects = inject.attr('projects', Projects)

   # Poor man's schema.
   DIRECTORY_JSON_KEYS = set(['id', 'path', 'files', 'directories', 'attributes'])
   REGISTRY_JSON_KEYS = set(['id', 'path', 'isolation', 'subkeys', 'values'])
   CREATE_REGISTRY_JSON_KEYS = set(['key', 'parentId'])
   NEW_FILE_JSON_KEYS = set(['name', 'data'])
   CREATE_PROJECT_JSON_KEYS = set(['datastore', 'runtimeId'])

   @staticmethod
   def _getBody(request, expectSet=None):
      message = json.load(request.environ['wsgi.input'])

      if not isinstance(message, dict):
         log.debug('Rejecting input: %s (type %s)', message, type(message))
         abort(httplib.BAD_REQUEST)

      if expectSet:
         inputSet = set(message.keys())

         if inputSet != expectSet:
            log.error('Rejecting input: expected keys %s, got %s', inputSet, expectSet)
            abort(httplib.BAD_REQUEST)

      return message

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

   @staticmethod
   def _jsonToIni(d):
      instance = ConfigParser.ConfigParser()
      instance.optionxform = str

      for section in d.keys():
         instance.add_section(section)

         for key, value in d[section].items():
            instance.set(section, key, value)

      return instance

   @staticmethod
   def _getProject(client, projectId, suitableStates=defs.Projects.AVAILABLE_STATES):
      project = client.GetProject(projectId)

      if project is None:
         abort(httplib.NOT_FOUND)

      if project.state not in suitableStates:
         abort(httplib.FORBIDDEN)

      return project

   def _updateAttributes(self, client, projectId, dirId, attributes):
      dirent = client.GetDirectory(dirId)

      if dirent is None:
         abort(httplib.NOT_FOUND)

      ini = self._jsonToIni(attributes)
      iniPath = path(dirent.path) / '##Attributes.ini'

      # NB: ##Attributes.ini must be written as ASCII
      if len(attributes) == 0:
         # If the attributes list disappears, nuke the file along with it.
         try:
            attrFile = self.projects.GetProjectFileByPath(projectId, iniPath)
            self.projects.DeleteFileByPath(projectId, attrFile.id)
         except projects.NoResultFound: # XXX: This is the SQLAlchemy exception.
            log.debug('No ##Attributes.ini exists for the directory -- nothing to delete')
            pass
      else:
         with self.projects.OpenProjectFile(projectId, iniPath, 'w', internal=True) as (f, fileId):
            ini.write(f)

   @ExceptionToError
   def create(self):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      message = self._getBody(request, self.CREATE_PROJECT_JSON_KEYS)
      datastore = message['datastore']
      runtimeId = message['runtimeId']

      client = self._getClient(request.environ)
      project = client.CreateProject(datastore, runtimeId)

      response.status = httplib.OK
      response.content_type = 'application/json'

      json.dump({'id': project.id}, response)

   @ExceptionToError
   def update(self, id):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      message = self._getBody(request)

      # Update runtime and/or state.
      client = self._getClient(request.environ)
      project = self._getProject(client, id)

      if 'runtimeId' in message and message['runtimeId'] != '':
         project.runtime_id = message['runtimeId']

      if 'state' in message and message['state'] in defs.Projects.CHANGEABLE_STATES:
         project.state = message['state']

      client.UpdateProject(project)

      response.status = httplib.OK

   @ExceptionToError
   def refresh(self, id):
      client = self._getClient(request.environ)
      client.RefreshProject(int(id))

   @ExceptionToError
   def show(self, id):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)

      client = self._getClient(request.environ)
      # Note: we don't use _getProject here because it is OK to ask for the
      # status of an unavailable project.
      project = client.GetProject(projectId)

      if project is None:
         abort(httplib.NOT_FOUND)

      datastore = client.GetDatastore(project.datastore_id)

      if not datastore.baseUrl:
         raise Exception('Files cannot be served because there is no '
                         'web server associated with their datastore.')

      files = []

      for f in client.GetProjectFiles(projectId):
         # XXX: Add test for quoting.
         f = {
            'url': u'%s/%s' % (datastore.baseUrl, urllib2.quote(f.path.encode('utf-8'))),
            'size': f.size,
            'filename': path(f.path).name,
         }

         files.append(f)

      if project.icon is not None:
         iconUrl = url(controller='projects',
                       action='get_icon',
                       id=projectId,
                       qualified=True)
      else:
         iconUrl = None

      msg = {
         'id': project.id,
         'state': project.state,
         'datastoreId': datastore.id,
         'subdir': project.subdir,
         'runtimeId': project.runtime_id,
         'files': files,
         'iconUrl': iconUrl
      }

      response.status = httplib.OK
      response.content_type = 'application/json'

      json.dump(msg, response)

   @ExceptionToError
   def logs(self, id):
      # Attempt to get the requested project
      client = self._getClient(request.environ)
      project = client.GetProject(id)
      if project is None:
         abort(httplib.NOT_FOUND)
      datastore = client.GetDatastore(project.datastore_id)

      log.info('Calling syscfg.zipProjectLogs() to zip up logs of project %s...', id)

      localProjectPath = path(datastore.localPath) / project.subdir
      filePath = syscfg.zipProjectLogs(localProjectPath, id)

      log.info('Project logs zip file created: %s' % filePath)

      fileLen = os.path.getsize(filePath)
      fileName = syscfg.projectLogsFileName(id)

      log.info("Returning zip log data with name '%s' and length %d", fileName, fileLen)

      return self._sendFileResponse(filePath, fileName)

   def delete(self, id):
      client = self._getClient(request.environ)
      projectId = int(id)

      # Enforce that the project is in an available state.
      project = self._getProject(client, projectId, defs.Projects.DELETABLE_STATES)

      # Immediately set the project to DELETING.
      self.projects.SetState(projectId, defs.Projects.DELETING)
      client.DeleteProject(projectId)

      response.status = httplib.OK

   @ExceptionToError
   def show_packageini(self, id):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      if project is None:
         abort(httplib.NOT_FOUND)

      with self.projects.OpenProjectFile(projectId, 'Package.ini', 'r', 'utf-16') as (packageIni, fileId):
         result = self._iniToJson(packageIni)

      response.status = httplib.OK
      response.content_type = 'application/json'

      json.dump(result, response)

   @ExceptionToError
   def get_icon(self, id):
      projectId = int(id)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      if project is None:
         abort(httplib.NOT_FOUND)

      response.content_type = 'image/png'
      response.write(project.icon)

   @ExceptionToError
   def update_packageini(self, id):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      message = json.load(request.environ['wsgi.input'])

      ini = self._jsonToIni(message)

      # OpenProjectFile writes to a temp file and does an atomic-rename.
      with self.projects.OpenProjectFile(projectId, 'Package.ini', 'w', 'utf-16', internal=True) as (newIni, fileId):
         ini.write(newIni)

      response.status = httplib.OK
      response.content_type = 'application/json'

      # TODO: Implement checking to see if we wrote out the same values as
      #       were already present in the ini file
      json.dump({'modified' : True}, response)

   @ExceptionToError
   def show_directory(self, id, did):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)
      dirId = int(did)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      if dirId < 0:
         dirId = project.directory_id

      dirent = client.GetDirectory(dirId)

      if dirent is None:
         abort(httplib.NOT_FOUND)

      # Accumulate files
      files = {}
      directories = {}
      attributes = {}

      for child in client.GetDirChildren(dirId):
         base = path(child.path).name

         if base == u'##Attributes.ini':
            # Hide attributes.ini from the file list, but populate the
            # attributes JSON with it.
            with self.projects.OpenProjectFile(projectId, child.path, 'r') as (ini, fileId):
               attributes = self._iniToJson(ini)
         elif child.hidden:
            continue
         elif child.isDirectory:
            directories[base] = url(controller='projects',
                                    action='show_directory',
                                    id=projectId, did=child.id,
                                    qualified=True)
         else:
            files[base] = url(controller='projects',
                              action='show_file',
                              id=projectId, fid=child.id,
                              qualified=True)

      result = {
         'id': dirent.id,
         'path': dirent.path,
         'files': files,
         'directories': directories,
         'attributes': attributes,
      }

      response.status = httplib.OK
      response.content_type = 'application/json'

      json.dump(result, response)

   @ExceptionToError
   def create_directory(self, id):
      log.debug('create_directory')
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)
      message = self._getBody(request, self.DIRECTORY_JSON_KEYS)
      client = self._getClient(request.environ)

      # Reject entries where 'id' is not null, files is not empty,
      # or directories is not empty.
      # XXX: should we accept a different abbreviated format for directory
      # creation? After all, it is a POST.
      if message['id'] or len(message['files']) > 0 or len(message['directories']) > 0:
         raise Exception('Invalid directory JSON for creation')

      # TODO: perhaps fail if normpath != the original path?
      createPath = path(message['path']).normpath()

      dirId = self.projects.CreateDirectory(projectId, createPath)
      self._updateAttributes(client, projectId, dirId, message['attributes'])

      response.status = httplib.OK
      response.content_type = 'application/json'

      json.dump({'url': url(controller='projects', action='show_directory', id=projectId, did=dirId, qualified=True)}, response)

   @ExceptionToError
   def update_directory(self, id, did):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)
      dirId = int(did)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      # XXX: Should ensure restricted fields match what is already in the DB.
      # For now let's just ignore the fields we don't allow modification for.
      message = self._getBody(request, self.DIRECTORY_JSON_KEYS)
      self._updateAttributes(client, projectId, dirId, message['attributes'])

      response.status = httplib.OK
      response.content_type = 'application/json'

      # TODO: Implement checking to see if we wrote out the same values as
      #       were already present
      json.dump({'modified' : True}, response)

   @ExceptionToError
   def create_file(self, id, did):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)
      dirId = int(did)
      message = self._getBody(request, self.NEW_FILE_JSON_KEYS)

      client = self._getClient(request.environ)
      dirObj = client.GetDirectory(dirId)
      project = client.GetProject(projectId)

      if dirObj is None or project is None:
         abort(httplib.NOT_FOUND)

      newPath = path(dirObj.path) / message['name']

      with self.projects.OpenProjectFile(projectId, newPath, 'wb') as (newFile, fileId):
         newFile.write(base64.b64decode(message['data']))

      response.status = httplib.OK
      response.content_type = 'application/json'

      json.dump({'url': url(controller='projects', action='show_file', id=projectId, fid=fileId, qualified=True)}, response)

   @ExceptionToError
   def show_file(self, id, fid):
      projectId = int(id)
      fileId = int(fid)

      client = self._getClient(request.environ)
      fileObj = client.GetDirectory(fid)
      project = client.GetProject(projectId)

      if fileObj is None or project is None:
         abort(httplib.NOT_FOUND)

      with self.projects.OpenProjectFile(projectId, fileObj.path, 'rb') as (f, fileId):
         shutil.copyfileobj(f, response)

         response.status = httplib.OK
         response.content_type = 'application/octet-stream'

   @ExceptionToError
   def delete_file(self, id, fid, directory):
      projectId = int(id)
      fileId = int(fid)
      isDirectory = int(directory)

      client = self._getClient(request.environ)
      fileObj = client.GetDirectory(fid)

      if fileObj is None:
         log.error('No such file by ID: %d', fileId)
         abort(httplib.NOT_FOUND)
      elif fileObj.isDirectory != isDirectory:
         if directory:
            methodTarget = 'directory'
         else:
            methodTarget = 'file'

         if fileObj.isDirectory:
            fileTarget = 'directory'
         else:
            fileTarget = 'file'

         log.error('Attempt to delete %s but target is a %s', methodTarget, fileTarget)
         abort(httplib.FORBIDDEN)

      self.projects.DeleteFile(projectId, fileId)

   @ExceptionToError
   def update_file(self, id, fid):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)
      fileId = int(fid)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)
      fileObj = client.GetDirectory(fid)

      if fileObj is None:
         abort(httplib.NOT_FOUND)
      elif fileObj.isDirectory:
         abort(httplib.FORBIDDDEN)

      with self.projects.OpenProjectFile(projectId, fileObj.path, 'wb') as (f, fileId):
         shutil.copyfileobj(request.environ['wsgi.input'], f)

      response.status = httplib.OK
      response.content_type = 'application/json'
      # TODO: Implement checking to see if we wrote out the same values as
      #       were already present
      json.dump({'modified': True}, response)

   @ExceptionToError
   def show_registry(self, id, rid):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)
      regId = int(rid)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      if regId < 0:
         regId = project.registry_id

      key = client.GetRegKey(regId)

      if key is None:
         abort(httplib.NOT_FOUND)

      result = {'id': key.id, 'path': key.path, 'isolation': key.isolation}

      # Accumulate subkeys and values
      subkeys = {}
      values = {}

      for child in client.GetRegSubKeys(key.id):
         base = ntpath.basename(child.path)
         subkeys[base] = {
            'url': url(controller='projects', action='show_registry', id=projectId, rid=child.id, qualified=True),
            'hasChildren': len(client.GetRegSubKeys(child.id)) > 0,
         }

      for value in client.GetRegValues(key.id):
         values[value.name] = {
            'type': value.regType,
            'data': InterpretData(value.regType, value.data, value.dataExpand),
            'nameExpand': value.nameExpand,
            'dataExpand': value.dataExpand,
         }

      result['subkeys'] = subkeys
      result['values'] = values

      response.status = httplib.OK
      response.content_type = 'application/json'
      json.dump(result, response)

   @ExceptionToError
   def update_registry(self, id, rid):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)
      regId = int(rid)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)
      key = client.GetRegKey(regId)

      if key is None:
         abort(httplib.NOT_FOUND)

      # XXX: Should ensure restricted fields match what is already in the DB.
      # For now let's just ignore the fields we don't allow modification for.
      message = self._getBody(request, self.REGISTRY_JSON_KEYS)

      # Serialize the whole set of values in the JSON message into
      # RegistryValues. We fake out the expansion settings, but that is an
      # ongoing problem; in Projects, the data and regType are compared with
      # the old value, and if they match, nothing is touched.
      values = []
      valueExpected = set(['data', 'type', 'nameExpand', 'dataExpand'])

      for name, value in message['values'].items():
         if set(value.keys()) != valueExpected:
            log.error('Malformed registry JSON: %s', value)
            abort(httplib.BAD_REQUEST)

         data = SerializeData(value['type'], value['data'], value['dataExpand'])
         values.append(RegistryValue(name=name,
                                     regType=value['type'],
                                     data=data,
                                     nameExpand=value['nameExpand'],
                                     dataExpand=value['dataExpand']))

      # Update isolation mode if necessary
      key.isolation = message['isolation']

      self.projects.UpdateRegistryKey(key, values, projectId)

      response.status = httplib.OK
      # TODO: Implement checking to see if we wrote out the same values as
      #       were already present
      response.content_type = 'application/json'
      json.dump({'modified' : True}, response)

   @ExceptionToError
   def create_registry(self, id):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      projectId = int(id)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      message = self._getBody(request, self.CREATE_REGISTRY_JSON_KEYS)

      # Do sub-verification of the registry JSON underneath.
      jsonKey = message['key']
      inputSet = set(jsonKey.keys())

      if inputSet != self.REGISTRY_JSON_KEYS:
         log.error('Rejecting input: expected keys %s, got %s',
                   inputSet, self.REGISTRY_JSON_KEYS)
         abort(httplib.BAD_REQUEST)

      parentId = int(message['parentId'])
      parentKey = client.GetRegKey(parentId)

      # Locate parent node of this key. Note that the immediate parents must
      # exist for now.
      if jsonKey['path'].endswith('\\'):
         # Cannot create key with trailing slash (prevents dirname from failing)
         log.error('Rejected key creation request due to bad path')
         abort(httplib.BAD_REQUEST)

      if ntpath.dirname(jsonKey['path']) != parentKey.path:
         # Cannot create key if the parent's path is not its immediate parent.
         log.error('Rejected key creation request due to bad parent')
         abort(httplib.BAD_REQUEST)

      for subkey in client.GetRegSubKeys(parentKey.id):
         # Cannot create same key twice under the same parent.
         if subkey.path == jsonKey['path']:
            log.error('Rejected key creation request; key already exists')
            abort(httplib.BAD_REQUEST)

      newKey = RegistryKey(jsonKey['path'], jsonKey['isolation'], False)

      if len(jsonKey['subkeys']) > 0:
         # subkeys are dynamically defined by creating new keys. When creating
         # a new key, by definition it has no subkeys so don't allow this.
         log.error('Rejected key creation request; request cannot contain subkeys')
         abort(httplib.BAD_REQUEST)

      newValues = []
      for valueName, valueData in jsonKey['values'].items():
         newData = SerializeData(valueData['type'], valueData['data'], valueData['dataExpand'])

         newValues.append(RegistryValue(name=valueName,
                                        regType=valueData['type'],
                                        data=newData,
                                        nameExpand=valueData['nameExpand'],
                                        dataExpand=valueData['dataExpand']))

      keyId = self.projects.CreateRegistryKey(parentId, newKey, newValues, projectId)

      result = {
         'url': url(controller='projects', action='show_registry',
                    id=projectId, rid=keyId, qualified=True),
      }

      # Write the response
      response.status = httplib.OK
      response.content_type = 'application/json'
      json.dump(result, response)

   @ExceptionToError
   def delete_registry(self, id, rid):
      projectId = int(id)
      keyId = int(rid)

      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      self.projects.DeleteProjectData(defs.Projects.TYPE_REGKEY,
                                      keyId,
                                      projectId)

      response.status = httplib.OK

   @ExceptionToError
   def rebuild(self, id):
      projectId = int(id)

      # Immediately set the project to REBUILDING.
      # This _getProject call enforces that the project is in an available
      # state before this can go through.
      client = self._getClient(request.environ)
      project = self._getProject(client, projectId)

      self.projects.SetState(projectId, defs.Projects.REBUILDING)
      self.projects.Rebuild(projectId)

      response.status = httplib.OK

   @ExceptionToError
   def import_projects(self):
      # Scan existing ThinApp projects from a datastore using
      # Package.ini as unique key. Valid dir formats are:
      # /xxxx/Package.ini
      # /..../xxxx/Package.ini
      # Then, xxxx will be used as project.subdir
      # Finally, it will create a new project in DB for each
      # directory.
      if request.content_type != 'application/json':
         abort(httplib.BAD_REQUEST)

      params = self._getBody(request, self.CREATE_PROJECT_JSON_KEYS)
      datastoreId = params['datastore']
      runtimeId = params['runtimeId']

      log.info('Starting to import projects from dataStore[%s] with runtime %s',
               datastoreId, runtimeId)

      result = self.projects.Import(datastoreId, runtimeId)

      response.status = httplib.OK
      response.content_type = 'application/json'

      json.dump(result, response)
