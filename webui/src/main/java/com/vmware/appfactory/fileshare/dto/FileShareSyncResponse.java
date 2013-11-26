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

package com.vmware.appfactory.fileshare.dto;

import java.util.List;

import com.vmware.appfactory.datasource.DataSourceObject;

/**
 * A DTO to hold a list of new, updated and deleted data source objects in
 * lists.
 *
 * @author saung
 * @since M8 08/04/2011
 */
public class FileShareSyncResponse<T extends DataSourceObject> {
   private final List<T> _newItems;
   private final List<T> _updatedItems;
   private final List<T> _deletedItems;

   /**
    * @param newItems
    * @param updatedItems
    * @param deletedItems
    */
   public FileShareSyncResponse(
         List<T> newItems,
         List<T> updatedItems,
         List<T> deletedItems) {
      super();
      _newItems = newItems;
      _updatedItems = updatedItems;
      _deletedItems = deletedItems;
   }

   /**
    * @return the newItems
    */
   public List<T> getNewItems() {
      return _newItems;
   }

   /**
    * @return the updatedItems
    */
   public List<T> getUpdatedItems() {
      return _updatedItems;
   }

   /**
    * @return the deletedItems
    */
   public List<T> getDeletedItems() {
      return _deletedItems;
   }

   /**
    * Get number of new items.
    * @return
    */
   public int getNumNewItems() {
      return (_newItems == null) ? 0 : getNewItems().size();
   }

   /**
    * Get number of updated items.
    * @return
    */
   public int getNumUpdatedItems() {
      return (_updatedItems == null) ? 0 : getUpdatedItems().size();
   }

   /**
    * Get number of deleted items.
    * @return
    */
   public int getNumDeletedItems() {
      return (_deletedItems == null) ? 0 : getDeletedItems().size();
   }

}
