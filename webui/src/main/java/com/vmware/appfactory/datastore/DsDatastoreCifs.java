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

package com.vmware.appfactory.datastore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import jcifs.smb.SmbFile;

import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.fileshare.model.FileShare;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * A specific DsDatastore implementation which represents datastores using the
 * CIFS file system. Since we only really support CIFS and nothing else, we
 * just extend AbstractDatastore, which does 90% of the work, and then add the
 * more complex methods like open(), copy(), etc.
 */
@JsonSerialize(include=Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class DsDatastoreCifs
   extends AbstractDatastore
{
   private static final int COPY_BUFFER_SIZE = 10000;

   /**
    * Create a new instance.
    * Do not call directly. Instead, use DsUtil.fromDTO()
    * @param dto
    */
   DsDatastoreCifs(Datastore dto)
   {
      super(dto);
      super.setType(Type.cifs);
   }


   /**
    * Create a new instance.
    */
   public DsDatastoreCifs(
         String uniqueName,
         String server,
         String path,
         String domain,
         String username,
         String password,
         String mountPath)
   {
      super(
         uniqueName,
         server,
         path,
         domain,
         username,
         password,
         mountPath);
      super.setType(Type.cifs);
   }


   /**
    * Get an SMB path to the root of the datastore.
    * TODO: Can we create a URI and use toString()?
    *
    * Note: this is private so that the field will not
    * be serialized via JSON.  It's not called from outside
    * the class or declared in an interface anyway.
    *
    * @return
    */
   private String getSmbUrl()
   {
      return getBaseUrl("smb", true);
   }


   @Override
   public String buildPath(String... components)
   {
      return buildPathWithSep(DsUtil.FILE_SEPARATOR, components);
   }


   @Override
   public void copy(MultipartFile source, String destination, ProgressListener pl)
      throws IOException
   {
      /* Create destination file */
      File destFile = new File(destination);

      /* Open input and output, for copying */
      InputStream is = source.getInputStream();
      OutputStream os = new FileOutputStream(destFile);

      try {
         /* We could use IOUtils.copy(), but this approach gives us progress: */
         long total = source.getSize();
         long soFar = 0;

         /* Transfer using a buffer */
         byte[] buffer = new byte[COPY_BUFFER_SIZE];
         int numRead;
         while ((numRead = is.read(buffer, 0, COPY_BUFFER_SIZE)) != -1) {
            os.write(buffer, 0, numRead);
            soFar += numRead;
            if (pl != null) {
               pl.update(soFar, total, 1);
            }
         }

         os.flush();
      } finally {
         os.close();
         is.close();
      }
   }


   @Override
   public InputStream openStream(String filePath)
      throws IOException
   {
      /* Create destination file */
      String url = buildPath(getSmbUrl(), filePath);
      SmbFile file = new SmbFile(url);
      return file.getInputStream();
   }


   /**
    * FIXME: move this method to a service class.
    */
   @Override
   public FileShare findOrCreateFileshare(FileShareDao fsDao)
   {
      FileShare fs = fsDao.findByDatastoreId(getId());

      if (fs == null) {

         fs = createFileShare();
         fsDao.create(fs);
      }
      return fs;
   }


   @Override
   public FileShare createFileShare()
   {
      final FileShare fs = new FileShare();

      fs.setName(getName());
      fs.setStatus(FileShare.Status.SCANNED);
      fs.setDatastoreName(getName());
      fs.setOkToScan(true);
      fs.setOkToConvert(false);
      fs.setServerPath(getServerPath());
      fs.setUsername(getUsername());
      fs.setPassword(getPassword());
      fs.setDatastoreId(getId());

      // TODO: Should we use the same datastore name for this new file share.
      fs.setDescription("Created automatically from datastore \"" + getName() + "\"");

      return fs;
   }


   @Override
   public String createDirsIfNotExists(String... directories)
      throws IOException
   {
      _log.debug("mount path = {}", getMountPath());
      /* Create an absolute directory path */
      String path = getMountPath();
      for (String dirName : directories) {
         path = buildPath(path, dirName) + "/";
      }
      _log.debug("absolute path = {}", path);
      /* Make a directory including nonexistent parent directories. */
      FileUtils.forceMkdir(new File(path));

      /* Return the full directory we just created */
      return path;
   }

   /**
    * Get this datastore type.
    */
   @Override
   public Type getType()
   {
      return Type.cifs;
   }

   /**
    * Get a URL path to the root of the datastore.
    * TODO: This is somewhat bogus. Try not to use it.
    *
    * @return
    */
   @Deprecated
   public String getBaseUrl(String scheme, boolean embedAuth)
   {
      /* We need at least a server or a path */
      if (!StringUtils.hasLength(getServer()) && !StringUtils.hasLength(getServerPath())) {
         return null;
      }

      /* Start with URL scheme */
      StringBuilder sb = new StringBuilder(scheme + "://");

      /* Optional domain */
      if (StringUtils.hasLength(getDomain())) {
         sb.append(getDomain()).append(";");
      }

      /* Insert 'user:pass@' if needed */
      if (embedAuth && requiresAuth()) {
         try {
            String pass = URLEncoder.encode(getPassword(), "UTF-8");

            sb.append(getUsername());
            sb.append(":");
            if (StringUtils.hasLength(pass)) {
               sb.append(pass);
            }
            sb.append("@");
         }
         catch(UnsupportedEncodingException ex) {
            /* Should not happen, but just in case */
            _log.error("Failed to embed auth into datastore URL", ex);
         }
      }

      /* Server and port */
      sb.append(getServer());
      if (getPort() > 0) {
         sb.append(":").append(getPort());
      }

      /* Share path */
      String share = StringUtils.hasLength(getShare())
            ? getShare().replace("\\", DsUtil.FILE_SEPARATOR) : "";
      sb.append(buildPath("", share));

      return sb.toString();
   }


   /**
    * Get a server path in xxx.xxx.xxx/share/path format.
    *
    * @return a server path string.
    */
   private String getServerPath()
   {
      String serverPath = null;

      if (StringUtils.hasLength(getShare())) {
         serverPath = getServer()
            + AfUtil.appendIfNotExist(DsUtil.FILE_SEPARATOR, getShare()
            .replace("\\", "/"), null);
      }
      else {
         serverPath = getServer() + DsUtil.FILE_SEPARATOR;
      }

      /* Trim any leading \\ or // */
      serverPath = StringUtils.trimLeadingCharacter(serverPath, '\\');
      serverPath = StringUtils.trimLeadingCharacter(serverPath, '/');

      /* Replace "\" with "/" because Samba format uses / */
      serverPath = serverPath.replace("\\", "/");

      return serverPath;
   }


   private boolean requiresAuth()
   {
      return StringUtils.hasLength(getUsername());
   }
}
