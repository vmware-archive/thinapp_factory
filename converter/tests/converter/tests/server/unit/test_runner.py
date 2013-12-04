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


import os
import unittest
import pkg_resources

from converter.server import runner

fixtures = pkg_resources.resource_filename('converter.tests', 'server/fixtures')

inShare = runner.SMBShare(os.path.join(fixtures, 'testshare/AppInstaller'),
                          r'\\1.2.3.4\AppInstaller',
                          'thinapp', 'test')

outShare = runner.SMBShare(os.path.join(fixtures, 'testshare/AppDeploy'),
                           r'\\1.2.3.4\AppDeploy',
                           'thinapp', 'test')

# XXX: VMInfo / VCInfo gone as afdeploy is gone
vmInfo = None

class TestRunner(unittest.TestCase):
   def setUp(self):
      self.runner = runner.ConverterRunner(_nocheck=True)
      self.job = runner.ConverterJob('TestApp', vmInfo, inShare, outShare)
      self.job2 = runner.ConverterJob('TestApp2', vmInfo, inShare, outShare)

   def test_01_validators(self):
      """ Test runner configuration validators """
      assert self.job.ValidateInt('1')
      assert self.job.ValidateInt(1)
      assert not self.job.ValidateInt('foo1')

      assert self.job.ValidateBool('true')
      assert self.job.ValidateBool('false')
      assert not self.job.ValidateBool('bananas')

      assert self.job.ValidateIE('none')
      assert self.job.ValidateIE('virtualized')
      assert self.job.ValidateIE('system')
      assert not self.job.ValidateIE('bananas')

      assert self.job.ValidateCommand('TestApp.exe')
      assert self.job.ValidateFile('TestApp.exe')
      assert self.job.ValidateCommand('TestApp.exe /S')
      assert not self.job.ValidateFile('TestApp.exe /S')

      assert self.job2.ValidateFile('Test App.exe')
      assert self.job2.ValidateCommand('Test App.exe /S')
      assert self.job2.ValidateCommand('Test App.exe /S')
      assert not self.job2.ValidateFile('Test App.exe /S')

      assert not self.job.ValidateFile('bananas.exe')
      assert not self.job.ValidateCommand('bananas.exe')

   def test_02_setters_getters(self):
      """ Test runner configuration """
      self.assertRaises(runner.JobValidateError,
                        lambda: self.job.SetInstallationCommand('TestApp.exez'))

      self.assertRaises(runner.JobValidateError,
                        lambda: self.job.SetIESelection('bananas'))

      self.job.SetInstallationCommand('TestApp.exe')
      self.job.SetProjectPostProcessingCommand('post.bat')
      self.job.SetPackageIniOverrideFile('override.ini')
      self.job.SetIESelection('system')

      self.job.SetInstallerTimeout(3600)
      self.job.SetDetectIdle('true')
      self.job.SetBuildAfterCapture('false')

      assert self.job.GetInstallationCommand() == 'TestApp.exe'
      assert self.job.GetProjectPostProcessingCommand() == 'post.bat'
      assert self.job.GetPackageIniOverrideFile() == 'override.ini'
      assert self.job.GetIESelection() == 'system'

      assert self.job.GetInstallerTimeout() == 3600
      assert self.job.GetDetectIdle() == 'true'
      assert self.job.GetBuildAfterCapture() == 'false'

   # XXX: This could use a test to check we get proper unicode filenames back.
