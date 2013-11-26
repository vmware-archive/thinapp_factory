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
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import com.vmware.appfactory.application.model.AppIcon;
import com.vmware.appfactory.common.AfHash;
import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.feed.exception.FeedJsonFormatException;

/**
 * Used as a common base class for all feed parsers no matter which version
 * they implement. This provides some common functionality for parsing nodes
 * which really don't change much (description, icon, etc).
 */
public abstract class AbstractFeedParser
   implements FeedParserInterface
{
   /**
    * Parse an application.
    * Every parser needs to know how to do this.
    *
    * @param node
    * @return
    * @throws FeedJsonFormatException
    */
   protected abstract FeedApplication parseApplication(JsonNode node)
      throws FeedJsonFormatException;


   /**
    * Examine a node to make sure all the specified property fields are
    * present. Throw an exception if any are missing.
    */
   protected void verifyRequiredFields(String name, JsonNode node, String... fields)
      throws FeedJsonFormatException
   {
      /* Check all required fields */
      for (String field : fields) {
         if (node.get(field) == null) {
            throw new FeedJsonFormatException(
                  node,
                  name + " node is missing field \"" + field + "\"");
         }
      }
   }


   /**
    * Parse a list of applications.
    *
    * @param appsN
    * @param feed
    * @return
    * @throws FeedJsonFormatException
    */
   protected List<FeedApplication> parseApplications(JsonNode appsN)
      throws FeedJsonFormatException
   {
      List<FeedApplication> apps = new ArrayList<FeedApplication>();

      if (appsN != null && appsN.isArray()) {
         Iterator<JsonNode> it = appsN.iterator();
         while (it.hasNext()) {
            JsonNode appNode = it.next();
            FeedApplication appsAndRecipes = parseApplication(appNode);
            apps.add(appsAndRecipes);
         }
      }
      return apps;
   }


   /**
    * Parse an icon from JSON data.
    * Throws an exception if any required field is missing.
    */
   protected AfHash parseHash(JsonNode hashNode)
      throws FeedJsonFormatException
   {
      JsonNode functionN = hashNode.get("function");
      JsonNode valueN = hashNode.get("value");

      /* Check for required field */
      if (functionN == null || valueN == null) {
         throw new FeedJsonFormatException(
               hashNode,
               "Hash node is missing \"function\" and/or \"value\"");
      }

      /* Create AfHash instance */
      String functionName = functionN.getTextValue();
      try {
         AfHash hash = new AfHash();
         hash.setFunction(AfHash.Function.valueOf(functionName.toUpperCase()));
         hash.setValue(valueN.getTextValue());
         return hash;
      }
      catch(IllegalArgumentException ex) {
         /* AfHash.Function.valueOf() must have failed */
         throw new FeedJsonFormatException(
               hashNode,
               "Invalid hash function \"" + functionName + "\"");
      }
   }


   /**
    * Read the common format for 'rich text' nodes, such as "description".
    * These nodes contain a MIME content type and the actual content, as two
    * separate child nodes. If 'content' is missing, throw an exception. If
    * 'type' is missing, default to "text/plain".
    */
   protected AfText parseText(JsonNode node)
      throws FeedJsonFormatException
   {
      JsonNode typeN = node.get("contentType");
      JsonNode contentN = node.get("content");

      /* Check for required child nodes */
      if (contentN == null) {
         throw new FeedJsonFormatException(
               node,
               "Text node is missing \"content\"");
      }

      /* Create the AfText instance */
      AfText text = new AfText();
      text.setContentType(typeN == null ? "text/plain" : typeN.getTextValue());
      text.setContent(contentN.getTextValue());
      return text;
   }


   /**
    * Parse an icon from JSON data.
    * "url" is a required field.
    * "size" (pixel width and height) is optional.
    * "contentType" (MIME type) is optional.
    *
    * @throws FeedJsonFormatException
    *    If any required field is missing.
    */
   protected AppIcon parseIcon(JsonNode iconNode)
      throws FeedJsonFormatException
   {
      JsonNode urlN = iconNode.get("url");
      JsonNode sizeN = iconNode.get("size");
      JsonNode typeN = iconNode.get("contentType");

      /* Check for required child nodes */
      if (urlN == null) {
         throw new FeedJsonFormatException(
               iconNode,
               "Icon node is missing \"url\"");
      }

      /* Create AfIcon instance */
      AppIcon icon = new AppIcon();
      icon.setUrl(urlN.getValueAsText());

      /* Size (optional) */
      if (sizeN != null) {
         int size = sizeN.getValueAsInt();
         icon.setSize(size);
      }

      /* Content type (optional) */
      if (typeN != null) {
         String type = typeN.getValueAsText();
         icon.setContentType(type);
      }

      return icon;
   }


   /**
    * Return true if the given node has at least one child from the given
    * set of children.
    *
    * @param node
    * @param children
    * @return
    */
   protected boolean hasAnyChild(JsonNode node, String... children)
   {
      for (String child : children) {
         if (node.has(child)) {
            return true;
         }
      }

      return false;
   }
}

