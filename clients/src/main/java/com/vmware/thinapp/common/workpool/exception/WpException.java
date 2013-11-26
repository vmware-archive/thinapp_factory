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

package com.vmware.thinapp.common.workpool.exception;

import com.vmware.thinapp.common.exception.BaseException;

/**
 * Class for all exceptions thrown from the workpool package.
 *
 * @author Keerthi Singri
 * @since M8, 15 August, 2011
 */
public class WpException
   extends BaseException
{
   private static final long serialVersionUID = 12322345L;

   /**
    * Create a new instance from the specified cause.
    * @param message
    */
   public WpException(String message)
   {
      super(message);
   }

   /**
    * @param cause
    */
   public WpException(Throwable cause) {
      super(cause);
   }

}
