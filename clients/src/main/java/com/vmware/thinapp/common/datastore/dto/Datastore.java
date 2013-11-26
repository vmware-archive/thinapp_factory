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

package com.vmware.thinapp.common.datastore.dto;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.vmware.thinapp.common.base.HasId;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Datastore implements HasId {

   public static enum Status {
      /** Datastore is OK to use. */
      online,
      /** Datastore is offline; cannot be used. */
      offline,
      /** Datastore cannot be used, or made online. */
      unavailable
   }

   public static enum Type {
      /** Datastores on a CIFS file share */
      cifs
   }

   private Long id;
   private String name;
   private String server;
   private int port;
   private String share;
   private String domain;
   private String username;
   private String password;
   private Status status;
   private Type type;
   private long size;
   private long used;
   private boolean mountAtBoot;
   private String baseUrl;
   private long leases;
   private String mountPath;

   public String getBaseUrl() {
      return baseUrl;
   }

   public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
   }

   public String getDomain() {
      return domain;
   }

   public void setDomain(String domain) {
      this.domain = domain;
   }

   public long getLeases() {
      return leases;
   }

   public String getName() {
      return name;
   }

   public String getPassword() {
      return password;
   }

   public String getServer() {
      return server;
   }

   public int getPort() {
      return port;
   }

   public String getShare() {
      return share;
   }

   public long getSize() {
      return size;
   }

   public Status getStatus() {
      return status;
   }

   public Type getType() {
      return type;
   }

   public long getUsed() {
      return used;
   }

   public String getUsername() {
      return username;
   }

   public String getMountPath() {
      return mountPath;
   }

   public boolean isMountAtBoot() {
      return mountAtBoot;
   }

   public void setLeases(long leases) {
      this.leases = leases;
   }

   public void setMountAtBoot(boolean mountAtBoot) {
      this.mountAtBoot = mountAtBoot;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public void setServer(String server) {
      this.server = server;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public void setShare(String share) {
      String shareLocal = share;
      /* Enforce UNC format */
      if (StringUtils.isNotBlank(shareLocal)) {
         shareLocal = shareLocal.replace("/", "\\");
      }
      this.share = shareLocal;
   }

   public void setSize(long size) {
      this.size = size;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   public void setType(Type type) {
      this.type = type;
   }

   public void setUsed(long used) {
      this.used = used;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public void setMountPath(String mountPath) {
      this.mountPath = mountPath;
   }

   /**
    * @return the id
    */
   @Override
   public Long getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(Long id) {
      this.id = id;
   }

   /**
    * Converts this object into a string without the password
    * field.(Security reasons)
    */
   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).
         setExcludeFieldNames(new String[] {"password"}).toString();
   }

   /**
    * Generate a hash code using
    * server, share, username, password and type fields.
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return new HashCodeBuilder(17, 31)
         .append(server)
         .append(share)
         .append(username)
         .append(password)
         .append(type)
         .append(status)
         .toHashCode();
   }

   /**
    * Equals is based on its super equals and
    * server, share, username, password and type fields.
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (this == obj) {
         return true;
      }

      // allow subclasses to participate
      // in the equals() comparison
      if (!(obj instanceof Datastore)) {
         return false;
      }
      final Datastore other = (Datastore) obj;
      return new EqualsBuilder().
         append(server, other.server).
         append(share, other.share).
         append(username, other.username).
         append(password, other.password).
         append(type, other.type).
         isEquals();
   }

}
