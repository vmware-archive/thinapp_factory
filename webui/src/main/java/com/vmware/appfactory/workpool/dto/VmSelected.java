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

package com.vmware.appfactory.workpool.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * This object is used to reference existing vms. The data represents all the
 * information needed to select a VM.
 *
 * @author Keerthi Singri
 * @since M8, 5 September, 2011
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class VmSelected
{
   private String _moid;
   private String _selectedVMName;

   /**
    * Default constructor.
    */
   public VmSelected() {
      /* Empty default constructor */
   }


   /**
    * Constructor setting _moid, _guest username and password.
    *
    * @param moid
    * @param selectedVMName
    */
   public VmSelected(String moid, String selectedVMName) {
      this._moid = moid;
      this._selectedVMName = selectedVMName;
   }


   /**
    * @return the _moid
    */
   public String getMoid()
   {
      return _moid;
   }


   /**
    * @param moid the _moid to set
    */
   public void setMoid(String moid)
   {
      this._moid = moid;
   }


   /**
    * @return the _selectedVMName
    */
   public String get_selectedVMName()
   {
      return _selectedVMName;
   }


   /**
    * @param selectedVMName the _selectedVMName to set
    */
   public void set_selectedVMName(String selectedVMName)
   {
      this._selectedVMName = selectedVMName;
   }


}
