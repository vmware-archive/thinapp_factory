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

package com.vmware.thinapp.common.util;

import javax.annotation.Nullable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfJson;

/**
* Used in AfUtilsTest to test custom serializer.
*/
public class SimpleDateBean {

   @Nullable
   private Long time;

   public SimpleDateBean() {
      this.time = null;
   }

   public SimpleDateBean(long time) {
      // strip off the millisecond values, as they won't
      // be stored in the string date representation
      this.time = (time / 1000L) * 1000L;
   }

   @JsonDeserialize(using=AfJson.CalendarDeserializer.class)
   public void setTime(@Nullable Long time)
   {
      if (null == time) {
         this.time = AfCalendar.NEVER;
      } else {
         this.time = time;
      }
   }

   @JsonSerialize(using=AfJson.CalendarSerializer.class)
   @Nullable
   public Long getTime()
   {
      return time;
   }

   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
