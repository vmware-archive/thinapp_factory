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


import atexit
import ConfigParser
import eventlet
import eventlet.tpool
import inject
import json
import logging
import os.path
import pkg_resources

from contextlib import closing
from path import path

from converter import defs
from converter.server import model, projects, storage, util
from converter.server.model import SMBShare
from converter.server.storage import DataStores, DataStoreNotFoundError

# XXX: Put this somewhere sane.
eventlet.monkey_patch()

# XXX: Put this somewhere sane.
logging.basicConfig()
log = logging.getLogger(__name__)

gp = eventlet.GreenPool()

class Server(object):
   projects = inject.attr('projects', projects.Projects)

   @inject.param('db')
   def _getItem(self, _type, id, db):
      sess = db.CreateSession()
      with closing(sess):
         obj = sess.query(_type).get(id)

         # For feature parity with sess.query
         if obj is None:
            return obj

         sess.refresh(obj)
         sess.expunge(obj)

         return obj

   @inject.param('db')
   def _getListItem(self, _type, attr, id, db):
      sess = db.CreateSession()
      with closing(sess):
         obj = sess.query(_type).get(id)

         # For feature parity with sess.query
         if obj is None:
            return obj

         ret = getattr(obj, attr)
         for item in ret:
            sess.refresh(item)
            sess.expunge(item)

         return ret

   def GetProject(self, projectId):
      return self._getItem(model.Project, projectId)

   def GetProjectFiles(self, projectId):
      return self._getListItem(model.Project, 'files', projectId)

   def GetDatastore(self, dsId):
      return self._getItem(model.DataStore, dsId)

   def GetDatastoreByName(self, dsName):
      return _getDatastoreByName(dsName);

   def GetDirectory(self, dirId):
      return self._getItem(model.ThinAppFile, dirId)

   # XXX: This might be better if it returned a non-Model object instead.
   def GetRegKey(self, keyId):
      return self._getItem(model.RegistryKey, keyId)

   def GetRegSubKeys(self, keyId):
      return self._getListItem(model.RegistryKey, 'subkeys', keyId)

   def GetRegValues(self, keyId):
      return self._getListItem(model.RegistryKey, 'values', keyId)

   def GetDirChildren(self, dirId):
      return self._getListItem(model.ThinAppFile, 'children', dirId)

   def Wait(self):
      gp.waitall()

   def Stop(self):
      self.projects.Stop()

      log.info('Waiting for project deletion jobs to finish.')
      self.projects.Wait()

      log.info('Server instance stopped.')

   def CreateProject(self, datastore, runtimeId):
      return self.projects.Create(datastore, runtimeId)

   def UpdateProject(self, project):
      self.projects.Update(project)

   def RefreshProject(self, projectId):
      return self.projects.Refresh(projectId)

   def DeleteProject(self, projectId):
      self.projects.Delete(projectId)

   def Start(self):
      # Do a consistency check.
      self.projects.Fsck()

      eventlet.spawn(self.projects.GenericLoop(self.projects.DeleteLoop))
      eventlet.spawn(self.projects.GenericLoop(self.projects.RebuildLoop))

      log.info('Server instance started.')

@inject.param('db')
@inject.param('datastores', storage.DataStores)
def _getDatastoreByName(name, db, datastores):
   sess = db.CreateSession()
   with closing(sess):
      ds = datastores.GetDatastoreByName(sess, name)

      # For feature parity with sess.query
      if ds is None:
         return ds

      sess.refresh(ds)
      sess.expunge(ds)

      return ds

@inject.param('datastores', storage.DataStores)
def CreateDatastoreIfNotExist(share, datastores):
   try:
      ds = _getDatastoreByName(share.name)
      return ds.id
   except DataStoreNotFoundError:
      return datastores.CreateDatastore(share, errorOnExists=True)

@inject.param('datastores', storage.DataStores)
def ConfigureDatastore(config, datastores):
   # For internal datastore.
   internal = SMBShare(name=storage.INTERNAL,
                       localPath=config['storage.internal.path'],
                       uncPath=config['storage.internal.share'],
                       domain=None,
                       username=config['storage.internal.user'],
                       password=config['storage.internal.password'],
                       baseUrl=config['storage.internal.base_url'])

   system = SMBShare(storage.SYSTEM,
                     config['storage.system.path'],
                     None,
                     None,
                     None,
                     None)

   # Create the system and internal DS if not exist and make them online.
   datastores.GoOnline(CreateDatastoreIfNotExist(internal))
   datastores.GoOnline(CreateDatastoreIfNotExist(system))
   # Bring datastores online that are mountAtBoot.  If any are not
   # able to be brought online startup will continue.
   datastores.BringDefaultDatastoresOnline()

def ConfigureDatabase(config, injector):
   dbUrl = config['sqlalchemy.url']
   db = model.Database(dbUrl)
   # XXX: This shouldn't be done on every start?
   db.Create()

   injector.bind('db', to=lambda: db)

def CreateServer(config):
   # Given all configuration options wire up everything required for a
   # server instance.
   injector = inject.Injector()
   inject.register(injector)

   # Normally you can just pass an instance here but
   # callable(pylons.config) returns True when it really shouldn't so
   # inject tries to create an instance instead of using as is.  The
   # same thing can be accomplished with bind_instance() in newer
   # versions of inject.
   injector.bind('config', to=lambda: config)

   ConfigureDatabase(config, injector)
   ConfigureDatastore(config)

   s = Server()

   atexit.register(s.Stop)

   return s
