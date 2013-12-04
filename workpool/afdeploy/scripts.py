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
import logging
import pyVmomi
import sys

from ConfigParser import ConfigParser

from afdeploy import conf
from afdeploy.connection import VCInfo, VIConnection
from afdeploy.worker.linked import LinkedCloneWorker

log = logging.getLogger(__name__)

CloneInfo = collections.namedtuple('CloneInfo', 'name moid osKey prodKey organization userName kmsServer variant guestPass resName resPool dsName')

def _itemsToDict(items):
   d = {}
   for k, v in items:
      if v.strip() == '':
         v = None

      d[k] = v
   return d

def _getVcInfo(config):
   vc = config.items('vc')
   vc = _itemsToDict(vc)
   return VCInfo(**vc)

def _handleException(fault, vcInfo, vmInfo):
      print >> sys.stderr, type(fault).__name__, ':'
      # skip meaningless and unassigned fault attributes
      attrnames = dir(fault)
      for name in attrnames:
         if name not in ('message', 'Array') and not name.startswith('_'):
            val = getattr(fault, name)
            if val:
               print >> sys.stderr, name, '=', val
      # vc and vm info/spec.
      vcInfo = vcInfo._replace(vcPassword = '*****')
      vmInfo = vmInfo._replace(guestPass = '*****', prodKey = '*****')
      print >> sys.stderr, '\n', vcInfo
      print >> sys.stderr, '\n', vmInfo

def CloneVm():
   if len(sys.argv) != 2:
      raise Exception('%s must be called with an ini input file.' % sys.argv[0])

   iniFile = sys.argv[1]
   config = ConfigParser()
   # So that key names don't get lower cased.
   config.optionxform = str
   config.read(iniFile)

   general = _itemsToDict(config.items('general'))
   logFile = general['logFile']
   logging.basicConfig(level=logging.DEBUG, filename=logFile)

   vcInfo = _getVcInfo(config)
   cloneInfo = _itemsToDict(config.items('vm'))
   guestInfo = _itemsToDict(config.items('guest'))
   cloneInfo['guestPass'] = guestInfo['password']

   info = CloneInfo(**cloneInfo)

   _cloneVmImpl(vcInfo, info)

def _waitForFinish(conn, vm):
   # Tools automatically installs itself on the guest. When toolsd comes up,
   # that indicates that the reboot that concludes the install is complete.
   # At that time, we can safely shut down the VM.
   log.info('Waiting for tools to come up...')
   conn.WaitForTools(vm, 3600, failDelete=True)

   # The tools' installation lowers screen's resolution to 640x480.
   # We revert it back to our vmrc console size.
   log.info('Set screen resolution to 800x600.')
   vm.SetScreenResolution(800, 600)

   # Initiate guest shutdown and wait for poweroff to occur.
   log.info('Waiting for guest to power down...')
   vm.ShutdownGuest()
   conn.WaitPowerOff(vm, 3600, failDelete=True)

   # XXX: This is called in _BlessTemplate in workers currently which
   # is weird.
   LinkedCloneWorker.FixDevices(conn, vm)

def _cloneVmImpl(vcInfo, cloneInfo):
   conn = None
   vm = None
   try:
      conn = VIConnection(vcInfo)
      conn.useAnnotations = False

      class FixedLinkedCloneWorker(LinkedCloneWorker):
         def _FindTemplateVM(self, lookIdent=None, installed=False):
            vm = pyVmomi.vim.VirtualMachine(cloneInfo.moid, conn.instance._stub)
            return vm

      linked = FixedLinkedCloneWorker(conn, cloneInfo)
      assert linked.templateVm
      vm = linked.AddInstanceImpl()
      linked.SettleImpl(vm)

      # This snapshot creation is normally done in base.Worker.Settle
      # but we don't want to run the code that modifies
      # self.instances.  So we just call SettleImpl above then do this
      # bit ourselves.
      log.info('Creating clean base snapshot for %s.', vm)
      task = vm.CreateSnapshot(conf.CLONE_CLEAN_SNAP_NAME,
                               'Clean snapshot for this clone',
                               False, # cold snapshot
                               False) # quiesce doesn't matter, VM is off
      conn.WaitTask(task, 'snapshot')

      print vm._moId
   except Exception as fault:
      if vm:
         vm.Destroy()
      _handleException(fault, vcInfo, cloneInfo)
   finally:
      if conn:
         conn.instance.content.sessionManager.Logout()
