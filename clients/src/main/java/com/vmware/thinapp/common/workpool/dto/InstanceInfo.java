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

package com.vmware.thinapp.common.workpool.dto;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Represents the VM details of a lease instance.
 */
public class InstanceInfo {
   public enum State {
      created,
      deleting,
      deleteFailed,
      instancing,
      instancingFailed,
      available;
   }

   private Long id;
   private String moid;
   private String guestUsername;
   private String guestPassword;
   private String vmxPath;
   private boolean autologon;
   private State state;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getMoid() {
      return moid;
   }

   public void setMoid(String moid) {
      this.moid = moid;
   }

   public String getVmxPath() {
      return vmxPath;
   }

   public void setVmxPath(String vmxPath) {
      this.vmxPath = vmxPath;
   }

   public String getGuestUsername() {
      return guestUsername;
   }

   public void setGuestUsername(String guestUsername) {
      this.guestUsername = guestUsername;
   }

   public String getGuestPassword() {
      return guestPassword;
   }

   public void setGuestPassword(String guestPassword) {
      this.guestPassword = guestPassword;
   }

   public boolean getAutologon() {
      return autologon;
   }

   public void setAutologon(boolean autologon) {
      this.autologon = autologon;
   }

   public State getState() {
      return state;
   }

   public void setState(State state) {
      this.state = state;
   }

   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).
         setExcludeFieldNames(new String[] {"guestPassword"}).toString();
   }
}
