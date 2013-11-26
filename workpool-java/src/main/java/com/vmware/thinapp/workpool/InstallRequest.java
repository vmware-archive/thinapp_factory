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
import com.vmware.thinapp.workpool.model.VmLocationModel;
import com.vmware.thinapp.workpool.model.VmPatternModel;

/**
 * Represents the data to pass to the create-vm script to create an easy installed VM.
 */
public class InstallRequest {
   private final VCConfigModel vcConfig;
   private final VmPatternModel vmPattern;
   private final HardwareConfiguration vmHardware;
   private String vmName;

   private String guestPassword;
   private final VmLocationModel vmLocation;
   private File logFile;

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
    * @param vmName
    * @param vcConfig
    * @param vmPattern
    * @param vmHardware
    * @param vmLocation
    * @param password
    */
   public InstallRequest(
           String vmName,
           VCConfigModel vcConfig,
           VmPatternModel vmPattern,
           HardwareConfiguration vmHardware,
           VmLocationModel vmLocation,
           String password) {
      this.vmName = vmName;
      this.vcConfig = vcConfig;
      this.vmPattern = vmPattern;
      this.vmHardware = vmHardware;
      this.vmLocation = vmLocation;
      this.guestPassword = password;
      try {
         this.logFile = File.createTempFile("installlog", null);
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
              .put("name", getVmName())
              .put("dsName", vmLocation.getDatastoreName())
              .put("resPool", vmLocation.getResourcePool())
              .put("resName", vmLocation.getComputeResource())
              .put("osKey", vmPattern.getOsInfo().getOsType().toString())
              .put("prodKey", vmPattern.getOsRegistration().getLicenseKey())
              .put("kmsServer", vmPattern.getOsRegistration().getKmsServer())
              .put("isoDsPath", vmPattern.getSourceIso())
              .put("variant", vmPattern.getOsInfo().getVariant())
              .put("netName", vmPattern.getNetworkName())
              .put("memoryMB", String.valueOf(vmHardware.getMemoryMB()))
              .put("diskMB", String.valueOf(vmHardware.getDiskMB()))
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
      // username is the name that the OS is registered to, not the name of the user account.
      ini.put("guest", "username", vmPattern.getOsRegistration().getUserName());
      ini.put("guest", "password", scrubber.transform("guest", "password", getGuestPassword()));

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

   public String getGuestPassword() {
      return guestPassword;
   }

   public void setGuestPassword(String guestPassword) {
      this.guestPassword = guestPassword;
   }

   public VCConfigModel getVcConfig() {
      return vcConfig;
   }

   public String getVmName() {
      return vmName;
   }

   public void setVmName(String vmName) {
      this.vmName = vmName;
   }

   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this)
              .setExcludeFieldNames(new String[]{"guestPassword"}).toString();
   }
}
