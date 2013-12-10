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

cdef extern from "errno.h":
   int errno

cdef extern from "sys/vfs.h":
   struct statfs_t "statfs":
      long f_type
      long f_bsize
      long f_blocks
      long f_bfree
      long f_bavail
      long f_files
      long f_ffree
      # We don't need f_fsid.
      # fsid_t f_fsid
      long f_namelen

   int statfs_c "statfs" (char *path, statfs_t *buf)

cdef extern from "sys/mount.h":
   int mount_c "mount" (char *source, char *target,
             char *filesystemtype, unsigned long mountflags,
             void *data)
   int umount_c "umount" (char *target)

import os

# Python's version of statfs (os.statvfs) doesn't give you the
# filesystem type.
def statfs(mount):
   cdef statfs_t buf
   statfs_c(mount, &buf)
   return buf

def mount(source, target, filesystem, mountflags, options):
   global errno

   # Note that options is actually supposed to be a void pointer that
   # is interpreted by the specific filesystem module but we'll just
   # assume it's a string list of options.
   res = mount_c(source, target, filesystem, mountflags, <char *>options)

   if res != 0:
      raise OSError(errno, os.strerror(errno))

def umount(target):
   global errno

   res = umount_c(target)

   if res != 0:
      raise OSError(errno, os.strerror(errno))
