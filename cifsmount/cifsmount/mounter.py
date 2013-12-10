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
import logging
import os
import re
import socket
import sys

from optparse import OptionParser
from path import path
from cifsmount import mount as cmount

MOUNT_ROOT = path('/mnt/cifs')

logging.basicConfig(level=logging.DEBUG)

log = logging.getLogger('cifsmount')

# From linux/magic.h (SMB_SUPER_MAGIC)
CIFS_FSTYPE = 0x517B

class Exit:
   ALREADY_MOUNTED = 64

# TODO:
# Note: Use exit codes 64-113 (user reserved)
# * Get lock to ensure only one instance running at a time?
#
# These are the errnos that are returned by mount:
#
# 6: Server exists, bad mount
# 13: bad cred, bad share
# 13: bad cred, good share
# 111: server up, no server listening
# 113: host down
# 5: can't lookup address

# Separates a UNC path into ('server', 'share', 'subdir1/subdir2/...')
# or ('server', 'share', None) if there is only a share.
#
# Note that the regex works by non-greedily matching up to / for the
# server then to (optionally) / for the share.  It may either stop
# there because it's the end of line ($) or it may continue to match
# more text because there are subdirectories.
UNC_RE = re.compile('//(.*?)/(.*?)(?:$|/(.*))')

# Ensure only alphanumeric values are accepted so that datastore names
# can't be things like '../foo' and be able to break out of MOUNT_ROOT
# and mount over other parts of the filesystem.
DATASTORE_RE = re.compile('^[\w-]+$')

# Set 'file_mode' and 'dir_mode' to enable group-level write access
FILE_MODE = '0660'

DIR_MODE = '0770'


# Need to validate UNC path for real somewhere else.
def SplitUnc(unc):
   return UNC_RE.match(unc).groups()

def _CallMount(datastore, domain, username, password, unc):
   log.debug('Got datastore name: %s.', datastore)
   log.debug('Got domain: %s.', domain)
   log.debug('Got username: %s.', username)
   log.debug('Got UNC path: %s.', unc)

   # TODO: Check to see if mounted.  What to do if it is?

   # Transform path to what Linux wants.
   unc = unc.replace('\\', '/')
   log.debug('Using UNC path: %s.', unc)
   target = MOUNT_ROOT / datastore
   stat = cmount.statfs(target)

   # Make sure it's not already mounted.  Could be a race if multiple
   # versions of this program are being run.
   if stat['f_type'] == CIFS_FSTYPE:
      sys.exit(Error.ALREADY_MOUNTED)

   try:
      target.makedirs()
   except OSError, e:
      if e.errno == errno.EEXIST:
         # TODO: Ensure nothing else is mounted.
         pass
      else:
         raise

   uid = int(os.environ['SUDO_UID'])
   gid = int(os.environ['SUDO_GID'])

   server, share, subdir = SplitUnc(unc)

   # We have to resolve the hostname from user-level (see mount.cifs).
   ip = socket.gethostbyname(server)

   for i in ('uid', 'gid', 'username', 'ip', 'domain'):
      val = locals()[i]

      if ',' in str(val):
         raise Exception("%s cannot contain ','." % i)

   # Passwords *can* have commas.  Escape them.
   password = password.replace(',', ',,')

   options = []

   if domain:
      options.append('dom=%s' % domain)

   # subdir might be either None (e.g. //server/share) or ''
   # (e.g. //server/share/).
   if subdir:
      options.append('prefixpath=%s' % subdir)

   options.extend([
      'user=%s' % username,
      'pass=%s' % password,
      'ip=%s' % ip,
      'uid=%d' % uid,
      'gid=%d' % gid,
      'file_mode=%s' % FILE_MODE,
      'dir_mode=%s' % DIR_MODE,
      # Use nounix so that we don't have to worry about symlinks.
      'nounix',
      'nosuid',
      'noexec',
      'iocharset=utf8',
   ])

   stropt = ','.join(options)

   # We have to only pass the server and share (without any other
   # subdirectories) and specify those subdirectories with preixpath
   # in options.
   uncMount = r'\\%s\%s' % (server, share)
   stroptStr = stropt.replace(password, '********')
   log.debug('Connecting to %s with options: %s.', uncMount, stroptStr)

   cmount.mount(uncMount, target, 'cifs', 0, stropt)

def mount():
   parser = OptionParser()
   parser.add_option('-d', '--datastore', dest='datastore', metavar='NAME')
   parser.add_option('-o', '--domain', dest='domain', metavar='DOMAIN')
   parser.add_option('-u', '--username', dest='username')
   # XXX: Don't pass in on command line.
   parser.add_option('-p', '--password', dest='password')
   parser.add_option('-s', '--unc', dest='unc')

   (options, args) = parser.parse_args()

   # OptionParser doesn't support required arguments due to gross
   # stupidity so hack it in.
   for i in ('datastore', 'username', 'password', 'unc'):
      if not getattr(options, i):
         sys.stderr.write('%s is required.\n\n' % i)
         parser.print_help()
         sys.exit(1)

   if not DATASTORE_RE.match(options.datastore):
      raise Exception('Invalid datastore.')

   _CallMount(options.datastore, options.domain, options.username, options.password, options.unc)

def umount():
   parser = OptionParser()
   parser.add_option('-d', '--datastore', dest='datastore', metavar='NAME')

   (options, args) = parser.parse_args()

   if not getattr(options, 'datastore'):
      sys.stderr.write('--datastore is required.\n\n')
      parser.print_help()
      sys.exit(1)

   if not DATASTORE_RE.match(options.datastore):
      raise Exception('Invalid datastore.')

   # TODO: Check if fs is mounted as cifs before unmounting?
   cmount.umount(MOUNT_ROOT / options.datastore)
