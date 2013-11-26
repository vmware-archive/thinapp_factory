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

package com.vmware.appfactory.common.base;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.AppFactory;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfForbiddenException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.cws.CwsClientService;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.datasource.model.DataSource;
import com.vmware.appfactory.datastore.DatastoreClientService;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.TaskFactory;
import com.vmware.appfactory.workpool.VIClientService;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.util.I18N;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * All controllers inherit from this in order to gain some basic functionality.
 * The BaseController provides global data to a common model, including the
 * current user, the page name (functional area of the application), etc.
 */
public abstract class AbstractController
   implements ApplicationEventPublisherAware
{
   @Resource
   protected AppFactory _af;

   @Resource
   protected AfDaoFactory _daoFactory;

   @Resource(name = "conversionsQueue")
   protected TaskQueue _conversionsQueue;

   @Resource(name = "scanningQueue")
   protected TaskQueue _scanningQueue;

   @Resource
   protected ConfigRegistry _config;

   @Resource
   protected CwsClientService _cwsClient;

   @Resource
   protected DatastoreClientService _dsClient;

   @Resource
   protected WorkpoolClientService _wpClient;

   @Resource
   protected VIClientService _viClient;

   @Resource
   protected TaskFactory _taskFactory;

   protected Logger _log;

   @Nullable
   private ApplicationEventPublisher applicationEventPublisher;


   /**
    * Instantiate this base class.
    * Configures a logger specific to the most-derived child class.
    */
   protected AbstractController()
   {
      _log = LoggerFactory.getLogger(getClass());
      applicationEventPublisher = null;
   }


   /**
    * Convert all thrown AfBadRequestException exceptions
    * into a 400 error code.
    * @param ex
    * @return
    */
   @ResponseBody
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ExceptionHandler({
                           AfBadRequestException.class,
                           InvalidDataException.class})
   public String handleBadRequestException(AfBadRequestException ex)
   {
      _log.error("Bad request!", ex);
      return ex.getMessage();
   }


   /**
    * Convert all thrown AfForbiddenException exceptions
    * into a 403 error code.
    * @param ex
    * @return
    */
   @ResponseBody
   @ResponseStatus(HttpStatus.FORBIDDEN)
   @ExceptionHandler(AfForbiddenException.class)
   public String handleForbiddenException(Exception ex)
   {
      _log.error("Internal server error!", ex);
      return ex.getMessage();
   }


   /**
    * Convert all thrown AfNotFoundException exceptions
    * into a 404 error code.
    * @param ex
    * @return
    */
   @ResponseBody
   @ResponseStatus(HttpStatus.NOT_FOUND)
   @ExceptionHandler({
                           AfNotFoundException.class,
                           FileNotFoundException.class})
   public String handleNotFoundException(Exception ex)
   {
      _log.error("Resource not found!", ex);
      return ex.getMessage();
   }


   /**
    * Convert all thrown AfConflictException exceptions
    * into a 409 error code.
    * @param ex
    * @return
    */
   @ResponseBody
   @ResponseStatus(HttpStatus.CONFLICT)
   @ExceptionHandler(AfConflictException.class)
   public String handleConflictException(Exception ex)
   {
      _log.error("Resource conflict!", ex);
      return ex.getMessage();
   }


   /**
    * Convert all thrown AfServerErrorException exceptions
    * into a 500 error code.
    * @param ex
    * @return
    */
   @ResponseBody
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   @ExceptionHandler({
                           AfServerErrorException.class,
                           RuntimeException.class,
                           CwsException.class,
                           WpException.class,
                           DsException.class})
   public String handleServerErrorException(Exception ex)
   {
      _log.error("Internal server error!", ex);
      return ex.getMessage();
   }

   /**
    * Get a required "long" parameter from a request. If the parameter is
    * missing or is not a long, an exception is thrown.
    */
   protected long getLongFromRequest(HttpServletRequest request,
                             String paramName)
         throws IllegalArgumentException,
            NumberFormatException
   {
      String str = request.getParameter(paramName);
      if (str == null || str.isEmpty()) {
         throw new IllegalArgumentException("Missing required parameter " + paramName + ".");
      }

      return Long.parseLong(str);
   }


   /**
    * Get an optional "long" parameter from a request. If the parameter is
    * missing the default is returned.
    */
   protected long getLongFromRequest(HttpServletRequest request,
                             String paramName,
                             long def)
         throws NumberFormatException
   {
      String str = request.getParameter(paramName);
      return (str == null ? def : Long.parseLong(str));
   }


   /**
    * Get a required "string" parameter from a request. If the parameter is
    * missing or is not a long, an exception is thrown.
    */
   protected String getStringFromRequest(HttpServletRequest request,
                                String paramName)
         throws IllegalArgumentException
   {
      String str = request.getParameter(paramName);
      if (str == null) {
         throw new IllegalArgumentException("Missing required parameter " + paramName + ".");
      }

      return str;
   }


   /**
    * Get an optional "string" parameter from a request. If the parameter is
    * missing the default is returned.
    */
   protected String getStringFromRequest(HttpServletRequest request,
                                String paramName,
                                String def)
   {
      String str = request.getParameter(paramName);
      return (str == null ? def : str);
   }


   /**
    * Get an optional "boolean" parameter from a request. If the parameter is
    * missing, the default is returned. Otherwise, values of "true" or "on"
    * return true and others return false.
    */
   protected boolean getBoolFromRequest(
         HttpServletRequest request,
         String paramName)
   {
      String str = request.getParameter(paramName);
      if (str == null) {
         return false;
      }
      return (str.equals("on") || str.equals("true"));
   }


   /**
    * Set HTTP headers on an HTTP response to hopefully force browsers to
    * prevent the response being cached. Especially important for dynamic
    * content updates. We're talking to you, Internet Explorer.
    */
   protected void setNoCacheHeaders(HttpServletResponse response)
   {
      response.addHeader("Pragma", "no-cache");
      response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
      response.setDateHeader("Expires", 0);
   }


   /**
    * Read the body from an HTTP request.
    */
   protected String readRequest(HttpServletRequest request)
      throws IOException
   {
      StringBuffer sb = new StringBuffer();
      String line;

      /* Read the entire message */
      while ((line = request.getReader().readLine()) != null) {
         sb.append(line);
      }

      String s = sb.toString();
      return s;
   }


   protected String getContextPath(HttpServletRequest request)
   {
      return request.getSession().getServletContext().getContextPath();
   }


   /**
    * Convert a list of builds into a map, grouping the builds by
    * application category.
    *
    * TODO: Multiple categories per app
    */
   protected Map<String,Collection<Build>> createBuildCategoryMap(List<Build> allBuilds)
   {
      Map<String,Collection<Build>> map = new TreeMap<String,Collection<Build>>();

      for (Build build : allBuilds) {
         String categoryName = "Uncategorized";

         Set<String> cats = build.getCategories();
         if (cats != null && !cats.isEmpty()) {
            // XXX only using first category here
            categoryName = cats.iterator().next();
         }

         Collection<Build> thesePkgs = map.get(categoryName);
         if (thesePkgs == null) {
            thesePkgs = new TreeSet<Build>();
            map.put(categoryName, thesePkgs);
         }
         thesePkgs.add(build);
      }

      return map;
   }

   /**
    * Deletes all the other stuff that points back to it.
    *
    * This will abort related tasks.
    */
   protected void deleteAppSourceRelated(DataSource appSource)
   {
      if(appSource == null) {
         throw new IllegalArgumentException("appSource must NOT be null!");
      }
      /*
       * Deleting an application source will delete the apps automatically, since there is a
       * direct parent-child mapping, but we need to clean up the task
       * queue first.
       */
      for (Application app : appSource.getApplications()) {
         for (TaskState state : _conversionsQueue.findActiveTasksForApp(app.getId())) {
            _conversionsQueue.abortTask(state.getId());
         }
      }
   }


   /**
    * This method provides the lastModifiedRecord for a given collection
    * of type AbstractRecord. If no records exist, then 0 is returned.
    *
    * @param list
    * @return
    */
   protected long getLastModifiedRecord(Collection<? extends AbstractRecord> list)
   {
      long lastModified = 0;
      if (CollectionUtils.isEmpty(list)) {
         return lastModified;
      }
      for (AbstractRecord ar : list) {
         if (lastModified < ar.getModified()) {
            lastModified = ar.getModified();
         }
      }
      return lastModified;
   }


   /**
    * Get a full server path url including current web app context.
    * e.g. http(s)://(hostname)(:optional_port)/(web_context)/
    *
    * @param request - a HttpServletRequest.
    * @return a server path url
    */
   protected String getFullContextPath(HttpServletRequest request) {
      if(request == null) {
         throw new IllegalArgumentException("request must NOT be null!");
      }
      int port = request.getLocalPort();
      final StringBuilder url = new StringBuilder();
      /* http(s)://hostname */
      url.append(request.getScheme()).append("://").append(request.getServerName());
      if(port != 80) {
         /* http(s)://hostname:xxxx */
         url.append(":" + port);
      }
      /* http(s)://hostname(:xxxx)/web_context/ */
      url.append(request.getContextPath()).append("/");

      return url.toString();
   }

   /**
    * For the convenience of subclasses, provide a shorthand notation for
    * translating string.
    */
   protected static String tr(Locale locale, String key)
   {
      return I18N.translate(locale, key);
   }

   /**
    * For the convenience of subclasses, provide a shorthand notation for
    * translating string.
    * @param locale
    * @param key
    * @param args
    * @return A translation of 'key'
    */
   protected static String fr(Locale locale, String key, String... args)
   {
      return I18N.format(locale, key, (Object[]) args);
   }

   /**
    * Get all feeds list
    * @return a list of feeds.
    */
   protected List<Feed> getFeedsList()
   {
      FeedDao feedDao = _daoFactory.getFeedDao();

      return feedDao.findAll();
   }

   /**
    * Apply Etag caching to the request. In the request header,
    * if the 'If-None-Match' field is either empty or does not match
    * with the new token; it then set current date in the 'Last-Modified'
    * field. Otherwise, it sends 304 code back in the response.
    *
    * @param request - a HttpServletRequest.
    * @param response - a HttpServletResponse.
    * @param newToken - a new token to be set in the response's Etag header.
    * @return it always returns previous token from the request's
    * 'If-None-Match' header.
    * @throws IOException if any error raised while setting
    * Etag caching related Http headers.
    */
   protected String applyETagCache(
         HttpServletRequest request,
         HttpServletResponse response,
         String newToken,
         boolean setExpires)
      throws IOException
   {
      if (!StringUtils.hasLength(newToken)) {
         throw new IllegalArgumentException("Invalid newToken - " + newToken);
      }

      // Get the current date/time
      final Calendar cal = Calendar.getInstance();
      cal.set(Calendar.MILLISECOND, 0);
      final Date lastModified = cal.getTime();

      response.setHeader("Pragma", "cache");
      response.setHeader("Cache-Control", "public, must-revalidate");

      // Set the Expires and Cache-Control: max-age headers to 1 year in the future
      if (setExpires) {
         // Get the date/time 1 year from now
         cal.add(Calendar.YEAR, 1);
         final Date expires = cal.getTime();

         response.addHeader("Cache-Control", "max-age=3153600");
         response.setDateHeader("Expires", expires.getTime());
      }

      // Always store new token in the ETag header.
      response.setHeader("ETag", newToken);
      String previousToken = request.getHeader("If-None-Match");

      if (newToken.equals(previousToken)) {
         response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
         response.setHeader("Last-Modified", request.getHeader("If-Modified-Since"));
      } else {
         response.setDateHeader("Last-Modified", lastModified.getTime());
      }
      return previousToken;
   }

   @Override
   public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
      this.applicationEventPublisher = applicationEventPublisher;
   }

   protected void publishEvent(ApplicationEvent event) {
      if (null != applicationEventPublisher) {
         applicationEventPublisher.publishEvent(event);
      }
   }
}
