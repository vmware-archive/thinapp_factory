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


import eventlet
import eventlet.queue
import json
import logging
from pubsub import pub

eventlet.monkey_patch()

DefaultTimeout = lambda *args, **kwargs: eventlet.Timeout(3, *args, **kwargs)

log = logging.getLogger(__name__)

class Client(object):
   def __init__(self, server):
      self._server = server

   def GetProject(self, projectId):
      return self._server.GetProject(projectId)

   def GetProjectFiles(self, projectId):
      return self._server.GetProjectFiles(projectId)

   def GetDatastore(self, dsId):
      return self._server.GetDatastore(dsId)

   def GetDirectory(self, dirId):
      return self._server.GetDirectory(dirId)

   def GetRegKey(self, keyId):
      return self._server.GetRegKey(keyId)

   def GetRegSubKeys(self, keyId):
      return self._server.GetRegSubKeys(keyId)

   def GetRegValues(self, keyId):
      return self._server.GetRegValues(keyId)

   def GetDirChildren(self, keyId):
      return self._server.GetDirChildren(keyId)

   def DeleteProject(self, projectId):
      self._server.DeleteProject(projectId)

   def CreateProject(self, datastore, runtimeId):
      return self._server.CreateProject(datastore, runtimeId)

   def UpdateProject(self, project):
      self._server.UpdateProject(project)

   def RefreshProject(self, projectId):
      return self._server.RefreshProject(projectId)
