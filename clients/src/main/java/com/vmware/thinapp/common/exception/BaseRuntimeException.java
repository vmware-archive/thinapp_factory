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
 * This is the base runtime exception class that should be used across all
 * of com.vmware.* packages instead of java.lang.RuntimeException. This class
 * provides a higher level abstraction for Runtime exceptions. This abstraction
 * can be made use of by the exceptionHandlers, custom loggers, etc.
 *
 * @author Keerthi Singri
 * @see BaseException
 */
public class BaseRuntimeException extends RuntimeException {

   private static final long serialVersionUID = 987654321L;

   /**
    * @param message
    */
   public BaseRuntimeException(String message) {
      super(message);
   }

   /**
    * @param cause
    */
   public BaseRuntimeException(Throwable cause) {
      super(cause.getMessage(), cause);
   }

   /**
    *
    * @param message
    * @param cause
    */
   public BaseRuntimeException(String message, Throwable cause) {
      super(message, cause);
  }

}
