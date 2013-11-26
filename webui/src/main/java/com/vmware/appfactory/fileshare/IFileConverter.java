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

package com.vmware.appfactory.fileshare;

import jcifs.smb.NtlmPasswordAuthentication;

import com.vmware.appfactory.datasource.DataSourceObject;

/**
 * This interface defines a convert operation on how to parse the directory layout
 * of the application installers.
 *
 * XXX Rename to IDataSourceObjectConverter when all changes are done.
 *
 * @author saung
 * @since M7 7/22/2011
 */
public interface IFileConverter<T extends DataSourceObject> {
   /**
    * Convert the installer path into an application instance.
    *
    * @param auth Authentication object currently being used.
    * @param installerName An installer name.
    * @param fullPath A full path to the installer.
    * @param parentDirs One or more parent directories.
    */
   public T convert(
         NtlmPasswordAuthentication auth,
         String installerName,
         String fullPath,
         String... parentDirs);

}
