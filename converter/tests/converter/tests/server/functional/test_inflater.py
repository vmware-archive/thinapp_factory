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


import pkg_resources

from fixtures import TempDir
from path import path

from converter.server.job import ZipInflater

class TestInflater(object):
   def testZipInflaterCanUnzip(self):
      with TempDir() as d:
         z = ZipInflater(d.path, pkg_resources.resource_filename('converter.tests', 'server/fixtures/textpad.zip'))
         z.Inflate()

         p = path(d.path)

         assert p / 'txpeng540.exe' in p.files()
