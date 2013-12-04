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


from converter.server import model, InflaterFactory, CancellationFlag
from converter.server.job import Job, NullInflater, ZipInflater
from converter.tests import stubs, DatabaseFixture
from converter.server.runner import ConverterResults
from converter import tests

from mock import Mock

def CreateStubbedJob(db, downloader=stubs.StubDownloader(),
                     runner=stubs.StubConverterRunnerSucceeds(), workpool=stubs.StubWorkPool(), createInflater=InflaterFactory()):
   job = Job(db, workpool, stubs.StubConversionRunner(runner, None, None, 'TextPad.exe'), createInflater=createInflater, cancelled=CancellationFlag())
   job._downloader = downloader
   return job

JOB_STATES = [
   'created',
   'queued',
   'downloading',
   'provisioning',
   'converting',
   'finished',
]

def CreateInflaterFactory():
   return InflaterFactory()

class TestJob(object):
   def setup(self):
      tests.Inject()

   def testDownloaderCalledDuringRun(self):
      """ Test that the downloader is proplery called when running """
      downloader = Mock()

      with DatabaseFixture() as f:
         job = CreateStubbedJob(f.db, downloader=downloader)

         job.Run()

         downloader.Acquire.assert_called_with()
         downloader.Release.assert_called_with()

   def testAllEventsFire(self):
      """ Test that events are fired for each action """
      with DatabaseFixture() as f:
         job = CreateStubbedJob(f.db)
         job._downloader = stubs.StubDownloader()

         receieved = []

         q = job.Subscribe()
         job.Run()

         for i in JOB_STATES:
            n = q.get()
            assert n == i
            receieved.append(n)

         assert receieved == JOB_STATES

   def testSuccessIsTrueWhenRunnerFails(self):
      """ Test that success is True when the runner succeeds """
      with DatabaseFixture() as f:
         job = CreateStubbedJob(f.db, runner=stubs.StubConverterRunnerSucceeds())

         j = f.session.query(model.Job).get(job.id)

         # Success is only set at the end of the job once a runner result
         # is known.
         assert j.success is None

         job.Run()

         f.session.refresh(j)

         assert j.success

   def testSuccessIsFalseWhenRunnerFails(self):
      """ Test that success is False when the runner fails """
      with DatabaseFixture() as f:
         job = CreateStubbedJob(f.db, runner=stubs.StubConverterRunnerFails())
         job.Run()

         j = f.session.query(model.Job).get(job.id)

         # Check by identity to ensure it's not None.
         assert j.success is False

   def testStatusGoesZeroToOneHundred(self):
      """ Test that job goes from 0 to 100 percent """
      with DatabaseFixture() as f:
         job = CreateStubbedJob(f.db)

         j = f.session.query(model.Job).get(job.id)
         assert j.percent == 0

         job.Run()

         f.session.refresh(j)
         assert j.percent == 100

   def testThatExceptionMarksJobAsFailed(self):
      """ Test that a job is properly marked as failed when an
      exception is raised in the WorkPool """
      with DatabaseFixture() as f:
         job = CreateStubbedJob(f.db, workpool=stubs.WorkPoolFailsOnAcquire())

         job.Run()

         j = f.session.query(model.Job).get(job.id)

         assert j.events[-1].name == 'finished'
         assert j.success is False

   def testCreateInflaterFromZip(self):
      """ Test that a zip inflater is given for a zip file """
      i = CreateInflaterFactory()

      assert isinstance(i.Create('/tmp/foo.zip'), ZipInflater)

   def testCreateInflaterFromUpperCaseZip(self):
      """ Test that a zip inflater is given for a zip file with an upper case zip extension """
      i = CreateInflaterFactory()

      assert isinstance(i.Create('/tmp/foo.ZIP'), ZipInflater)


   def testCreateNullInflaterFromExe(self):
      """ Test that a null inflater is given from an exe file """
      i = CreateInflaterFactory()

      assert isinstance(i.Create('/tmp/foo.exe'), NullInflater)

   def testParentPathIsReturnedFromNull(self):
      """ Test that the null inflater passes back the expected path """
      # If we give it a path like /tmp/job-5/foo.exe then the root job
      # path is /tmp/job-5.
      n = NullInflater('/tmp/jobs/job-5/setup.exe')
      assert n.Inflate() == '/tmp/jobs/job-5'

   def testInflateIsCorrectlyCalled(self):
      """ Test that inflate is correctly called during a job run """
      with DatabaseFixture() as f:
         inflaterFactory = Mock()
         inflater = Mock()

         job = CreateStubbedJob(f.db, runner=stubs.StubConverterRunnerSucceeds(), downloader=stubs.ZipStubDownloader(), createInflater=inflaterFactory)

         inflaterFactory.Create.return_value = inflater
         inflater.Inflate.return_value = '/tmp/inflated/job-n'

         job.Run()

         inflater.Inflate.assert_called_with()
