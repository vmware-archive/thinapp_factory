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

package com.vmware.appfactory.fileshare.dao;

import com.vmware.appfactory.common.dao.AfDao;
import com.vmware.appfactory.fileshare.model.FileShare;

/**
 * Interface for dealing with AfFileShare objects.
 * Custom methods that do not apply to AfRecord instances in general are declared here.
 */
public interface FileShareDao extends AfDao<FileShare> {
   /**
    * Find an existing file share with the given name.
    * If found, it is returned, else null.
    *
    * @param name
    * @return
    */
   public FileShare findByName(String name);

   /**
    * Find a file share with the given datastore id.
    *
    * @param id - a datastore Id.
    * @return an instance of file share.
    */
   public FileShare findByDatastoreId(Long id);

}
