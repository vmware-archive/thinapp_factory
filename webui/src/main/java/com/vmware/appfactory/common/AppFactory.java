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
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.vmware.appfactory.common.runner.LicenseStatus;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.cws.CwsSettingsRegValue;
import com.vmware.thinapp.common.exception.BaseRuntimeException;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * A utility class for passing common "global" data to all views from any
 * controller.
 *
 * TODO: Some of the stuff in here is just constants, so should be moved to
 * AfConstant. AfConstant should also be added to the default VTL model.
 */
@Component
public class AppFactory
{
   private static final String PRODUCT_NAME = "VMware ThinApp Factory";

   @Value("${webui.version}")
   private String _version;

   @Value("${webui.build}")
   private String _build;

   @Value("${webui.devmode.enable}")
   private boolean _devModeDeploy;

   @Value("${webui.devmode.upload.dir}")
   private String _devModeUploadDir;

   @Value("${webui.external-url.how-to-video}")
   private String _howToVideoUrl;

   @Value("${webui.external-url.app-feed-guide}")
   private String _appFeedGuideUrl;

   @Value("${webui.external-url.feedback}")
   private String _feedbackUrl;

   @Resource
   private ConfigRegistry _config;

   @Resource
   private LicenseStatus _licenseStatus;

   @Resource(name="kmsActivationKeyMap")
   private Map<String, String> _kmsActivationKeyMap;

   private String _cacheBreaker;

   /**
    * @return the _kmsKeyMap
    */
   public Map<String, String> getActivationKeyMap()
   {
      return _kmsActivationKeyMap;
   }


   /**
    * Method to get the kms activation code by a specific OsType
    * Supported osTypeNames: OsType implementation class names.
    *
    * @param osTypeName
    * @return
    */
   public String getKMSActivationKeyByOsType(String osTypeName)
   {
      if(osTypeName == null) {
         throw new BaseRuntimeException("Invalid input");
      }
      return _kmsActivationKeyMap.get(osTypeName);
   }


   /**
    * Get the official application name.
    * @return The official application name.
    */
   public String getProductName()
   {
      return PRODUCT_NAME;
   }


   /**
    * Return if the webui is deployed in development mode or other.
    * @return true if development mode.
    */
   public boolean isDevModeDeploy() {
      return _devModeDeploy;
   }


   /**
    * Returns the upload directory if set.
    *
    * During dev mode, this value is used over the default system temp folder.
    * @return
    */
   public String getDevModeUploadDir()
   {
      return _devModeUploadDir;
   }


   /**
    * Get the current AppFactory UI version.
    * This is read from system properties:
    * see webui.properties for values.
    * @return TAF UI version number.
    */
   public String getVersion()
   {
      return _version;
   }


   /**
    * Get the current AppFactory UI build number.
    * This is read from system properties:
    * see webui.properties for values.
    * @return TAF UI build number.
    */
   public String getBuild()
   {
      return _build;
   }


   /**
    * The view which is shown when TAF is first opened.
    * @return
    */
   public String getDefaultView()
   {
      return "/dashboard/index";
   }


   /**
    * Icon used for apps that don't have one.
    * @return
    */
   public AfIcon getDefaultIcon()
   {
      return AfIcon.DEFAULT_APPLICATION_ICON;
   }


   /**
    * Format Unix epoch milliseconds into a date and time string using the local
    * (on the server) time zone.
    *
    * @param date
    * @return
    */
   public String formatDate(long date)
   {
      return AfCalendar.Format(date, true);
   }


   /**
    * Get the skin name selected in the configuration.
    * @return
    */
   public String getSkinName()
   {
      return _config.getString(ConfigRegistryConstants.GEN_SKIN);
   }


   /**
    * Check whether or not in-browser JavaScript logging is enabled.
    * NOTE: The default value is false.
    * @return true if JavaScript logging is enabled; return false otherwise.
    *
    * Note: turning this on also turns off css/js compression
    */
   public boolean getJavaScriptLoggingEnabled()
   {
      return _config.getBool(ConfigRegistryConstants.DEBUG_JAVASCRIPT_LOGGING);
   }

   /**
    * Get all the supported Windows registry data types.
    * @return
    */
   public CwsSettingsRegValue.Type[] getRegistryValueTypes()
   {
      return CwsSettingsRegValue.Type.values();
   }


   /**
    * Get the license expiration status.
    * @return True if expired.
    */
   public boolean getIsExpired()
   {
      return _licenseStatus.isExpired();
   }


   /**
    * Get the number of days before expiration to show warning message.
    * @return Number of days before showing warning.
    */
   public long getNumDaysToShowWarningBeforeExpired()
   {
      return _licenseStatus.getNumDaysToShowWarningBeforeExpired();
   }


   /**
    * Get the number of days until expiration.
    * @return Number of days before expiration.
    */
   public long getNumDaysToExpiration()
   {
      return _licenseStatus.getNumDaysToExpiration();
   }


   /**
    * @return the howToVideoUrl
    */
   public String getHowToVideoUrl()
   {
      return _howToVideoUrl;
   }

    /**
     * @return the appFeedGuideUrl
     */
    public String getAppFeedGuideUrl() {
        return _appFeedGuideUrl;
    }

   /**
    * @return the feedbackUrl
    */
   public String getFeedbackUrl() {
      return _feedbackUrl;
   }

    private void initCacheBreaker() {
        if (_devModeDeploy) {
            // also add a static ID which changes on each deploy
            _cacheBreaker = UUID.randomUUID().toString();
        } else {
            _cacheBreaker = _version + _build;
        }
    }

    public String getCacheBreakingContextPath(String servletContextPath) {
        if (null == _cacheBreaker) {
            initCacheBreaker();
        }
        // prepend build number to context path
        return servletContextPath + '/' + _cacheBreaker;
    }

    public String getStaticResourceContextPath(String servletContextPath) {
        return getCacheBreakingContextPath(servletContextPath) + "/static";
    }
}
