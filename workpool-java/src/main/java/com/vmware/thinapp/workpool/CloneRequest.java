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

package com.vmware.thinapp.workpool;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.ini4j.Ini;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.thinapp.workpool.model.VmImageModel;
import com.vmware.thinapp.workpool.model.VmLocationModel;

/**
 * Represents the data to pass to the clone-vm script to create a clone.
 */
public class CloneRequest {
   private final VmLocationModel vmLocation;
   private final VCConfigModel vcConfig;
   private final File logFile;
   private final VmImageModel vmImage;
   private final String workpoolName;
   private final String guestPassword;

   public static final IniScrubber redactedScrubber = new IniScrubber(
           new ImmutableMultimap.Builder<String, String>()
                   .put("vc", "vcPassword")
                   .put("vm", "prodKey")
                   .put("guest", "password")
                   .build()
   );

   public static final NullTransform nullScrubber = new NullTransform();

   /**
    * Constructor.
    *
    * @param vmLocation
    * @param vcConfig
    * @param vmImage
    * @param workpoolName
    * @param guestPassword
    */
   public CloneRequest(
           VmLocationModel vmLocation,
           VCConfigModel vcConfig,
           VmImageModel vmImage,
           String workpoolName,
           String guestPassword) {
      this.vmLocation = vmLocation;
      this.vcConfig = vcConfig;
      this.vmImage = vmImage;
      this.workpoolName = workpoolName;
      this.guestPassword = guestPassword;
      try {
         this.logFile = File.createTempFile("clonelog", null);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public String toIni(IniTransform scrubber) {
      Ini ini = new Ini();

      Map<String, String> vc = new ImmutableMap.Builder<String, String>()
              .put("vcHost", vcConfig.getHost())
              .put("dcName", vcConfig.getDatacenter())
              .put("vcUsername", vcConfig.getUsername())
              .put("vcPassword", vcConfig.getPassword())
              .build();

      Map<String, String> vm = new ImmutableMap.Builder<String, String>()
              .put("moid", vmImage.getMoid())
              .put("name", workpoolName)
              .put("osKey", vmImage.getOsInfo().getOsType().toString())
              .put("prodKey", vmImage.getOsRegistration().getLicenseKey())
              .put("organization", vmImage.getOsRegistration().getOrganization())
              .put("userName", vmImage.getOsRegistration().getUserName())
              .put("kmsServer", vmImage.getOsRegistration().getKmsServer())
              .put("variant", vmImage.getOsInfo().getVariant())
              .put("resName", vmLocation.getComputeResource())
              .put("resPool", vmLocation.getResourcePool())
              .put("dsName", vmLocation.getDatastoreName())
              .build();

      // General settings.
      ini.put("general", "logFile", logFile.getAbsolutePath());

      // VC connection information.
      for (Map.Entry<String, String> entry : vc.entrySet()) {
         ini.put("vc", entry.getKey(), scrubber.transform("vc", entry.getKey(), entry.getValue()));
      }

      // VM creation information.
      for (Map.Entry<String, String> entry : vm.entrySet()) {
         ini.put("vm", entry.getKey(), scrubber.transform("vm", entry.getKey(), entry.getValue()));
      }

      // Guest customization.
      ini.put("guest", "password", scrubber.transform("guest", "password", guestPassword));

      StringWriter writer = new StringWriter();
      try {
         ini.store(writer);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      return writer.toString();
   }

   public File getLogFile() {
      return logFile;
   }

   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this)
              .setExcludeFieldNames(new String[]{"guestPassword"}).toString();
   }
}
