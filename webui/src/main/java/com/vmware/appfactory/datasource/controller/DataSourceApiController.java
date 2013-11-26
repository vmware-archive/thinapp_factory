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

package com.vmware.appfactory.datasource.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.datasource.model.DataSource;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.fileshare.model.FileShare;


/**
 * This controller handles all the datasource-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class DataSourceApiController
   extends AbstractApiController
{
   /**
    * Since we want to tweak the JSON responses, we need a custom mapper.
    * We could have used JSON Views, but they define what you want to include,
    * not exclude, so that's a lot more work.
    */
   private static final ObjectMapper DATASOURCE_SUMMARY_MAPPER;

   static {
      DATASOURCE_SUMMARY_MAPPER = new ObjectMapper();
      DATASOURCE_SUMMARY_MAPPER.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

      DATASOURCE_SUMMARY_MAPPER.getSerializationConfig().addMixInAnnotations(
            Feed.class,
            DataSourceMixIn.class);

      DATASOURCE_SUMMARY_MAPPER.getSerializationConfig().addMixInAnnotations(
            FileShare.class,
            DataSourceMixIn.class);
   }

   /**
    * When serializing all data sources back to the client, skip the arrays
    * of applications and recipes.
    */
   @JsonIgnoreProperties({
                               "applications",
                               "recipes"})
   public static final class DataSourceMixIn {
      /* No body needed */
   }


   /**
    * Get a list of all the known data sources.
    *
    * @param sort Sort the data (defaults to false)
    * @throws AfServerErrorException
    */
   @RequestMapping(value="/sources", method=RequestMethod.GET)
   public void getAllSources(
         HttpServletResponse response,
         @RequestParam(required=false) boolean sort)
      throws AfServerErrorException
   {
      try {
         List<Feed> feeds = super.getFeedsList();
         List<FileShare> fileshares = _daoFactory.getFileShareDao().findAll();

         List<DataSource> sources = new ArrayList<DataSource>();
         sources.addAll(feeds);
         sources.addAll(fileshares);

         if (sort) {
            Collections.sort(sources);
            for (DataSource datasource : sources) {
               Collections.sort(datasource.getApplications());
               Collections.sort(datasource.getRecipes());
            }
         }

         DATASOURCE_SUMMARY_MAPPER.writeValue(response.getWriter(), sources);
         return;
      }
      catch(Exception ex) {
         throw new AfServerErrorException(ex);
      }
   }
}
