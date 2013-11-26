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

import com.vmware.thinapp.common.exception.BaseException;

/**
 * Exception for handling all failures from the feed converter.
 *
 * @author saung
 * @since v1.0 4/19/2011
 */
public class FeedConverterException
   extends BaseException
{
   private static final long serialVersionUID = -1388133462325873656L;

   private final FeedConverterErrorCode _errorCode;

   /**
    * @param message
    */
   public FeedConverterException(FeedConverterErrorCode code, String message) {
      super(message);
      _errorCode = code;
   }

   /**
    * @param cause
    */
   public FeedConverterException(FeedConverterErrorCode code, Throwable cause) {
      super(cause);
      _errorCode = code;
   }

   public FeedConverterErrorCode getErrorCode() {
      return _errorCode;
   }
}
