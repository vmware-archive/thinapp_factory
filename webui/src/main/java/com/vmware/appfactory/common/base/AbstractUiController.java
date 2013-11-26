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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.dao.AfDao;
import com.vmware.thinapp.common.util.I18N;


/**
 * Base class for all controllers that service the user interface.
 * This adds a little more functionality to the BaseController which is
 * often used in any of the view controllers.
 */
public abstract class AbstractUiController
   extends AbstractController
{
   public static final String PAGE_TITLE_KEY = "pageTitle";

   protected ModelMap getBaseModel(HttpServletRequest request)
   {
      return getBaseModel(request, null, null);
   }

   /**
    * Create a default model which subclasses can add to. This default model
    * contains data that is needed by (or useful to) all controllers and their
    * views.
    */
   protected ModelMap getBaseModel(@Nonnull HttpServletRequest request,
                                   @Nullable Locale locale,
                                   @Nullable String titleKey)
   {
      ModelMap model = new ModelMap();

      if (locale != null && titleKey != null) {
         model.put(PAGE_TITLE_KEY, tr(locale, titleKey));
      }

      /* Add "global" variables to the model */
      model.put("appfactory", _af);
      String contextPath = request.getSession().getServletContext().getContextPath();
      model.put("contextPath", contextPath);
      model.put("staticPath", _af.getStaticResourceContextPath(contextPath));
      model.put("String", String.class);

      /* Request-specific variables */
      model.put("serverName", request.getServerName());
      model.put("serverPort", request.getServerPort());
      model.put("serverAddress", request.getLocalAddr());

      /*
       * Build a key -> localized text map for the resource bundle so the
       * client will have access to it.
       */
      ResourceBundle rb = I18N.getResourceBundle(request.getLocale());
      Map<String,String> m = new HashMap<String,String>();
      for(String key : rb.keySet()) {
         // Translation lookup on client uses uppercase key.
         m.put(key.toUpperCase(), rb.getString(key));
      }
      model.put("translationTable",m);

      return model;
   }


   /**
    * Look up all database records that are references in an HTTP request. All
    * values for a named parameter are assumed to be record IDs, and are fetched
    * from the database using the specified DAO.
    */
   protected <T extends AbstractRecord> List<T> getSelectedObjects(
         HttpServletRequest request,
          String tag,
          AfDao<T> dao)
   {
      List<T> records = new ArrayList<T>();

      String[] vals = request.getParameterValues(tag);
      if (vals != null) {
         for (String val : vals) {
            Long id = Long.valueOf(val);
            T record = dao.find(id);
            if (record != null) {
               records.add(record);
            }
         }
      }

      return records;
   }


   /**
    * @param request
    * @param ex
    * @return
    */
   @ExceptionHandler(org.springframework.core.NestedRuntimeException.class)
   public ModelAndView resolveNestedException(
         HttpServletRequest request,
         Exception ex)
   {
      Throwable cause = ex.getCause();
      return resolveException(request, cause);
   }


   /**
    * @param request
    * @param ex
    * @return
    */
   @ExceptionHandler(Exception.class)
   public ModelAndView resolveException(
         HttpServletRequest request,
         Throwable ex)
   {
      ModelMap mm = getBaseModel(request);
      mm.put("ex", ex);

      // I don't know why we want to check for this exception, but it seems
      // to be one specific to Tomcat 6.  We don't want to depend on the class
      // itself, because then we'd be including Tomcat 6 code inside our .war,
      // which totally breaks our webapp if deployed on Tomcat 7.
      //
      // Instead, just look for the proper name.
      //
      if (ex.getClass().getName().contains("ClientAbortException")) {
         /* Client left too early: OK to ignore this */
         return null;
      }

      if (ex instanceof org.hibernate.exception.JDBCConnectionException) {
         mm.put("title", "Database connection error");
         mm.put("explanation",
               "AppFactory is unable to connect to its underlying database server." +
               " Until the database is restarted, you will not be able to use this application." +
               " Please contact your AppFactory administrator.");
      }

      return new ModelAndView("/error/internal", mm);
   }

}
