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

package com.vmware.thinapp.common.converter.dto;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonProperty;

public class ConversionResponse {

   private Long jobId;

   /**
    * Default constructor.
    */
   public ConversionResponse() {
      /* Empty */
   }

   /**
    * Constructor to set the jobId
    * @param jobId
    */
   public ConversionResponse(Long jobId) {
      this.jobId = jobId;
   }

   @JsonProperty("id")
   public Long getJobId() {
      return jobId;
   }

   @JsonProperty("id")
   public void setJobId(Long jobId) {
      this.jobId = jobId;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
