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

package com.vmware.thinapp.common.vi.util;

import java.util.Arrays;
import java.util.List;

/**
 * This is a constants file containing constants that could be used. They
 * reduce dynamic values all over the code.
 *
 * @author Keerthi Singri
 * @since M8, August 20, 2011
 */
public class VIConstants
{
   /**
    * The following constants help avoid vijava API Faults for invalidType

    */
   public static final String DATASTORE         = "Datastore";
   public static final String DATACENTER        = "Datacenter";
   public static final String FOLDER            = "Folder";
   public static final String VIRTUAL_MACHINE	= "VirtualMachine";
   public static final String COMPUTE_RESOURCE  = "ComputeResource";
   public static final String NETWORK           = "Network";

   /**
    * These are constants that are used to represent the key value pairs
    * that are passed as part of VINode custom properties
    */
   public static final String FILE_SIZE         = "fileSize";
   public static final String LAST_MODIFIED     = "lastModified";
   public static final String GUEST_FULLNAME    = "guestFullName";
   public static final String GUEST_ID          = "guestId";

   /**
    * List of guestId's that are supported by TAF
    * @see http://pubs.vmware.com/vsphere-50/index.jsp?topic=%2Fcom.vmware.wssdk.apiref.doc_50%2Fvim.vm.GuestOsDescriptor.GuestOsIdentifier.html
    */
   public static final List<String> GUEST_ID_SUPPOTRED = Arrays.asList(
         "winvista64Guest", "windows7_64Guest", "windows8_64Guest", "winXPPro64Guest",
         "winvistaGuest", "windows7Guest", "windows8Guest", "winXPProGuest");
}
