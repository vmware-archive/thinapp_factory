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

package com.vmware.appfactory.common;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang.StringUtils;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * A class which is embedded into any data model POJO that needs to store
 * an error message.
 *
 * @author levans
 *
 */
@Embeddable
public class AfFailure
{
   /** Max length for the summary field */
   public static final int SUMMARY_LEN = 512;

   /** Max length for the details field */
   public static final int DETAILS_LEN = 4096;

   @Column(length=SUMMARY_LEN)
   private String _summary;

   @Column(length=DETAILS_LEN)
   private String _details;


   /**
    * Create a new instance with empty (null) fields.
    */
   public AfFailure()
   {
      /* Nothing to do */
   }


   /**
    * Create a new instance from the given error.
    * The summary is set to the error message; the detail remains null.
    *
    * @param error
    */
   public AfFailure(Throwable error)
   {
      _summary = error.getMessage();
      if (StringUtils.isBlank(_summary)) {
         _summary = error.getClass().getName();
      }
      _details = null;
   }


   /**
    * Set the summary of the failure.
    * @param summary
    */
   public void setSummary(String summary)
   {
      _summary = AbstractRecord.truncate(summary, SUMMARY_LEN);
   }


   /**
    * Get the summary of the failure.
    * @return
    */
   public String getSummary()
   {
      return _summary;
   }


   /**
    * Set the details about the failure.
    * @param details
    */
   public void setDetails(String details)
   {
      _details = AbstractRecord.truncate(details, DETAILS_LEN);
   }


   /**
    * Get details about the failure.
    * @return
    */
   public String getDetails()
   {
      return _details;
   }
}
