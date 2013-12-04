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


class Dispositions:
   SUCCEEDED = 'succeeded'
   FAILED = 'failed'

class Events:
   CREATED = 'created'
   QUEUED = 'queued'
   DOWNLOADING = 'downloading'
   PROVISIONING = 'provisioning'
   CONVERTING = 'converting'
   FINISHED = 'finished'
   CANCELLING = 'cancelling'
   CANCELLED = 'cancelled'

class Projects:
   CREATED = 'created'
   AVAILABLE = 'available'
   DIRTY = 'dirty'
   REBUILDING = 'rebuilding'
   DELETING = 'deleting'
   DELETED = 'deleted'
   TYPE_REGKEY = 'regkey'
   TYPE_REGVALUE = 'regvalue'
   TYPE_FILE = 'file'
   AVAILABLE_STATES = (AVAILABLE, DIRTY)
   DELETABLE_STATES = (CREATED, AVAILABLE, DIRTY)
   CHANGEABLE_STATES = (CREATED, AVAILABLE, DIRTY, DELETED)
