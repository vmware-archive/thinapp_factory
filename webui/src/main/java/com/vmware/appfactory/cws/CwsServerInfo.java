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

package com.vmware.appfactory.cws;

/**
 * A call to getServerInfo() will receive and return data in this format.
 */
public final class CwsServerInfo
{
   /**
    * Server date/time in UTC, format: YYYY-MM-DDTHH:MM:SSZ
    */
   public String date;

   /**
    * Uptime of the server in seconds
    */
   public long uptime;

   /**
    * UUID generated at every boot
    */
   public String boot_id;
}