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

# Common exception classes and base classes needed to define Workers.
import os
import sys
import uuid
import logging
from collections import namedtuple

from afdeploy import conf

# If a toolchain exists, use it to get pyvpx.
toolchain = os.environ.get('TCROOT', '/build/toolchain')

if os.path.exists(toolchain):
   pyVmomiPath = os.path.join(toolchain, 'noarch', 'pyvpx-4.1.0-258902')
   sys.path.append(pyVmomiPath)

# If not, let's hope it's on the local filesystem where Python can see it.
import pyVmomi

log = logging.getLogger(__name__)

InstanceInfo = namedtuple('InstanceInfo', 'vmxPath guestUsername guestPassword')

class WorkerException(Exception):
   pass

class WorkerManagerException(WorkerException):
   pass

class InstanceAddError(WorkerException):
   pass

class InstanceRemoveError(WorkerException):
   pass

class InstanceAcquireError(WorkerException):
   pass

class InstanceReleaseError(WorkerException):
   pass

# What basically amounts to a mutable namedtuple.
class InstanceState(object):
   def __init__(self, reserved=False, ready=True):
      self.reserved = reserved
      self.ready = ready

# A virtual version of LinkedCloneWorker and FullInstallWorker.
# The details of creating new instances are not handled here, but
# most everything else is.
class Worker(object):
   def __init__(self, conn, info):
      self.conn = conn
      self.info = info
      self.pendingCount = 0
      self.instances = {}

   def AddInstance(self):
      """
         Takes no arguments. If the maximum number of instances has not been
         reached, begins the creation of a new instance of the Worker and
         returns a MOR to a VirtualMachine. If not, throws InstanceAddError.

         The creation of the worker has to be defined by AddInstanceImpl.
         Otherwise, a NotImplementedError will be raised.
      """
      count = self.GetInstanceCount()
      assert count <= self.info.maxCount

      if count + self.pendingCount == self.info.maxCount:
         raise InstanceAddError('Template %s is already instantiated %d times and cannot be further instantiated.' % (self.info.name, count))

      # Now that we can do it, actually add the VM.
      self.pendingCount += 1
      vm = self.AddInstanceImpl()
      self.pendingCount -= 1

      vmxPath = vm.config.files.vmPathName

      self.RegisterInstance(vmxPath, reserved=False, ready=False)

      return vm

   def HaveInstance(self, vmxPath):
      """
         To be used mainly by subclasses and not consumers of this class.
         Defined so that subclasses can avoid touching self.instances
         directly.
      """
      return vmxPath in self.instances

   def RegisterInstance(self, vmxPath, reserved=False, ready=True):
      """
         To be used mainly by subclasses and not consumers of this class.
         Defined so that subclasses can avoid touching self.instances
         directly.
      """
      self.instances[vmxPath] = InstanceState(reserved, ready)

   def RemoveInstance(self, vmxPath=None):
      """
         This default implementation assumes that removing an instance
         involves finding one in self.instances that isn't reserved and
         actually deleting it.

         vmxPath: ESX-style locator for the .vmx file in particular to delete.
                  If you don't care, omit it, and we'll try to delete whatever
                  is free.

         Returns nothing.
      """
      if self.GetInstanceCount() == 0:
         raise InstanceRemoveError('RemoveInstance called, but no instances to remove')

      if vmxPath is not None:
         if vmxPath not in self.instances.keys():
            raise InstanceRemoveError('Cannot remove an instance that does not belong to this template')
         elif self.instances[vmxPath].reserved: # is the VM reserved?
            raise InstanceRemoveError('Requested removal of specific instance, but that instance is reserved')
      else:
         # Find a VM that is not reserved, nuke it.
         vmxPath = self.GetFirstFreeVM()
         if vmxPath is None:
            raise InstanceRemoveError('RemoveInstance called but all instances are reserved')

      # Get VM object from VMX path
      vmObj = self.conn.GetVMByPath(vmxPath)
      log.info('Deleting VM: %s' % vmObj.name)
      vmObj.Destroy()

      # Delete from list
      del self.instances[vmxPath]

   def AcquireInstance(self):
      """
         Takes no arguments. Finds the first nonreserved VM, reserves it,
         and returns it. If there are no nonreserved VMs available, but the
         template specifies a higher instance limit, then calls AddInstance
         to create one and then reserves that. If at the limit, throws
         InstanceAcquireError.

         Returns a tuple of (vmObj, info) where the variables are:

         vmObj: a MOR to a VirtualMachine, which could be None. If it is not
         None, the caller must call Settle(vmObj) first to be able to use
         this VM. If not, it is not necesary and the VM is immediately
         available for use.

         info: an InstanceInfo tuple describing how to access this VM through
         hostd. Always present.
      """
      reserveVm = self.GetFirstFreeVM()
      vm = None

      if reserveVm is None:
         try:
            vm = self.AddInstance()
            reserveVm = vm.config.files.vmPathName
         except InstanceAddError:
            # Die early, and let the caller back off and wait.
            log.error('Tried to reserve, no instances free and cannot create more!')
            return (None, None)

      # Set reserved state. For new instances, this implicitly creates the
      # self.instances entry for them.
      self.instances[reserveVm].reserved = True

      # If a VM was created, vm will be non-None. That indicates that the
      # caller must pass that MOR to Settle as documented above.
      return (vm, Worker.MakeInstanceInfo(reserveVm, self.info.guestUser, self.info.guestPass))

   def ReleaseInstance(self, instance):
      """
         Relinquishes a lease on an instance tuple such as the kind returned
         by AcquireInstance.
      """
      if instance.vmxPath not in self.instances:
         raise InstanceReleaseError('Cannot release a clone that does not belong to us')
      elif not self.instances[instance.vmxPath].reserved:
         raise InstanceReleaseError('Cannot release a clone that has not been acquired')

      self.instances[instance.vmxPath].reserved = False

   def GetInstanceCount(self):
      """
         Returns the total number of available instances, reserved or not.
      """
      return len(self.instances)

   def GetReservedCount(self):
      """
         Returns the number of instances that are currently reserved.
      """
      return sum(self.instances.values())

   # This isn't strictly public, but is a helper function useful to any
   # subclass of Worker.
   def GetFirstFreeVM(self):
      """
         Semi-private function. Returns the first free instance known
         to this Worker.
      """
      def predicate(p):
         state = self.instances[p]
         return (not state.reserved) and state.ready

      freeVms = filter(predicate, self.instances.keys())
      if len(freeVms) == 0:
         return None

      return freeVms[0]

   def Settle(self, vm):
      """
         Performs whatever actions are needed to wait for an installation
         on the VM designated by 'vm' to complete, after which the VM is
         automatically made ready for use by AcquireInstance.
      """
      self.SettleImpl(vm)
      log.info('Creating clean base snapshot for %s.', vm)
      task = vm.CreateSnapshot(conf.CLONE_CLEAN_SNAP_NAME,
                               'Clean snapshot for this clone',
                               False, # cold snapshot
                               False) # quiesce doesn't matter, VM is off
      self.conn.WaitTask(task, 'snapshot')
      self.instances[vm.config.files.vmPathName].ready = True

   def Refresh(self):
      """
         Method to refresh the contents of self.instances from VI. The
         subclass must define RefreshImpl to correctly retrieve the most
         up-to-date list of the instances.

         After initializing, your subclass should call Refresh as soon as it
         has all the data it needs to do so. It is not done automatically in
         this parent class to allow greater flexibility in subclasses.
      """
      newInstances = self.RefreshImpl()

      oldInstancesSet = set(self.instances.keys())
      newInstancesSet = set(newInstances)

      # We need to preserve reservations while keeping track of VMs
      # that get added and removed.
      added   = newInstancesSet - oldInstancesSet
      removed = oldInstancesSet - newInstancesSet

      for vm in removed:
         if oldInstancesSet[vm]: # reserved
            raise InstanceException('A reserved VM was deleted spuriously')
         del self.instances[vm]

      # XXX: We assume freshly added VMs are ready. Should we?
      for vm in added:
         self.instances[vm] = InstanceState(reserved=False, ready=True)

      log.info('Have %d instance(s) of %s in inventory:' % (len(self.instances), self.info.name))

      for vm, state in self.instances.items():
         log.info('\t%s (ready: %s, reserved: %s)' % (vm, state.ready, state.reserved))

   def UseCustomVM(self, vm):
      """
         Should only need to be called once in the lifetime of the workpool.
         Does whatever it needs to the input VM such that it will get picked
         up again when this workpool reinitializes with the same settings.

         Then, if applicable, this VM should be designated as the parent /
         template VM for this workpool instance for the current run.
         The base implementation does some cursory sanity checks and then
         hands it off to UseCustomVMImpl.

         Does NOT have to be defined for a functional worker. Not all worker
         types need to define this.
      """

      summary = vm.summary

      # Make sure the guest IDs match
      hostdGuestId = '%sGuest' % self.info.osKey
      if hostdGuestId != summary.config.guestId:
         raise WorkerException('Custom VM OS key does not match template OS key (%s != %s)' % (hostdGuestId, summary.config.guestId))

      # As a precaution, do not use powered-on VMs
      if summary.runtime.powerState == 'poweredOn':
         raise WorkerException('Custom VM is powered on, please shut it down first')

      # Make sure Tools is marked as installed (outdated is OK for now)
      if summary.guest.toolsVersionStatus == 'guestToolsNotInstalled':
         raise WorkerException('VMware Tools is not installed on this guest')

      return self.UseCustomVMImpl(vm)

   # The following must be defined by the subclass
   def RefreshImpl(self):
      """
         Should poke VI and return a list of the VMX paths of detected
         instances in the VI environment. The semantics of what to actually
         do with that list is handled by Worker.Refresh.
      """
      raise NotImplementedError, 'Pure virtual RefreshImpl called'

   def AddInstanceImpl(self):
      """
         Implementation-specific function for AddInstance. Should actually
         create the VM and return the MOR to it.
      """
      raise NotImplementedError, 'Pure virtual AddInstanceImpl called'

   def SettleImpl(self, vm):
      """
         Takes a MOR to a VM object and does whatever it needs to conclude
         the install on it after kicking it off. Most correct implementations
         will have a Settle function, otherwise you are probably spending
         too much time waiting inside AddInstance.
      """
      raise NotImplementedError, 'Pure virtual SettleImpl called'

   def UseCustomVMImpl(self, vm):
      """
         Performs implementation-specific tasks after UseCustomVM is called.
         See the documentation for UseCustomVM for what this might do.
      """
      raise NotImplementedError, 'This worker does not support custom VMs'

   def PreInstall(self):
      """
         Takes no arguments. Should kick off an arbitrary long-running task
         (e.g. Windows install) that can be done in the background while
         doing something else (e.g. deploying the appliance.) The Worker is
         responsible for detecting this preinstall and doing whatever is needed
         to conclude it.

         Returns nothing.
      """
      raise NotImplementedError, 'Pure virtual PreInstall called'

   @staticmethod
   def MakeInstanceInfo(vmxPath, guestUser, guestPass):
      """
         Return a data object of VM information.
      """
      return InstanceInfo(vmxPath=vmxPath,
                          guestUsername=guestUser,
                          guestPassword=guestPass)

   def UpdateAnnotation(self, vm, kind, id=None):
      """
         A convenience method to re-annotate the given VM with an updated kind
      """
      group = None
      oldId = None

      try:
         (oldKind, group, oldId) = vm.config.annotation.split(':', 2)
      except ValueError:
         pass

      if group is not None and group != self.info.name:
         raise WorkerException('SetAnnotation called on VM from different group (expected %s, got %s)' % (self.info.name, group))

      # Sanity checks. Use old ID if possible; if an ID is explicitly specified
      # warn if it is different than the one already assigned to it.
      if id is None:
         if oldId is not None:
            id = oldId
         else:
            log.warning('Assigning new UUID to %s', vm)
            id = uuid.uuid4()
      elif oldId is not None and oldId != id:
         log.warning('Overwriting UUID for %s with %s', vm, id)

      reconf = pyVmomi.vim.vm.ConfigSpec()
      reconf.annotation = '%s:%s:%s' % (kind, self.info.name, id)

      task = vm.Reconfigure(reconf)
      self.conn.WaitTask(task, 'VM annotation update')
