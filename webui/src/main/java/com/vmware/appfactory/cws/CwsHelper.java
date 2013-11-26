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

package com.vmware.appfactory.cws;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.recipe.model.RecipeCommand;
import com.vmware.appfactory.recipe.model.RecipeFile;
import com.vmware.appfactory.recipe.model.RecipeStep;
import com.vmware.thinapp.common.converter.dto.Command;
import com.vmware.thinapp.common.converter.dto.CommandList;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;
import com.vmware.thinapp.common.converter.dto.ConversionRequest;
import com.vmware.thinapp.common.converter.dto.DsLocation;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.util.AfConstant;
import com.vmware.thinapp.common.util.AfUtil;
import com.vmware.thinapp.common.workpool.dto.Workpool;

public class CwsHelper
{
   protected static Logger _log = LoggerFactory.getLogger(CwsHelper.class);

   /**
    * Create a new ConversionRequest instance from the deconstructed parts
    * of an application, a recipe, and some output options.
    *
    * @param download
    * @param appInstallCommands
    * @param recipe
    * @param outputDatastoreId
    * @param workpool
    * @param runtimeId
    * @param enableQR
    * @param tagQR
    * @return
    * @throws CwsException
    */
   public static ConversionRequest generateConversionRequest(
           AppDownload download,
           List<AppInstall> appInstallCommands,
           Recipe recipe,
           Map<String, String> recipeVarValues,
           Long outputDatastoreId,
           Workpool workpool,
           Long runtimeId,
           boolean enableQR,
           String tagQR)
      throws CwsException
   {
      List<ProjectFile> requestFiles = new ArrayList<ProjectFile>();
      Map<ConversionPhase, CommandList> requestSteps = new HashMap<ConversionPhase, CommandList>();

      /* Create the input files from the given download */
      requestFiles.add(new ProjectFile(null, download.getURI().toString()));

      /*
       * If there is no recipe, or the recipe has no "install" commands defined,
       * then use the install commands from the given application.
       */
      if (recipe == null || !recipe.hasStepCommands(ConversionPhase.install)) {
         List<Command> installCommands = new ArrayList<Command>();
         for (AppInstall installCommand : appInstallCommands) {
            installCommands.add(new Command(null, installCommand.getCommand()));
         }
         requestSteps.put(ConversionPhase.install, new CommandList(installCommands));
      }
      else {
         _log.warn("Application install command(s) ignored: using install command(s) from recipe");
      }

      /* Process the recipe if one was given */
      if (recipe != null) {
         /* Grab the files from the recipe */
         if (recipe.getFiles() != null) {
            for (RecipeFile file : recipe.getFiles()) {
               requestFiles.add(new ProjectFile(
                     file.getName(),
                     file.getURI().toString()));
            }
         }

         /* Grab the commands from each step from the recipe */
         for (ConversionPhase phase : recipe.getSteps().keySet()) {
            /* Get the recipe step for the current phase */
            RecipeStep step = recipe.getStep(phase);
            if (step.getCommands() != null) {
               /* Get the current list of commands for the current phase */
               List<Command> currCommands = null;
               if (requestSteps.get(phase) == null
                     || requestSteps.get(phase).getCommands() == null) {
                  currCommands = new ArrayList<Command>();
               } else {
                  currCommands = requestSteps.get(phase).getCommands();
               }

               /* Add each command from the recipe step to the list of current
                * commands
                */
               for (RecipeCommand command : step.getCommands()) {
                  currCommands.add(new Command(command.getLabel(), command.getCommand()));
               }

               /* Create a command list if one doesn't already exist and we have
                * at least one command */
               if (currCommands.size() > 0) {
                  if (requestSteps.get(phase) == null) {
                     CommandList commands = new CommandList(currCommands);
                     requestSteps.put(phase, commands);
                  } else if (requestSteps.get(phase).getCommands() == null) {
                     requestSteps.get(phase).setCommands(currCommands);
                  }
               }
            }
         }
      }

      if (requestFiles.isEmpty()) {
         throw new CwsException("Conversion request contains no input files");
      }

      if (requestSteps.get(ConversionPhase.install) == null ||
            requestSteps.get(ConversionPhase.install).getCommands() == null ||
            requestSteps.get(ConversionPhase.install).getCommands().isEmpty()) {
         throw new CwsException("Conversion request has no install commands");
      }

      /* Swap variables for values in all recipe commands */
      if (recipeVarValues != null) {
         for (CommandList commands : requestSteps.values()) {
            commands.swapVariables(recipeVarValues);
         }
      }

      /*
       * Swap "$appfile" for application download.
       * We only need the relative path, since the 'files' section defines
       * the absolute URL where the file will be copied/downloaded from.
       */
      String relFileName = "\\\"" + calcAppFileName(download.getURI()) + "\\\"";
      _log.debug("Using relative filename for $appfile: {}", relFileName);
      for (CommandList commands : requestSteps.values()) {
         commands.swapVariable(AfConstant.APPFILE_VARIABLE, relFileName);
      }

      /* Where to put the output */
      DsLocation output = new DsLocation();
      output.setUrl("datastore://" + outputDatastoreId);

      /* Enable quality reporting if necessary */
      if (enableQR) {
         /* Get the current list of commands or an empty list if none exist */
         CommandList prebuildCommandList = requestSteps.get(ConversionPhase.prebuild);
         if (prebuildCommandList == null) {
            prebuildCommandList = new CommandList();
         }

         List<Command> prebuildCommands = prebuildCommandList.getCommands();
         if (prebuildCommands == null) {
            prebuildCommands = new ArrayList<Command>();
         }
         CwsHelper.enableQR(prebuildCommands, tagQR);
         prebuildCommandList.setCommands(prebuildCommands);
         requestSteps.put(ConversionPhase.prebuild, prebuildCommandList);
      }

      /* Return the conversion request object */
      return new ConversionRequest(
            requestFiles,
            output,
            requestSteps,
            workpool,
            runtimeId);
   }

   public static void enableQR(List<Command> prebuildCommands, String tagQR) {
      final String iniFile = "\"%ProjectDirectory%\\package.ini\"";
      final String enableQRCommand = String.format("setoption.exe %s %s 1 %s",
            CwsSettingsIni.BUILD_OPTIONS, CwsSettingsIni.QR_ENABLED_KEY, iniFile);
      final String defineQRTagCommand = String.format("setoption.exe %s %s %s %s",
            CwsSettingsIni.BUILD_OPTIONS, CwsSettingsIni.QR_TAG_KEY, tagQR, iniFile);

      /* Create prebuild commands for setting the flags to enable QR. */
      prebuildCommands.add(new Command("Enable Quality Reporting", enableQRCommand));
      prebuildCommands.add(new Command("Define Quality Reporting Tag", defineQRTagCommand));
   }

   private static String calcAppFileName(URI fileUri)
   {
      URL url = null;
      try {
         url = fileUri.toURL();
      } catch (MalformedURLException ex) {
         // Given URI isn't a valid URL, so just try to get a filename from the
         // URI itself instead of making an HTTP request.
         // Do nothing, we'll handle this later.
      } catch (IllegalArgumentException ex) {
         // Same as above
      }

      String filename = AfUtil.getFilenameFromUrl(url);

      if (filename == null) {
         // HTTP headers failed to provide a filename, default to using the URI
         // itself.
         filename = AfUtil.extractLastURIToken(fileUri);
         _log.debug("Extracted filename from appFile URI: {}", filename);
      }

      return filename;
   }
}
