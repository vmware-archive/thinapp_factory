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
import httplib
import inject
import json
import logging
import os.path
import urllib2

from converter import defs
from converter.web.lib.base import *

from path import path
from pylons import config

from converter.server.model import SMBShare
from converter.server.storage import DataStores, DataStoreLease, DataStoreNotFoundError, MountError

log = logging.getLogger(__name__)

class StorageController(BaseController):
   # inject doesn't play nicely with Pylons as a param.
   ds = inject.attr('ds', DataStores)

   @ExceptionToError
   def acquire(self, id):
      lease = self.ds.Acquire(id)
      response.write(json.dumps({ 'name': lease.datastoreName, 'id': lease.id }))
      response.content_type = 'application/json'
      response.status = httplib.OK

   @ExceptionToError
   def release(self, id, releaseid):
      id = int(id)

      lease = DataStoreLease(releaseid, id, None)
      self.ds.Release(lease)
      response.status = httplib.OK

   @ExceptionToError
   def create(self):
      if request.content_type != 'application/json':
         abort(httplib.BAD_REQUEST)

      share = self._jsonToSMBShare(request.body)

      if share.name in ('system', 'internal'):
         response.status = httplib.CONFLICT
         return;

      id = self.ds.CreateDatastore(share, errorOnExists=False)
      newLocation = request.path_url + '/' + str(id)
      # Set new resource url in the response 'Location' header.
      response.headers['Location'] = newLocation
      response.status = httplib.CREATED

      if 'online' in request.GET and request.GET['online'] == '1':
         try:
            self.ds.GoOnline(id)
         except:
            log.error('Failed to mount new datastore - %s', share.name)
            self.ds.DeleteDatastore(id)
            response.status = httplib.BAD_REQUEST

   @ExceptionToError
   def update(self, id):
      if request.content_type != 'application/json':
         abort(httplib.BAD_REQUEST)

      share = self._jsonToSMBShare(request.body)

      self.ds.UpdateDatastore(id, share, errorOnExists=False)

      response.status = httplib.OK

   @staticmethod
   def _jsonToSMBShare(body):
      msg = json.loads(body)

      smbpw = msg['password']
      msgStr = str(msg).replace(smbpw, '********')
      log.debug('Received create/update request: %s.', msgStr)

      assert msg['type'] == 'cifs'

      share = SMBShare(name=msg['name'],
                       localPath=None,
                       uncPath='\\\\%s\\%s' % (msg['server'], msg['share']),
                       domain=msg.get('domain'),
                       username=msg['username'],
                       password=msg['password'],
                       baseUrl=msg.get('baseUrl'))

      return share

   @ExceptionToError
   def delete(self, id):
      self.ds.DeleteDatastore(id)
      response.status = httplib.OK

   @ExceptionToError
   def online(self, id):
      self.ds.GoOnline(id)
      response.status = httplib.OK

   @ExceptionToError
   def offline(self, id):
      self.ds.GoOffline(id)
      response.status = httplib.OK

   @ExceptionToError
   @ExceptionToCode(DataStoreNotFoundError, httplib.NOT_FOUND)
   def show(self, id):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      status = self.ds.GetStatus(id)
      response.write(json.dumps(status))

      response.status = httplib.OK
      response.content_type = 'application/json'

   @ExceptionToError
   def list(self):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      status = []

      for i in self.ds.GetDataStoreList():
         status.append(self.ds.GetStatus(i))

      response.write(json.dumps(status))

      response.status = httplib.OK
      response.content_type = 'application/json'
