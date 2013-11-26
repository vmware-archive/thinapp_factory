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

import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.velocity.VelocityLayoutView;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

import com.google.common.collect.Maps;
import com.vmware.appfactory.config.ConfigRegistry;

/**
 * Selects from one of multiple {@link VelocityLayoutView}s based on the
 * velocity template path used to render the request.
 *
 * Inspired by this forum post:
 * http://forum.springsource.org/showthread.php?71320-Spring-MVC-Velocity-multiple-layout-mappings
 *
 * Note: this URL is based on where the VIEW comes from, not the URL of the page
 * load itself.  So a page loaded from http://host/some_url, but rendered with the
 * view of /another/view.vm would be matched against "/another/view.vm".
 *
 * This operation is only performed when the UI is in "new UI" mode.
 *
 * @author rude
 */
public class VelocityMultipleLayoutAndToolsViewResolver extends VelocityViewResolver {

    protected final Map<Pattern, String> layoutMap = Maps.newLinkedHashMap();

    private static Pattern wildcardPattern = Pattern.compile(".*");

    private String layoutKey = VelocityLayoutView.DEFAULT_LAYOUT_KEY;

    private String screenContentKey = VelocityLayoutView.DEFAULT_SCREEN_CONTENT_KEY;

   private String legacyLayout = VelocityLayoutView.DEFAULT_LAYOUT_URL;

    @Resource
    private ConfigRegistry _config;

   /**
     * Requires VelocityLayoutView.
     *
     * @see org.springframework.web.servlet.view.velocity.VelocityLayoutView
     */
    @Override
    protected Class<?> requiredViewClass() {
        return VelocityLayoutView.class;
    }

    /**
     * Set the context key used to specify an alternate layout to be used instead
     * of the default layout. Screen content templates can override the layout
     * template that they wish to be wrapped with by setting this value in the
     * template, for example:<br>
     * <code>#set( $layout = "MyLayout.vm" )</code>
     * <p>The default key is "layout", as illustrated above.
     *
     * @param layoutKey the name of the key you wish to use in your
     *                  screen content templates to override the layout template
     * @see VelocityLayoutView#setLayoutKey
     */
    public void setLayoutKey(final String layoutKey) {
        this.layoutKey = layoutKey;
    }

    /**
     * Set the name of the context key that will hold the content of
     * the screen within the layout template. This key must be present
     * in the layout template for the current screen to be rendered.
     * <p>Default is "screen_content": accessed in VTL as
     * <code>$screen_content</code>.
     *
     * @param screenContentKey the name of the screen content key to use
     * @see VelocityLayoutView#setScreenContentKey
     */
    public void setScreenContentKey(final String screenContentKey) {
        this.screenContentKey = screenContentKey;
    }

    @Override
    protected AbstractUrlBasedView buildView(final String viewName) throws Exception {

        // supposedly this is a legit cast because we returned the same class in
        // requiredViewClass()
        VelocityLayoutView view = (VelocityLayoutView) super.buildView(viewName);

        if (this.layoutKey != null) {
            view.setLayoutKey(this.layoutKey);
        }

        if (this.screenContentKey != null) {
            view.setScreenContentKey(this.screenContentKey);
        }

        if (_config.isNewUI()) {

           String normalizedViewName = viewName;
           if (normalizedViewName.startsWith("/")) {
              // remove the "/"
              //
              normalizedViewName = normalizedViewName.substring(1);
           }
           for (Map.Entry<Pattern,String> entry : layoutMap.entrySet() ) {
               if (entry.getKey().matcher( normalizedViewName ).matches()) {
                   view.setLayoutUrl(entry.getValue());
                   break;
               }
           }
        } else {
           view.setLayoutUrl(legacyLayout);
        }

        return view;
    }

   /**
    * Sets the mapping of view template path regular expressions to templates,
    * as set in mvc-spring.xml.
    *
    * An example map from that file is:
    *
    *      <property name="layoutMap">
    *        <map>
    *          <entry key="newui/*" value="templates/layout.vm" />
    *          <entry key="*" value="templates/legacy.vm" />
    *        </map>
    *      </property>
    *
    * This is an ordered map.  Each view path is compared against every entity
    * in turn, and the first matching one is returned.
    *
    * If key is empty, it is the same as "*".
    *
    * Note that the keys here refer to the paths of the Velocity view
    * tempate files, not the page URLs.
    *
    * @param layoutMap     A map as defined above.
    */
   public void setLayoutMap(@Nullable Map<String, String> layoutMap) {
       // copy the map, transforming the values as follows:
       //   * becomes .*
       // each value becomes a regexp object
       //
       synchronized (this.layoutMap) {
           this.layoutMap.clear();
           if (null != layoutMap) {
               for (Map.Entry<String,String> entry: layoutMap.entrySet()) {
                   Pattern regex = compileUserRegex(entry.getKey());
                   this.layoutMap.put(regex, entry.getValue());
               }
           }
       }
   }

   /**
    * The layout to use when new_UI is set to false.
    *
    * @param legacyLayout
    */
   public void setLegacyLayout(String legacyLayout) {
      this.legacyLayout = legacyLayout;
   }

   /**
    * Compiles a "user regex" into a Java regex Pattern.
    *
    * The only thing different between a "user regex" and a java
    * standard one is that a user regex expects:
    *    *
    * instead of
    *    .*
    *
    * e.g. a user would enter
    *        /admin/*
    *      rather than
    *        /admin/.*
    *
    * If the user input is empty or null, a pattern representing
    * ".*" (stored in wildcardPattern) is returned.
    *
    * @param input
    * @return
    */
   public static Pattern compileUserRegex(String input) {
       if (null == input) {
           return wildcardPattern;
       }
       String mappingRegex = StringUtils.replace(input, "*", ".*");
       return Pattern.compile(mappingRegex);
   }
}