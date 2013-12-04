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


import collections
import inject
import logging
import re
import sqlalchemy
import subprocess
import sys

from path import path
from sqlalchemy.orm import exc as ormexc

from converter.server import model

from cifsmount import mount, mounter #@UnresolvedImport

log = logging.getLogger(__name__)

# Datastore for the file share within the appliance.
INTERNAL = 'internal'
# Data where database, etc. resides (/home/user)
SYSTEM = 'system'
# XXX: Need to see if there are any concurrency issues.

class DatastoreException(Exception):
   """ Base exception for all datastore errors """

class DatastoreExistsError(DatastoreException):
   def __init__(self, name):
      self.name = name

   def __str__(self):
      return 'The datastore %s already exists.' % self.name

class DataStoreLease(object):
   # Gives the local path and smb details.
   def __init__(self, id, datastoreName, share):
      self.id = id
      self.datastoreName = datastoreName
      self.share = share

class DataStoreInUseError(DatastoreException):
   def __init__(self, name, inUse):
      self.name = name
      self.inUse = inUse

   def __str__(self):
      return '%s is in use with %d leases.' % (self.name, self.inUse)

class InvalidDataStoreError(DatastoreException):
   pass

class MountError(DatastoreException):
   def __init__(self, msg, code):
      self.msg = msg
      self.code = code

class DataStoreNotFoundError(DatastoreException):
   def __init__(self, name):
      self.name = name

   def __str__(self):
      return 'No such datastore %s.' % self.name

class LeaseDoesNotExistError(DatastoreException):
   def __init__(self, lease):
      self.lease = lease

   def __str__(self):
      return 'The specified lease %s does not exist.' % self.lease

class DataStoreOfflineError(DatastoreException):
   pass

@inject.appscope
class DataStores(object):
   # What to do with leases on startup?  Kill?
   # Should we check for any mounts on startup and try to unmount??
   @inject.param('db')
   @inject.param('config')
   def __init__(self, db, config):
      # name: reference count
      self.leases = collections.defaultdict(lambda: collections.defaultdict(lambda: 0))
      self.db = db
      self.config = config

      self.leaseNumber = 0

      # Reset all datastores to known state.
      log.info('Reseting all datastores to offline state.')

      sess = self.db.CreateSession()

      # We need the autosession out here so that the inner AutoSession in
      # GetDataStoreList doesn't expire the objects we want to use.
      with model.AutoSession(sess):
         for ds in self.GetDataStoreList(sess):
            try:
               self.GoOffline(ds)
            except MountError, e:
               log.debug('%s was probably already offline.', ds)

   def GetDataStoreList(self, sess=None):
      if not sess:
         sess = self.db.CreateSession()

      with model.AutoSession(sess):
         dsList = [n[0] for n in sess.execute(sqlalchemy.select([model.DataStore.id]))]

      return dsList

   def VerifyLeases(self, id):
      num = self.GetLeases(id)

      if num > 0:
         raise DataStoreInUseError(id, num)

   def GetStatus(self, id):
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         ds = self.GetDatastore(sess, id)
         capacity = None
         used = None

         if ds.status == 'online':
            stat = mount.statfs(ds.localPath)
            blockSize = stat['f_bsize']

            # There is also f_bfree which gives the total amount of free
            # space.  f_bavail is the number of blocks available to an
            # unprivileged user (which we are).
            available = stat['f_bavail'] * blockSize
            capacity = stat['f_blocks'] * blockSize
            used = capacity - available

         # Get file used, total, etc.
         return {
            'id' : id,
            'name': ds.name,
            'type': 'cifs',
            'server': ds.server,
            'share': ds.share,
            'size': capacity,
            'used': used,
            'status': ds.status,
            'domain': ds.domain,
            'username': ds.username,
            'password': ds.password,
            'mountAtBoot': True,
            'leases': self.GetLeases(id),
            'mountPath' : ds.localPath,
            'baseUrl': ds.baseUrl,
         }

   def _changeState(self, id, state, sess):
      log.debug('Request received for %s to go %s.', id, state)

      ds = self.GetDatastore(sess, id)

      # XXX: Session use could be more fine grained here, but we do need to
      # close the session if anything happens.
      with model.AutoSession(sess):
         # Return if already online.
         if ds.status == state:
            log.info('%s[%d] is already %s.', ds.name, ds.id, state)
            return

         if ds.name in (SYSTEM, INTERNAL):
            ds.status = state
            log.info('No mounting/unmounting is required for the %s datastore', ds.name)
            return

         share = ds.GetAsShare()
         dsId = str(ds.id);
         # XXX: Can't figure out how to look up
         # where scripts were installed to (not sure if it's even
         # possible) so use the default directory.
         #
         # An absolute path is required even though cifsmount is in
         # our PATH to do sudo being built with SECURE_PATH.
         bin = path(sys.exec_prefix) / 'bin'

         if state == 'online':
            ds.localPath = mounter.MOUNT_ROOT / dsId

            cmd = bin / 'cifsmount'

            # The username may have the domain in it in either form
            # user\domain or user/domain.  The two values have to be
            # separated out when calling the mounter.
            username = share.username.replace('\\', '/')
            domainUser = username.split('/')

            if len(domainUser) == 2:
               domain, username = domainUser
            else:
               domain, username = '', share.username

            res = subprocess.Popen(['sudo', cmd,
                                    '--datastore', dsId,
                                    '--domain', domain,
                                    '--username', username,
                                    '--password', share.password,
                                    '--unc', share.uncPath],
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
            stdout, stderr = res.communicate()
            for s in (stderr, stdout):
               if s.strip():
                  log.warning('Output from cifsmount:\n')
                  log.warning(s)
                  log.warning('Output done from cifsmount.\n')
         else:
            ds.localPath = None

            cmd = bin / 'cifsumount'
            res = subprocess.Popen(['sudo', cmd,
                                    '--datastore', dsId], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = res.communicate()
            for s in (stderr, stdout):
               if s.strip():
                  log.warning('Output from cifsmount:\n')
                  log.warning(s)
                  log.warning('Output done from cifsmount.\n')

         code = res.wait()

         if code != 0:
            if state == 'offline':
               log.info('Failed to unmount datastore %s with code: %d. '
                        'Assuming already offline.',
                        ds.name, code)
            else:
               log.error('%s was unable to go %s with code: %d', ds.name, state, code)
               raise MountError('Mounter exited non-zero: %d.' % code, code)

         ds.status = state

         log.info('%s has gone %s.', ds.name, state)

   def GoOnline(self, id):
      sess = self.db.CreateSession()
      return self._changeState(id, 'online', sess)

   def GoOffline(self, id):
      self.VerifyLeases(id)
      sess = self.db.CreateSession()
      return self._changeState(id, 'offline', sess)

   def GetLeases(self, id):
      # Returns number of leases held.
      return len(self.leases[id])

   def CreateDatastore(self, share, errorOnExists=True):
      sess = self.db.CreateSession()
      ds = model.DataStore.FromShare(share)
      sess.add(ds)
      sess.commit()

      # XXX: Log full datastore but mask password
      log.debug('Created new datastore %s[id=%d]', ds.name, ds.id)
      # Update baseUrl with DB generated id if not internal/system.
      if ds.name not in (SYSTEM, INTERNAL):
         ds.baseUrl = self.config['storage.base_url'] + str(ds.id)
         sess.merge(ds)
         sess.commit()

      log.debug('New datastore baseUrl = %s', ds.baseUrl)

      return ds.id;

   def UpdateDatastore(self, id, share, errorOnExists=True):
      sess = self.db.CreateSession()

      try:
         with model.AutoSession(sess):
            ds = self.GetDatastore(sess, id)
            log.info("Updating existing datastore [name:%s,id:%d]", ds.name, ds.id)

            # Ensure 'system' and 'internal' names remain unchanged.
            if ds.name in (SYSTEM, INTERNAL) and ds.name != share.name:
               raise DatastoreException('Changing the %s datastore name is not allowed.', ds.name);

            if ds.status != 'offline':
               # XXX: Use a better exception.
               raise DataStoreInUseError(ds.name % " is not in the offline mode yet!")

            if errorOnExists:
               raise DatastoreExistsError(id)

            # Just update the info in case it changed.
            with model.AutoSession(sess):
               updated = model.DataStore.FromShare(share)
               updated.id = ds.id
               sess.merge(updated)
      except DataStoreNotFoundError:
         log.debug('The %s[id=%d] datastore was not found', share.name, id)
         raise DataStoreNotFoundError('The %s datastore was not found.', share.name);

   @staticmethod
   def GetDatastore(sess, id):
      ds = sess.query(model.DataStore).filter_by(id=id).first()
      if ds is None:
         raise DataStoreNotFoundError('Invalid datastore id [%d]', id)
      else:
         return ds

   @staticmethod
   def GetDatastoreByName(sess, name):
      try:
         ds = sess.query(model.DataStore).filter_by(name=name).one()
      except ormexc.NoResultFound:
         # raised by .one()
         raise DataStoreNotFoundError(name)

      if ds is None:
         raise DataStoreNotFoundError('The %s datastore was not found!', name)
      else:
         return ds

   def DeleteDatastore(self, id):
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         ds = self.GetDatastore(sess, id)
         if ds.name in (SYSTEM, INTERNAL):
            raise DatastoreException('Deleting the %s datastore is prohibited!', ds.name)

         self.VerifyLeases(id)
         self.GoOffline(id)
         sess.delete(ds)

   def BringDefaultDatastoresOnline(self):
      for id in self.GetDataStoreList():
         log.info('Getting status for DS %d', id)
         status = self.GetStatus(id)

         if status['mountAtBoot']:
            log.info('Bringing default datastore %d online.', id)
            try:
               self.GoOnline(id)
            except DatastoreException:
               log.exception('Unable to bring datastore %d online that was to be mounted ' \
                             'at boot. Continuing.', id)

   def Acquire(self, dsId):
      sess = self.db.CreateSession()

      with model.AutoSession(sess):
         try:
            ds = sess.query(model.DataStore).filter_by(id=dsId).one()

            if ds.name == SYSTEM:
               raise DatastoreException('The system partition is read-only.')

            share = ds.GetAsShare()

            if ds.status != 'online':
               raise DataStoreOfflineError
         except ormexc.NoResultFound:
            # raised by .one()
            raise DataStoreNotFoundError(dsId)

      # Note that share is not a SQLAlchemy object, so no need
      # to keep the session open or expunge the object
      lease = DataStoreLease(self.leaseNumber, dsId, share)
      self.leases[dsId][lease.id] = lease

      self.leaseNumber += 1

      return lease

   def Release(self, lease):
      if lease.datastoreName in self.leases and \
             lease.id in self.leases[lease.datastoreName]:
         del self.leases[lease.datastoreName][lease.id]
      else:
         raise LeaseDoesNotExistError(lease)
