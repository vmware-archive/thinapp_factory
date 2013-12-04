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
import sys
import logging
import random
import socket

from afdeploy import conf
import base

log = logging.getLogger(__name__)

# If a toolchain exists, use it to get pyvpx.
toolchain = os.environ.get('TCROOT', '/build/toolchain')

if os.path.exists(toolchain):
   pyVmomiPath = os.path.join(toolchain, 'noarch', 'pyvpx-4.1.0-258902')
   sys.path.append(pyVmomiPath)

# If not, let's hope it's on the local filesystem where Python can see it.
import pyVmomi

# A class representing a single template and a list / count of all its clones.
# Does not do any management of whether any given VM is powered on, reserved,
# etc. That is the job of the WorkerManager.
class LinkedCloneWorker(base.Worker):
   # Map of variant type -> KMS key
   KMS_KEY_MAP = {
      'Windows Vista ENTERPRISE': 'VKK3X-68KWM-X2YGT-QR4M6-4BWMV',
      'Windows 7 ENTERPRISE': '33PXH-7Y6KF-2VJC9-XBBR8-HVTHH',
   }

   def __init__(self, conn, info):
      base.Worker.__init__(self, conn, info)

      # Linked clone specific state
      self.templateVm   = None

      # For now enforce Windows Vista or 7 Enterprise for KMS, as early
      # as possible in the worker's life.
      if self.info.osKey in ('winvista', 'windows7') and \
         self.info.kmsServer is not None and \
         self.info.variant not in self.KMS_KEY_MAP:
         raise base.WorkerException('Cannot install %s through KMS with variant %s' % (self.info.osKey, self.info.variant))

      # May be None. Note that we don't care to find in-flight template
      # installs with this query, only finished ones. That happens in
      # InstallTemplate.
      vm = self._FindTemplateVM()

      if vm is not None:
         self._SetVM(vm)

   def AddInstanceImpl(self):
      if not self.templateVm:
         self._InstallTemplate() # implicitly defines self.templateVm
         if not self.templateVm:
            raise base.InstanceAcquireError('Tried to install template for instance add, but failed!')

      destFolder = self.templateVm.parent
      index = self.conn.GetIndex()

      while True:
         vmName = '%s Instance %05x' % (self.info.name, random.randint(0, 0xfffff))
         if index.FindChild(entity=destFolder, name=vmName) is None:
            break

      log.info('Cloning to new VM: %s' % vmName)

      task = self.templateVm.Clone(destFolder, vmName, self.cloneSpec)
      self.conn.WaitTask(task, 'VM clone')

      # it's success if we get here
      log.debug('Powering on new VM to complete customization.')

      newVm = task.info.result

      try:
         # This (setting video ram) should have been done on the template
         # but we still do it again just in case an old template is used.
         LinkedCloneWorker.SetVideo(self.conn, newVm)
         task = newVm.PowerOn()

         self.conn.WaitTask(task, 'VM clone first boot')
      except:
         newVm.Destroy()
         raise

      # We return the VM to you now, but it is still booting and customizing.
      # Call Settle(newVm) to be sure it is ready for use.
      return newVm

   def SettleImpl(self, vm):
      # When creating clones, we wait for a power-off signal to indicate
      # completion of customization.
      self.conn.WaitPowerOff(vm, 3600, failDelete=True)
      vmxPath = vm.config.files.vmPathName
      log.info('VM %s has been settled and is now powered off.' % vmxPath)

   def UseCustomVMImpl(self, vm):
      self._BlessTemplate(vm)
      self._SetVM(vm)

   def PreInstall(self):
      self._InstallTemplate(postinstall=False)

   def _InstallTemplate(self, postinstall=True):
      if self.templateVm:
         return self.templateVm

      # In this critical moment, re-search for template VMs that may be
      # in an installing state, regardless of what we know from before.
      vm = self._FindTemplateVM(installing=True)

      if not vm:
         log.debug('No %s template found, installing a new one', self.info.name)
         creator = newvm.NewEasyInstallVM(self.conn, self.info,
                                          '%s Template' % self.info.name)
         vm = creator.Install()
         LinkedCloneWorker.SetVideo(self.conn, vm)

         # Set the annotation
         self.UpdateAnnotation(vm, conf.VM_ANNOTATION_LINKED_INSTALLING)
      else:
         (kind, vmName, id) = vm.config.annotation.split(':', 2)
         if kind == conf.VM_ANNOTATION_LINKED:
            # We magically got a complete template, just use it.
            log.debug('Discovered a completely installed template, using it.')
            self.templateVm = vm
            return self.templateVm
         else:
            log.debug('Template %s is currently installing', self.info.name)

      # If the VM is installing (whether the install began earlier or just
      # now), wait for it to power off and run all post-install ops on it.
      # Do all this unless postinstall=False (deployer case.)
      if postinstall and vm and vm.config.annotation.startswith(conf.VM_ANNOTATION_LINKED_INSTALLING):
         # Tools automatically installs itself on the guest. When toolsd comes
         # up, that indicates that the reboot that concludes the install is
         # complete.  At that time, we can safely shut down the VM.
         log.info('Waiting for template install to finish.')
         self.conn.WaitForTools(vm, 3600, failDelete=True)

         # The tools' installation lowers screen's resolution to 640x480.
         # We revert it back to our vmrc console size.
         log.info('Set screen resolution to 800x600.')
         vm.SetScreenResolution(800, 600)

         log.info('Shutting down VM.')
         vm.ShutdownGuest()
         self.conn.WaitPowerOff(vm, 3600, failDelete=True)

         self._BlessTemplate(vm)

   def _BlessTemplate(self, vm):
      log.info('Finalizing new template in group %s', self.info.name)
      LinkedCloneWorker.FixDevices(self.conn, vm)
      self.UpdateAnnotation(vm, conf.VM_ANNOTATION_LINKED)

      # Take cold snapshot for link-cloning. Though Tools is not fully
      # installed yet (needs a reboot), it will need a full boot/reboot
      # cycle for the customization phase anyway.
      task = vm.CreateSnapshot(conf.CLONE_SNAP_NAME,
                               'Snapshot for linked clones of this VM',
                               False, # cold snapshot
                               False) # quiesce doesn't matter, VM is off

      self.conn.WaitTask(task, 'snapshot')

      # Finally, mark the VM as a template
      log.debug('Marking this VM as a template')
      vm.MarkAsTemplate()

      # Explicitly set up the VM with a clone spec, get its ID, etc.
      # This also sets self.templateVm
      self._SetVM(vm)

   def RefreshImpl(self):
      if not self.templateVm:
         raise base.WorkerException('Template VM not installed yet for %s' % self.info.name)

      # Helper for GetVMByProperty
      pathNames = []
      def _InstanceFinder(props):
         if 'config.annotation' not in props:
            return

         if props['config.annotation'] is not None:
            try:
               (vmType, ident) = props['config.annotation'].split(':', 1)
            except ValueError:
               return

            if vmType == conf.VM_ANNOTATION_LINKED_SLAVE and ident == self.templateId:
               pathNames.append(props['config.files.vmPathName'])

      self.conn.GetVMByProperty(('config.annotation',
                                 'config.files.vmPathName'),
                                _InstanceFinder, many=True)

      return pathNames

   def _SetVM(self, vm):
      self.templateVm    = vm
      self.templatePath  = vm.config.files.vmPathName
      if self.conn.useAnnotations:
         (kind, vmName, id) = vm.config.annotation.split(':', 2)
         self.templateId    = id

         if vmName != self.info.name:
            raise base.WorkerException('Tried to link %s template to %s VM' % (self.info.name, vmName))

      # Find cloning snapshot for linked clone
      self.cloneSnap = self.conn.FindSnapshot(self.templateVm, conf.CLONE_SNAP_NAME)
      if self.cloneSnap is None:
         raise base.WorkerException('Template "%s" has no cloning snapshot (%s)' % (self.info.name, conf.CLONE_SNAP_NAME))

      # Ensure the host on which the template lives, which is where the
      # linked clone will reside, supports delta disk backings (aka has
      # linked clone support.)
      host = vm.runtime.host
      if not self.conn.HostCanLinkClone(host):
         raise base.WorkerException('VM host %s does not support linked clones' % host.name)

      # Clone spec will be the same for all clones, so prepare it now.
      self.cloneSpec = self._MakeCloneSpec()

      # Refresh all existing instances
      if self.conn.useAnnotations:
         self.Refresh()

   def _MakeCloneSpec(self):
      cloneSpec = pyVmomi.Vim.Vm.CloneSpec()
      cloneSpec.config = pyVmomi.Vim.Vm.ConfigSpec()
      if self.conn.useAnnotations:
         cloneSpec.config.annotation = '%s:%s' % (conf.VM_ANNOTATION_LINKED_SLAVE, self.templateId)

      customSpec = pyVmomi.Vim.Vm.Customization.Specification()
      reloSpec = pyVmomi.Vim.Vm.RelocateSpec()
      sysPrep = pyVmomi.Vim.Vm.Customization.Sysprep()
      globalIp = pyVmomi.Vim.Vm.Customization.GlobalIPSettings()
      userData = pyVmomi.Vim.Vm.Customization.UserData()
      guiUnattended = pyVmomi.Vim.Vm.Customization.GuiUnattended()
      guiRunOnce = pyVmomi.Vim.Vm.Customization.GuiRunOnce()
      adapterMap = pyVmomi.Vim.Vm.Customization.AdapterMapping()

      # Set everything!
      adapterMap.adapter = pyVmomi.Vim.Vm.Customization.IPSettings()
      adapterMap.adapter.ip = pyVmomi.Vim.Vm.Customization.DhcpIpGenerator()

      userData.computerName = pyVmomi.Vim.Vm.Customization.PrefixNameGenerator()

      userData.computerName.base = conf.CLONE_NETBIOS_PREFIX
      userData.fullName = self.info.userName
      userData.orgName = self.info.organization

      if self.info.prodKey:
         userData.productId = self.info.prodKey
      else:
         # Check was made earlier to ensure that variant is in the map
         assert self.info.osKey in ('windows7', 'winvista')
         userData.productId = LinkedCloneWorker.KMS_KEY_MAP[self.info.variant]

      # We must set autologon to at least 1 so that the VM can shut itself down.
      guiUnattended.autoLogon = True
      # Set to something obscenely large so that we always autologon.
      # This only needs to be as large as the number of consecutive
      # logons required since the count is reset when reverting to a
      # previous snapshot.
      guiUnattended.autoLogonCount = 999
      guiUnattended.password = pyVmomi.Vim.Vm.Customization.Password()
      guiUnattended.password.plainText = True
      guiUnattended.password.value = self.info.guestPass
      # XXX: Should get time zone from appliance UI or something
      guiUnattended.timeZone = 4

      sysPrep.guiUnattended = guiUnattended
      sysPrep.guiRunOnce = pyVmomi.Vim.Vm.Customization.GuiRunOnce()

      # On Vista and above, customizing rearms the activation. We have
      # to do this because customization does not take care of it for us.
      # And even if it does, doing it again does not hurt.
      # This applies to both KMS and MAK activation.
      # See http://communities.vmware.com/message/1525854
      cmdList = []

      if self.info.osKey in ('winvista', 'windows7'):
         # Also, if we do KMS, we must re-assign the KMS server
         if self.info.kmsServer:
            cmdList.append(r'cscript.exe \windows\system32\slmgr.vbs -skms %s' % self.info.kmsServer)

         cmdList.append(r'cscript.exe \windows\system32\slmgr.vbs -ato')

         # Finally, hide the set network location dialog. This means that
         # ALL networks will be considered public! Which should be fine for
         # typical worker VM usage.
         # See http://www.msfn.org/board/topic/99339-found-a-workaround-for-the-network-location-bug/
         cmdList.append('reg add "HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\NetworkList\NewNetworks" /v NetworkList /t REG_MULTI_SZ /d 00000000 /f')
      else:
         # Inhibit Security Center and Windows Tour crap.
         cmdList.append(r'regedit /s C:\finalize.reg')

      cmdList.append('shutdown -s -t 0')

      sysPrep.guiRunOnce.commandList = cmdList
      sysPrep.identification = pyVmomi.Vim.Vm.Customization.Identification()
      sysPrep.identification.joinWorkgroup = conf.CLONE_WORKGROUP
      sysPrep.userData = userData

      customSpec.identity = sysPrep
      customSpec.globalIPSettings = globalIp
      customSpec.options = pyVmomi.Vim.Vm.Customization.WinOptions()
      customSpec.options.changeSID = True
      customSpec.options.deleteAccounts = True
      customSpec.nicSettingMap = [adapterMap]

      resource = self.conn.GetComputeResource(self.info.resName)
      if self.info.resPool:
         pool = self.conn.GetResourcePool(resource, self.info.resPool)
      else:
         pool = resource.resourcePool

      # This is the linked clone magic
      reloSpec.diskMoveType = pyVmomi.Vim.Vm.RelocateSpec.DiskMoveOptions.createNewChildDiskBacking
      reloSpec.pool = pool

      cloneSpec.customization = customSpec
      cloneSpec.location = reloSpec
      cloneSpec.powerOn = False
      cloneSpec.snapshot = self.cloneSnap
      cloneSpec.template = False

      cloneSpec.config.extraConfig = []
      opt = pyVmomi.Vim.Option.OptionValue()
      opt.key = "com.vmware.thinappfactory"
      opt.value = socket.gethostname()
      cloneSpec.config.extraConfig.append(opt)

      return cloneSpec

   def _FindTemplateVM(self, lookIdent=None, installing=False):
      """
         A utility function to find AppFactory style template VMs.
      """
      def _TemplateFinder(p):
         if 'config.annotation' not in p:
            return False

         if p['config.annotation'] is not None:
            try: (vmType, name, ident) = p['config.annotation'].split(':', 2)
            except ValueError: return False

            isFinishedTemplate = vmType == conf.VM_ANNOTATION_LINKED and \
                                 'config.template' in p and \
                                 p['config.template']

            isInstallingTemplate = installing and \
                                   vmType == conf.VM_ANNOTATION_LINKED_INSTALLING

            if (isFinishedTemplate or isInstallingTemplate) and \
               name == self.info.name and \
               (lookIdent is None or lookIdent == ident):
               return True

      keySet = ('config.annotation', 'config.template')
      return self.conn.GetVMByProperty(keySet, _TemplateFinder)

   @staticmethod
   def FixDevices(conn, vmObj):
      # Fix toolinstall.c breakage; on ESX 4.1 and before it will reset the
      # CD device to a hosted deviceType. We must fix this or cloning will fail
      # Note that this is idempotent, so calling it on a VM that's already fixed
      # is a no-op.
      reconfig = pyVmomi.Vim.Vm.ConfigSpec()
      flopSpec = pyVmomi.Vim.Vm.Device.VirtualDeviceSpec()
      cdSpec = pyVmomi.Vim.Vm.Device.VirtualDeviceSpec()

      for device in vmObj.config.hardware.device:
         if isinstance(device, pyVmomi.Vim.Vm.Device.VirtualCdrom):
            cdSpec.device = device
         elif isinstance(device, pyVmomi.Vim.Vm.Device.VirtualFloppy):
            flopSpec.device = device
         if cdSpec.device is not None and flopSpec.device is not None:
            break

      if cdSpec.device is None or flopSpec.device is None:
         # This isn't the VM we're looking for, do nothing.
         return True

      devices = []

      if cdSpec.device is not None:
         cdSpec.device.backing = pyVmomi.Vim.Vm.Device.VirtualCdrom.RemoteAtapiBackingInfo()
         cdSpec.device.backing.useAutoDetect = True
         cdSpec.operation = pyVmomi.Vim.Vm.Device.VirtualDeviceSpec.Operation.edit
         devices.append(cdSpec)

      if flopSpec.device is not None:
         flopSpec.device.backing = pyVmomi.Vim.Vm.Device.VirtualFloppy.RemoteDeviceBackingInfo()
         flopSpec.device.backing.useAutoDetect = True
         flopSpec.operation = pyVmomi.Vim.Vm.Device.VirtualDeviceSpec.Operation.edit
         devices.append(flopSpec)

      if len(devices) > 0:
         reconfig.deviceChange = devices

         task = vmObj.Reconfigure(reconfig)
         conn.WaitTask(task, 'VM reconfiguration')
      else:
         log.warning('No devices to reconfigure for %s' % vmObj)

      return True

   @staticmethod
   def AddExtraConfig(extraCfgs, key, value):
      opt = pyVmomi.Vim.Option.OptionValue()
      opt.key = key
      opt.value = value
      extraCfgs.append(opt)

   @staticmethod
   def SetVideo(conn, vmObj):
      log.info('reconfig video')
      reconfig = pyVmomi.Vim.Vm.ConfigSpec()
      reconfig.extraConfig = []
      reconfig.deviceChange = []

      LinkedCloneWorker.AddExtraConfig(reconfig.extraConfig, "svga.maxWidth", "1600")
      LinkedCloneWorker.AddExtraConfig(reconfig.extraConfig, "svga.maxHeight", "1200")
      LinkedCloneWorker.AddExtraConfig(reconfig.extraConfig, "svga.vramSize", "8388608")
      LinkedCloneWorker.AddExtraConfig(reconfig.extraConfig, "svga.autodetect", "FALSE")

      for device in vmObj.config.hardware.device:
         if isinstance(device, pyVmomi.Vim.Vm.Device.VirtualVideoCard):
            if device.videoRamSizeInKB >= 8192:
               return

            device.videoRamSizeInKB = 8192
            spec = pyVmomi.Vim.Vm.Device.VirtualDeviceSpec()
            spec.operation = pyVmomi.Vim.Vm.Device.VirtualDeviceSpec.Operation.edit
            spec.device = device
            reconfig.deviceChange.append(spec)

      task = vmObj.Reconfigure(reconfig)
      conn.WaitTask(task, 'VM video reconfiguration')

