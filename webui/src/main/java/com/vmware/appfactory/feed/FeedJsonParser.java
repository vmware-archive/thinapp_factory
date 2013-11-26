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

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;

import com.vmware.appfactory.feed.exception.FeedJsonFormatException;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.thinapp.common.util.AfJson;

/**
 * This class reads a ThinAppFactory feed in JSON format.
 * It will look for a "version" field in the data, and use that to invoke a
 * parser specific to that version.
 */
public class FeedJsonParser
{
   /**
    * Parse a feed in JSON format and create an AfFeed instance from it.
    * If any part of the JSON data is invalid, an exception is thrown and
    * no AfFeed instance is created.
    *
    * @param rootNode Root node of the JSON data.
    * @return A new feed created from the JSON data.
    * @throws FeedJsonFormatException
    *    If the feed contains any errors.
    */
   public Feed parse(JsonNode rootNode)
      throws FeedJsonFormatException
   {
      JsonNode verNode = rootNode.get("version");
      if (verNode == null) {
         throw new FeedJsonFormatException(rootNode, "Missing feed version");
      }

      int version = verNode.getValueAsInt();
      switch(version) {
         case 3:
            return (new FeedJsonParserV3()).parse(rootNode);
         case 4:
            return (new FeedJsonParserV4()).parse(rootNode);
        default:
           throw new FeedJsonFormatException(rootNode, "Invalid feed version");
      }
   }

   /**
    * Creates a new feed by reading from an InputStream.
    *
    * @param is   an InputStream which is already opened for
    *             reading.  This method will close the stream
    *             before it returns.
    *
    * @return
    * a new Feed object, if the input stream could be parsed
    *
    * @throws IOException
    * when the InputStream could not be read, or the stream
    * did not contain a valid JSON document.
    *
    * @throws FeedJsonFormatException
    * when the stream contained a valid JSON document, but
    * the JSON was not in a known feed format.
    */
   public static Feed readStream(InputStream is)
         throws IOException, FeedJsonFormatException {
      JsonNode node;

      try {
         /* Read JSON data */
         node = AfJson.ObjectMapper().readTree(is);
      }
      catch(JsonParseException ex) {
         throw new FeedJsonFormatException(
               null,
               "JSON parse error: " + ex.getMessage(),
               ex);
      }
      finally {
         is.close();
      }

      /* Create a feed from the JSON data */
      FeedJsonParser parser = new FeedJsonParser();
      return parser.parse(node);
   }

}
