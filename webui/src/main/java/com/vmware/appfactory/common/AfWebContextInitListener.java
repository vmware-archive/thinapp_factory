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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.thinapp.common.util.AfConstant;

/**
 * Servlet context listener that a web container will be called during startup.
 * It is a typical way of loading all 1-time initialization for application scope.
 *
 * @author saung
 * @since v1.0 4/5/2011
 */
public class AfWebContextInitListener implements ServletContextListener {
   /**
    * get the logger
    */
   private static final Logger log = LoggerFactory.getLogger(AfWebContextInitListener.class);

   /**
    * This method will be invoked during web app startup.
    *
    * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
    */
   @Override
   public void contextInitialized(ServletContextEvent event) {
      log.info("####################::AfWebContextInit - START::#####################");
      AfConstant.WEB_CONTEXT_REAL_PATH = event.getServletContext().getRealPath(AfConstant.FILE_SEPARATOR);
      log.info("WEB_CONTEXT_REAL_PATH=" + AfConstant.WEB_CONTEXT_REAL_PATH);
      log.info("####################::AfWebContextInit - END::#######################");
   }

   /**
    * This method will be invoked during web app graceful, orderly shutdown.
    *
    * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
    */
   @Override
   public void contextDestroyed(ServletContextEvent event) {
      log.info("####################::AfWebContextDestroy - START::#####################");
      // do clean up work here!
      log.info("####################::AfWebContextDestroy - END::#######################");
   }

}
