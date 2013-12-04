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


import base64
import httplib
import eventlet
import json
import os

from eventlet.green import urllib2
from nose.plugins.skip import SkipTest
from urlobject import URLObject as URL

class Dispositions:
   SUCCEEDED = 'succeeded'
   FAILED = 'failed'

class Client(object):
   def __init__(self, url, port):
      self.url = URL.parse(url).with_port(port)

   def CreateJob(self, message):
      headers = { 'Content-Type': 'application/json',
                  'Accept': 'application/json' }

      url = self.url / 'conversions'
      request = urllib2.Request(url, json.dumps(message), headers)
      response = urllib2.urlopen(request)

      # Job should have been created.
      assert response.code == httplib.CREATED

      msg = json.load(response)
      return Job(msg['id'], self.url)

class Job(object):
   def __init__(self, id_, url):
      self.id = id_
      self.url = url

   def Get(self):
      url = self.url / 'conversions' / str(self.id)
      return _getJson(url)

   def Cancel(self):
      url = self.url / 'conversions' / str(self.id) / 'cancel'
      request = urllib2.Request(url, data='')
      response = urllib2.urlopen(request)

   def GetProject(self):
      status = self.Get()
      project = Project(status['project_id'], self.url)
      return project

class Project(object):
   def __init__(self, id_, url):
      self.id = id_
      self.url = url

   def Get(self):
      url = self.url / 'projects' / str(self.id)
      return _getJson(url)

   def GetPackageIni(self):
      url = self.url / 'projects' / str(self.id) / 'packageini'
      return _getJson(url)

   def PutPackageIni(self, data):
      url = self.url / 'projects' / str(self.id) / 'packageini'
      _sendJson(url, data, 'PUT', noResponse=True)

   def GetDirectory(self, id=None):
      url = self.url / 'projects' / str(self.id) / 'directory'

      if id:
         url /= str(id)

      return _getJson(url)

   def CreateDirectory(self, data):
      url = self.url / 'projects' / str(self.id) / 'directory' / 'new'
      return _sendJson(url, data, 'POST')

   def PutDirectory(self, dirId, data):
      url = self.url / 'projects' / str(self.id) / 'directory' / str(dirId)
      return _sendJson(url, data, 'PUT')

   def DeleteDirectory(self, dirId):
      url = self.url / 'projects' / str(self.id) / 'directory' / str(dirId)
      return _httpDelete(url)

   def GetRegistry(self, id=None):
      url = self.url / 'projects' / str(self.id) / 'registry'

      if id:
         url /= str(id)

      return _getJson(url)

   def CreateRegistry(self, data):
      url = self.url / 'projects' / str(self.id) / 'registry' / 'new'
      return _sendJson(url, data)

   def PutRegistry(self, data, keyId):
      url = self.url / 'projects' / str(self.id) / 'registry' / str(keyId)
      _sendJson(url, data, 'PUT', noResponse=True)

   def DeleteRegistry(self, keyId):
      url = self.url / 'projects' / str(self.id) / 'registry' / str(keyId)
      return _httpDelete(url)

   def GetFile(self, fileId):
      url = self.url / 'projects' / str(self.id) / 'file' / str(fileId)
      return _getData(url)

   def CreateFile(self, dirId, data):
      url = self.url / 'projects' / str(self.id) / 'directory' / str(dirId) / 'new_file'
      return _sendJson(url, data, 'POST')

   def PutFile(self, fileId, data):
      url = self.url / 'projects' / str(self.id) / 'file' / str(fileId)
      return _sendData(url, data, 'PUT')

   def DeleteFile(self, fileId):
      url = self.url / 'projects' / str(self.id) / 'file' / str(fileId)
      return _httpDelete(url)

   def Delete(self):
      url = self.url / 'projects' / str(self.id) / 'delete'

      request = urllib2.Request(url, data='')
      response = urllib2.urlopen(request)

   def Rebuild(self):
      url = self.url / 'projects' / str(self.id) / 'rebuild'

      request = urllib2.Request(url, data='')
      response = urllib2.urlopen(request)

def _getData(url):
   request = urllib2.Request(url)
   response = urllib2.urlopen(request)

   return response.read()

def _sendData(url, data, method='POST', mime='application/octet-stream', noResponse=False):
   headers = { 'Content-Type': mime }
   request = urllib2.Request(url, data, headers)
   request.get_method = lambda: method

   response = urllib2.urlopen(request)

   if not noResponse:
      return response.read()

def _getJson(url):
   headers = { 'Accept': 'application/json' }
   request = urllib2.Request(url, None, headers)
   response = urllib2.urlopen(request)

   return json.load(response)

def _sendJson(url, data, method='POST', noResponse=False):
   headers = { 'Content-Type': 'application/json',
               'Accept': 'application/json' }

   request = urllib2.Request(url, json.dumps(data), headers)
   request.get_method = lambda: method
   response = urllib2.urlopen(request)

   if not noResponse:
      return json.load(response)

def _httpDelete(url):
   request = urllib2.Request(url)
   request.get_method = lambda: 'DELETE'
   response = urllib2.urlopen(request)

   return response.code

def _getIdFromUrl(url):
   # Just get the last path component of the URL.
   return int(url.split('/')[-1])

def _expectHttpFail(why, func, *args, **kwargs):
   success = True

   try:
      func(*args, **kwargs)
   except urllib2.HTTPError, e:
      success = False

   assert not success, why

def _waitUntilState(obj, *validStates):
   # TODO: Add timeout!

   while True:
      msg = obj.Get()

      if msg['state'] in validStates:
         break
      else:
         eventlet.sleep(5)

   return msg

def _waitUntilDone(job):
   return _waitUntilState(job, 'finished')

def _verifyFiles(job, numFiles=1):
   # Download converted package from URL.
   # Exclude cmd.exe from counting towards number of packages.
   project = job.GetProject().Get()

   files = filter(lambda f: f['fileName'] != 'cmd.exe', project['files'])

   assert len(files) == numFiles

   for pkg in files:
      r = urllib2.Request(pkg['url'])
      r.get_method = lambda: 'HEAD'

      response = urllib2.urlopen(r)

      # Ensure size matches
      assert int(response.headers['content-length']) == pkg['size']

      # TODO: Ensure filename matches.

class TestConverter:
   # TODO: Test relative paths to make sure they're rejected.
   # TODO: Need to test running a job with a datastore authenticated
   # with domain user.

   def setup(self):
      self.c = Client('http://localhost', 5000)


   def testConversionShouldSucceedFromLocalDatastore(self):
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)

      assert status['result']['disposition'] == Dispositions.SUCCEEDED, 'Conversion failed.'

      _verifyFiles(job)

   def testConversionFromCommandLineWithUnicodeFilename(self):
      """ Test that a conversion works if passed a command line instead of an exe with a unicode filename """
      message = {
         'input': {
            'url': u'http://filehost.yourcorp.com/بيةzip.msi'
         },
         'output': {
            'url': 'datastore://internal/testing'
          },
         'installation_command': 'msiexec /q /i %D INSTALLDIR="C:\\Program Files\\7-Zip"'
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)

      assert status['result']['disposition'] == Dispositions.SUCCEEDED, 'Conversion failed.'

      _verifyFiles(job, 2)

   def testExit16Path(self):
      """ A generic conversion request that should succeed """
      message = {
         'input': {
            'url': 'datastore://internal/exit16/Mozilla/Firefox/4.0/en/R1',
         },
         'output': {
            'url': 'datastore://internal',
          },
         'installation_command': 'msiexec /qb /i firefox.msi',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)

      assert status['result']['disposition'] == Dispositions.SUCCEEDED, 'Conversion failed.'

      _verifyFiles(job)

   def testConversionInputFromDatastore(self):
      """ A generic conversion request that should succeed """
      message = {
         'input': {
            'url': 'datastore://internal/testing/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/from/one/to/another',
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)

      assert status['result']['disposition'] == Dispositions.SUCCEEDED, 'Conversion failed.'

      _verifyFiles(job)

   def testConversionShouldFail(self):
      """ A generic conversion request that should fail """

      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/badInstaller.bat',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': None
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)

      assert status['result']['disposition'] == Dispositions.FAILED

   def testConversionWithUnicodeFilename(self):
      """ Test a conversion that results in a package with a unicode name """

      message = {
         'input': {
            'url': u'https://ninite.com/api/installer?app=uTorrent&key=vmware',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /nocache /silent',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)

      assert status['result']['disposition'] == Dispositions.SUCCEEDED, 'Conversion failed.'

      _verifyFiles(job)

   def testConversionFromZipFileFromURL(self):
      """ Test creating a package from a zip file """
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/textpad.zip',
         },
         'output': {
            'url': 'datastore://internal/output'
          }
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)
      _verifyFiles(job, 1)

   def testConversionFromZipFileFromCIFS(self):
      """ Test creating a package from a CIFS input share """
      message = {
         'input': {
            'url': 'datastore://internal/testing/textpad.zip',
         },
         'output': {
            'url': 'datastore://internal/output'
          }
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)
      _verifyFiles(job, 1)

   def testConversionWithOverride(self):
      """ Test creating a package with override to build the MSI """
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/textpad-with-package-options.zip',
         },
         'output': {
            'url': 'datastore://internal/output'
          }
      }

      job = self.c.CreateJob(message)
      status = _waitUntilDone(job)
      _verifyFiles(job, 2)

   def testCancelJob(self):
      """ Test that a running job can be cancelled """
      # XXX: What happens if job is not running?
      #
      # XXX: In practice this only tests what happens when cancelling
      # during the packager phase (which is at least the more
      # complicated phase) but download and provisioning are untested.
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilState(job, 'converting')

      job.Cancel()

      _waitUntilState(job, 'cancelling')

      # XXX: This is all full of race conditions but we have no proper
      # event or notification sy stem yet

      status = _waitUntilState(job, 'cancelled')

      assert 'result' not in status

   def testDeleteProject(self):
      """ Test deleting a project from storage """
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilState(job, 'finished')

      project = job.GetProject()
      project.Delete()

      _waitUntilState(project, 'deleted')

   def testProjectDataImport(self):
      """
         Test importing registry and filesystem hierarchies into database
         and reading the values back out
      """
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilState(job, 'finished')

      project = job.GetProject()
      registry = project.GetRegistry()

      def _checkValue(path, value, typ, data, dExpand, nExpand):
         curKey = registry
         fullPath = '\\'.join(path)

         for p in path:
            assert p in curKey['subkeys'], \
                   'Expected registry key %s missing' % fullPath
            curKey = _getJson(curKey['subkeys'][p]['url'])

         assert value in curKey['values'], \
                'Expected registry value %s missing from key %s' % \
                (value, fullPath)

         compareData = curKey['values'][value]['data']
         compareType = curKey['values'][value]['type']
         compareDataExpand = curKey['values'][value]['dataExpand']
         compareNameExpand = curKey['values'][value]['nameExpand']

         assert typ == compareType, \
                'Expected type "%s" for key %s, value %s, got "%s" instead' % \
                (typ, fullPath, value, compareType)

         assert data == compareData, \
                'Expected data "%s" for key %s, value %s, got "%s" instead' % \
                (data, fullPath, value, compareData)

         assert dExpand == compareDataExpand, \
                'Expected dataExpand "%s" for key %s, value %s, got "%s" instead' % \
                (dExpand, fullPath, value, compareDataExpand)

         assert nExpand == compareNameExpand, \
                'Expected nameExpand "%s" for key %s, value %s, got "%s" instead' % \
                (nExpand, fullPath, value, compareNameExpand)

      # Test a REG_SZ value (double expansion)
      path  = ('HKEY_LOCAL_MACHINE', 'SOFTWARE', 'Helios', 'TextPad', '5.4.0')
      typ   = 'REG_SZ'
      value = 'InstallPath'
      data  = '%ProgramFilesDir%\\TextPad 5\\'
      dExpand = True
      nExpand = False

      _checkValue(path, value, typ, data, dExpand, nExpand)

      # Test a REG_DWORD value
      path    = ('HKEY_LOCAL_MACHINE', 'SOFTWARE', 'Microsoft', 'Windows',
                 'CurrentVersion', 'Uninstall',
                 '{B6EC7388-E277-4A5B-8C8F-71067A41BA64}')
      typ     = 'REG_DWORD'
      value   = 'EstimatedSize'
      data    = 5400
      dExpand = False
      nExpand = False

      _checkValue(path, value, typ, data, dExpand, nExpand)

      directory = project.GetDirectory()

      for section in ('files', 'directories', 'path', 'attributes'):
         assert 'directories' in directory, 'Malformed JSON output'

      assert '%AppData%' in directory['directories'], \
             'Missing directory in JSON output'
      subdir = _getJson(directory['directories']['%AppData%'])

      expectedAttributes = {
         'Isolation': {
            'DirectoryIsolationMode': 'WriteCopy',
         }
      }

      assert subdir['attributes'] == expectedAttributes, \
             'Unexpected attribute settings in JSON output: %s' % \
             repr(subdir['attributes'])

      # Test Package.ini access
      packageIni = project.GetPackageIni()

      expectedSections = (
         'ARPPRODUCTICON.exe',
         'BuildOptions',
         'Compression',
         'DDEOPN32.exe',
         'Isolation',
         'NewShortcut1.exe',
         'Runjava.exe',
         'TextPad (1).exe',
         'TextPad 5.dat',
         'TextPad.exe',
         'URIFileOpen.exe',
         'cmd.exe',
         'plumb.exe',
         'regedit.exe',
      )

      assert len(packageIni.keys()) == len(expectedSections), \
             'Generated Package.ini only has %d sections' % \
             len(packageIni.keys())

      for section in expectedSections:
         assert section in packageIni.keys(), \
                'Missing Package.ini section %s' % section

      # XXX: More comprehensive testing later.

   def testRegistryCreateUpdateDelete(self):
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilState(job, 'finished')

      project = job.GetProject()
      registry = project.GetRegistry()

      # Create a new key under HKLM
      assert 'HKEY_LOCAL_MACHINE' in registry['subkeys']
      hklm = _getJson(registry['subkeys']['HKEY_LOCAL_MACHINE']['url'])

      message = {
         'parentId': hklm['id'],
         'key': {
            'id': None,
            'path': 'HKEY_LOCAL_MACHINE\\TestKey',
            'isolation': 'sb_only',
            'subkeys': {},
            'values': {
               '': {
                  'type': 'REG_SZ',
                  'data': 'Test',
                  'nameExpand': False,
                  'dataExpand': True,
               },
               'Foobar': {
                  'type': 'REG_BINARY',
                  'data': [1, 2, 3, 4, 5],
                  'nameExpand': False,
                  'dataExpand': False,
               },
               'Baz': {
                  'type': 'REG_MULTI_SZ',
                  'data': ['A', 'B', 'C'],
                  'nameExpand': False,
                  'dataExpand': False,
               },
               'Quux': {
                  'type': 'REG_DWORD',
                  'data': 42,
                  'nameExpand': False,
                  'dataExpand': False,
               },
            }
         }
      }

      reply = project.CreateRegistry(message)
      newKey = _getJson(reply['url'])

      for section in ('path', 'isolation', 'subkeys', 'values'):
         assert newKey[section] == message['key'][section], \
                '%s section mismatch: %s <-> %s' % \
                (section, newKey[section], message['key'][section])

      # Modify a value, remove a value, add a new one, and keep one
      message['key']['values']['']['data'] = 'Test2'
      del message['key']['values']['Foobar']
      message['key']['values']['Josh'] = {
         'type': 'REG_EXPAND_SZ',
         'data': 'X',
         'nameExpand': True,
         'dataExpand': False,
      }

      # Set the ID to the real ID
      message['key']['id'] = newKey['id']

      project.PutRegistry(message['key'], newKey['id'])
      modKey = _getJson(reply['url']) # same URL as before

      for section in ('path', 'isolation', 'subkeys', 'values'):
         assert modKey[section] == message['key'][section], \
                '%s section mismatch: %s <-> %s' % \
                (section, modKey[section], message['key'][section])

      # Delete the key entirely
      project.DeleteRegistry(newKey['id'])

      # Check that it no longer exists
      success = False
      try:
         delKey = _getJson(reply['url'])
      except urllib2.HTTPError, e:
         success = True

      assert success, 'Key still existed at %s after deletion' % reply['url']

      # Check that its parent no longer reports that it exists
      hklm = _getJson(registry['subkeys']['HKEY_LOCAL_MACHINE']['url'])
      assert 'TestKey' not in hklm['subkeys']

   def testRebuild(self):
      """
         Break a project by messing up its Package.ini. Try and rebuild it.
         Watch it fail. Unbreak it and rebuild it and it should go back to
         state 'available'. Also, make a change to the registry and verify
         with the low-level registry parser that the change reflected in
         the vregtool txt file.
      """
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilState(job, 'finished')

      # Set up a rebuild that will fail
      project = job.GetProject()
      ini = project.GetPackageIni()
      save = ini['TextPad.exe']['Source']
      ini['TextPad.exe']['Source'] = 'does-not-exist'

      project.PutPackageIni(ini)

      projJson = project.Get()
      assert projJson['state'] == 'dirty'

      project.Rebuild()

      projJson = project.Get()
      assert projJson['state'] == 'rebuilding'

      # Did it fail?
      _waitUntilState(project, 'available', 'dirty')
      projJson = project.Get()
      assert projJson['state'] == 'dirty'

      # Fix the failing change
      ini['TextPad.exe']['Source'] = save
      project.PutPackageIni(ini)

      # Also make a change to the registry through the API
      regPath = ('HKEY_LOCAL_MACHINE', 'SOFTWARE', 'Helios', 'TextPad')

      # TODO: could be a useful standalone method later.
      def traverse(reg, path):
         for part in regPath:
            assert part in reg['subkeys']
            reg = _getJson(reg['subkeys'][part]['url'])
         return reg

      message = traverse(project.GetRegistry(), regPath)
      assert 'CurrentVersion' in message['values']
      message['values']['CurrentVersion']['data'] = '1.2.3'
      project.PutRegistry(message, message['id'])

      # This rebuild should succeed
      project.Rebuild()

      _waitUntilState(project, 'available', 'dirty')
      projJson = project.Get()
      assert projJson['state'] == 'available'

      message = project.GetRegistry(message['id'])
      assert message['values']['CurrentVersion']['data'] == '1.2.3'

   def testDirectoryCreateUpdateDelete(self):
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilState(job, 'finished')

      project = job.GetProject()

      rootDir = project.GetDirectory()
      rootId = rootDir['id']

      # Create a directory with attributes.
      attributes = {
         'Isolation': {
            'DirectoryIsolationMode': 'WriteCopy',
         }
      }

      # Attempt to perform directory traversal with .., also try to create
      # directory with obfuscated path (too many slashes, etc.)
      directory = {
         'id': None,
         'path': '%ProgramFilesDir%/..//..',
         'files': {},
         'directories': {},
         'attributes': attributes,
      }

      _expectHttpFail('Create directory with obfuscated path should fail',
                      project.CreateDirectory, directory)

      # Create normally with attributes
      directory['path'] = '%ProgramFilesDir%/Test Directory'

      response = project.CreateDirectory(directory)
      newId = _getIdFromUrl(response['url'])

      # Verify the attributes match when read back out.
      newDir = project.GetDirectory(newId)

      assert newDir['attributes'] == attributes, \
             'Remote attributes do not match source attributes'

      # Put new attributes into the directory and ensure attributes match.
      attributes['Isolation']['DirectoryIsolationMode'] = 'Merged'

      project.PutDirectory(newId, directory)

      # Verify the attributes match when read back out.
      newDir = project.GetDirectory(newId)

      assert newDir['attributes'] == attributes, \
             'Remote attributes do not match source attributes'

      # Create the directory again, it should fail.
      _expectHttpFail('Create directory with obfuscated path should fail',
                      project.CreateDirectory, directory)

      # Create a file in the directory, then attempt to delete the directory
      # while a file still exists inside.
      data = os.urandom(256)
      dummyFile = {
         'name': 'dont-delete-me.txt',
         'data': base64.b64encode(data),
      }

      response = project.CreateFile(newId, dummyFile)
      fileId = _getIdFromUrl(response['url'])

      _expectHttpFail('Deleting nonempty directory should fail',
                      project.DeleteDirectory, newId)

      # Create another directory inside that directory as well, try the same.
      subDir = directory.copy()
      subDir['path'] = '%ProgramFilesDir%/Test Directory/Test Subdirectory'

      response = project.CreateDirectory(subDir)
      subId = int(response['url'].split('/')[-1])

      _expectHttpFail('Deleting nonempty directory should fail',
                      project.DeleteDirectory, newId)

      # Delete the subdir and the original file. Now the top level directory
      # should be deleteable.
      project.DeleteDirectory(subId)
      project.DeleteFile(fileId)
      project.DeleteDirectory(newId)

   def testFileCreateUpdateDelete(self):
      message = {
         'input': {
            'url': 'http://filehost.yourcorp.com/txpeng540.exe',
         },
         'output': {
            'url': 'datastore://internal/output'
          },
         'installation_command': '%D /S /V/quiet',
      }

      job = self.c.CreateJob(message)
      status = _waitUntilState(job, 'finished')

      project = job.GetProject()

      # Attempt to create/update a number of protected files.
      directory = project.GetDirectory()
      rootId = directory['id']

      # Get ID of Program Files directory
      progFilesId = int(directory['directories']['%ProgramFilesDir%'].split('/')[-1])

      data = os.urandom(256)
      dummyFile = {
         'name': '##Attributes.ini',
         'data': base64.b64encode(data),
      }

      # XXX: expect a specific code, like FORBIDDEN. The problem is that
      # we achieve this in the server by raising exceptions inside
      # OpenProjectFile.
      _expectHttpFail('Should not be able to create ##Attributes.ini',
                      project.CreateFile, progFilesId, dummyFile)

      for badFile in ('Package.ini', 'HKEY_USERS.txt', 'bin', 'build.bat'):
         dummyFile['name'] = badFile
         _expectHttpFail('Should not be able to create restricted file %s' % \
                         badFile,
                         project.CreateFile, rootId, dummyFile)

      # Create a normal file, and then create the same file again with
      # a different casing.
      dummyFile['name'] = 'Test.dat'
      response = project.CreateFile(progFilesId, dummyFile)
      newId = _getIdFromUrl(response['url'])

      dummyFile['name'] = 'test.dat'
      _expectHttpFail('Creating Test.dat and test.dat should fail',
                      project.CreateFile, progFilesId, dummyFile)

      # Create a file then fetch it and assert that the data contained is
      # unchanged.
      assert project.GetFile(newId) == data, \
             'Data from remote file does not match source (POST)'

      # Update a file and verify the project is dirtied and that the data
      # actually got updated.
      newData = os.urandom(256)
      project.PutFile(newId, newData)

      assert project.GetFile(newId) == newData, \
             'Data from remote file does not match source (PUT)'

      # Delete a file and verify it no longer shows up in the listing.
      project.DeleteFile(newId)

      listing = project.GetDirectory(progFilesId)
      assert 'Test.dat' not in listing['files'], \
             'Deleted file appears in directory listing'
