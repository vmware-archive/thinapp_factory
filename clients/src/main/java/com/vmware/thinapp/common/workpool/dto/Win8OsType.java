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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * This class indicates an OSType for Win 8.
 */
public class Win8OsType extends OsType {
   /** The variants for Win8. */
   public enum Variant {
      professional,
      enterprise,
      ultimate
   }

   private Variant variant;

   public Win8OsType() {
      /* default constructor */
   }

   public Win8OsType(Variant variant) {
      this.variant = variant;
   }

   /**
    * @return the variant
    */
   public Variant getVariant() {
      return variant;
   }

   /**
    * @param variant the variant to set
    */
   public void setVariant(Variant variant) {
      this.variant = variant;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
   }

   @Override
   public String getOsVariantName() {
      return getVariant().name();
   }
}
