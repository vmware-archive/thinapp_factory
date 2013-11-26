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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.google.common.collect.ImmutableMap;
import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.thinapp.common.util.I18N;

/**
 * User: rude
 * Date: 11/13/11
 * Time: 2:55 PM
 * <p/>
 * This interceptor performs three operations:
 * 1. it sets the selected tab by using the controller's {@link SelectedTab} annotation,
 * if available
 * 2. optionally, it replaces the title in the model's PAGE_TITLE_KEY variable
 * to the one supplied in titleMapping.  If this happens, the old title will
 * be saved under the "legacyTitle" model property.
 * 3. optionally, it switches the view to be rendered to the one supplied in
 * viewMapping.  If this happens, the old view name will appear under the
 * "legacyView" model property.
 * <p/>
 * These three things will <b>only happen when the <code>newUI</code> configuration</b>
 * setting is set to true.
 */
@SuppressWarnings({"TypeMayBeWeakened"})
public class NewUiInterceptor extends HandlerInterceptorAdapter {

   @Resource
   private ConfigRegistry _config;

   @Resource
   private AppFactory _af;

   @Nonnull
   private Map<String, String> titleMapping = Collections.emptyMap();

   @Nonnull
   private Map<String, String> viewMapping = Collections.emptyMap();

   @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
         throws Exception {

      // handle the case where the user switches from the new UI back to the old
      //
      // if the user asks for      /dashboard/index
      // instead redirect them to  /#/dashboard/index
      //
      if (!useNewUi() && !AfWebHandlerInterceptor.isApiRequest(handler)) {

         String pathInfo = request.getPathInfo();
         if (null != pathInfo && !pathInfo.isEmpty() && pathInfo.startsWith("/newui")) {
            response.setHeader("Pragma", "no-cache");
            response.sendRedirect("/");
            return false;
         }
      }
      return true;
   }

   @Override
   public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
      if (useNewUi()) {
         if (null != modelAndView) {
            ModelMap map = modelAndView.getModelMap();
            if (null != map) {
               map.put("newUI", true);
               map.put("legacyUI", true);

               map.put("tabs", SelectedTab.Tabs.values());
               map.put("compressJSandCSS", !_af.getJavaScriptLoggingEnabled());
               // figure out selected tab from the annotation
               //
               selectTab(handler, map);

               String normalizedViewName = normalizeViewName(modelAndView.getViewName());
               if (null != normalizedViewName) {
                  // see if we should translate the title
                  //
                  remapTitle(request.getLocale(), normalizedViewName, map);

                  // remap the view, if appropriate
                  //
                  remapView(modelAndView, normalizedViewName, map);
               }
            }
         }
      } else {
         /* Empty */
      }
   }

   /**
    * Sets the "selectedTab" property on the ModelMap to the value obtained
    * from the associated controller's {@see SelectedTab} annotation.
    *
    * @param handler The handler of the request, presumably a {@see Controller}.
    * @param map     The current model map of the request.  The "selectedTab" property
    *                will be set on this model if the controller is annotated.
    */
   private static void selectTab(@Nullable Object handler,
                                 @Nonnull ModelMap map) {
      if (null != handler) {
         SelectedTab annotation = handler.getClass().getAnnotation(SelectedTab.class);
         if (null != annotation) {
            SelectedTab.Tabs tab = annotation.value();
            if (null != tab) {
               map.put("selectedTab", tab.toString());
            }
         }
      }
   }

   /**
    * When newUI is set to true, this will remap the title of the given
    * view page.
    * <p/>
    * The new value is the name of the key in "messages.properties", which
    * will be looked up by the I18N class and the value of the lookup set
    * into the $pageTitle velocity variable.
    * The $pageTitle that was previously set, if any, will be placed in
    * the $legacyPageTitle variable.
    *
    * @param locale      Locale to use to look up the localized version of the new title (via {@link I18N}),
    *                    as supplied by the http request.
    * @param currentView A normalized (see {@link #normalizeViewName(String)}) view name
    * @param map         The current model map of the request.
    */
   private void remapTitle(@Nullable Locale locale,
                           @Nonnull String currentView,
                           @Nonnull ModelMap map) {
      String newTitleKey = titleMapping.get(currentView);
      if (null != newTitleKey) {
         String newPageTitle = I18N.translate(locale, newTitleKey);
         Object legacyTitle = map.get(AbstractUiController.PAGE_TITLE_KEY);
         map.put("legacyPageTitle", legacyTitle);
         map.put(AbstractUiController.PAGE_TITLE_KEY, newPageTitle);
      }
   }

   /**
    * When newUI is set to true, this will substitute the view (which is about
    * to be rendered) with a different one.
    *
    * @param modelAndView The current modelAndView of the request, about to be rendered.
    * @param currentView  A normalized (see {@link #normalizeViewName(String)}) view name
    * @param map          The current model map of the request.  If a replacement occurs, the name
    *                     of the previous view will be set in the "legacyView" model property.
    *                     <p/>
    *                     By saving the old view name, this allows views which will "wrap" old ones
    *                     as follows:
    *                     <p/>
    *                     <code>
    *                     ##example-new-view.vm
    *                     <h1>This is a new view, wrapping the old one!</h1>
    *                     <p/>
    *                     ## the old view goes here
    *                     #parse($legacyView)
    *                     </code>
    */
   private void remapView(@Nonnull ModelAndView modelAndView,
                          @Nonnull String currentView,
                          @Nonnull ModelMap map) {
      String newViewName = viewMapping.get(currentView);
      if (null != newViewName) {
         map.put("legacyView", currentView);
         modelAndView.setViewName(newViewName);
      }
   }

   private boolean useNewUi() {
      // This can still render old ui using upgrade, always return newUI
      // _config.getBool(ConfigRegistry.UI_NEW);
      return _config.isNewUI();
   }

   @SuppressWarnings({"NullableProblems"})
   public void setTitleMapping(@Nullable Map<String, String> titleMapping) {
      this.titleMapping = normalizeKeys(titleMapping);
   }

   @SuppressWarnings({"NullableProblems"})
   public void setViewMapping(@Nullable Map<String, String> viewMapping) {
      this.viewMapping = normalizeKeys(viewMapping);
   }

   /**
    * Attempts to convert all view names that Velocity understands to
    * a canonical format so that they can be compared.
    * <p/>
    * For instance, the following four strings all represent the same
    * view in velocity:
    * <code>
    * directory/myview
    * directory/myview.vm
    * /directory/myview
    * directory/myview.vm
    * </code>
    * <p/>
    * This function would change any of these inputs to
    * <code>
    * directory/myview.vm
    * </code>
    * <p/>
    * This is the format which appears to work best for the #parse() command.
    * <p/>
    * In particular, it removes any leading "/" from a view name, and
    * appends a ".vm" if it does not already exist.
    *
    * @param viewName
    * @return
    */
   @Nullable
   private static String normalizeViewName(@Nullable String viewName) {
      if (null == viewName) {
         return null;
      }
      String newName = viewName;
      if (viewName.startsWith("/")) {
         // remove the "/"
         //
         newName = viewName.substring(1);
      }
      if (!newName.endsWith(".vm")) {
         newName += ".vm";
      }
      return newName;
   }

   /**
    * Returns a copy of viewMapping with all of the keys normalized
    * by the normalizeViewName function
    *
    * @param viewMapping
    * @return
    */
   @Nonnull
   private static Map<String, String> normalizeKeys(@Nullable Map<String, String> viewMapping) {
      if (null == viewMapping) {
         return Collections.emptyMap();
      }
      Map<String, String> result = new HashMap<String, String>(viewMapping.size());
      for (Map.Entry<String, String> entry : viewMapping.entrySet()) {
         result.put(normalizeViewName(entry.getKey()), entry.getValue());
      }
      return ImmutableMap.copyOf(result);
   }
}
