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

package com.vmware.appfactory.common.dto;

public class WorkpoolAndImageFailCount
{
   private int _wpFailCount;

   private int _vmImageFailCount;

   private long _timestamp;


   /**
    * This is the only way to set values while creating this object.
    * @param wpFailCount
    * @param imgFailCount
    * @param timestamp
    */
   public WorkpoolAndImageFailCount(int wpFailCount, int imgFailCount, long timestamp)
   {
      this._wpFailCount = wpFailCount;
      this._vmImageFailCount = imgFailCount;
      this._timestamp = timestamp;
   }


   /**
    * @return the wpFailCount
    */
   public int getWpFailCount()
   {
      return _wpFailCount;
   }


   /**
    * @return the vmImageFailCount
    */
   public int getVmImageFailCount()
   {
      return _vmImageFailCount;
   }


   /**
    * @return the _timestamp
    */
   public long getTimestamp()
   {
      return _timestamp;
   }
}