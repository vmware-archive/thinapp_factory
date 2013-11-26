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

package com.vmware.test.integration;


import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appfactory.application.AppHelper;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.file.FileType;
import com.vmware.appfactory.fileshare.CifsHelper;
import com.vmware.appfactory.fileshare.IFileConverter;

import static junit.framework.Assert.*;

/**
 * Test AfCifsHelper class.
 *
 * @author saung
 * @since v1.0 4/5/2011
 */
public class CifsHelperTest {
   protected static final Logger _log = LoggerFactory.getLogger(CifsHelperTest.class);
   private static final String CIFS_SHARED_SERVER = "10.20.132.65";
   private static final String CIFS_SHARED_PATH = "foo/cifs_shared";
   private static final String CIFS_SHARED_URL_PATH = "smb://" + CIFS_SHARED_SERVER + "/" + CIFS_SHARED_PATH + "/";
   private static final String GROUP = "WORKGROUP";
   private static final String USERNAME = "foo";
   private static final String PASSWORD = "bar";

   /**
    * Test NTLM client authentication
    */
   @Test
   public void authNTLMClient() {
      NtlmPasswordAuthentication auth = CifsHelper.authNTLMClient(GROUP, USERNAME, PASSWORD);
      assertNotNull(auth);
   }

   /**
    * Test SMB Url conversion method.
    */
   @Test
   public void getSMBUrl() {
      String smbUrl = CifsHelper.getSMBUrl(CIFS_SHARED_SERVER, CIFS_SHARED_PATH);
      assertEquals(CIFS_SHARED_URL_PATH, smbUrl);
   }

   /**
    * Test CIFS crawler.
    *
    * @throws Exception if any error raised during operation.
    */
   @Test
   public void testCrawl() throws Exception {
      final int dirDepth = 6;

      final IFileConverter<Application> converter = new IFileConverter<Application>() {

         @Override
         public Application convert(
               NtlmPasswordAuthentication auth,
               String installerNameIn,
               String fullPath,
               String... parentDirs)
         {
            String installerName = installerNameIn;
            String appUri = "";
            try {
               final Application app = createApp(installerName, appUri);
               return app;
            }
            catch(URISyntaxException ex) {
               _log.warn("Invalid application URI {} (Skipped)", appUri);
               return null;
            }
         }
      };
      final List<Application> result =
         CifsHelper.crawl(CIFS_SHARED_URL_PATH, CifsHelper.authNTLMClient(GROUP, USERNAME, PASSWORD), dirDepth, converter);
      assertNotNull(result);
      assertTrue(checkCrawlResult(result, createExpectedApps()));
   }

   /**
    * Test CIFS crawler.
    *
    * @throws Exception if any error raised during operation.
    */
   @Test
   public void listFiles() throws Exception {
      int dirDepth = 6;
      final List<String> result = CifsHelper.crawl(CIFS_SHARED_URL_PATH, CifsHelper.authNTLMClient(GROUP, USERNAME, PASSWORD), dirDepth);
      assertNotNull(result);
      assertTrue(checkFiles(result, createExpectedFilesList()));
   }

   /**
    * Test domain and username parsing.
    */
   @Test
   public void testParseDomainAndUserName() {
      String[] actual = CifsHelper.parseDomainAndUsername("foo");
      String[] expected = new String[] { "", "foo" };
      assertTrue(Arrays.equals(actual, expected));

      actual = CifsHelper.parseDomainAndUsername("domain/bar");
      expected = new String[] { "domain", "bar" };
      assertTrue(Arrays.equals(actual, expected));

      String input = "domain\\zoo";
      actual = CifsHelper.parseDomainAndUsername(input);
      expected = new String[] { "domain", "zoo" };
      assertTrue(Arrays.equals(actual, expected));
   }

   private List<String> createExpectedFilesList() {
      final List<String> files = new ArrayList<String>();
      files.add(CIFS_SHARED_URL_PATH + "file1.txt");
      files.add(CIFS_SHARED_URL_PATH + "Google/Chrome/9.1.2.3/US/R0/chrome.exe");
      files.add(CIFS_SHARED_URL_PATH + "Google/Chrome/10.0.648.204/US/R0/chrome.exe");
      files.add(CIFS_SHARED_URL_PATH + "Vendor/Product/Version/i10n/Release/file2.exe");
      return files;
   }

   private boolean checkFiles(List<String> scannedFiles, List<String> expected) {
      for(String file : scannedFiles) {
         if(!expected.remove(file)) {
            return false;
         }
      }
      return (expected.size() == 0);
   }

   protected final Application createApp(String installerName, String url) throws URISyntaxException {
      final Application app = new Application();
      app.setDownload(AppHelper.createDownload(url));
      FileType fileType = FileType.parseType(installerName);
      if (fileType.getDefaultInstallCommand(installerName) != null) {
         app.setInstalls(new AppInstall(fileType.getDefaultInstallCommand(installerName)));
      }
      return app;
   }

   private List<Application> createExpectedApps() throws URISyntaxException {
      final List<Application> apps = new ArrayList<Application>();
      apps.add(createApp("msiexec /qn /i myapp.msi", ""));
      apps.add(createApp("msiexec /qn /i goog.msi", ""));
      apps.add(createApp("msiexec /qn /i second-chrome.msi", ""));
      apps.add(createApp("msiexec /qn /i chrome.msi", ""));
      apps.add(createApp("Firefox-Setup-4.0.1.exe", ""));
      apps.add(createApp("a-chrome.exe", ""));
      apps.add(createApp("third-chrome.exe", ""));
      apps.add(createApp("chrome.exe", ""));
      apps.add(createApp("installer.exe", ""));
      return apps;
   }

   private boolean checkCrawlResult(List<Application> result, List<Application> expected) {
      assertTrue(result.size() == expected.size());
      for(Application app : result) {
         if(!expected.remove(app)) {
            return false;
         }
      }
      return (expected.size() == 0);
   }

}
