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

package com.vmware.appfactory.datastore;

import java.net.URI;
import java.net.URISyntaxException;

import com.vmware.appfactory.file.FileHelper;
import com.vmware.thinapp.common.datastore.dto.Datastore;

/**
 * Datastore specific helpers / utilities are defined here.
 *
 * @author Keerthi Singri
 * @since 5/11/2011
 */
public class DsUtil
{
   /**
    * CWS web service data store URL Scheme.
    * TODO: Wherever this is used, you can probably use a URI
    */
   public static final String DATASTORE_URI_SCHEME = "datastore";

   /**
    * CWS web service data store file separator in the download url.
    */
   public static final String FILE_SEPARATOR = "/";

   /**
    * 'internal' datastore name.
    */
   public static final String INTERNAL = "internal";


   /**
    * Generates URI to a datasource file.
    * @param dsId
    * @param pathList
    * @return
    * @throws URISyntaxException
    */
   public static URI generateDatastoreURI(
         Long dsId,
         String... pathList)
      throws URISyntaxException
   {
      return new URI(
            DATASTORE_URI_SCHEME,
            dsId.toString(),
            FileHelper.constructFilePath2(FILE_SEPARATOR, pathList),
            null);
   }


   public static DsDatastore[] fromDTO(Datastore[] dtos)
   {
      DsDatastore[] array = new DsDatastore[dtos.length];

      for (int i = 0; i < dtos.length; i++) {
         array[i] = fromDTO(dtos[i]);
      }
      return array;
   }


   /**
    * Convert a common Datastore DTO into an implementation of the more
    * elaborate DsDatastore interface.
    * @param dto
    * @return
    */
   public static DsDatastore fromDTO(Datastore dto)
   {
      Datastore.Type type = dto.getType();
      if (null == type) {
         type = Datastore.Type.cifs;
      }
      switch(type) {
         case cifs:
            return new DsDatastoreCifs(dto);
      }

      return null;
   }
}
