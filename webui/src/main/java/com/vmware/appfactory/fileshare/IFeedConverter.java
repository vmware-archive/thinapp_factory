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

import java.util.List;

import com.vmware.appfactory.datasource.DataSourceObject;
import com.vmware.appfactory.fileshare.exception.FeedConverterException;

/**
 * This interface defines all key operations of an "exit16" CIFS fileshare
 * which can contain data source objects (applications or recipes).
 *
 * @author saung
 * @since v1.0 4/5/2011
 */
public interface IFeedConverter<T extends DataSourceObject>
{
   /**
    * Scan all files from CIFS shared mount and return a list of data
    * source items.
    *
    * @param smbUrl - a Samba Url that starts with 'smb://' and ends with a trailing slash '/'.
    * @param group - a group/domain name (e.g. WORKGROUP)
    * @param username - a valid smb username.
    * @param password - a valid smb password.
    * @param appTimeStamp - a timestamp to set for "lastRemoteUpdate" attribute in all applications.
    * @param cwsDataStoreId - an unique CWS data store ID.
    * @return a list of successfully scanned objects.
    * @throws FeedConverterException if any failure happened while crawling CIFS share folders.
    */
   public List<T> scanObjects(
         String smbUrl,
         String group,
         String username,
         String password,
         long appTimeStamp,
         Long cwsDataStoreId)
      throws FeedConverterException;


}
