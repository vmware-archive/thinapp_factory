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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.vmware.appfactory.datasource.DataSourceObject;
import com.vmware.thinapp.common.util.AfUtil;

import jcifs.Config;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Utility class to do some plumbing work in CIFS-to-appFeed conversion process.
 *
 * @author saung
 * @since v1.0 4/5/2011
 */
public class CifsHelper {
   /**
    * jCIFS Client Property for setting plain text password encryption.
    * @see "http://jcifs.samba.org/src/docs/api/overview-summary.html#scp"
    */
   private static final String JCIFS_SMB_CLIENT_DISABLE_PLAIN_TEXT_PASSWORDS_PROPERTY = "jcifs.smb.client.disablePlainTextPasswords";
   /**
    * Need to disable this flag so that the client will work with Windows CIFS share.
    * @see "http://lists.samba.org/archive/jcifs/2005-June/005156.html"
    */
   private static Boolean DISABLE_PLAIN_TEXT_PASSWORDS = Boolean.FALSE;
   /**
    * SMB Url path slash which is specific to Samba client (here we use jCIFS)
    * and may not work for other clients that use UNC style path.
    */
   private static final String SMB_URL_SLASH = "/";
   /**
    * jCIFS SMB url scheme
    * Syntax -  smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]file]]][?[param=value[param2=value2[...]]]
    */
   private static final String SMB_URL_SCHEME = "smb://";
   /**
    * get the logger
    */
   private static final Logger log = LoggerFactory.getLogger(CifsHelper.class);
   /**
    * Maximum directory level allowed to do crawling.
    */
   private static final int MAX_DIR_DEPTH = 100;


   /**
    * Crawl all the files under CIFS mount.
    *
    * @param smbUrl - a CIFS Samba mount url.
    * @param auth - a NTLM authentication instance.
    * @param dirDepth - a maximum directory depth to be crawled.
    * @return a list of file names with full path.
    * @throws IOException if any error raised during crawling.
    */
   public static List<String> crawl(String smbUrl, NtlmPasswordAuthentication auth, int dirDepth) throws IOException {
      if (AfUtil.anyEmpty(smbUrl)) {
         throw new IllegalArgumentException("Invalid Samba server url ->" + smbUrl);
      }
      if(dirDepth < 0 || dirDepth > MAX_DIR_DEPTH) {
         throw new IllegalArgumentException("dirDepth must be within 0-" + MAX_DIR_DEPTH
               + ", but it is set to " + dirDepth);
      }
      final SmbFile f = new SmbFile(smbUrl, auth);
      final List<String> scannedfiles = new ArrayList<String>();
      traverse(f, dirDepth, scannedfiles);

      return scannedfiles;
   }

   /**
    * Crawl all the files under CIFS mount.
    *
    * @param smbUrl - a CIFS Samba mount url.
    * @param auth - a NTLM authentication instance.
    * @param dirDepth - a maximum directory depth to be crawled.
    * @param converter - a converter to use during the crawl.
    * @return a list of file names with full path.
    * @throws MalformedURLException
    * @throws SmbException if any error raised during crawling.
    */
   public static <T extends DataSourceObject> List<T> crawl(
         String smbUrl,
         NtlmPasswordAuthentication auth,
         int dirDepth,
         IFileConverter<T> converter)
      throws SmbException, MalformedURLException
   {
      if (AfUtil.anyEmpty(smbUrl)) {
         throw new IllegalArgumentException("Invalid Samba server url ->" + smbUrl);
      }

      if (dirDepth < 0 || dirDepth > MAX_DIR_DEPTH) {
         throw new IllegalArgumentException("dirDepth must be within 0-" + MAX_DIR_DEPTH
               + ", but it is set to " + dirDepth);
      }

      final SmbFile f = new SmbFile(smbUrl, auth);
      final List<T> scannedItems = new ArrayList<T>();
      traverse(auth, f, dirDepth, converter, scannedItems);

      return scannedItems;
   }

   /**
    * Authenticate a NTLM client.
    *
    * @param domainOrGroup - a domain or group name (Optional)
    * @param username - a valid username.
    * @param password - a valid password.
    * @return an instance of NtlmPasswordAuthentication instance.
    *
    * @see "http://davenport.sourceforge.net/ntlm.html"
    */
   public static NtlmPasswordAuthentication authNTLMClient(
         String domainOrGroup,
         String username,
         String password)
   {
      Config.setProperty(JCIFS_SMB_CLIENT_DISABLE_PLAIN_TEXT_PASSWORDS_PROPERTY, DISABLE_PLAIN_TEXT_PASSWORDS.toString());
      log.info("jCIFS Client config = " + Config.getProperty(JCIFS_SMB_CLIENT_DISABLE_PLAIN_TEXT_PASSWORDS_PROPERTY));

      if (!StringUtils.hasLength(username)) {
         // Assume anonymous access
         return NtlmPasswordAuthentication.ANONYMOUS;
      }
      return new NtlmPasswordAuthentication(
            domainOrGroup,
            username == null ? "" : username,
            password == null ? "" : password);
   }

   /**
    * Get a SMB Url for given server and path.
    *
    * @param server - a Samba server hostname or IP address.
    * @param path - a share path.
    * @return a SMB Url
    */
   public static String getSMBUrl(String server, String path) {
      return (SMB_URL_SCHEME + getSMBServerPath(server, path));
   }


   /**
    * Get a SMB acceptable server + adding necessary seperators.
    *
    * @param server
    * @param path
    * @return
    */
   public static String getSMBServerPath(String server, String path) {
      return (server + AfUtil.appendIfNotExist(SMB_URL_SLASH, path, SMB_URL_SLASH));
   }


   /**
    * Get a SMB Url for given share path.
    *
    * @param serverPath - a Samba server hostname or IP address and share path
    * @return a SMB Url
    */
   public static String getSMBUrl(String serverPath) {
      if (!StringUtils.hasLength(serverPath)) {
         throw new IllegalArgumentException("serverPath is empty or null");
      }
      // Replace '\' with SMB Url /
      String serverPathFixed = serverPath.replace("\\", SMB_URL_SLASH);
      return String.valueOf(SMB_URL_SCHEME + AfUtil.appendIfNotExist(null, serverPathFixed, SMB_URL_SLASH));
   }

   /**
    * Parse jCifs domain and user name from the given string.
    *
    * @param str a string of either user name or domain/user name (or domain \ user name)
    * @return an array containing { "", username } or { domain, username }
    */
   public static String[] parseDomainAndUsername(String str) {
      if (!StringUtils.hasLength(str)) {
         return new String[]{ "", null };
      }
      String domainUser = str.replace("\\", "/");
      String[] temp = domainUser.split("/");
      // Assume the given string contains just a username.
      String[] domainUserArray = new String[]{ "", str };

      if (temp != null && temp.length == 2) {
         domainUserArray[0] = temp[0];
         domainUserArray[1] = temp[1];
      }
      return domainUserArray;
   }

   /**
    * Recursive method - traverse all files under a directory.
    *
    * @param f - a Samba file mount.
    * @param depth - a directory depth level.
    * @param scannedFiles - a list of accumulated files.
    *
    * @throws SmbException if any failure happened while being scanned.
    */
   private static void traverse(SmbFile f, int depth, List<String> scannedFiles) throws SmbException {
      if (depth <= 0) {
         return;
      }
      final SmbFile[] files = f.listFiles();
      for (SmbFile smbFile: files) {
         if (smbFile.isDirectory()) {
            traverse( smbFile, depth - 1, scannedFiles);
         } else {
            // getPath() will return filename with full path.
            if (log.isDebugEnabled()) {
               log.debug("File found =" + smbFile.getPath());
            }
            scannedFiles.add(smbFile.getPath());

         }
      }
   }

   /**
    * Recursive method - traverse all files under a directory.
    *
    * @param auth Authentication object currently in use.
    * @param f A Samba file mount.
    * @param depth A directory depth level.
    * @param converter A converter implementation.
    * @param scannedItems A list of accumulated items converted from files.
    * @throws SmbException If any failure happened while being scanned.
    */
   private static <T extends DataSourceObject> void traverse(
         NtlmPasswordAuthentication auth,
         SmbFile f,
         int depth,
         IFileConverter<T> converter,
         List<T> scannedItems)
      throws SmbException
   {
      if (depth <= 0) {
         return;
      }

      final SmbFile[] files = f.listFiles();
      for (final SmbFile smbFile: files) {
         if (smbFile.isDirectory()) {
            traverse(auth, smbFile, depth - 1, converter, scannedItems);
         }
         else {
            // getPath() will return filename with full path.
            String fileName = smbFile.getName();
            String path = smbFile.getParent();
            if (log.isDebugEnabled()) {
               log.debug("File {} found under {}", fileName, path);
            }
            final T item = converter.convert(auth, fileName, path);
            if (item != null) {
               scannedItems.add(item);
            }
         } // end if
      } // end for
   }

}
