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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * Interface to group guest OS types.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
   @JsonSubTypes.Type(WinXPProOsType.class),
   @JsonSubTypes.Type(WinVistaOsType.class),
   @JsonSubTypes.Type(Win7OsType.class),
   @JsonSubTypes.Type(Win8OsType.class)})
public abstract class OsType {
   /**
    * Return the OsType string for all OsTypes.
    * This is a generic method that is used for retrieving OsType String.
    *
    * @return
    */
   public final String getOsTypeName() {
      return this.getClass().getSimpleName();
   }

   @JsonIgnore
   public final void setOsTypeName(@SuppressWarnings("unused") String dummy) {
      // Do nothing, dummy.
   }

   /**
    * Return the OsVariant string for all OsTypes.
    * Implementation for retrieving OsVariant is specific to each implementation.
    *
    * @return
    */
   public String getOsVariantName() {
      return StringUtils.EMPTY;
   }

   @JsonIgnore
   public final void setOsVariantName(@SuppressWarnings("unused") String dummy) {
      // do nothing, dummy.
   }
}
