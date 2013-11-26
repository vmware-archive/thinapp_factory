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

package com.vmware.appfactory.common.base;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.common.dao.AfDao;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;


/**
 * API Controllers provide a RESTful HTML interface to AppFactory using
 * JSON-formatted messages.
 *
 * All URLs to these controllers start with "/api" to distinguish them from
 * URLs that provide the web-based user interface.
 *
 */
@RequestMapping("/api")
public abstract class AbstractApiController
   extends AbstractController
{
   private final HashFunction hashFunction = Hashing.goodFastHash(128);

   /**
    * Sets an e-tag on the request calculated from the given DAO.
    *
    * If the request contains the same e-tag, this means the resource
    * has not been modified since the last request, and the browser cache
    * already contains the whole response.
    *
    * In this case, we'll set the HTTP headers to indicate that the
    * response has not been modified, and return true.
    *
    * @param request    - web request, used to get client's etag (if any)
    * @param response   - web response, used to set 304 response if necessary
    * @param objCollection - A collection of objects. If set, the e-tag is computed based on the hashCode
    *                      for each of the objects.
    * @param daoList    - one or more DAOs to query.  The e-tag is computed using the current state of each
    *                      of these tables, such that if any changes the computed e-tag should change.
    * @return
    * true - if the client already has a cached version of the resource.
    * When this is returned, this method has written an HTTP response,
    * and the caller should simply return immediately.
    *
    * false - if the E-Tag header has been set and the caller should
    * proceed to generate and write a normal response.
    *
    * @throws IOException
    * if the 304 response could not be written
    */
   public boolean checkModified(@Nonnull HttpServletRequest request,
                                @Nonnull HttpServletResponse response,
                                Collection<? extends Object> objCollection,
                                @Nonnull AfDao... daoList) throws IOException {

      String charSequence = "optimus-prime";
      Hasher hasher = hashFunction.newHasher().putString(charSequence);

      for (AfDao dao: daoList) {
         long lastModified = dao.lastModified();
         long size = dao.countAll();
         hasher.putLong(lastModified)
               .putLong(size);
      }

      // If the objects are not present, put a 0 value indicating 0 sized objects.
      if (CollectionUtils.isNotEmpty(objCollection)) {
         for (Object o: objCollection) {
            if (o != null) {
               hasher.putInt(o.hashCode());
            } else {
               // Indicating that there was an object but null.
               hasher.putInt(0);
            }
         }
      }

      String newToken = hasher.hash().toString();

      String oldToken = applyETagCache(request,response, newToken, false);
      return newToken.equals(oldToken);
   }

   /**
    * Process a request for a cached icon resource.
    *
    * @param entityId id of the application or build whose icon is being requested
    * @param iconId index of icon to access
    * @param iconHash hash of the icon to access
    * @param dao
    * @param request
    * @param response
    * @return binary data for the icon or null if the data has not changed
    * @throws AfNotFoundException
    * @throws IOException
    */
   protected byte[] processIconRequest(
         Long entityId,
         Integer iconId,
         String iconHash,
         AfDao<? extends AbstractApp> dao,
         HttpServletRequest request,
         HttpServletResponse response) throws AfNotFoundException, IOException {
      /* Get the abstract app */
      AbstractApp abstractApp = dao.find(entityId);

      if (abstractApp == null) {
         throw new AfNotFoundException(String.format("No such entity id %d.", entityId));
      }

      if (abstractApp.getIcons() == null || iconId < 0 || iconId >= abstractApp.getIcons().size()) {
         throw new AfNotFoundException(
               String.format("No such icon id %d for entity id %d.", iconId, entityId));
      }

      AfIcon icon = abstractApp.getIcons().get(iconId);

      if (icon.getIconBytes() == null) {
         throw new AfNotFoundException(
               String.format("No icon data for icon id %d of entity id %d.", iconId, entityId));
      }

      if (icon.getIconHash() == null || !icon.getIconHash().equals(iconHash)) {
         throw new AfNotFoundException(
               String.format("No hash matching %s for icon id %d of entity id %d.", iconHash, iconId, entityId));
      }

      response.setContentType(icon.getContentType());

      // Set the proper caching HTTP headers
      String requestHash = applyETagCache(request, response, iconHash, true);

      // Return no message body if the icon data hasn't changed
      if (iconHash.equals(requestHash)) {
         return null;
      }
      return icon.getIconBytes();
   }
}
