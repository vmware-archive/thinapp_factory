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

package com.vmware.appfactory.common;


/**
 * Used as a key to identify an application.
 * This interface exists to ensure that anything that claims to identify itself
 * as an application, or claims to identify applications, adheres to the
 * scheme we have chosen for uniquely identifying applications.
 */
public interface ApplicationKey
{
   /**
    * Get the name of the application.
    * @return
    */
   public String getName();

   /**
    * Get the version of the application.
    * @return
    */
   public String getVersion();

   /**
    * Get the locale of the application.
    * Could be null or empty.
    * @return
    */
   public String getLocale();

   /**
    * Get the installer revision of the application.
    * Could be null or empty.
    * @return
    */
   public String getInstallerRevision();
}
