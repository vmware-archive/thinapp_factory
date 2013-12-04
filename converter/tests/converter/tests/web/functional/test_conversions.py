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

from routes import url_for

from converter import lib, server
from converter.server import model

from converter.tests import web, stubs, ClientFixture, DatabaseFixture, JobFixture

def GetEnviron(clientFixture):
   return {'converter.client': clientFixture.Create()}

class TestConverter(web.TestController):
   def testConverterFromCIFSInput(self):
      message = {
         'input': {
            'url': 'cifs://filehost.yourcorp.com/packages/input/TextPad',
            'login': 'user',
            'password': 'user'
         },
         'output': {
            'url': 'cifs://filehost.yourcorp.com/packages/output/TextPad',
            'login': 'user',
            'password': 'user'
          },
         'installation_command': '/S /V/quiet',
         'vm': {
            'href': 'http://localhost:9081/pools/win-xp-sp3'
         }
      }

      headers = { 'Content-Type': 'application/json',
                  'Accept': 'application/json' }

      with DatabaseFixture() as dbFixture:
         clientFixture = ClientFixture(dbFixture.db)

         response = self.app.post(url_for(controller='/conversions'), json.dumps(message), headers, extra_environ=GetEnviron(clientFixture))
         assert response.status_int == httplib.CREATED
         msg = response.json

         assert isinstance(msg['id'], int)

   def testWrongAcceptHeaders(self):
      with DatabaseFixture() as dbFixture:
         clientFixture = ClientFixture(dbFixture.db)

         headers = { 'Accept': 'bad/header' }
         response = self.app.post(url_for(controller='/conversions'), '', headers, extra_environ=GetEnviron(clientFixture), status=httplib.NOT_ACCEPTABLE)

   def testWrongContentType(self):
      with DatabaseFixture() as dbFixture:
         clientFixture = ClientFixture(dbFixture.db)

         headers = { 'Content-Type': 'bad/header',
                     'Accept': 'application/json' }
         response = self.app.post(url_for(controller='/conversions'), '', headers, extra_environ=GetEnviron(clientFixture), status=httplib.BAD_REQUEST)

   def testDispositionIsFailureOnFailure(self):
      with DatabaseFixture() as dbFixture:
         clientFixture = ClientFixture(dbFixture.db)
         jobFixture = JobFixture(dbFixture.db)

         job = jobFixture.CreateFailedJob()

         headers = { 'Content-Type': 'application/json',
                     'Accept': 'application/json' }
         response = self.app.get(url_for(controller='/conversions', action='show', id=job.id), None, headers, extra_environ=GetEnviron(clientFixture))

         assert response.json == {
            'id': job.id,
            'state': 'finished',
            'percent': 100,
            'result': {
               'disposition': 'failure',
               'files': []
               }
            }

   def testDispositionIsSuccessOnSuccess(self):
      with DatabaseFixture() as dbFixture:
         clientFixture = ClientFixture(dbFixture.db)
         jobFixture = JobFixture(dbFixture.db)

         job = jobFixture.CreateSucceededJob()

         headers = { 'Content-Type': 'application/json',
                     'Accept': 'application/json' }
         response = self.app.get(url_for(controller='/conversions', action='show', id=job.id), None, headers, extra_environ=GetEnviron(clientFixture))

         assert response.json == {
            'id': job.id,
            'state': 'finished',
            'percent': 100,
            'result': {
               'disposition': 'success',
               'files': [{'url': 'http://localhost/packages/job-%d/TestPackage.exe' % job.id, 'size': 1050, 'fileName': 'TestPackage.exe'}]
               }
            }
