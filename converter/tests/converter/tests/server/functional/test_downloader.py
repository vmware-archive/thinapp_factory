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
from converter.server import Downloader

from fixture import TempIO
from nose.plugins.skip import SkipTest

class TestDownloader(object):
   def testDownloadWithContentDispositionFileName(self):
      t = path(TempIO(True))
      d = Downloader(t, 'https://ninite.com/api/installer?app=Chrome&key=vmware')

      downloadPath = t / 'Ninite Chrome Installer.exe'
      res = d.Acquire()

      # Ensure that Acquire returns the path to the downloaded file.
      # Perhaps put this in its own test.
      assert res == downloadPath
      assert downloadPath in t.files()

      d.Release()
      assert not t.exists()

   def testDownloadWithoutContentDisposition(self):
      t = path(TempIO(True))
      d = Downloader(t, 'http://filehost.yourcorp.com/txpeng540.exe')
      d.Acquire()
      assert t / 'txpeng540.exe' in t.files()

      d.Release()
      assert not t.exists()

   def testWithProxy(self):
      raise SkipTest

   def testWithoutProxy(self):
      raise SkipTest
