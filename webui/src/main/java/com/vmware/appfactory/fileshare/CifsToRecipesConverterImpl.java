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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.feed.FeedJsonParserV4;
import com.vmware.appfactory.feed.exception.FeedJsonFormatException;
import com.vmware.appfactory.fileshare.exception.FeedConverterErrorCode;
import com.vmware.appfactory.fileshare.exception.FeedConverterException;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.recipe.model.RecipeFile;
import com.vmware.thinapp.common.util.AfConstant;
import com.vmware.thinapp.common.util.AfJson;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * Implementation of IFeedConverter which scans a CIFS file share looking for
 * recipes.
 *
 * @author levans
 */
public class CifsToRecipesConverterImpl
   implements IFeedConverter<Recipe>
{
   static final Logger _log = LoggerFactory.getLogger(CifsToRecipesConverterImpl.class);

   @Resource
   private ConfigRegistry _config;


   @Override
   public List<Recipe> scanObjects(
         final String smbUrl,
         String group,
         String username,
         String password,
         long appTimeStamp,
         final Long cwsDataStoreId)
      throws FeedConverterException
   {
      String recipeDir = _config.getString(ConfigRegistryConstants.FILESHARE_RECIPE_DIR);
      if (StringUtils.isEmpty(recipeDir)) {
         return new ArrayList<Recipe>();
      }

      String recipeUrl = smbUrl + recipeDir;
      if (!recipeUrl.endsWith("/")) {
         recipeUrl += "/";
      }

      try {
         int maxDepth = 2;

         return CifsHelper.crawl(
               recipeUrl,
               CifsHelper.authNTLMClient(group, username, password),
               maxDepth,
               new IFileConverter<Recipe>() {
                  @Override
                  public Recipe convert(
                     NtlmPasswordAuthentication auth,
                     String fileName,
                     String fullSmbPath,
                     String... parentDirs)
                  {
                     Recipe recipe = null;
                     _log.debug("Recipe crawler found: " + fullSmbPath);
                     if (fileName.endsWith(AfConstant.RECIPE_FILE_EXTENSION)) {
                        /** Exclude SAMBA URL from the path */
                        final int smbUrlLength = smbUrl.length();
                        String relativePath = fullSmbPath.substring(smbUrlLength);

                        recipe = createRecipe(
                              auth,
                              cwsDataStoreId,
                              fileName,
                              fullSmbPath,
                              relativePath);
                     }
                     return recipe;
                  }
               });
      }
      catch (SmbException ex) {
         int status = ex.getNtStatus();
         FeedConverterErrorCode errCode = FeedConverterErrorCode.fromNtStatus(status);
         _log.info("Recipe crawl failed:" +
               " status=" + status +
               " code=" + errCode +
               " error=" + ex.getMessage());

         if (errCode == FeedConverterErrorCode.NotFound) {
            /* OK if not found, it's optional anyway */
            return null;
         }

         String message = "Recipe scan of " + recipeUrl + " failed: " + ex.getMessage();
         throw new FeedConverterException(errCode, message);
      }
      catch (IOException ex) {
         throw new FeedConverterException(
               FeedConverterErrorCode.Other,
               ex);
      }
   }

   Recipe createRecipe(
         NtlmPasswordAuthentication auth,
         Long dataStoreId,
         String fileName,
         String smbPath,
         String relativePath)
   {
      try {
         _log.debug("Creating Recipe:");
         _log.debug("    fileName = " + fileName);
         _log.debug("    smbPath = " + smbPath);
         _log.debug("    relativePath = " + relativePath);

         SmbFile file = new SmbFile(smbPath + fileName, auth);

         /* Parse file into JSON */
         InputStream is = file.getInputStream();
         JsonNode node = AfJson.ObjectMapper().readTree(is);
         is.close();

         /* Create a feed from the JSON data */
         FeedJsonParserV4 parser = new FeedJsonParserV4();
         Recipe recipe = parser.parseRecipe(node);

         /* Convert relative URLs to absolute URLs */
         URI parentURI = DsUtil.generateDatastoreURI(
            // preview scans have no datastore
            dataStoreId == null ? new Long(0) : dataStoreId,
            relativePath);

         for (RecipeFile recFile : recipe.getFiles()) {
            if (recFile.getURI() == null) {
               recFile.setURI(AfUtil.relToAbs(recFile.getPath(), parentURI));
            }
         }

         return recipe;
      }
      // XXX error handling needs more love?
      catch (MalformedURLException ex) {
         _log.error("Can't read recipe: Bad URL", ex);
      }
      catch (URISyntaxException ex) {
         _log.error("Can't read recipe: Bad URI", ex);
      }
      catch (IOException ex) {
         _log.error("Can't read recipe: I/O error", ex);
      }
      catch (FeedJsonFormatException ex) {
         _log.error("Can't read recipe: Feed format error", ex);
      }

      return null;
   }
}
