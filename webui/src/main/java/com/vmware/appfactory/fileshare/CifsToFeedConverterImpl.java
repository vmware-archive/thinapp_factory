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

package com.vmware.appfactory.fileshare;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Resource;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.file.FileHelper;
import com.vmware.appfactory.file.FileType;
import com.vmware.appfactory.fileshare.exception.FeedConverterErrorCode;
import com.vmware.appfactory.fileshare.exception.FeedConverterException;

/**
 * It implements IFeedConverter interface and converts Exit16 CIFS installer files
 * to App factory JSON feed format - version 3.
 * Currently, it is ONLY supported NTLM authentication for the jCIFS client.
 *
 * Note: Bad name: should really be CifsToApplicationsConverterImpl. Rename it
 * once all the reviews are in!
 *
 * @author saung
 * @since v1.0 4/5/2011
 */
public class CifsToFeedConverterImpl implements IFeedConverter<Application> {
   /**
    * get the logger
    */
   static final Logger _log = LoggerFactory.getLogger(CifsToFeedConverterImpl.class);
   /**
    * SMB url separator.
    */
   public static final String SMB_URL_SEPARATOR = "/";
   /**
    * XXX list of supported file types; defined in bean definition at app-config.xml
    */
   private final EnumSet<FileType> _supportedTypes = EnumSet.noneOf(FileType.class);
   /**
    * a feed version number (Spring IoC injected)
    */
   private int _feedVersion;

   @Resource
   protected ConfigRegistry _config;

   /**
    * Constructor to create new instance and initialize file extension Regex pattern.
    */
   public CifsToFeedConverterImpl() {
      /* Set default filename patterns */
      _supportedTypes.add(FileType.exe);
      _supportedTypes.add(FileType.msi);
      _supportedTypes.add(FileType.iso);
   }


   /**
    * Set the pattern used for recognizing applications that we create
    * install commands for.
    * @param fileTypes List of FileType enum values
    */
   public void setSupportedFileExtensions(String[] fileTypes) {
      _supportedTypes.clear();
      for (String str : fileTypes) {
         FileType ft = FileType.valueOf(str);
         if (ft != null) {
            _supportedTypes.add(ft);
         }
      }
   }


   /**
    * Scan all installer files from CIFS shared mount and return a list of apps.
    *
    * @param smbUrl - a Samba Url
    * @param group - a group/domain name (e.g. WORKGROUP)
    * @param username - a valid smb username.
    * @param password - a valid smb password.
    * @return a list of successfully scanned apps.
    * @throws FeedConverterException
    */
   @Override
   public List<Application> scanObjects(
         String smbUrl,
         String group,
         String username,
         String password,
         final long appTimeStamp,
         final Long cwsDataStoreId) throws FeedConverterException {

      try {
         final int smbUrlLength = smbUrl.length();
         final String[] layout = getDirLayout().split(SMB_URL_SEPARATOR);
         final String recipeDir = _config.getString(ConfigRegistryConstants.FILESHARE_RECIPE_DIR);
         final IFileConverter<Application> converter = new IFileConverter<Application>()
         {
            @SuppressWarnings("synthetic-access")
            @Override
            public Application convert(
                  NtlmPasswordAuthentication auth,
                  String installerName,
                  String fullPath,
                  String... parentDirs)
            {
               /* Exclude SAMBA URL from the path */
               String pathToInstaller = fullPath.substring(smbUrlLength);

               /* Determine type of file */
               FileType fileType = FileType.parseType(installerName);

               /* If this is inside the recipes folder, skip it */
               if (StringUtils.isNotBlank(recipeDir)) {
                  if (pathToInstaller.startsWith(recipeDir)) {
                     _log.info("Looks like a recipe file -> {} (Skipped)", installerName);
                     return null;
                  }
               }

               /* If this is inside the upload folder, skip it */
               if (pathToInstaller.startsWith(FileHelper.UPLOAD_DIR + SMB_URL_SEPARATOR)) {
                  _log.info("Looks like an uploaded file -> {} (Skipped)", installerName);
                  return null;
               }

               /* Is it supported? */
               if (!_supportedTypes.contains(fileType)) {
                  //_log.info("Not supported file extension -> {} (Skipped)", installerName);
                  return null;
               }

               Application app = null;
               try {
                  app = createApplication(
                        cwsDataStoreId,
                        pathToInstaller,
                        installerName,
                        layout,
                        appTimeStamp,
                        fileType);
                  if (app == null) {
                     _log.warn("Found invalid file {} (Skipped)", installerName);
                  }
               }
               catch(Exception ex) {
                  _log.warn("Failed to convert installer {} to app : {}",
                        installerName,
                        ex.getMessage());
               }

               return app;
            }
         };

         return CifsHelper.crawl(smbUrl, CifsHelper.authNTLMClient(group, username, password), getMaxDirDepth(), converter);
      }
      catch (SmbException ex) {
         int status = ex.getNtStatus();
         FeedConverterErrorCode errCode = FeedConverterErrorCode.fromNtStatus(status);

         _log.info("Application crawl failed:" +
               " status=" + status +
               " code=" + errCode +
               " error=" + ex.getMessage());

         throw new FeedConverterException(errCode, ex);
      }
      catch (IOException ex) {
         throw new FeedConverterException(
               FeedConverterErrorCode.Other,
               ex);
      }
   }


   /**
    * Create an application using the given inputs.
    *
    * CWS data store url format -
    *    e.g. url: "datastore://exit15/Software/Office 2010/setup.exe"
    *
    * @param smbUrllength
    * @param fileWithFullPath
    * @param dirDepth
    * @param timestamp
    * @return an application instance.
    * @throws URISyntaxException
    */
   final Application createApplication(
         Long dataStoreId,
         String installerParentDir,
         String installerName,
         String[] layout,
         long timestamp,
         FileType fileType) throws URISyntaxException
   {
      _log.debug("Creating application from {}", installerName);
      final String[] installerDirs = installerParentDir.split(SMB_URL_SEPARATOR);

      // set all meta data fields from layout directory pattern.
      final Application app = new Application();

      // Apply meta-data mapping to either the installer dir path >= or <= the layout depth.
      if (installerDirs.length > 0 && layout.length > 0) {
         for (int i = 0; i < layout.length && i < installerDirs.length; i++) {
            app.setAppInfoField(layout[i], installerDirs[i]);
         }
      }
      // set download location
      AppDownload download = new AppDownload();
      download.setURI(DsUtil.generateDatastoreURI(
            // preview scans have no datastore
            dataStoreId == null ? new Long(0) : dataStoreId,
            installerParentDir, installerName));
      app.setDownload(download);
      _log.debug("    download = " + download.getURI().toString());

      // set default command with silent option.
      String cmd = fileType.getDefaultInstallCommand(installerName);
      if (cmd != null) {
         app.setInstalls(new AppInstall(cmd));
         _log.debug("    install = " + app.getInstall().getCommand());
      }

      // set default icon (null uses the default icon provided by the UI)
      app.setIcons(null);
      app.setLastRemoteUpdate(timestamp);

      return app;
   }


   /**
    * @return the feedVersion
    */
   public int getFeedVersion() {
      return _feedVersion;
   }


   /**
    * @param feedVersion the feedVersion to set
    */
   public void setFeedVersion(int feedVersion) {
      _feedVersion = feedVersion;
   }

   /**
    * a directory layout to use for application meta-data mapping.
    * @return the dirLayout
    */
   public String getDirLayout() {
      return _config.getString(ConfigRegistryConstants.FILESHARE_DIR_LAYOUT);
   }

   /**
    * Compare dir layout's depth and max dir depth from config, and
    * return the highest one as the max dir depth to crawl.
    * @return maximum directory depth to crawl.
    */
   public int getMaxDirDepth() {
      int layoutDepth = (StringUtils.isNotBlank(getDirLayout())) ? getDirLayout().split(SMB_URL_SEPARATOR).length : 0;
      /** to include parent / dir */
      layoutDepth++;
      int maxDepth = (int) _config.getLong(ConfigRegistryConstants.FILESHARE_MAX_DIR_DEPTH_SCAN);
      return (maxDepth > layoutDepth) ? maxDepth : layoutDepth;
   }

}
