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


from converter import lib, server
from converter.server import model

from converter.tests import stubs

import fixtures

class DatabaseFixture(fixtures.Fixture):
   def setUp(self):
      self.db = model.Database('sqlite:///:memory:')
      self.db.Create()
      self.session = self.db.CreateSession()

   def cleanUp(self):
      self.db.Destroy()

class ClientFixture(object):
   def __init__(self, db):
      self.db = db
      self.downloader = stubs.StubDownloader
      self.workpool = stubs.StubWorkPool()
      self.runner = stubs.StubConverterRunnerSucceeds

   def with_(self, **kwargs):
      for k, v in kwargs.iteritems():
         setattr(self, k, v)

      return self

   def Create(self):
      jobFactory = server.JobFactory(self.db, self.workpool, self.downloader, self.runner).Create
      self.server = server.Server(self.db, jobFactory)
      return lib.Client(self.server)

class JobFixture(object):
   def __init__(self, db):
      self.db = db
      self.session = db.CreateSession()

   def _createFinishedJob(self):
      job = model.Job()
      job.percent = 100
      job.events.append(model.Event('finished'))

      self.session.add(job)

      return job

   def CreateFailedJob(self):
      job = self._createFinishedJob()
      job.success = False

      self.session.commit()

      return job

   def CreateSucceededJob(self):
      job = self._createFinishedJob()
      job.success = True

      job.files.append(model.File('TestPackage.exe', 1050))

      self.session.commit()

      return job

def Inject():
   """ Setup default injections """
   import inject
   from pylons import config

   i = inject.Injector()
   # The examples show passing an instance to but it seems to want a
   # callable so fake one.
   i.bind('config', to=lambda: config)

   # XXX: Ideally this would come from the ini file.  I think you have
   # to use paste to load it.
   config['converter.job_log_dir'] = '/tmp/test-job-logs'

   inject.register(i)

class SameType(object):
   """ Used with mock library when you only want to verify a parameter
   was of a certain type.  Is there no way to do this just with the
   library. """
   def __init__(self, type):
      self.type = type

   def __eq__(self, obj):
      return isinstance(obj, self.type)
