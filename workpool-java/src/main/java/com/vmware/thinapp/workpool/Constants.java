/* ***********************************************************************
 * VMware ThinApp Factory
 * Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ***********************************************************************/

package com.vmware.thinapp.workpool;

/**
 * Commonly used constants.
 */
public class Constants {
   /** Default guest username. */
   static final String GUEST_USERNAME = "administrator";

   /** Default guest password. */
   static final String GUEST_PASSWORD = "thinstalled!";

   /** Snapshot name of the clean snapshot created after instancing a VM. */
   static final String THINAPP_CLEAN_SNAPSHOT = "ThinApp Clean Snapshot";

   /** Description for the clean snapshot created after instancing a VM. */
   static final String THINAPP_CLEAN_SNAPSHOT_DESCRIPTION = "Clean base snapshot";

   /** Snapshot used on images to create clones off of. */
   static final String THINAPP_CLONING_SNAPSHOT = "ThinApp Cloning Snapshot";
}