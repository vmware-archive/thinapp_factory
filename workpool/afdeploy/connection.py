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

# This file defines a helper that does not abstract pyVmomi, but provides
# helpers for long-winded workflows and common tasks.

import os
import re
import stat
import math
import sys
import time
import base64
import logging

from contextlib import closing
from collections import namedtuple
log = logging.getLogger(__name__)

import httplib
import urllib
import urllib2

# If a toolchain exists, use it to get pyvpx.
toolchain = os.environ.get('TCROOT', '/build/toolchain')

if os.path.exists(toolchain):
   pyVmomiPath = os.path.join(toolchain, 'noarch', 'pyvpx-4.1.0-258902')
   sys.path.append(pyVmomiPath)

# If not, let's hope it's on the local filesystem where Python can see it.
import pyVmomi
import pyVim.connect

# Local library of string literals used in configuration file.
from afdeploy import conf

VCInfo = namedtuple('VCInfo', 'vcHost vcUsername vcPassword dcName')

class VIException(Exception):
   pass

class VIConnection(object):
   RESTRICT_DIR = 0
   RESTRICT_FILE = 1

   USER_AGENT = 'ThinAppFactory CWS/1.0'

   def __init__(self, vcInfo, _namespace='vim25/4.0'):
      self.vcInfo = vcInfo
      # This may be overriden later with a call to SetDefaultDatacenterName
      self.dcName = vcInfo.dcName

      log.debug('Connecting to VC, host=%s, user=%s, dc=%s', vcInfo.vcHost, vcInfo.vcUsername, vcInfo.dcName)

      # Initiate the connection (may throw exception)
      self.instance = pyVim.connect.Connect(vcInfo.vcHost,
                                            user=vcInfo.vcUsername,
                                            pwd=vcInfo.vcPassword,
                                            namespace=_namespace)

      self.pathRe = re.compile('\[([^\]]+)\] (.*)')

      self.productLineId = self.instance.content.about.productLineId

      self.useAnnotations = True

   def GetProduct(self):
      return self.productLineId

   def HostCanLinkClone(self, host):
      return host.capability.deltaDiskBackingsSupported

   def SetDefaultDatacenterName(self, dcName):
      if self.dcName is not None:
         log.warning('Overriding default datacenter name %s with %s',
                     self.dcName, dcName)
      self.dcName = dcName

   def GetDefaultDatacenterName(self, default=None):
      if default:
         return default
      elif self.dcName:
         return self.dcName
      raise VIException('GetDefaultDatacenterName() called but no datacenter set')

   def GetConnectInfo(self):
      # Datacenter name may have been updated since the initial connect.
      # Build a vcInfo that represents this properly.
      # XXX: It's pretty ugly that this needs to be done. Maybe we need
      # a first-class VCInfo object instead of a nonmutable tuple type?
      return connection.VCInfo(vcHost=self.vcInfo.vcHost,
                               vcUsername=self.vcInfo.vcUsername,
                               vcPassword=self.vcInfo.vcPassword,
                               dcName=self.GetDefaultDatacenterName())

   def GetHost(self, resName, dcName=None):
      r = self.GetComputeResource(resName, dcName)
      if len(r.host) > 1:
         raise VIException('GetHost called but multiple hosts available for CR')
      elif len(r.host) == 0:
         raise VIException('GetHost called but no hosts available for CR')

      return r.host[0]

   def GetIndex(self):
      return self.instance.content.searchIndex

   def GetRootFolder(self):
      return self.instance.content.rootFolder

   def GetHostFolder(self, dcName=None):
      return self.GetDatacenter(dcName).hostFolder

   def GetVMFolder(self, dcName=None):
      return self.GetDatacenter(dcName).vmFolder

   ### Generic locator method for all types of MORs ###
   def GetObjectsByName(self, typ, predicate, root=None):
      # Search in root folder (all DCs, etc.) by default
      if root is None:
         root = self.GetRootFolder()

      resList = self._FindAll(pathSet=('name',),
                              objType=typ,
                              root=root)

      ret = []

      for result in resList:
         (obj, props) = result
         if predicate is None or predicate(props):
            ret.append(obj)

      return ret

   def GetObjectByName(self, typ, lookName=None, root=None):
      def predicate(props):
         return props['name'] == lookName

      res = self.GetObjectsByName(typ, predicate, root)
      if len(res) == 0:
         raise VIException('No %s named %s' % (typ.__name__, lookName))
      elif len(res) > 1:
         raise VIException('More than one %s named %s' % (typ.__name__, lookName))

      return res[0]

   def GetAllObjectNames(self, typ, root=None):
      names = []

      # Looks odd, but is cheaper than consulting the objects after being
      # returned by GetObjectsByName.
      def predicate(props):
         names.append(props['name'])

      self.GetObjectsByName(typ, predicate, root)
      return names

   def GetAllObjects(self, typ, root=None):
      return self.GetObjectsByName(typ, None, root)

   ### Locating VirtualMachine objects (per-Datacenter) ###
   def GetAllVMNames(self, dcName=None):
      dcName = self.GetDefaultDatacenterName(dcName)
      root = self.GetVMFolder(dcName)

      return self.GetAllObjectNames(pyVmomi.Vim.VirtualMachine, root)

   ### Locating ComputeResource objects (per-Datacenter) ###
   def GetAllComputeResourceNames(self, dcName=None):
      if dcName is not None:
         root = self.GetHostFolder(dcName)
      else:
         root = None

      return self.GetAllObjectNames(pyVmomi.Vim.ComputeResource, root)

   def GetComputeResource(self, resName, dcName=None):
      if dcName is not None:
         root = self.GetHostFolder(dcName)
      else:
         root = None

      return self.GetObjectByName(pyVmomi.Vim.ComputeResource, resName, root)

   ### Locating ResourcePool objects (per-ComputeResource) ###
   def GetAllResourcePoolNames(self, resource):
      # Root resource pool is the blank entry in the list.
      ret = []
      prefixMap = {}
      foundRoot = False
      poolList = self._FindAll(pathSet=('name', 'resourcePool'),
                               objType=pyVmomi.Vim.ResourcePool,
                               root=resource)

      # poolList is very fast and gives us the full hierarchy though we have
      # to piece it back together based on the managed objects.
      for pool in poolList:
         poolObj = pool[0]
         name, subpools = pool[1]['name'], pool[1]['resourcePool']
         key = str(poolObj)

         if key in prefixMap:
            fullName = '/'.join([prefixMap[key], name])
         else:
            # Must be the root. Hopefully.
            assert not foundRoot, 'More than one root detected?'
            foundRoot = True
            fullName = name

         for subpool in subpools:
            prefixMap[str(subpool)] = fullName

         ret.append(fullName)

      return ret

   def GetResourcePool(self, resource, poolName):
      # XXX: A bit expensive, but the simplest way to go.
      poolTree = poolName.split('/')
      obj = resource.resourcePool

      for pool in poolTree:
         obj = self.GetObjectByName(pyVmomi.Vim.ResourcePool, pool, obj)

      return obj

   ### Locating Datacenter objects (global) ###
   def GetAllDatacenterNames(self):
      return self.GetAllObjectNames(pyVmomi.Vim.Datacenter)

   def GetDatacenter(self, dcName=None):
      dcName = self.GetDefaultDatacenterName(dcName)
      return self.GetObjectByName(pyVmomi.Vim.Datacenter, dcName)

   ### Locating Datastore objects (per-Datacenter) ###
   def GetAllDatastoreNames(self, dcName=None):
      if dcName is not None:
         root = self.GetHostFolder(dcName)
      else:
         root = None

      return self.GetAllObjectNames(pyVmomi.Vim.Datastore, root)

   def GetAllDatastores(self, dcName=None):
      if dcName is not None:
         root = self.GetHostFolder(dcName)
      else:
         root = None

      return self.GetAllObjects(pyVmomi.Vim.Datastore, root)

   def GetDatastore(self, dsName, dcName=None):
      if dcName is not None:
         root = self.GetHostFolder(dcName)
      else:
         root = None

      return self.GetObjectByName(pyVmomi.Vim.Datastore, dsName, root)

   def GetVMByPath(self, fullPath, dcName=None):
      return self.GetIndex().FindByDatastorePath(self.GetDatacenter(dcName), fullPath)

   def GetVMByProperty(self, keys, picker, many=False, dcName=None):
      """
         Note: returns a list if many is True, a single VM if many is False.
      """

      vms = self._FindAll(pathSet=keys,
                          objType=pyVmomi.Vim.VirtualMachine,
                          root=self.GetVMFolder(dcName))
      if many:
         ret = []
      else:
         ret = None

      for result in vms:
         (vm, props) = result
         if picker(props):
            if many:
               ret.append(vm)
            else:
               ret = vm
               break

      return ret

   def GetOVFProperty(self, vm, key):
      """ Retrieves a named OVF property from vm.config.vAppConfig.property """
      for prop in vm.config.vAppConfig.property:
         if prop.id == key:
            return prop.value

      return None

   def MakePath(self, dsName, path):
      return '[%s] %s' % (dsName, path)

   def ParsePath(self, fullPath):
      m = self.pathRe.match(fullPath)
      return (m.group(1), m.group(2))

   def FindSnapshot(self, vmObj, snapName):
      def snapHelper(tree):
         for branch in tree:
            if branch.name == snapName:
               return branch.snapshot
            elif len(branch.childSnapshotList) != 0:
               ret = snapHelper(branch.childSnapshotList)
               if ret is not None:
                  return ret
         return None

      if vmObj.snapshot is not None:
         return snapHelper(vmObj.snapshot.rootSnapshotList)
      else:
         return None

   def WaitForTools(self, vm, maxSleepTime, failDelete=True):
      def ToolsPredicate(vm):
         # We observe (guestToolsRunning, guestToolsNotInstalled) *during*
         # setup. Wait until toolsVersionStatus reports something other than
         # guestToolsNotInstalled to declare that the real Tools are indeed
         # running, after the reboot.
         return vm.summary.guest.toolsRunningStatus == 'guestToolsRunning' and \
                vm.summary.guest.toolsVersionStatus != 'guestToolsNotInstalled'

      # XXX: The remainConstant setting may not be necessary anymore,
      #      but for right now it can't hurt.
      self._WaitHelper(ToolsPredicate, vm, maxSleepTime, failDelete,
                       'Tools install', remainConstant=60)

   def WaitPowerOff(self, vm, maxSleepTime, failDelete=True):
      def PowerPredicate(vm):
         return vm.summary.runtime.powerState == 'poweredOff'

      self._WaitHelper(PowerPredicate, vm, maxSleepTime, failDelete,
                       'Guest shutdown')

   def WaitTask(self, task, actionName='job', hideResult=False):
      log.info('Waiting for %s to complete.' % actionName)

      while task.info.state == pyVmomi.Vim.TaskInfo.State.running:
         time.sleep(2)

      if task.info.state == pyVmomi.Vim.TaskInfo.State.success:
         if task.info.result is not None and not hideResult:
            log.info('%s completed successfully, result: %s' % (actionName, task.info.result))
         else:
            log.info('%s completed successfully.' % actionName)
      else:
         log.error('%s did not complete successfully: %s' % (actionName, task.info.error))
         raise task.info.error # should be a Fault... check XXX

      # may not always be applicable, but can't hurt.
      return task.info.result

   def FindDatastoreByName(self, resName, dsName, dsType=None):
      for store in self.GetComputeResource(resName).datastore:
         if (dsType is None or hasattr(store.info, dsType)) and \
            store.info.name == dsName:
            return store
      return None

   def FindAppliance(self, picker, dcName=None, root=None):
      """
         Find appliance VMs (that have OVF configurations) in a datacenter
         or an arbitrary root (hopefully a Folder.) You can discriminate by
         passing in a picker function that receives the property of
         config.vAppConfig.product as an argument. By returning True,
         that VM will be added to the list. Returns a list of tuples of
         (name, object ref).
      """
      dcName = self.GetDefaultDatacenterName(dcName)

      if root is None and dcName is not None:
         root = self.GetDatacenter(dcName).vmFolder
      elif root is None:
         raise VIException('Must either specify a root or a datacenter name')

      resList = self._FindAll(objType=pyVmomi.Vim.VirtualMachine,
                              root=root,
                              pathSet=('name', 'config.vAppConfig.product',))

      ret = {}

      for vm, props in resList:
         if 'config.vAppConfig.product' not in props:
            continue

         if picker(props['config.vAppConfig.product']):
            ret[props['name']] = vm

      return ret

   def GetOVFSettingsForVM(self, vm):
      """
         Retrieve the OVF settings for a VM, as a dictionary of key value pairs.
         Thus, SetOVFSettingsForVM(vm, GetOVFSettingsForVM(vm)) is a no-op.
      """
      return dict([(p.id, p.value) for p in vm.config.vAppConfig.property])

   def SetOVFSettingsForVM(self, vm, values):
      """
         Set the OVF settings for a VM, with values passed in as a dictionary.
         Values that are not in the dictionary are not updated nor removed.
         Note: no way to delete keys for now, but why would you want to?
      """

      # Get the current config
      config = vm.config.vAppConfig
      configSpec = pyVmomi.Vim.vApp.VmConfigSpec()

      for prop in config.property:
         if prop.id in values:
            # XXX: No type checking right now.
            propSpec = pyVmomi.Vim.vApp.PropertySpec()
            propSpec.operation = 'edit'
            propSpec.info = prop
            propSpec.info.value = values[prop.id]

            configSpec.property.append(propSpec)

      # Send it back.
      spec = pyVmomi.Vim.Vm.ConfigSpec()
      spec.vAppConfig = configSpec

      task = vm.Reconfigure(spec)
      self.WaitTask(task, 'VM OVF reconfigure')

   def DatastoreListDirectory(self, dsName, path, dcName=None):
      """
         Results are in the form of
         { f1: (f1_isdir, f1_size), f2: (f2_isdir, f2_size), ...}
      """
      try:
         ds = self.GetDatastore(dsName)
      except VIException:
         # SUPER XXX: For some reason the datastore names are all messed up
         # on Workstation. If we wanted datastore name 'standard' and failed,
         # try 'Datastore-1'. To be clear: TEMPORARY FIX FOR M5 ONLY.
         if dsName == 'standard':
            ds = self.GetDatastore('Datastore-1')
         else:
            raise

      browser = ds.browser

      spec = pyVmomi.Vim.Host.DatastoreBrowser.SearchSpec()
      spec.details = pyVmomi.Vim.Host.DatastoreBrowser.FileInfo.Details()

      spec.details.fileSize = True
      spec.details.fileType = True
      spec.details.modification = False
      spec.details.fileOwner = False

      dsPath = self.MakePath(dsName, path)

      task = browser.Search(dsPath, spec)
      self.WaitTask(task, 'datastore search', hideResult=True)

      # WaitTask will raise exception so if we are here everything is OK.
      return dict([(f.path, (isinstance(f, pyVmomi.Vim.Host.DatastoreBrowser.FolderInfo), f.fileSize)) for f in task.info.result.file])

   def DatastorePathExists(self, dsName, path, dcName=None, restrict=None):
      log.debug("Checking if '[%s] %s' exists on the server." % (dsName, path))

      # Get the directory listing. We can't query files directly.
      # If we get a FileNotFound we just return False. (This means the dirname
      # did not exist at all.)
      parent = os.path.dirname(path)

      try:
         files = self.DatastoreListDirectory(dsName, parent)
      except pyVmomi.Vim.Fault.FileNotFound:
         log.info('directory %s not present on datastore', parent)
         return False

      base = os.path.basename(path)

      if base in files:
         if restrict == VIConnection.RESTRICT_DIR:
            return files[base][0]
         elif restrict == VIConnection.RESTRICT_FILE:
            return not files[base][0]
         return True

      return False

   # Convenience functions for the above
   def DatastoreFileExists(self, dsName, path, dcName=None):
      return self.DatastorePathExists(dsName, path, dcName,
                                      restrict=VIConnection.RESTRICT_FILE)

   def DatastoreDirExists(self, dsName, path, dcName=None):
      return self.DatastorePathExists(dsName, path, dcName,
                                      restrict=VIConnection.RESTRICT_DIR)

   def DatastoreMkdir(self, dsName, path):
      try:
         self.instance.content.fileManager.MakeDirectory(self.MakePath(dsName, path), self.GetDatacenter())
      except pyVmomi.Vim.Fault.FileAlreadyExists:
         log.debug('Directory %s already exists on server, nothing to do.' % path)
      return True

   def DatastoreUpload(self, dsName, localPath, remotePath, dcName=None, size=None, uploadCb=None):
      # Create a bare data connection using httplib to the server
      dcName = self.GetDefaultDatacenterName(dcName)
      path = '/folder/' + urllib.pathname2url(remotePath)
      params = urllib.urlencode((('dcPath', dcName), ('dsName', dsName)))

      # So that we don't have to catch a 401 error and send authorization
      # after one occurs, just bake the authorization in beforehand.
      authdata = base64.b64encode('%s:%s' % (self.vcInfo.vcUsername, self.vcInfo.vcPassword))

      # Calculate size using stat unless it is passed in. If you're passing
      # in a file that cannot be sized using stat, make sure to pass in a size
      # or we will fall over here.
      if size is None:
         size = os.stat(localPath)[stat.ST_SIZE]

      conn = VIHTTPSConnection(self.vcInfo.vcHost, uploadCb)
      request = '%s?%s' % (path, params)

      log.debug('Issuing PUT request: %s', request)
      with closing(conn) as c:
         c.putrequest('PUT', '%s?%s' % (path, params))
         c.putheader('User-Agent', VIConnection.USER_AGENT)
         c.putheader('Content-Type', 'application/octet-stream')
         c.putheader('Content-Length', size)
         c.putheader('Authorization', 'Basic %s' % authdata)
         c.endheaders()

         with open(localPath, 'rb') as source:
            c.sendFile(source)

         resp = c.getresponse()

      log.debug('Upload status: %s', resp.status)

      # CREATED will be returned for new files, OK for overwritten ones
      return resp.status in (httplib.CREATED, httplib.OK)

   def DatastoreDownload(self, dsName, remotePath, localPath, dcName=None):
      opener = self._GetDatastoreOpener()
      url = self._GetDatastoreURL(dcName, dsName, remotePath)
      log.debug('Fetching URL %s' % url)
      req = urllib2.Request(url)

      log.info("Downloading file '%s' from datastore '%s'." % (remotePath, dsName))
      try:
         CHUNK = 1048576

         src = opener.open(req)
         with open(localPath, 'wb') as fp:
            while True:
               chunk = src.read(CHUNK)
               if not chunk: break
               fp.write(chunk)
      except urllib2.HTTPError:
         log.exception('Unable to download URL %s', url)
         return False

      return True

   def DatastoreDelete(self, dsName, path, dcName=None):
      fileManager = self.instance.content.fileManager
      datacenter = self.GetDatacenter(dcName)

      task = fileManager.DeleteDatastoreFile(self.MakePath(dsName, path),
                                             datacenter)

      # Note: if something screws up, this throws an exception.
      self.WaitTask(task, 'deletion of file %s' % path)

   def _WaitHelper(self, pred, vm, maxSleepTime, failDelete, reason, remainConstant=0):
      curSleepTime = 0
      interval     = 10
      iterations   = 0
      maxIteration = math.ceil(remainConstant / (1.0 * interval))
      name         = vm.name

      while True:
         if pred(vm):
            # This catches the case where remainConstant == 0.
            if iterations >= maxIteration - 1:
               log.debug('Predicate for %s on %s stayed true for %d seconds, returning success.' % (reason, name, remainConstant))
               return True
            else:
               iterations += 1
         else:
            iterations = 0

         time.sleep(interval)

         if iterations == 0:
            curSleepTime += interval
            if curSleepTime >= maxSleepTime:
               # Something is wrong; power off the VM, trash it, then
               # raise an exception.
               if failDelete:
                  task = vm.PowerOff()
                  self.WaitTask(task, 'VM power-off')
                  vm.Destroy()
               raise VIException('%s timed out after %d seconds' % (reason, maxSleepTime))

   def _GetFileNamesFromIndex(self, html):
      parser = VCIndexParser()
      parser.feed(html)

      return parser.GetIndexEntries()

   def _GetDatastoreOpener(self):
      log.debug('Authenticating with username %s' % self.vcInfo.vcUsername)

      pwMgr = urllib2.HTTPPasswordMgrWithDefaultRealm()
      pwMgr.add_password(None, 'https://%s/folder' % self.vcInfo.vcHost,
                         self.vcInfo.vcUsername, self.vcInfo.vcPassword)

      handler = urllib2.HTTPBasicAuthHandler(pwMgr)
      opener = urllib2.build_opener(handler, urllib2.HTTPSHandler(debuglevel=0))
      opener.addheaders = [('User-Agent', VIConnection.USER_AGENT)]
      return opener

   def _GetDatastoreURL(self, dcName, dsName, path):
      dcName = self.GetDefaultDatacenterName(dcName)
      dcName = dcName.replace(' ', '%20')
      dsName = dsName.replace(' ', '%20')
      path = path.replace(' ', '%20')

      # All paths are relative to root. So absolute paths must be trimmed.
      path = path.lstrip('/')

      url = 'https://%s/folder/%s?dcPath=%s&dsName=%s' % (self.vcInfo.vcHost, path, dcName, dsName)
      return url

   def _FindAll(self, root, objType, pathSet=()):
      viewMgr = self.instance.content.viewManager

      # Start iteration at the VM folder
      viewRef = viewMgr.CreateContainerView(container=root, recursive=True)
      collector = self.instance.content.propertyCollector

      # Create ObjectSpec to begin traversal
      oSpec = pyVmomi.Vmodl.Query.PropertyCollector.ObjectSpec()
      oSpec.obj = viewRef
      oSpec.skip = True

      tSpec = pyVmomi.Vmodl.Query.PropertyCollector.TraversalSpec()
      tSpec.name = 'traverseEntities'
      tSpec.path = 'view'
      tSpec.skip = False
      tSpec.type = pyVmomi.Vim.View.ContainerView
      oSpec.selectSet = [tSpec]

      pSpec = pyVmomi.Vmodl.Query.PropertyCollector.PropertySpec()
      pSpec.type = objType
      if len(pathSet) == 0:
         log.warning('No pathSet passed in, very expensive query.')
         pSpec.all = True
      pSpec.pathSet = list(pathSet)

      fSpec = pyVmomi.Vmodl.Query.PropertyCollector.FilterSpec()
      fSpec.objectSet = [oSpec]
      fSpec.propSet = [pSpec]

      props = collector.RetrieveContents([fSpec])

      # Parse this into a simpler list of tuples.
      # [(vmObj1, {'prop1': 'val1', 'prop2': 'val2'}), ...]
      ret = []
      for obj in props:
         properties = {}
         for prop in obj.propSet:
            properties[prop.name] = prop.val

         ret.append((obj.obj, properties))

      return ret

class VIHTTPSConnection(httplib.HTTPSConnection):
   """
      Code which allows progress reporting in a HTTPSConnection when using the
      new sendFile method that allows you to use a file pointer as a source
      instead of a string buffer.
   """
   def __init__(self, host, uploadCb=None, *args, **kwargs):
      httplib.HTTPSConnection.__init__(self, host, *args, **kwargs)
      self.uploadCb = uploadCb

   def sendFile(self, fp):
      # Start with a small chunk size. If it takes less than 2 seconds to
      # upload that chunk, then double it. If it takes more than 4 seconds,
      # halve it (until a base size of 128K.) There is no upper limit.
      CHUNK_SMALLEST = 128 * 1024
      CHUNK_LO_INTERVAL = 2
      CHUNK_HI_INTERVAL = 4

      chunkSize = 1024 * 1024
      bytesSent = 0

      while True:
         buf = fp.read(chunkSize)

         if buf == '':
            break

         start = time.time()
         httplib.HTTPSConnection.send(self, buf)
         end = time.time()
         bytesSent += len(buf)

         if end - start < CHUNK_LO_INTERVAL:
            chunkSize *= 2
            log.debug('Increase chunk size to %d', chunkSize)
         elif end - start > CHUNK_HI_INTERVAL and chunkSize > CHUNK_SMALLEST:
            chunkSize /= 2
            log.debug('Decrease chunk size to %d', chunkSize)

         if self.uploadCb:
            self.uploadCb(bytesSent)
