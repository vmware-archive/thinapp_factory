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

package com.vmware.appfactory.cws.exception;

import com.vmware.thinapp.common.exception.BaseException;

/**
 * An exception to report errors with any part of the CWS client.
 */
public class CwsException
   extends BaseException
{
   private static final long serialVersionUID = 1L;

   /**
    * @param cause
    */
   public CwsException(Throwable cause)
   {
      super(cause);
   }

   /**
    * @param string
    */
   public CwsException(String string)
   {
      super(string);
   }
}
