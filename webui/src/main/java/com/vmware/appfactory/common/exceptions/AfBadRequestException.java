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

package com.vmware.appfactory.common.exceptions;

import com.vmware.thinapp.common.exception.BaseException;

/**
 * Exception thrown from request mappings to indicate
 * an invalid request (400)
 */
public class AfBadRequestException
   extends BaseException
{
   /** Auto generated */
   private static final long serialVersionUID = -2286762590601968344L;

   /**
    * @param msg
    */
   public AfBadRequestException(String msg) {
      super(msg);
   }

   /**
    * @param cause
    */
   public AfBadRequestException(Throwable cause) {
      super(cause);
   }

   /**
    *
    * @param message
    * @param cause
    */
   public AfBadRequestException(String message, Throwable cause) {
      super(message, cause);
  }
}
