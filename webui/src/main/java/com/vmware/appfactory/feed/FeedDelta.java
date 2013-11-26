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

package com.vmware.appfactory.feed;


/**
 * Describes a difference between a remote feed and our current picture of it.
 * This will be either new, changed, or deleted data.
 */
public class FeedDelta<T>
{
   /**
    * Type of change detected.
    */
   public static enum Action
   {
      /** New data appears in the feed. */
      ADD,
      /** Data no longer appears in the feed. */
      DELETE,
      /** Data in the feed has changed. */
      CHANGE
   }

   private final T _data;

   private final Action _action;


   /**
    * Create a new AfFeedDelta instance.
    * @param data
    * @param action
    */
   public FeedDelta(T data, Action action)
   {
      _data = data;
      _action = action;
   }


   /**
    * Get the data affected by the change.
    * @return
    */
   public T getData()
   {
      return _data;
   }


   /**
    * Get the type of change.
    * @return
    */
   public Action getAction()
   {
      return _action;
   }
}
