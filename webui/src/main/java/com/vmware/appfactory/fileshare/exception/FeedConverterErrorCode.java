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

package com.vmware.appfactory.fileshare.exception;

import jcifs.smb.NtStatus;

public enum FeedConverterErrorCode {
   AccessDenied,
   NotFound,
   Other;

   /*
    * Convert one of the bajillion NT error codes defined in
    * jcifs.smb.NtStatus into one of our few.
    */
   public static FeedConverterErrorCode fromNtStatus(int ntStatus) {
      switch (ntStatus) {
         case NtStatus.NT_STATUS_ACCESS_DENIED:
         case NtStatus.NT_STATUS_ACCOUNT_DISABLED:
         case NtStatus.NT_STATUS_ACCOUNT_LOCKED_OUT:
         case NtStatus.NT_STATUS_ACCOUNT_RESTRICTION:
         case NtStatus.NT_STATUS_LOGON_FAILURE:
         case NtStatus.NT_STATUS_LOGON_TYPE_NOT_GRANTED:
         case NtStatus.NT_STATUS_NETWORK_ACCESS_DENIED:
         case NtStatus.NT_STATUS_PASSWORD_EXPIRED:
         case NtStatus.NT_STATUS_PASSWORD_MUST_CHANGE:
         case NtStatus.NT_STATUS_WRONG_PASSWORD:
            return AccessDenied;

         case NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND:
         case NtStatus.NT_STATUS_BAD_NETWORK_NAME:
         case NtStatus.NT_STATUS_INSTANCE_NOT_AVAILABLE:
         case NtStatus.NT_STATUS_NO_SUCH_DEVICE:
         case NtStatus.NT_STATUS_NO_SUCH_DOMAIN:
         case NtStatus.NT_STATUS_NO_SUCH_FILE:
         case NtStatus.NT_STATUS_NOT_FOUND:
         case NtStatus.NT_STATUS_OBJECT_PATH_NOT_FOUND:
            return NotFound;

         default:
            return Other;
      }
   }
}
