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


from mock import Mock

from converter.server import JobFactory, InflaterFactory, CancellationFlag
from converter.tests import stubs, DatabaseFixture, SameType

class TestServer(object):
   """
   All of the factory classes are currently untested (Downloader,
   Runner, etc.).  It is tested that the factories are called as
   expected but not that the factories instantiate objects in the way
   we expect.
   """

   def testCreateJobFromMessage(self):
      """ Test that a job is created properly from a JSON message """
      # Trying to test that objects are correctly constructed but
      # don't really care about interactions yet.

      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe'
         },
         'installation_command': '/S /V/quiet',
      }

      createDownloader = Mock()
      createConversionRunner = Mock()

      with DatabaseFixture() as f:
         factory = JobFactory(f.db, stubs.StubWorkPool(), createDownloader,
                              createConversionRunner, createInflater=InflaterFactory(), ).Create
         job = factory(message)

         createDownloader.assert_called_with('job-%d' % job.id, message['input']['url'], SameType(CancellationFlag))
         createConversionRunner.assert_called_with(message['installation_command'], SameType(CancellationFlag))
