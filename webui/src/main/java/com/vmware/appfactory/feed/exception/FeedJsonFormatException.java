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

package com.vmware.appfactory.feed.exception;

import org.codehaus.jackson.JsonNode;

import com.vmware.thinapp.common.exception.BaseException;

/**
 * Exception class used to report invalid or badly formatted data in
 * an application feed.
 */
public class FeedJsonFormatException
   extends BaseException
{
   private static final long serialVersionUID = 1L;

   private final JsonNode _node;


   /**
    * Create a new instance.
    *
    * @param node
    * @param message
    */
   public FeedJsonFormatException(JsonNode node, String message)
   {
      super(message);
      _node = node;
   }


   /**
    * Create a new instance.
    *
    * @param node
    * @param message
    * @param reason
    */
   public FeedJsonFormatException(
         JsonNode node,
         String message,
         Throwable reason)
   {
      super(message, reason);
      _node = node;
   }


   /**
    * Get the JSON node which triggered the exception.
    * @return The JSON node which triggered the exception.
    */
   public JsonNode getNode()
   {
      return _node;
   }
}
