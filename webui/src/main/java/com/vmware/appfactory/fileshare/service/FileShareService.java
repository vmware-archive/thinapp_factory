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

package com.vmware.appfactory.fileshare.service;

import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfForbiddenException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.datasource.AppsAndRecipes;
import com.vmware.appfactory.fileshare.dto.FileShareRequest;
import com.vmware.appfactory.fileshare.dto.FileShareSyncResponses;
import com.vmware.appfactory.fileshare.model.FileShare;

/**
 * An interface to define all file share related services.
 *
 * @author saung
 * @since M7 7/26/2011
 */
public interface FileShareService
{
   /**
    * Scan a given file share.
    *
    * @param fileshare - a file share to be scanned.
    * @return Applications and recipes.
    * @throws AfNotFoundException
    * @throws AfForbiddenException
    * @throws AfServerErrorException
    */
   public AppsAndRecipes scan(FileShare fileshare)
      throws AfNotFoundException, AfForbiddenException, AfServerErrorException;

   /**
    * Re-scan an existing file share.
    *
    * @param request - a file share request
    * @return a FileShareSyncResponses instance.
    * @throws AfNotFoundException
    * @throws AfConflictException
    * @throws AfForbiddenException
    * @throws AfServerErrorException
    */
   public FileShareSyncResponses sync(FileShareRequest request)
      throws AfNotFoundException, AfConflictException, AfForbiddenException, AfServerErrorException;

   /**
    * Create a new file share and store all installers found in the
    * file share.
    *
    * @param request - a file share request.
    * @return a file share instance.
    * @throws AfServerErrorException
    * @throws AfForbiddenException
    */
   public FileShare createFileShare(FileShareRequest request)
      throws AfServerErrorException, AfForbiddenException;

   /**
    * Update an existing file share settings and its associated
    * applications' meta-data.
    *
    * @param request - a file share request.
    *  @return a file share instance.
    * @throws AfNotFoundException
    * @throws AfConflictException
    * @throws AfForbiddenException
    * @throws AfServerErrorException
    */
   public FileShare updateFileShare(FileShareRequest request)
      throws AfNotFoundException, AfConflictException, AfForbiddenException, AfServerErrorException;

   /**
    * Delete an existing file share.
    *
    * @param fileShareId - a valid file share id.
    * @return a file share instance that just deleted.
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    * @throws AfServerErrorException
    */
   public FileShare deleteFileShare(Long fileShareId)
      throws AfNotFoundException, AfBadRequestException, AfServerErrorException;

}
