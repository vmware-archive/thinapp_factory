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

import org.codehaus.jackson.JsonNode;

import com.vmware.appfactory.feed.exception.FeedJsonFormatException;
import com.vmware.appfactory.feed.model.Feed;

/**
 * Serves as the common definition for all feed parsers. Each time the feed
 * format changes, a new implementation should be created for it (inherit from
 * AbstractFeedParser to pick up some common methods).
 */
public interface FeedParserInterface
{
   /**
    * Parse the root node and return the final feed.
    *
    * @param rootNode
    * @return
    * @throws FeedJsonFormatException
    */
   public Feed parse(JsonNode rootNode)
      throws FeedJsonFormatException;
}
