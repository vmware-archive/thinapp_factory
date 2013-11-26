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

package com.vmware.appfactory.common;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.runner.LicenseStatus;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.push.PushController;
import com.vmware.thinapp.common.util.AfConstant;

/**
 * Intercepts all MVC requests.
 * This is used to simulate a slow server by intercepting all handlers after
 * they have been invoked but before the response is returned, and injecting
 * a thread sleep.
 * The 'preHandle' method intercepts every non-GET call to the '/api/xxxx' endpoints
 * and then checks the license. If expired, it will return HTTP 420 code.
 *
 * @author levans
 */
public class AfWebHandlerInterceptor
   extends WebContentInterceptor
{
   private static final String EULA_URL = "/admin/eula";
   private static final String STATIC_URL = "/static";

   private final Logger _log = LoggerFactory.getLogger(AfWebHandlerInterceptor.class);

   @Resource
   private ConfigRegistry _config;

   @Resource
   private LicenseStatus _licenseStatus;

   @Resource
   private AppFactory _af;

   /**
    * @see org.springframework.web.servlet.mvc.WebContentInterceptor#preHandle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object)
    */
   @Override
   public boolean preHandle(
         HttpServletRequest request,
         HttpServletResponse response,
         Object handler)
      throws ServletException
   {
       if (logger.isWarnEnabled()) {
           request.setAttribute("TafHandleStartTimestamp", System.currentTimeMillis());
       }

       // set the content type of API requests
       boolean apiRequest = isApiRequest(handler);
       if (apiRequest) {
          if (handler instanceof PushController) {
             // IE9, IE8 does not like setting the content type to
             // response.setContentType("application/json");
             //
             // IE8, IE9 will open a "download" dialog when loading the tasks page.  To work around this,
             // treat this as text until IE can get its act together.
             response.setContentType("text/plain");
          } else {
             // note: our javascript code still requires that most AJAX calls use the application/json
             // mime type, and not all controllers do this.  So its still necessary. (see bug 816009)
             response.setContentType("application/json");
          }
          response.setCharacterEncoding("UTF-8");
       }

      // this is key for our IE support
      //
      // see http://msdn.microsoft.com/en-us/library/ie/cc288325(v=vs.85).aspx
      // for more info.
      //
      // IE has a mode called "compatibility view".  This is a different set of
      // rendering and parsing rules designed to work well with old intranet sites
      // written in the ie6 days.  In so doing, this moves IE further away
      // from the standards of other browsers.
      //
      // IE defaults to rendering all "intranet" sites, such as our appliance,
      // in "compatibility view".
      //
      // This HTTP header instructs IE to not do that for our pages, and to use their
      // standards-compliant renderer instead.
      //
      response.addHeader("X-UA-Compatible","IE=9");

      if (!apiRequest
            && !isEulaRequest(request)
            && !isStaticRequest(request)
            && !_config.getBool(ConfigRegistryConstants.GEN_EULA_ACCEPTED)) {
         try {
            response.sendRedirect(request.getContextPath() + EULA_URL);
            return false; // no more processing
         }
         catch(IOException ex) {
            /* We tried, but failed */
         }
      }

      if (apiRequest
            && isExpired()
            && !isStaticRequest(request)
            && !request.getMethod().equals("GET")) {
         try {
            response.sendError(AfConstant.EXPIRED_LICENSE_HTTP_CODE, "Expired licence");
            return false;
         } catch (IOException e) {
            _log.error("Failed to set expired license code in the response!", e);
         }

      }

      super.preHandle(request, response, handler);
      return true;
   }

   /**
    * After a request has been handled, possibly sleep for a while to
    * simulate a slow server.
    */
   @Override
   public void postHandle(
         HttpServletRequest request,
         HttpServletResponse response,
         Object handler,
         ModelAndView modelAndView)
      throws Exception
   {
      if (_af.isDevModeDeploy()) {
         long delay = 0;
         String path = request.getPathInfo();

         /* Match path to a configured delay */
         if (isApiRequest(handler)) {
            delay = _config.getLong(ConfigRegistryConstants.DEBUG_WEBUI_API_DELAY);
         }
         else if (path.startsWith("/ds") || path.startsWith("/cws") || path.startsWith("/wp")) {
            delay = _config.getLong(ConfigRegistryConstants.DEBUG_WEBUI_SIMS_DELAY);
         }
         else {
            delay = _config.getLong(ConfigRegistryConstants.DEBUG_WEBUI_UI_DELAY);
         }

         if (delay > 0) {
            /* If there is a delay, sleep for a while */
            _log.warn("Simulated response delay of " + delay + "ms for " + path);
            try {
               Thread.sleep(delay);
            }
            catch(InterruptedException ex) {
               /* Nothing */
            }
         }
      }

      if (logger.isTraceEnabled()) {
         Object startTime = request.getAttribute("TafHandleStartTimestamp");
         if (startTime instanceof Long) {
            long now = System.currentTimeMillis();
            long duration = now - ((Long)startTime);
            String message = "profile: " + duration + " ms: " + request.getMethod() + " " + request.getPathInfo();
            logger.trace(message);
            if (!response.isCommitted()) {
               response.setHeader("X-TafProfiling", duration + "ms");
            }
         }
      }
      super.postHandle(request, response, handler, modelAndView);
   }

   /**
    * Check the license status.
    * @return true if it expired; return false otherwise.
    */
   private boolean isExpired() {
      return _licenseStatus.isExpired();
   }

   /**
    * Check whether the request is an API request to an AbstractApiConroller
    *
    * @param handler
    * @return true if the request's path starts with '/api'; otherwise, return false.
    */
   public static boolean isApiRequest(Object handler) {
       return handler instanceof AbstractApiController;
   }

   /**
    * Check whether the request is for accessing the EULA.
    * @param request a HttpServletRequest instance.
    * @return
    */
   private static boolean isEulaRequest(HttpServletRequest request) {
      return request.getPathInfo().endsWith(EULA_URL);
   }

   /**
    * Check whether the request is for accessing static content. (/static)
    * @param request HttpServletRequest instance.
    * @return
    */
   private static boolean isStaticRequest(HttpServletRequest request) {
      return request.getServletPath().equals(STATIC_URL);
   }
}
