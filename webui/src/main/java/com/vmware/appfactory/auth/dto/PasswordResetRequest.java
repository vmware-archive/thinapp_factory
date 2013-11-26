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

package com.vmware.appfactory.auth.dto;

import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * This class handles password request form.
 */
public class PasswordResetRequest {
   public String oldPassword;
   public String newPassword;

   /**
    * Validate the request.
    * Rules:
    * - The old password could be empty, but the new password.
    * - The new password cannot be the same as the old password.
    * @throws InvalidDataException
    */
   public void validate() throws AfBadRequestException {
      if (AfUtil.anyEmpty(newPassword)) {
         throw new AfBadRequestException("New password cannot be empty!");
      }
      if (newPassword.equals(oldPassword)) {
         throw new AfBadRequestException("New password must be different from old password!");
      }
   }
}
