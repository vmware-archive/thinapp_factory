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

package com.vmware.thinapp.manualmode;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;

public class Config {
   @Value("#{systemProperties['http.proxyHost'] ?: ''}")
   private String httpProxyHost;

   @Value("#{systemProperties['http.proxyPort'] ?: 0}")
   private int httpProxyPort;

   @Value("#{systemProperties['http.nonProxyHosts'] ?: ''}")
   private String httpNonProxyHosts;

   @Autowired
   private Util util;

   private String licenseKey;
   private String licenseUser;
   private String setProxyFileName;
   private String setOptionFileName;
   private String backupEventlogsScriptName;

   // A directory to store installers on a datastore or appliance (internal DS).
   private String installersDir;

   private String setProxyExe;
   private String setOptionExe;
   private String backupEventlogsScriptPath;

   @SuppressWarnings("unused")
   @PostConstruct
   private void init() throws Exception {
      // Load the setproxy.exe utility from manualmode.jar's classpath.
      setProxyExe = util.extractFromJar(setProxyFileName, "setproxy");
      // Load the setoption.exe utility from manualmode.jar's classpath.
      setOptionExe = util.extractFromJar(setOptionFileName, "setoption");
      // Load the backupeventlogs.vbs script from manualmode.jar's classpath
      backupEventlogsScriptPath = util.extractFromJar(backupEventlogsScriptName, "backupeventlogs");
   }

   public String getLicenseUser() {
      return licenseUser;
   }

   @Autowired
   public void setLicenseUser(String user) {
      this.licenseUser = user;
   }

   public String getLicenseKey() {
      return licenseKey;
   }

   @Autowired
   public void setLicenseKey(String license) {
      this.licenseKey = license;
   }

   @Required
   public void setSetProxyFileName(String setProxyFileName) {
      this.setProxyFileName = setProxyFileName;
   }

   @Required
   public void setSetOptionFileName(String setOptionFileName) {
      this.setOptionFileName = setOptionFileName;
   }

   @Required
   public void setBackupEventlogsScriptName(String backupEventlogsScriptName) {
      this.backupEventlogsScriptName = backupEventlogsScriptName;
   }

   /**
    * Get an absolute path to setproxy.exe file from host.
    * @return a full path string
    */
   public String getSetProxyExePath() {
      return setProxyExe;
   }

   /**
    * Get an absolute path to setoption.exe file from host.
    * @return a full path string
    */
   public String getSetOptionExePath() {
      return setOptionExe;
   }

   /**
    * Get an absolute path to backupeventlogs.vbs script from host
    * @return a full path string
    */
   public String getBackupEventlogsScriptPath() {
      return backupEventlogsScriptPath;
   }

   public String getHttpProxyHost() {
      return httpProxyHost;
   }

   public int getHttpProxyPort() {
      return httpProxyPort;
   }

   public String getHttpNonProxyHosts() {
      return httpNonProxyHosts;
   }

   public String getInstallersDir() {
      return this.installersDir;
   }

   /**
    * Set installers dir (Spring IoC setter)
    *
    * @param installersDir a directory.
    * @see applicationContext.xml
    */
   public void setInstallersDir(String installersDir) {
      this.installersDir = installersDir;
   }
}
