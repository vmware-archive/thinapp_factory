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

package com.vmware.appfactory.feed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonNode;
import org.springframework.util.StringUtils;

import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppIcon;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.feed.exception.FeedJsonFormatException;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.recipe.model.RecipeAppKey;
import com.vmware.appfactory.recipe.model.RecipeCommand;
import com.vmware.appfactory.recipe.model.RecipeFile;
import com.vmware.appfactory.recipe.model.RecipeStep;
import com.vmware.appfactory.recipe.model.RecipeVariable;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfConstant;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * This class reads a "version 4" ThinAppFactory feed in JSON format.
 */
public class FeedJsonParserV4
   extends AbstractFeedParser
{
   private static final String[] REQUIRED_APPLICATION_FIELDS = { "name", "version", "file" };
   private static final String[] REQUIRED_RECIPE_FIELDS = { "name" };
   private static final String[] REQUIRED_RECIPE_COMMAND_FIELDS = { "label", "command" };
   private static final Pattern RECIPE_VAR_NAME_PATTERN = Pattern.compile(AfConstant.VARIABLE_NAME_REGEX);

   @Override
   public Feed parse(JsonNode rootNode)
      throws FeedJsonFormatException
   {
      JsonNode incsN = rootNode.get("includes");
      JsonNode nameN = rootNode.get("name");
      JsonNode descN = rootNode.get("description");
      JsonNode appsN = rootNode.get("applications");
      JsonNode recipesN = rootNode.get("recipes");

      /* Create feed instance */
      Feed feed = new Feed();

      /* Feed description (optional) */
      if (descN != null) {
         feed.setDescription(parseText(descN));
      }

      /* Feed name (optional) */
      if (nameN != null) {
         feed.setName(nameN.getTextValue());
      }

      /* Includes (optional) */
      parseIncludes(incsN, feed);

      /* Application list (optional) */
      if (appsN != null) {
         List<FeedApplication> apps = parseApplications(appsN);
         for (FeedApplication app : apps) {
            feed.addApplication(app);
            for (Recipe recipe : app.getRecipes()) {
               feed.addRecipe(recipe);
            }
         }
      }

      /* Recipe list (optional) */
      if (recipesN != null) {
         List<Recipe> recipes = parseRecipes(recipesN);
         for (Recipe recipe : recipes) {
            feed.addRecipe(recipe);
         }
      }

      return feed;
   }


   /**
    * Parse an application in version 4 format.
    */
   @Override
   public FeedApplication parseApplication(JsonNode appNode)
      throws FeedJsonFormatException
   {
      verifyRequiredFields(
         "Application",
         appNode,
         REQUIRED_APPLICATION_FIELDS);

      /* Create application, with all required data */
      FeedApplication app = new FeedApplication();
      app.setName(appNode.get("name").getValueAsText());
      app.setVersion(appNode.get("version").getValueAsText());

      /* Application files */
      JsonNode fileN = appNode.get("file");
      AppDownload file = parseApplicationFile(fileN);
      app.setDownload(file);

      /* Description? */
      JsonNode descN = appNode.get("description");
      if (descN != null) {
         try {
            app.setDescription(parseText(descN));
         }
         catch(Exception ex) {
            throw new FeedJsonFormatException(
                  appNode,
                  "Invalid \"description\" for application \"" +
                  app.getDisplayName() + "\"",
                  ex);
         }
      }

      /* Categories? */
      JsonNode catsN = appNode.get("categories");
      if (catsN != null && catsN.isArray()) {
         Set<String> categories = new HashSet<String>();

         Iterator<JsonNode> catsIt = catsN.iterator();
         while (catsIt.hasNext()) {
            String name = catsIt.next().getTextValue();
            categories.add(name);
         }

         app.setCategories(categories);
      }

      /* Vendor? */
      JsonNode vendorN = appNode.get("vendor");
      if (vendorN != null) {
         app.setVendor(vendorN.getTextValue());
      }

      /* Icons? */
      JsonNode iconsN = appNode.get("icons");
      if (iconsN != null && iconsN.isArray()) {
         List<AppIcon> icons = new ArrayList<AppIcon>();

         Iterator<JsonNode> iconsIt = iconsN.iterator();
         while (iconsIt.hasNext()) {
            AppIcon icon = parseIcon(iconsIt.next());
            icons.add(icon);
         }

         app.setIcons(icons);
      }

      /* EULA? */
      JsonNode eulaN = appNode.get("eula");
      if (eulaN != null) {
         app.setEula(parseText(eulaN));
      }

      /* Last remote update? */
      JsonNode updateN = appNode.get("lastRemoteUpdate");
      if (updateN != null && StringUtils.hasLength(updateN.getValueAsText())) {
         try {
            long when = AfCalendar.Parse(updateN.getValueAsText());
            app.setLastRemoteUpdate(when);
         }
         catch(Exception ex) {
            throw new FeedJsonFormatException(
               appNode,
               "Invalid \"lastRemoteUpdate\" for application \"" +
               app.getDisplayName() + "\" (" + ex.getMessage() + ")",
               ex);
         }
      }

      /* Locale? */
      JsonNode localeN = appNode.get("locale");
      if (localeN != null && StringUtils.hasLength(localeN.getValueAsText())) {
         app.setLocale(localeN.getValueAsText());
      }

      /* Installer revision? */
      JsonNode instRevNode = appNode.get("installerRevision");
      if (instRevNode != null && StringUtils.hasLength(instRevNode.getValueAsText())) {
         app.setInstallerRevision(instRevNode.getValueAsText());
      }

      /* Application-specific install or recipe */
      JsonNode installNode = appNode.get("install");
      JsonNode recipeNode = appNode.get("recipe");
      JsonNode recipesNode = appNode.get("recipes");

      /* There can be only one! */
      int count = 0 +
         (installNode != null ? 1 : 0) +
         (recipeNode != null ? 1 : 0) +
         (recipesNode != null ? 1 : 0);

      if (count > 1) {
            throw new FeedJsonFormatException(
               appNode,
               "Application can define only one of \"install\", \"recipe\", or \"recipes\"");
      }

      if (installNode != null) {
         // XXX temporary: create old-style AppInstalls for now
         List<AppInstall> installs = parseAppInstallOrArray(app, installNode);
         app.setInstalls(installs);
      }
      else if (recipeNode != null) {
         app.addRecipe(parseRecipe(recipeNode));
      }
      else if (recipesNode != null) {
         app.setRecipes(parseRecipes(recipesNode));
      }

      /**
       * Make sure each recipe that was embedded into this application has an
       * AppKey that refers to it.
       */
      app.linkRecipesToApp();
      return app;
   }


   private void parseIncludes(
         @SuppressWarnings("unused") JsonNode includesNode,
         @SuppressWarnings("unused") Feed feed)
   {
      // TODO: see bug 682188
   }


   /**
    * Parse an application 'install' node for compatibility with v3 feeds.
    * The 'install' can be a single node with a 'command' property, or a list
    * of nodes with a 'command' property.
    *
    * @param app Application being parsed.
    * @param installNode
    * @return
    * @throws FeedJsonFormatException
    */
   private List<AppInstall> parseAppInstallOrArray(
         FeedApplication app,
         JsonNode installNode)
      throws FeedJsonFormatException
   {
      List<AppInstall> installs = new ArrayList<AppInstall>();

      if (installNode.isArray()) {
         /* Parse each array item */
         Iterator<JsonNode> it = installNode.iterator();
         while (it.hasNext()) {
            AppInstall install = parseAppInstall(app, installNode);
            installs.add(install);
         }
      }
      else {
         /* Just a single install */
         AppInstall install = parseAppInstall(app, installNode);
         installs.add(install);
      }

      return installs;
   }


   /**
    * Parse an application 'install' node.
    * Although this looks very much like a recipe command, we do not parse
    * the 'label' field, so it needs its own parser.
    *
    * @param app Application being parsed.
    * @param installNode
    * @return
    * @throws FeedJsonFormatException
    */
   private AppInstall parseAppInstall(
         FeedApplication app,
         JsonNode installNode)
      throws FeedJsonFormatException
   {
      JsonNode cmdNode = installNode.get("command");
      if (cmdNode == null) {
         throw new FeedJsonFormatException(
               installNode,
               String.format("Install node for application \"%s\" has no \"command\"",
               app.getDisplayName()));
      }

      AppInstall install = new AppInstall();
      install.setCommand(cmdNode.getTextValue());
      return install;
   }


   /**
    * Parse an array of recipes.
    *
    * @param recipesN
    * @return
    * @throws FeedJsonFormatException
    */
   private List<Recipe> parseRecipes(JsonNode recipesN)
      throws FeedJsonFormatException
   {
      List<Recipe> recipes = new ArrayList<Recipe>();

      if (recipesN != null && recipesN.isArray()) {
         Iterator<JsonNode> it = recipesN.iterator();
         while (it.hasNext()) {
            JsonNode recipeNode = it.next();
            recipes.add(parseRecipe(recipeNode));
         }
      }

      return recipes;
   }


   /**
    * Parse a single recipe node.
    *
    * @param recipeNode
    * @return
    */
   public Recipe parseRecipe(JsonNode recipeNode)
      throws FeedJsonFormatException
   {
      verifyRequiredFields(
            "Recipe",
            recipeNode,
            REQUIRED_RECIPE_FIELDS);

      /* Create recipe with all required data */
      Recipe recipe = new Recipe();

      /* Name */
      JsonNode nameNode = recipeNode.get("name");
      recipe.setName(nameNode.getValueAsText());

      /* Description? */
      JsonNode descN = recipeNode.get("description");
      if (descN != null) {
         try {
            recipe.setDescription(parseText(descN));
         }
         catch(Exception ex) {
            throw new FeedJsonFormatException(
                  recipeNode,
                  "Invalid \"description\" for recipe \"" +
                  recipe.getName() + "\"",
                  ex);
         }
      }

      /* Application keys? */
      try {
         JsonNode appKeysNode = recipeNode.get("appKeys");

         /* Because we once said 'appliesTo' would work */
         if (appKeysNode == null) {
            appKeysNode = recipeNode.get("appliesTo");
         }

         if (appKeysNode != null && appKeysNode.isArray()) {
            Iterator<JsonNode> appKeyIt = appKeysNode.iterator();
            while (appKeyIt.hasNext()) {
               RecipeAppKey appKey = parseRecipeAppKey(appKeyIt.next());
               recipe.addAppKey(appKey);
            }
         }

         /*
          * Note: a precise-matching AppKey is added once the application has
          * been parsed, if no precise-matching AppKey was found in the JSON.
          */
      }
      catch(FeedJsonFormatException ex) {
         throw new FeedJsonFormatException(
            recipeNode,
            "\"appKeys\" error for recipe \"" + recipe.getName() + "\": " + ex.getMessage(),
            ex);
      }

      /* Recipe files? */
      try {
         JsonNode filesNode = recipeNode.get("files");
         if (filesNode != null && filesNode.isArray()) {
            Iterator<JsonNode> filesIt = filesNode.iterator();
            while (filesIt.hasNext()) {
               RecipeFile file = parseRecipeFile(filesIt.next());
               recipe.addFile(file);
            }
         }
      }
      catch(FeedJsonFormatException ex) {
         throw new FeedJsonFormatException(
            recipeNode,
            "\"file\" error for recipe \"" + recipe.getName() + "\": " + ex.getMessage(),
            ex);
      }

      /* Steps? This supports "recipe.steps.install" */
      JsonNode stepsNode = recipeNode.get("steps");
      if (stepsNode != null) {
         if (hasAnyChild(recipeNode, AfUtil.toNames(ConversionPhase.values()))) {
            throw new FeedJsonFormatException(
                  recipeNode,
                  "Recipe contains both \"steps\" and phase nodes.");
         }
         parseConversionPhasesFrom(stepsNode, recipe);
      }
      else {
         /* Commands? This supports "recipe.install" */
         parseConversionPhasesFrom(recipeNode, recipe);
      }

      /* Variables? */
      try {
         JsonNode variablesNode = recipeNode.get("variables");
         if (variablesNode != null && variablesNode.isArray()) {
            Iterator<JsonNode> varsIt = variablesNode.iterator();
            while (varsIt.hasNext()) {
               RecipeVariable var = parseRecipeVariable(varsIt.next());
               recipe.addVariable(var);
            }
         }
      }
      catch(FeedJsonFormatException ex) {
         throw new FeedJsonFormatException(
            recipeNode,
            "\"variable\" error for recipe \"" + recipe.getName() + "\": " + ex.getMessage(),
            ex);
      }

      return recipe;
   }


   /**
    * Parse child nodes which match conversion phases, and add them to the
    * given recipe. This is broken out because we support two slightly
    * different formats: recipe.insall and recipe.steps.install.
    *
    * @param node
    * @param recipe
    * @throws FeedJsonFormatException
    */
   private void parseConversionPhasesFrom(JsonNode node, Recipe recipe)
      throws FeedJsonFormatException
   {
      for (ConversionPhase phase : ConversionPhase.values()) {
         JsonNode stepNode = node.get(phase.name());
         if (stepNode != null) {
            recipe.setStep(phase, parseRecipeStep(stepNode));
         }
      }
   }


   /**
    * Parse a recipe variable.
    *
    * @param varNode
    * @return
    * @throws FeedJsonFormatException
    */
   private RecipeVariable parseRecipeVariable(JsonNode varNode)
      throws FeedJsonFormatException
   {
      verifyRequiredFields(
         "Variable",
         varNode,
         new String[] { "name" } );

      JsonNode nameNode = varNode.get("name");
      JsonNode requiredNode = varNode.get("required");
      JsonNode patternNode = varNode.get("pattern");

      String name = nameNode.getValueAsText();
      if (!RECIPE_VAR_NAME_PATTERN.matcher(name).matches()) {
         throw new FeedJsonFormatException(
            varNode,
            "\"" + name + "\" is not a valid variable name");
      }

      RecipeVariable var = new RecipeVariable();
      var.setName(nameNode.getValueAsText());

      if (requiredNode != null) {
         var.setRequired(requiredNode.getValueAsBoolean(false));
      }

      if (patternNode != null) {
         var.setPattern(patternNode.getValueAsText());
      }

      return var;
   }


   /**
    * Parse a recipe step, such the 'install', 'precapture' or 'prebuild'
    * nodes inside a 'recipe' node.
    *
    * @param stepNode
    * @return
    * @throws FeedJsonFormatException
    */
   private RecipeStep parseRecipeStep(JsonNode stepNode)
      throws FeedJsonFormatException
   {
      RecipeStep step = new RecipeStep();

      JsonNode cmdsNode = stepNode.get("commands");
      if (cmdsNode != null && cmdsNode.isArray()) {
         Iterator<JsonNode> it = cmdsNode.iterator();
         while (it.hasNext()) {
            JsonNode cmdNode = it.next();
            step.addCommand(parseRecipeCommand(cmdNode));
         }
      }

      return step;
   }


   /**
    * Parse a 'command' node.
    * This needs to include a 'command' property, and may also contain
    * a 'label'.
    *
    * @param commandNode
    * @return
    * @throws FeedJsonFormatException
    */
   private RecipeCommand parseRecipeCommand(JsonNode commandNode)
      throws FeedJsonFormatException
   {
      verifyRequiredFields(
            "Recipe Command",
            commandNode,
            REQUIRED_RECIPE_COMMAND_FIELDS);

      JsonNode labelNode = commandNode.get("label");
      JsonNode cmdNode = commandNode.get("command");

      RecipeCommand cmd = new RecipeCommand();
      cmd.setCommand(cmdNode.getTextValue());
      cmd.setLabel(labelNode.getTextValue());

      return cmd;
   }


   /**
    * Parse an application file.
    * A URL is required. A name is optional.

    * @param node
    * @return
    * @throws FeedJsonFormatException
    */
   private AppDownload parseApplicationFile(JsonNode node)
      throws FeedJsonFormatException
   {
      JsonNode nameNode = node.get("name");
      JsonNode urlNode = node.get("url");

      if (urlNode == null) {
         throw new FeedJsonFormatException(
               node,
               "Application file has no \"url\" property");
      }

      /* Download could be relative path or absolute URI */
      AppDownload download = new AppDownload();
      download.setLocation(urlNode.getValueAsText());

      if (nameNode != null) {
         download.setName(nameNode.getValueAsText());
      }

      return download;
   }


   /**
    * Parse a recipe file.
    * The file must include either a URL or URI (URI is preferred, but URL
    * is supported for user-friendliness).

    * @param node
    * @return
    * @throws FeedJsonFormatException
    */
   private RecipeFile parseRecipeFile(JsonNode node)
      throws FeedJsonFormatException
   {
      JsonNode urlNode = node.get("url");
      JsonNode uriNode = node.get("uri");

      /* Just URL or URI, not both */
      if (urlNode != null && uriNode != null) {
         throw new FeedJsonFormatException(
               node,
               "Recipe file has both \"url\" and \"uri\" properties");
      }

      /* Location = either URL or URI */
      JsonNode locationNode = (urlNode != null ? urlNode : uriNode);
      if (locationNode == null) {
         throw new FeedJsonFormatException(
               node,
               "Recipe file has no \"url\" or \"uri\" property");
      }

      JsonNode nameNode = node.get("name");
      JsonNode pathNode = node.get("path");
      JsonNode descNode = node.get("description");

      /* Create file: could be relative path or absolute URI */
      RecipeFile file = new RecipeFile();
      file.setLocation(locationNode.getValueAsText());

      if (nameNode != null) {
         file.setName(nameNode.getValueAsText());
      }

      if (pathNode != null) {
         file.setPath(pathNode.getValueAsText());
      }

      if (descNode != null) {
         file.setDescription(descNode.getTextValue());
      }

      return file;
   }


   private RecipeAppKey parseRecipeAppKey(JsonNode node)
      throws FeedJsonFormatException
   {
      /*
       * Sometimes there is an 'application' node which contains the
       * key fields, and sometimes there is just the key fields themselves.
       */
      JsonNode appNode = node.get("application");
      if (appNode != null) {
         return parseRecipeAppKey(appNode);
      }

      RecipeAppKey key = new RecipeAppKey();

      if (node.has("name")) {
         key.setName(node.get("name").getValueAsText());
      }
      if (node.has("version")) {
         key.setVersion(node.get("version").getValueAsText());
      }
      if (node.has("locale")) {
         key.setLocale(node.get("locale").getValueAsText());
      }
      if (node.has("installerRevision")) {
         key.setInstallerRevision(node.get("installerRevision").getValueAsText());
      }

      return key;
   }
}
