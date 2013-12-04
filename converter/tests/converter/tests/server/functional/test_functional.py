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


#!/usr/bin/env python
# Test script for the runner. No individualized test cases yet.
# XXX: Probably does not work as afdeploy is now gone

import logging
import runner

from path import path

logging.basicConfig(level=logging.DEBUG)

inShare = runner.SMBShare('in', '/home/user/packages/input',
                          r'\\filehost.yourcorp.com\packages\input',
                          'user', 'user')

outShare = runner.SMBShare('out', '/home/user/packages/output',
                             r'\\filehost.yourcorp.com\packages\output',
                             'user', 'user')

r = runner.ConverterRunner()
# XXX: vmInfo -> None
job = runner.ConverterJob('TextPad', None, inShare, outShare)
job.SetInstallationCommand('txpeng540.exe /S /V/quiet')

def progressCallback(percent, detail, state):
   print 'Job %s - %d%% - %s' % (state, percent, detail)

result = r.RunJob(job, progressCallback)

assert result.files == [path('bin/TextPad.exe')]

# XXX: Maybe true anonymous shares as well?
