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

package com.vmware.thinapp.common.exception;

/**
 * This is the base checked exception class that should be used across all
 * of com.vmware.* packages, instead of java.lang.Exception. This class
 * provides a higher level abstraction for all the exceptions.
 *
 * @author Keerthi Singri
 * @see BaseRuntimeException
 */
public class BaseException extends Exception {

   private static final long serialVersionUID = 123456789L;

   /**
    * @param message
    */
   public BaseException(String message) {
      super(message);
   }

   /**
    * @param cause
    */
   public BaseException(Throwable cause) {
      super(cause.getMessage(), cause);
   }

   /**
    *
    * @param message
    * @param cause
    */
   public BaseException(String message, Throwable cause) {
      super(message, cause);
  }

}
