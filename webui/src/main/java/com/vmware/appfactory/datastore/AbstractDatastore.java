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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.thinapp.common.datastore.dto.Datastore;

/**
 * This class implements most of the DsDatastore interface, but leaves
 * some of the more tricky bits to a more concrete subclass. It is basically
 * just a wrapper around the common Datastore DTO class.
 */
abstract class AbstractDatastore
   extends Datastore
   implements DsDatastore
{
   protected Logger _log = LoggerFactory.getLogger(getClass());

   /**
    * Create a new instance from basic data.
    * Do not call directly: use DsUtil.fromDTO() instead.
    * @see DsUtil#fromDTO
    *
    * @param uniqueName
    * @param server
    * @param share
    * @param domain
    * @param username
    * @param password
    */
   AbstractDatastore(
         String uniqueName,
         String server,
         String share,
         String domain,
         String username,
         String password,
         String mountPath)
   {
      setName(uniqueName);
      setServer(server);
      setShare(share);
      setDomain(domain);
      setUsername(username);
      setPassword(password);
      setMountPath(mountPath);
   }

   /**
    * Create a new instance from a DTO object.
    * Do not call directly: use DsUtil.fromDTO() instead.
    * @see DsUtil#fromDTO
    */
   AbstractDatastore(Datastore dto)
   {
      setType(dto.getType());
      setId(dto.getId());
      setName(dto.getName());
      setServer(dto.getServer());
      setPort(dto.getPort());
      setShare(dto.getShare());
      setDomain(dto.getDomain());
      setUsername(dto.getUsername());
      setPassword(dto.getPassword());
      setStatus(dto.getStatus());
      setSize(dto.getSize());
      setUsed(dto.getUsed());
      setBaseUrl(getBaseUrl());
      setLeases(getLeases());
      setMountPath(dto.getMountPath());
   }

   @Override
   public boolean isModifiable()
   {
      // CWS doesn't provide this flag, so infer it from the name
      // TODO: Remove this once Datastore supports implements it
      String name = getName();
      return name == null || (!name.equals("internal") && !name.equals("system"));
   }

   @JsonIgnore
   @Deprecated
   public void setModifiable(@SuppressWarnings("unused") boolean modifiable) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isWritable()
   {
      // CWS doesn't provide this flag, so infer it from the name
      // TODO: Remove this once Datastore supports implements it
      String name = getName();
      return name == null || (!name.equals("system"));
   }

   @SuppressWarnings("NumericCastThatLosesPrecision")
   @Override
   public int getPctFree() {
      long size = getSize();
      long used = getUsed();
      if (size <= 0 || used <= 0 || used > size) {
         return -1;
      }
      long bytesFree = size - used;

      // we do things this way (rather than bytesFree * 100L / size)
      // to avoid numeric overflow
      return (int)(bytesFree / (size / 100L));
   }

   /**
    * This only exists so that we can round-trip an object
    * via JSON.  It will be set when the "name" field is
    * set.
    *
    * @param ignored not used
    *
    * @deprecated use {@see setName} instead
    */
   @Deprecated
   @JsonIgnore
   public void setWritable(boolean ignored) {
      throw new UnsupportedOperationException();
   }

   /**
    * Construct a file or directory path from the list of path components,
    * using the specified path separator.
    *
    * @param separator
    * @param components
    * @return
    */
   protected String buildPathWithSep(String separator, String... components)
   {
      String result = components[0];

      for (int i = 1; i < components.length; i++) {
         if (!result.endsWith(separator) && !components[i].startsWith(separator)) {
            result += separator;
         }
         result += components[i];
      }
      return result;
   }
}
