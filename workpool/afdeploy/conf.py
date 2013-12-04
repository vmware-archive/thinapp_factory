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

# Config sections
SECTION_LOCAL_ENV = 'LocalEnvironment'
SECTION_VC_ENV = 'VCEnvironment'
SECTION_TEMPLATE = 'Template'

# Config keys
VC_HOST = 'VirtualMachineHost'
VC_USERNAME = 'HostLoginUserName'
VC_PASSWORD = 'HostLoginPassword'
VC_DC_NAME = 'Datacenter'
TEMPLATE_WORKER_TYPE = 'WorkerType'
TEMPLATE_COMPUTE_RESOURCE = 'ComputeResource'
TEMPLATE_RESOURCE_POOL = 'ResourcePool'
TEMPLATE_VM_NAME = 'Name'
TEMPLATE_VM_DS = 'Datastore'
TEMPLATE_VM_MEMORY = 'MemoryMB'
TEMPLATE_VM_DISK = 'DiskMB'
TEMPLATE_VM_GUESTID = 'GuestID'
TEMPLATE_VM_GUESTVARIANT = 'GuestVariant'
TEMPLATE_VM_INITIAL = 'InitialDeployCount'
TEMPLATE_VM_MAX = 'MaxDeployCount'
TEMPLATE_GUEST_USER = 'GuestUser'
TEMPLATE_GUEST_PASS = 'GuestPassword'
TEMPLATE_ISO_PATH = 'IsoPath'
TEMPLATE_OS_KEY = 'LicenseKey'
TEMPLATE_KMS_SERVER= 'KmsServer'
TEMPLATE_NET_NAME = 'Network'
LOCAL_ISO_TMP_DIR = 'IsoTmpDir'

# Other constant strings we care about
CLONE_SNAP_NAME = 'ThinApp Cloning Snapshot'
CLONE_NETBIOS_PREFIX = 'thinconv'
CLONE_WORKGROUP = 'thinapp'
CLONE_USER_FULLNAME = 'Converter User'
CLONE_USER_ORGNAME = 'VMware, Inc.'
CLONE_CLEAN_SNAP_NAME = 'ThinApp Clean Snapshot'

VM_ANNOTATION_LINKED = 'appfactory-linked'
VM_ANNOTATION_LINKED_INSTALLING = 'appfactory-linked-installing'
VM_ANNOTATION_LINKED_SLAVE = 'appfactory-linked-slave'
VM_ANNOTATION_SINGLE = 'appfactory-single'
VM_ANNOTATION_FULL = 'appfactory-full'
VM_ANNOTATION_FULL_INSTALLING = 'appfactory-full-installing'
