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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.appfactory.workpool.controller.WorkpoolApiController;
import com.vmware.thinapp.common.workpool.dto.OsRegistration;
import com.vmware.thinapp.common.workpool.dto.OsType;
import com.vmware.thinapp.common.workpool.dto.WinXPProOsType;
import com.vmware.thinapp.common.workpool.dto.Workpool.State;

/**
 * The definition of a JSON request workpool used for creating a new workpool
 *
 * @see WorkpoolApiController#createWorkpool
 * @since M8, 15 August, 2011
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class JsonWorkpoolRequest
{
   /** Workpool base fields */
   private Long _id;
   private String _name;
   private int _maximum;
   private State _state;


   /**
    * The workpool creation request could be one of these types. Workpool
    * creation is dependent on this and whether clone is supported.
    */
   public enum SourceType
   {
      /** Browse the datastore to upload an iso. */
      selectISO,
      /** Upload an iso to a datastore */
      uploadISO,
      /** Select a vm from the list of vms present */
      selectVM,
      /** Use an existing TAF VM Image saved previously */
      existingImage
   }

   /** Used to distinguish workpool creation request types. */
   private SourceType _sourceType;

   /**
    * The workpool creation request can have two ways of setting licenses
    * and is applicable for non winXPPro osTypes
    */
   public enum LicenseType
   {
      /** KMS server to use for licensing. System picks KMS activation key */
      kmsServer,
      /** Regular license key */
      licenseKey
   }

   /** Used to distinguish type of licensing to use. */
   private LicenseType _licType;

   /** Indicates if clone is supported on the virtual infrastructure */
   private boolean _cloneSupported;

   /** OsType and OSRegistration required for associating with a workpool */
   private OsType _osType;
   private OsRegistration _osRegistration;

   /** ISO info used to create a new vm */
   private String _sourceIso;
   private String _sourceDatastore;
   private String _networkName;

   /** Info for selected vm */
   private VmSelected _vmSelected;

   /** Capture an existing vmImageId that user selects. */
   private Long _vmImageId;

   /**
    * Default constructor with its children objects initialized.
    */
   public JsonWorkpoolRequest()
   {
      /* Empty */
   }


   /**
    * @return the _osRegistration
    */
   public OsRegistration getOsRegistration()
   {
      return _osRegistration;
   }


   /**
    * @param osRegistration the _osRegistration to set
    */
   public void setOsRegistration(OsRegistration osRegistration)
   {
      this._osRegistration = osRegistration;
   }


   /**
    * @return the _vmImageId
    */
   public Long getVmImageId()
   {
      return _vmImageId;
   }


   /**
    * @param vmImageId the _vmImageId to set
    */
   public void setVmImageId(Long vmImageId)
   {
      this._vmImageId = vmImageId;
   }


   /**
    * @return the id
    */
   public Long getId()
   {
      return _id;
   }


   /**
    * @param id the id to set
    */
   public void setId(Long id)
   {
      this._id = id;
   }


   /**
    * @return the name
    */
   public String getName()
   {
      return _name;
   }


   /**
    * @param workpoolName the name to set
    */
   public void setName(String workpoolName)
   {
      this._name = workpoolName;
   }


   /**
    * @return the maximum
    */
   public int getMaximum()
   {
      return _maximum;
   }


   /**
    * @param maximum the maximum to set
    */
   public void setMaximum(int maximum)
   {
      this._maximum = maximum;
   }


   /**
    * @return the _state
    */
   public State getState()
   {
      return _state;
   }


   /**
    * @param state the _state to set
    */
   public void setState(State state)
   {
      this._state = state;
   }


   /**
    * @return the _cloneSupported
    */
   public boolean isCloneSupported()
   {
      return _cloneSupported;
   }


   /**
    * @param cloneSupported the _cloneSupported to set
    */
   public void setCloneSupported(boolean cloneSupported)
   {
      this._cloneSupported = cloneSupported;
   }


   /**
    * @return the _osType
    */
   public OsType getOsType()
   {
      return _osType;
   }


   /**
    * @param osType the osType to set
    */
   public void setOsType(OsType osType)
   {
      this._osType = osType;
   }


   /**
    * @return the _sourceIso
    */
   public String getSourceIso()
   {
      return _sourceIso;
   }


   /**
    * @param sourceIso the sourceIso to set
    */
   public void setSourceIso(String sourceIso)
   {
      this._sourceIso = sourceIso;
   }


   /**
    * @return the _sourceDatastore
    */
   public String getSourceDatastore()
   {
      return _sourceDatastore;
   }


   /**
    * @param sourceDatastore the sourceDatastore to set
    */
   public void setSourceDatastore(String sourceDatastore)
   {
      this._sourceDatastore = sourceDatastore;
   }


   /**
    * @return the _networkName
    */
   public String getNetworkName()
   {
      return _networkName;
   }


   /**
    * @param networkName the _networkName to set
    */
   public void setNetworkName(String networkName)
   {
      this._networkName = networkName;
   }


   /**
    * @return the _sourceType
    */
   public SourceType getSourceType()
   {
      return _sourceType;
   }


   /**
    * @param sourceType the _sourceType to set
    */
   public void setSourceType(SourceType sourceType)
   {
      this._sourceType = sourceType;
   }


   /**
    * @return the _licType
    */
   public LicenseType getLicType()
   {
      return _licType;
   }


   /**
    * @param licType the _licType to set
    */
   public void setLicType(LicenseType licType)
   {
      this._licType = licType;
   }


   /**
    * @return the _vmSelected
    */
   public VmSelected getVmSelected()
   {
      return _vmSelected;
   }


   /**
    * @param vmSelected the selectedVMName to set
    */
   public void setVmSelected(VmSelected vmSelected)
   {
      this._vmSelected = vmSelected;
   }



   /**
    * Validate the input while creating a workpool.
    *
    * The validation is specific to various scenario and these are covered here.
    * If validation fails, it throws InvalidDataException.
    *
    * 1. ExistingImage selection auto mandates a selection - no validation.
    * 2. Using a vm to create a clone/associate a workpool instance needs field
    *    validation on: osType, osRegistration, _vmSelected
    * 3. Using an iso to create a new vm(image/workpool instance) needs field
    *    validation on: osType, osRegistration, _sourceIso, _networkName
    *
    * @throws InvalidDataException
    */
   public void validateCreate() throws InvalidDataException
   {
      this.validateEdit();

      // Validate OsType and Registration for all cases but existingImage.
      if (SourceType.existingImage != this.getSourceType()) {
         validateOsTypeAndRegistration();
      }

      if (SourceType.selectVM == this.getSourceType()) {
         // Workpool instance/vmImage creation using an existing image.
         if(_vmSelected == null || StringUtils.isEmpty(_vmSelected.getMoid())) {
            throw new InvalidDataException("Selecting a vm is required.");
         }
      }
      else if (SourceType.selectISO == this.getSourceType()) {
         // Workpool instance/vmImage creation by using an iso and creating a new vm.
         if(StringUtils.isEmpty(_sourceIso)) {
            throw new InvalidDataException(
                  "Selecting the windows installer(iso) is required.");
         }
         if(StringUtils.isEmpty(_networkName)) {
            throw new InvalidDataException(
                  "Selecting a virtual infrastructure network is required.");
         }
      }
   }


   /**
    * Validation methods to validate require _osType and _osRegistration
    * @throws InvalidDataException
    */
   private void validateOsTypeAndRegistration() throws InvalidDataException
   {
      if (_osType == null || _osRegistration == null) {
         throw new InvalidDataException("Invalid guest OS data and registration");
      }

      // License key is needed when osType is winXPPro or if licType = licenseKey.
      if (_osType instanceof WinXPProOsType || _licType == LicenseType.licenseKey) {
         if (StringUtils.isEmpty(_osRegistration.getLicenseKey())) {
            throw new InvalidDataException("'License key' is required");
         }
      }
      else {
         // licType = kmsServer and osType != winXPPro, validate kms server
         if (StringUtils.isEmpty(_osRegistration.getKmsServer())) {
            throw new InvalidDataException("'License Key Management Server' is required");
         }
      }
      if (StringUtils.isEmpty(_osRegistration.getUserName())) {
         throw new InvalidDataException("'Username' for registing the license key is required");
      }
      if (StringUtils.isEmpty(_osRegistration.getOrganization())) {
         throw new InvalidDataException("'Organization' for registering license key is required");
      }
   }


   /**
    * Validate the input while editing a workpool.
    *
    * @throws InvalidDataException
    */
   public void validateEdit() throws InvalidDataException
   {
      if (StringUtils.isEmpty(this.getName())) {
         throw new InvalidDataException("Required fields missing");
      }
      if (this.getMaximum() <= 0) {
         throw new InvalidDataException("Invalid maximum workpool instances value");
      }
   }


   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.DEFAULT_STYLE)
         .setExcludeFieldNames(new String[]{"licenseKey"}).toString();
   }

}
