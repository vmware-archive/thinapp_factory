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
 * An extension of ApplicationKey which allows fields to be changed.
 * @see ApplicationKey
 */
public interface MutableApplicationKey
   extends ApplicationKey
{
   /**
    * Set the name of the application.
    * @param name
    */
   public void setName(String name);

   /**
    * Set the version of the application.
    * @param version
    */
   public void setVersion(String version);

   /**
    * Set the locale of the application.
    * Could be null or empty.
    * @param locale
    */
   public void setLocale(String locale);

   /**
    * Set the installer revision of the application.
    * Could be null or empty.
    * @param rev
    */
   public void setInstallerRevision(String rev);
}
