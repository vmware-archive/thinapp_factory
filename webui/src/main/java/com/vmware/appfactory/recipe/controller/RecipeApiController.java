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

package com.vmware.appfactory.recipe.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfForbiddenException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.recipe.ExportMixIns;
import com.vmware.appfactory.recipe.dao.RecipeDao;
import com.vmware.appfactory.recipe.dto.RecipeFileUploadResponse;
import com.vmware.appfactory.recipe.dto.RecipeStoreRequest;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.recipe.model.RecipeAppKey;
import com.vmware.appfactory.recipe.model.RecipeCommand;
import com.vmware.appfactory.recipe.model.RecipeFile;
import com.vmware.appfactory.recipe.model.RecipeStep;
import com.vmware.appfactory.recipe.model.RecipeVariable;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfConstant;
import com.vmware.thinapp.common.util.AfUtil;


/**
 * Handles all the API calls related to recipes.
 */
@Controller
public class RecipeApiController
   extends AbstractApiController
{
   /** Used for serializing recipes for export */
   private static final ObjectMapper EXPORT_OBJECT_MAPPER;

   static {
      EXPORT_OBJECT_MAPPER = new ObjectMapper();
      EXPORT_OBJECT_MAPPER.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

      EXPORT_OBJECT_MAPPER.getSerializationConfig().addMixInAnnotations(
            Recipe.class,
            ExportMixIns.Recipe.class);

      EXPORT_OBJECT_MAPPER.getSerializationConfig().addMixInAnnotations(
            RecipeAppKey.class,
            ExportMixIns.Record.class);

      EXPORT_OBJECT_MAPPER.getSerializationConfig().addMixInAnnotations(
            RecipeFile.class,
            ExportMixIns.RecipeFile.class);

      EXPORT_OBJECT_MAPPER.getSerializationConfig().addMixInAnnotations(
            RecipeVariable.class,
            ExportMixIns.Record.class);

      EXPORT_OBJECT_MAPPER.getSerializationConfig().addMixInAnnotations(
            RecipeStep.class,
            ExportMixIns.Record.class);

      EXPORT_OBJECT_MAPPER.getSerializationConfig().addMixInAnnotations(
            RecipeCommand.class,
            ExportMixIns.Record.class);
   }

   /**
    * Return a list of all recipes.
    * @param sort
    * @return
    */
   @ResponseBody
   @RequestMapping(
         value = "/recipes",
         method = RequestMethod.GET)
   public List<Recipe> getAllrecipes(
         @RequestParam(required=false) boolean sort)
   {
      List<Recipe> recipes = _daoFactory.getRecipeDao().findAll();

      if (sort) {
         Collections.sort(recipes);
      }

      return recipes;
   }


   /**
    * Return one recipe.
    * @param id
    * @return
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/recipes/{id}",
         method = RequestMethod.GET)
   public Recipe getRecipe(
         @PathVariable Long id)
      throws AfBadRequestException, AfNotFoundException
   {
      if (id == null) {
         throw new AfBadRequestException("Invalid recipe ID");
      }

      Recipe record = _daoFactory.getRecipeDao().find(id);
      if (record == null) {
         throw new AfNotFoundException("Recipe " + id + " not found");
      }

      return record;
   }


   /**
    * Update an existing recipe.
    * @param id
    * @param request
    *
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/recipes/{id}",
         method = RequestMethod.PUT)
   public void updateRecipe(
         @PathVariable Long id,
         @RequestBody RecipeStoreRequest request)
      throws AfBadRequestException, AfNotFoundException, AfForbiddenException
   {
      if (id == null) {
         throw new AfBadRequestException("Invalid recipe ID");
      }

      RecipeDao dao = _daoFactory.getRecipeDao();
      Recipe record = dao.find(id);

      if (record == null) {
         throw new AfNotFoundException("recipe " + id + " not found");
      }
      if (record.isReadOnly()) {
         throw new AfForbiddenException("Recipe " + id + " is read-only");
      }

      record.setName(request.name);
      record.setDescription(request.description);

      record.setVariables(request.variables);
      record.setFiles(request.files);
      record.setSteps(request.steps);
      record.setAppKeys(request.appKeys);

      dao.update(record);
   }


   /**
    * Create a new recipe.
    *
    * @param request
    * @return A recipe containing only the id and name that was created.
    *
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    * @throws AfConflictException If the recipe name is already in use
    */
   @ResponseBody
   @RequestMapping(
         value = "/recipes",
         method = RequestMethod.POST)
   public Recipe createRecipe(
         @RequestBody RecipeStoreRequest request)
      throws AfBadRequestException, AfNotFoundException, AfConflictException
   {
      RecipeDao dao = _daoFactory.getRecipeDao();
      String name = request.name;

      /* Check for unique name */
      if (dao.findByName(name) != null) {
         if (!request.renameIfNeeded) {
            throw new AfConflictException("Recipe \"" + name + "\" already exists.");
         }
         name = dao.findUniqueName(name);
      }

      Recipe recipe = new Recipe();

      recipe.setName(name);
      recipe.setDescription(request.description);

      recipe.setVariables(request.variables);
      recipe.setFiles(request.files);
      recipe.setSteps(request.steps);
      recipe.setAppKeys(request.appKeys);

      Long id = dao.create(recipe);

      // Return the recipe name and id in a recipe object.
      Recipe result = new Recipe();
      result.setId(id);
      result.setName(recipe.getName());
      return result;
   }


   /**
    * Clone an existing recipe. Makes an exact duplicate, with just the
    * name changed (to "Copy Of <Original Name>").
    *
    * @param id
    *
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/recipes/{id}/clone",
         method = RequestMethod.POST)
   public void cloneRecipe(
         @PathVariable Long id)
      throws AfBadRequestException, AfNotFoundException
   {
      if (id == null) {
         throw new AfBadRequestException("Invalid recipe ID");
      }

      /* Get current recipe */
      RecipeDao dao = _daoFactory.getRecipeDao();
      Recipe recipe = dao.find(id);
      if (recipe == null) {
         throw new AfNotFoundException("recipe " + id + " not found");
      }

      /* Clone into a new recipe with a new name. */
      Recipe newRecipe = recipe.clone();
      newRecipe.setName(dao.findUniqueName(recipe.getName()));

      /* Save as a new recipe */
      dao.create(newRecipe);
   }


   /**
    * Delete a recipe.
    * @param id
    *
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/recipes/{id}",
         method = RequestMethod.DELETE)
   public void deleteRecipe(
         @PathVariable Long id)
      throws AfBadRequestException, AfNotFoundException
   {
      if (id == null) {
         throw new AfBadRequestException("Invalid recipe type");
      }

      RecipeDao dao = _daoFactory.getRecipeDao();
      Recipe record = dao.find(id);

      if (record == null) {
         throw new AfNotFoundException("recipe " + id + " not found");
      }

      dao.delete(record);
      return;
   }


   /**
    * Export a recipe to a temporary server file (Stage 1 of 2).
    *
    * In order to return download data in response to AJAX calls, and properly
    * handle errors, we need to export recipes in two stages:
    *
    * Stage 1: In response to a POST, export the recipe to a local temporary
    * ZIP file, and return an ID that maps to that file. In the case of an
    * error, throw an exception instead.
    *
    * Stage 2: In response to a GET which includes an export ID, pipe the
    * corresponding temporary file to the client as an attachment, then delete
    * the temporary file.
    *
    * @param recipeId
    * @param request
    * @param response
    *
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    */
   @ResponseBody
   @RequestMapping(
         value = "/recipes/{recipeId}/export",
         method = RequestMethod.POST)
   public String exportRecipeToServerFile(
         @PathVariable Long recipeId,
         HttpServletRequest request,
         HttpServletResponse response)
      throws AfBadRequestException, AfNotFoundException, AfServerErrorException
   {
      if (recipeId == null) {
         throw new AfBadRequestException("Invalid/missing recipe id");
      }

      /* Get the recipe to be exported */
      Recipe recipe = _daoFactory.getRecipeDao().find(recipeId);
      if (recipe == null) {
         throw new AfNotFoundException("Invalid recipe id " + recipeId);
      }

      /*
       * We export everything to a temporary file first. That way, if there are
       * any errors, we can respond with an error *before* writing any data.
       * If we start writing data, the client gets it even if we encounter an
       * error, which we don't want.
       */
      OutputStream os = null;
      File tempFile = null;

      try {
         // TODO: What if TEMP space is limited and the export file is big?
         tempFile = File.createTempFile("vmtaf", ".zip");
         tempFile.deleteOnExit();
         os = new FileOutputStream(tempFile);

         /* Write recipe to a temporary file */
         _log.debug("Writing recipe to temp ZIP file " + tempFile.getAbsolutePath());
         writeToZip(os, recipe);
         IOUtils.closeQuietly(os);
         _log.debug("Temp ZIP file OK");

         /* Save file with user session */
         String key = "recipe-export-" + recipe.getId();
         request.getSession().setAttribute(key, tempFile);
         _log.debug("Mapped session key {} to {}", key, tempFile);

         return key;
      }
      catch(IOException ex) {
         _log.error("Temp ZIP file failed", ex);
         if (tempFile != null) {
            tempFile.delete();
         }
         throw new AfServerErrorException(ex);
      }
      finally {
         IOUtils.closeQuietly(os);
      }
   }


   /**
    * Write a temporary recipe export file as an attachment (Stage 2 of 2).
    *
    * In order to return download data in response to AJAX calls, and properly
    * handle errors, we need to export recipes in two stages:
    *
    * Stage 1: In response to a POST, export the recipe to a local temporary
    * ZIP file, and return an ID that maps to that file. In the case of an
    * error, throw an exception instead.
    *
    * Stage 2: In response to a GET which includes an export ID, pipe the
    * corresponding temporary file to the client as an attachment, then delete
    * the temporary file.
    *
    * @param recipeId
    * @param exportId
    * @param request
    * @param response
    *
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    */
   @RequestMapping(
         value="/recipes/{recipeId}/export/{exportId}",
         method=RequestMethod.GET)
   public void exportRecipe(
         @PathVariable Long recipeId,
         @PathVariable String exportId,
         HttpServletRequest request,
         HttpServletResponse response)
      throws AfBadRequestException, AfNotFoundException, AfServerErrorException
   {
      if (recipeId == null) {
         throw new AfBadRequestException("Invalid/missing recipe id");
      }

      if (exportId == null) {
         throw new AfBadRequestException("Invalid/missing export id");
      }

      /* Get the recipe to be exported */
      Recipe recipe = _daoFactory.getRecipeDao().find(recipeId);
      if (recipe == null) {
         throw new AfNotFoundException("Invalid recipe id " + recipeId);
      }

      /*
       * Map the export ID back to the temporary file.
       */
      File exportFile = (File) request.getSession().getAttribute(exportId);
      request.getSession().removeAttribute(exportId);
      if (exportFile == null) {
         throw new AfBadRequestException("Invalid export id " + exportId);
      }

      /*
       * Now we know the temporary file, write it back to the client in the
       * response body.
       */
      InputStream is = null;

      try {
         /* Set the response to be a download file */
         response.setHeader(
            AfUtil.CONTENT_DISPOSITION,
            "attachment; filename=" + toFileName(recipe.getName() + ".zip"));

         /* Copy ZIP file to response body */
         _log.debug("Copying temp ZIP file to response body");
         is = new FileInputStream(exportFile);
         IOUtils.copy(is, response.getOutputStream());
         _log.debug("Copy of temp ZIP file OK");
      }
      catch(IOException ex) {
         _log.debug("Copy of temp ZIP file failed", ex);
         throw new AfServerErrorException(ex);
      }
      finally {
         IOUtils.closeQuietly(is);
         exportFile.delete();
      }

      return;
   }


   @ResponseBody
   @RequestMapping(
         value = "/recipes/uploadfile",
         method = RequestMethod.POST)
   public RecipeFileUploadResponse uploadRecipeFile(
         @RequestParam(value="recipeFile") MultipartFile recipeFile)
      throws AfBadRequestException, AfServerErrorException
   {
      _log.debug("Uploading recipe file");
      _log.debug("  Original Name = " + recipeFile.getOriginalFilename());
      _log.debug("  File Size     = " + recipeFile.getSize());

      try {
         DsDatastore ds = null;
         Long dsId = Long.valueOf(_config.getLong(ConfigRegistryConstants.DATASTORE_DEFAULT_RECIPE_ID));
         _log.debug("  DS ID = " + dsId);

         if (dsId == null) {
            throw new AfBadRequestException("No datastore selected for recipe uploads");
         }

         ds = _dsClient.findDatastore(dsId, true);
         if (ds == null) {
            throw new AfBadRequestException("Invalid datastore ID " + dsId + " for recipe uploads");
         }

         /* Create a unique folder using timestamp */
         String dest = ds.createDirsIfNotExists("recipe-files", "" + AfCalendar.Now());
         _log.debug("  Created folder " + dest);

         dest = ds.buildPath(dest, recipeFile.getOriginalFilename());
         _log.debug("  Copying to " + dest);

         ds.copy(recipeFile, dest, null);
         URI uri = DsUtil.generateDatastoreURI(dsId, dest);
         _log.debug("  Copy complete: URI = " + uri.toString());

         RecipeFileUploadResponse response = new RecipeFileUploadResponse();
         response.uri = uri;
         return response;
      }
      catch (DsException ex) {
         throw new AfServerErrorException(ex);
      }
      catch (IOException ex) {
         throw new AfServerErrorException(ex);
      }
      catch (URISyntaxException ex) {
         throw new AfServerErrorException(ex);
      }
   }


   // TODO: Make this a method in Recipe.java
   private void writeToZip(OutputStream os, Recipe recipe)
      throws IOException
   {
      InputStream is = null;
      ZipOutputStream zos = null;

      try {
         zos = new ZipOutputStream(os);
         _log.debug("Opened a ZIP stream");

         /* Write recipe into the ZIP stream */
         String jsonFileName = toFileName(recipe.getName() + AfConstant.RECIPE_FILE_EXTENSION);
         _log.debug("Writing the JSON file ({})", jsonFileName);
         zos.putNextEntry(new ZipEntry(jsonFileName));
         String json = EXPORT_OBJECT_MAPPER.writeValueAsString(recipe);
         zos.write(json.getBytes());

         /* Copy all payload files into the temporary directory */
         for (RecipeFile file : recipe.getFiles()) {
            if (file.getPath() != null) {
               try {
                  writeRecipeFileToZip(recipe, file, zos);
               }
               catch(IOException ex) {
                  throw new IOException(
                        "Failed to export recipe file " + file.getURI().toString(),
                        ex);
               }
            }
         }

         /* Done with the ZIP file */
         zos.finish();
         zos = null;
      }
      catch(IOException ex) {
         IOUtils.closeQuietly(zos);
         IOUtils.closeQuietly(is);
         throw ex;
      }
   }


   // TODO: Make this a method in Recipe.java or RecipeFile.java
   private void writeRecipeFileToZip(Recipe recipe, RecipeFile file, ZipOutputStream zos)
      throws IOException
   {
      _log.debug("Writing recipe payload file {}, URI = {}",
            file.getPath(),
            file.getURI().toString());

      URI uri = file.getURI();
      InputStream is = null;

      try {
         zos.putNextEntry(new ZipEntry(file.getPath()));

         if (uri.getScheme().equals(DsUtil.DATASTORE_URI_SCHEME)) {
            /*
             * Use datastore methods to copy the file from the datastore
             * into the ZIP file.
             */
            Long dsId = Long.valueOf(uri.getHost());
            DsDatastore ds = _dsClient.findDatastore(dsId, true);
            if (ds == null) {
               throw new URISyntaxException(
                  uri.toString(),
                  "Datastore URI has invalid ID " + uri.getHost());
            }

            is = ds.openStream(uri.getPath());
            IOUtils.copy(is, zos);
         }
         else {
            /*
             * Use regular HTTP methods to copy file from URL into the ZIP file.
             */
            URL url = uri.toURL();
            is = new BufferedInputStream(url.openStream());
            IOUtils.copy(is, zos);
         }
      }
      catch (URISyntaxException ex) {
         throwBadLocationException(recipe, uri.toString());
      }
      catch(DsException ex) {
         throwBadLocationException(recipe, uri.toString());
      }
      catch(MalformedURLException ex) {
         throwBadLocationException(recipe, uri.toString());
      }
      finally {
         IOUtils.closeQuietly(is);
      }
   }


   /**
    * Convert an object name (e.g. "Office 2007/2010 From ISO.zip") into
    * a decent-looking filename (e.g. "Office_2007_2010_From_ISO.zip")
    *
    * @param originalName
    * @return
    */
   private String toFileName(String originalName)
   {
      /* Replace spaces and slashes with an underscore */
      String newName = originalName.replaceAll("[ /\\\\]", "_");
      return newName;
   }


   private void throwBadLocationException(
         Recipe recipe,
         String path)
      throws IOException
   {
      String msg = String.format(
            "Recipe %s has a file with a malformed URI %s",
            recipe.getName(),
            path);
      _log.warn(msg);
      throw new IOException(msg);
   }
}
