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


from path import path

from converter.server.runner import ConverterRunner, ConverterResults

class StubWorkPoolRequest(object):
   def wait(self):
      pass

class StubWorkPool(object):
   # Note: these are just filler values that are invalid.
   def AcquireInstance(self, group):
      class StubAcquireRequest(StubWorkPoolRequest):
         def wait(self):
            # Dummied out; Python workpool removed
            vm = None
            return vm
      return StubAcquireRequest()

   def ReleaseInstance(self, group, vmInfo):
      return StubWorkPoolRequest()

class WorkPoolFailsOnAcquire(object):
   def AcquireInstance(self):
      raise Exception('Failed to acquire')

   def ReleaseInstance(self, vmInfo):
      pass

class StubConversionRunner(object):
   def __init__(self, runner, inputShare, outputShare, installationCommand):
      self._runner = runner

   def Run(self, vmInfo, appDir, installerName, progress):
      return self._runner.RunJob(vmInfo, None, progress)

class StubDownloader(object):
   def __init__(self, downloadRoot='/tmp/no/good/dir', url='http://localhost/file.exe'):
      self.downloadRoot = path(downloadRoot)

   def Acquire(self):
      return self.downloadRoot / 'test.exe'

   def Release(self):
      pass

class ZipStubDownloader(object):
   def __init__(self, downloadRoot='/tmp/no/good/dir', url='http://localhost/file.zip'):
      self.downloadRoot = path(downloadRoot)

   def Acquire(self):
      return self.downloadRoot / 'file.zip'

   def Release(self):
      pass

class StubConverterRunnerSucceeds(object):
   def __init__(self, *args, **kwargs):
      pass

   def RunJob(self, vmInfo, name, progress):
      progress(0, 'message', ConverterRunner.PROGRESS_STARTING)
      progress(10, 'message', ConverterRunner.PROGRESS_RUNNING)
      progress(100, 'message', ConverterRunner.PROGRESS_SUCCEEDED)

      return ConverterResults(files=[(u'bin/TextPad.exe', 1050)])

class StubConverterRunnerFails(object):
   def __init__(self, *args, **kwargs):
      pass

   def RunJob(self, vmInfo, name, progress):
      progress(0, 'message', ConverterRunner.PROGRESS_STARTING)
      progress(10, 'message', ConverterRunner.PROGRESS_RUNNING)
      progress(100, 'message', ConverterRunner.PROGRESS_FAILED)

      return ConverterResults(files=[])
