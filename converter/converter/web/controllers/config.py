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


import httplib
import json
import logging
import shutil
import os

from converter.web.lib.base import *
from converter.server import util
from pylons import config

import syscfg

log = logging.getLogger(__name__)

class ConfigController(BaseController):
   @ExceptionToError
   def logs(self):
      log.info('Calling syscfg.zipApplianceLogs() to zip up the appliance logs...')

      filePath = syscfg.zipApplianceLogs()

      log.info('Appliance logs zip file created: %s', filePath)

      fileLen = os.path.getsize(filePath)
      fileName = syscfg.applianceLogsFileName()

      log.info("Returning zip log data with name '%s' and length %d", fileName, fileLen)

      return self._sendFileResponse(filePath, fileName)

   def getTimeSync(self):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      log.info('Calling syscfg.getTimeSync() to get the state of host/guest '\
               'time synchronization...')
      sync, resultMsg = syscfg.getTimeSync()

      if resultMsg is not None:
         raise Exception(resultMsg)

      json.dump(sync, response)
      response.status = httplib.OK
      response.content_type = 'application/json'

   def enableTimeSync(self):
      log.info('Calling syscfg.setTimeSync(True) to set the state of'\
               'host/guest time synchronization...')

      resultMsg = syscfg.setTimeSync(True)

      if resultMsg is not None:
         raise Exception(resultMsg)

      response.status = httplib.OK

   def disableTimeSync(self):
      log.info('Calling syscfg.setTimeSync(False) to set the state of'\
               'host/guest time synchronization...')

      resultMsg = syscfg.setTimeSync(False)

      if resultMsg is not None:
         raise Exception(resultMsg)

      response.status = httplib.OK

   def getInfo(self):
      if request.accept.best_match(['application/json']) is None:
         abort(httplib.NOT_ACCEPTABLE)

      log.info('Calling syscfg.getInfo() to get appliance info')

      json.dump(syscfg.getInfo(), response)
      response.status = httplib.OK
      response.content_type = 'application/json'

   def clearAdminPwd(self):
      log.info("Re-setting 'admin' password to empty...")

      util.ResetAdminPassword()
      response.status = httplib.OK
