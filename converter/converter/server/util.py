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


import errno
import inject
import logging
import os.path
import weakref
import datetime

from path import path
from sqlalchemy import create_engine

log = logging.getLogger(__name__)

def FixPermissions(dirname):
   """
   Recurse across all directories and files in dirname (excluding
   dirname itself) and reset file and directory permissions to 644 and
   755, respecitvely.  Links are not followed.
   """

   def func(arg, dirname, fnames):
      if path(dirname).islink():
         log.debug('Not processing symlink directory: %s.', dirname)
         del fnames[:]
      else:
         for f in fnames:
            filePath = path(dirname) / f

            if filePath.islink():
               log.debug('Not processing symlink: %s.', filePath)
               continue
            elif filePath.isfile():
               filePath.chmod(0644)
            elif filePath.isdir():
               filePath.chmod(0755)
            else:
               log.warning('Unknown file type: %s.', filePath)
               continue

   os.path.walk(dirname, func, None)

def ScanProjectDirs(mountPath):
   if mountPath is None or mountPath == '':
      raise ValueError('Invalid mountPath - ' + mountPath)

   baselen = len(mountPath) + 1
   validDirs = []
   invalidDirsMap = {}
   p = path(mountPath)

   for i in p.walkfiles('Package.ini'):
      projDir = i.dirname()[baselen:]
      if len(projDir) > 0:
         missingFiles = CheckRequiredProjectFiles(i.dirname())
         if len(missingFiles) > 0:
            # invalid project
            # key -> value : projDir -> missingFiles
            invalidDirsMap[projDir] = missingFiles
         else:
            validDirs.append(projDir)

   result = { 'Valid_Dirs' : validDirs, 'Invalid_Dirs_Map' : invalidDirsMap }

   return result

# Check a list of files that are required to be present in the project folder.
def CheckRequiredProjectFiles(fullPath):
   filesToCheck = ('build.bat',
                     'HKEY_CURRENT_USER.txt',
                     'HKEY_LOCAL_MACHINE.txt',
                     'HKEY_USERS.txt')
   missingFiles = []
   for f in filesToCheck:
      filePath = path(fullPath) / f
      if not filePath.isfile():
         log.warn('%s not found in %s. Hence skipping the project from the import!', f, fullPath)
         missingFiles.append(f)

   return missingFiles

@inject.param('config')
def ResetAdminPassword(config):
   dbUrl = config['sqlalchemy.url']
   # Since the default url points to converter DB, need to switch to the appfactory DB
   # because Spring security tables are residing in that DB.
   dbUrl = dbUrl.replace('converter', 'appfactory')
   # Create a new DB connection to 'appfactory' DB.
   engine = create_engine(dbUrl)
   conn = engine.connect()
   # Set 'admin' password to empty
   txn = conn.begin()
   try:
      result = conn.execute("UPDATE users SET password='' WHERE username='admin'")
      txn.commit()
      if result > 0:
         log.info("Admin's password has been set to empty.")
   except:
      txn.rollback()
      raise
   finally:
      conn.close()
