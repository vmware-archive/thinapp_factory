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

package com.vmware.appfactory.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.vmware.appfactory.common.dto.EachOption;
import com.vmware.appfactory.common.dto.GroupOption;
import com.vmware.appfactory.common.dto.SingleOption;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.dao.ConfigDao;
import com.vmware.appfactory.config.model.ConfigSetting;
import com.vmware.appfactory.datastore.DatastoreClientService;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.converter.client.ThinAppRuntimeClient;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;

import static com.vmware.appfactory.config.ConfigParam.*;
import static com.vmware.appfactory.config.ConfigRegistryConstants.*;

/**
 * This class handles all configuration operations for AppFactory. It defines
 * all the available configuration parameters, and methods to set and get values
 * for each one.
 *
 * Defaults for parameters are defined in resources/config.properties. If no
 * default is defined there, the default value will be 0, false, or "" as
 * appropriate.
 */
@SuppressWarnings("MissortedModifiers")
public class ConfigRegistry {
   /**
    * Get the logger
    */
   private final Logger _log = LoggerFactory.getLogger(ConfigRegistry.class);

   /**
    * This is the set of default configuration parameters.
    */
   private static final Map<String,ConfigParam> _PARAM_MAP = init();

   @Resource
   private ConfigDao _configDao;

   @GuardedBy(value="this")
   private Map<String, ConfigSetting> _settingsInDBMap;

   private LoadingCache<String, String> _stringCache;
   private LoadingCache<String, Boolean> _booleanCache;
   private LoadingCache<String, Integer> _intCache;
   private LoadingCache<String, Long> _longCache;

   /**
    * Initialize the config parameters map
    * @return a map of config parameters.
    */
   private static Map<String,ConfigParam> init() {
      final ImmutableMap.Builder<String,ConfigParam> builder = ImmutableMap.builder();
      int n = 0;
      /** Horizon **/
      builder.put(HORIZON_ENABLED,
            newBooleanInstance(CONF_GROUP_HORIZON, n++, HORIZON_ENABLED, false,
                  "Enabling horizon integration prompts for configuring horizon."));
      builder.put(HORIZON_URL,
            newStringInstance(CONF_GROUP_HORIZON, n++, HORIZON_URL, false));
      builder.put(HORIZON_IGNORE_CERT_WARN,
            newBooleanInstance(CONF_GROUP_HORIZON, n++, HORIZON_IGNORE_CERT_WARN, false));
      builder.put(HORIZON_IDP_ACTIVATION_TOKEN,
            newStringInstance(CONF_GROUP_HORIZON, n++, HORIZON_IDP_ACTIVATION_TOKEN, false));
      builder.put(HORIZON_CLIENT_USERNAME,
            newStringInstance(CONF_GROUP_HORIZON, n++, HORIZON_CLIENT_USERNAME, false));
      builder.put(HORIZON_CLIENT_PASSWORD,
            newStringInstance(CONF_GROUP_HORIZON, n++, HORIZON_CLIENT_PASSWORD, false));
      builder.put(HORIZON_OAUTH2_TOKEN,
            newStringInstance(CONF_GROUP_HORIZON, n++, HORIZON_OAUTH2_TOKEN, false));


      /** General **/
      builder.put(GEN_REFRESH_PERIOD_SECS,
            newLongInstance(CONF_GROUP_GENERAL, n++, GEN_REFRESH_PERIOD_SECS, false,
            "seconds (0 to suspend)"));
      builder.put(GEN_SKIN,
            newSelectInstance(CONF_GROUP_GENERAL, n++, GEN_SKIN, false, new SkinValues()));
      builder.put(GEN_EULA_ACCEPTED,
            newBooleanInstance(CONF_GROUP_GENERAL, n++, GEN_EULA_ACCEPTED, false));

      /** Feeds **/
      builder.put(FEED_RETRY_FAILED_SCAN,
            newBooleanInstance(CONF_GROUP_FEEDS, n++, FEED_RETRY_FAILED_SCAN, true));
      builder.put(FEED_RETRY_FAILED_CONVERT,
            newBooleanInstance(CONF_GROUP_FEEDS, n++, FEED_RETRY_FAILED_CONVERT, true));
      builder.put(FEED_RESCAN_PERIOD_MINS,
            newLongInstance(CONF_GROUP_FEEDS, n++, FEED_RESCAN_PERIOD_MINS, true, "minutes"));
      builder.put(FEEDS_MAX_CONVERT_ATTEMPTS,
            newLongInstance(CONF_GROUP_FEEDS, n++, FEEDS_MAX_CONVERT_ATTEMPTS, false, "attempts"));

      /** File Share **/
      builder.put(FILESHARE_MAX_DIR_DEPTH_SCAN,
            newLongInstance(CONF_GROUP_FILESHARES, n++, FILESHARE_MAX_DIR_DEPTH_SCAN, true, "subdirectories"));
      builder.put(FILESHARE_DIR_LAYOUT,
            newSelectInstance(CONF_GROUP_FILESHARES, n++, FILESHARE_DIR_LAYOUT, true, new DirLayoutValues()));
      builder.put(FILESHARE_RECIPE_DIR,
            newStringInstance(CONF_GROUP_FILESHARES, n++, FILESHARE_RECIPE_DIR, false));
      builder.put(FILESHARE_OVERRIDE_APP_INFO_IN_RESCAN,
            newBooleanInstance(CONF_GROUP_FILESHARES, n++, FILESHARE_OVERRIDE_APP_INFO_IN_RESCAN, true));

      /** Tasks **/
      builder.put(TASKQ_MAX_PROJECTS_PER_BATCH,
            newIntegerInstance(CONF_GROUP_TASKS, n++, TASKQ_MAX_PROJECTS_PER_BATCH, true, "per batch request"));
      builder.put(TASKQ_MAX_CONCURRENT,
            newIntegerInstance(CONF_GROUP_TASKS, n++, TASKQ_MAX_CONCURRENT,
            true, "conversion tasks (-1 to auto-set equal to # of vm images)"));
      builder.put(TASKQ_MAX_FINISHED_COUNT,
            newIntegerInstance(CONF_GROUP_TASKS, n++, TASKQ_MAX_FINISHED_COUNT,
            true, "completed conversion tasks to keep (-1 for the max, limited to 1000.  must restart server to take effect)"));
      builder.put(TASKQ_MAX_CONCURRENT_SCANS,
            newIntegerInstance(CONF_GROUP_TASKS, n++, TASKQ_MAX_CONCURRENT_SCANS,
            true, "feed scan tasks (-1 to auto-set to number of subscribed feeds)"));
      builder.put(TASKQ_MAX_FINISHED_SCANS,
            newIntegerInstance(CONF_GROUP_TASKS, n++, TASKQ_MAX_FINISHED_SCANS,
            true, "completed feed scan tasks to keep (-1 for the max, limited to 1000.  must restart server to take effect)"));

      /** CWS **/
      builder.put(CWS_SERVICE_URL,
            newStringInstance(CONF_GROUP_CWS, n++, CWS_SERVICE_URL, false));
      builder.put(CWS_CONVERSIONS_URL,
            newStringInstance(CONF_GROUP_CWS, n++, CWS_CONVERSIONS_URL, false));
      builder.put(CWS_PAUSED,
            newBooleanInstance(CONF_GROUP_CWS, n++, CWS_PAUSED, false));
      builder.put(THINAPP_RUNTIME_ID,
            newSelectInstance(CONF_GROUP_CWS, n++, THINAPP_RUNTIME_ID, true,
              new ConfigParamOptions() {
                 @Override
                 public List<EachOption> getValues(ConfigRegistry reg) {
                    return reg.getRuntimeOptions();
                 }
              }));
      builder.put(CWS_STALL_TIMEOUT,
            newLongInstance(CONF_GROUP_CWS, n++, CWS_STALL_TIMEOUT, true, "secs"));
      builder.put(CWS_STALL_CPU,
            newLongInstance(CONF_GROUP_CWS, n++, CWS_STALL_CPU, true, "%"));
      builder.put(CWS_STALL_NET,
            newLongInstance(CONF_GROUP_CWS, n++, CWS_STALL_NET, true, "%"));
      builder.put(CWS_STALL_DISK,
            newLongInstance(CONF_GROUP_CWS, n++, CWS_STALL_DISK, true, "%"));
      builder.put(CWS_ENABLE_QUALITY_REPORTING,
            newBooleanInstance(CONF_GROUP_CWS, n++, CWS_ENABLE_QUALITY_REPORTING, true));
      builder.put(CWS_QUALITY_REPORTING_TAG,
            newStringInstance(CONF_GROUP_CWS, n++, CWS_QUALITY_REPORTING_TAG, false));

      /** Datastore **/
      builder.put(DATASTORE_SERVICE_URL,
            newStringInstance(CONF_GROUP_STORAGE, n++, DATASTORE_SERVICE_URL, false));
      builder.put(DATASTORE_DEFAULT_OUTPUT_ID,
            newSelectInstance(CONF_GROUP_STORAGE, n++, DATASTORE_DEFAULT_OUTPUT_ID, true, new DefaultDatastoreValues()));
      builder.put(DATASTORE_DEFAULT_RECIPE_ID,
            newSelectInstance(CONF_GROUP_STORAGE, n++, DATASTORE_DEFAULT_RECIPE_ID, true, new DefaultDatastoreValues()));
      builder.put(DATASTORE_SHOW_OFFLINE,
            newBooleanInstance(CONF_GROUP_STORAGE, n++, DATASTORE_SHOW_OFFLINE, true));

      /** Workpool **/
      builder.put(WORKPOOL_SERVICE_URL,
            newStringInstance(CONF_GROUP_WORKPOOL, n++, WORKPOOL_SERVICE_URL, false));
      builder.put(WORKPOOL_DEFAULT_WORKPOOL,
            newSelectInstance(CONF_GROUP_WORKPOOL, n++, WORKPOOL_DEFAULT_WORKPOOL, false, new WorkpoolValues()));
      builder.put(WORKPOOL_VI_TYPE,
            newSelectInstance(CONF_GROUP_WORKPOOL, n++, WORKPOOL_VI_TYPE, false, new viTypes()));
      builder.put(WORKPOOL_DEFAULT_MAX_INSTANCE,
            newIntegerInstance(CONF_GROUP_WORKPOOL, n++, WORKPOOL_DEFAULT_MAX_INSTANCE,
            false, "(applicable when deployed to vCenter)"));
      builder.put(WORKPOOL_SHOW_SETUP_ALERT,
            newBooleanInstance(CONF_GROUP_WORKPOOL, n++, WORKPOOL_SHOW_SETUP_ALERT, false));

      /** Debug **/
      builder.put(DEBUG_JAVASCRIPT_LOGGING,
            newBooleanInstance(CONF_GROUP_DEBUG, n++, DEBUG_JAVASCRIPT_LOGGING, false));
      builder.put(DEBUG_JSON_LOGGING,
            newBooleanInstance(CONF_GROUP_DEBUG, n++, DEBUG_JSON_LOGGING, false));
      builder.put(DEBUG_WEBUI_UI_DELAY,
            newLongInstance(CONF_GROUP_DEBUG, n++, DEBUG_WEBUI_UI_DELAY, false, "ms"));
      builder.put(DEBUG_WEBUI_API_DELAY,
            newLongInstance(CONF_GROUP_DEBUG, n++, DEBUG_WEBUI_API_DELAY, false, "ms"));
      builder.put(DEBUG_WEBUI_SIMS_DELAY,
            newLongInstance(CONF_GROUP_DEBUG, n++, DEBUG_WEBUI_SIMS_DELAY, false, "ms"));

      return builder.build();
   }

   /**
    * This method will be invoked DURING spring initialization
    * because it is needed for setDefaults() to work.  Note that
    * it so happens that _configDao is set already by spring,
    * so we are lucky that this method does not crash.
    */
   private synchronized void initDbMapIfNecessary() {

      if (null != _settingsInDBMap) {
         // already initialized
         return;
      }

      // on a new DB (or on an update), ensure that a row exists for
      // every setting in _PARAM_MAP.
      for (String key: _PARAM_MAP.keySet()) {

         ConfigSetting setting = _configDao.findForKey(key);
         if (null == setting) {
            // add an empty entry
            setting = new ConfigSetting();
            setting.setKey(key);
            setting.setValue(null);
            _configDao.create(setting);
         }
      }

      /* Load all settings from DB */
      _settingsInDBMap = loadFromDB();

      _stringCache =
         CacheBuilder.newBuilder()
                     .concurrencyLevel(20)
                     .build(
                        new CacheLoader<String, String>() {
                           @Override
                           public String load(String key) {
                              return getStringInternal(key);
                           }
                        });

      _booleanCache =
         CacheBuilder.newBuilder()
                     .concurrencyLevel(20)
                     .build(
                        new CacheLoader<String, Boolean>() {
                           @Override
                           public Boolean load(String key) throws AfNotFoundException, WpException {
                              return getBoolInternal(key);
                           }
                        });

      _intCache =
         CacheBuilder.newBuilder()
                     .concurrencyLevel(20)
                     .build(
                        new CacheLoader<String, Integer>() {
                           @Override
                           public Integer load(String key) throws AfNotFoundException, WpException {
                              return getIntegerInternal(key);
                           }
                        });

      _longCache =
         CacheBuilder.newBuilder()
                     .concurrencyLevel(20)
                     .build(
                        new CacheLoader<String, Long>() {
                           @Override
                           public Long load(String key) {
                              return getLongInternal(key);
                           }
                        });
   }

   /**
    * Load all config settings from the DB.
    *
    * @return a map of config settings <param_key,ConfigSetting>
    */
   private Map<String, ConfigSetting> loadFromDB() {
      final List<ConfigSetting> settingsInDBList = _configDao.findAll();
      return Maps.uniqueIndex(settingsInDBList, new Function<ConfigSetting, String>() {
            /** Key function **/
            @Override
            public String apply(ConfigSetting setting) {
               return setting.getKey();
            }
      });
   }

   /**
    * Construct a list of options for ThinApp runtimes.
    */
   private List<EachOption> getRuntimeOptions() {
      List<ThinAppRuntime> runtimes = getRuntimes();
      Collection<EachOption> rt = Collections2.transform(
              runtimes,
              new Function<ThinAppRuntime, EachOption>() {
                 @Override
                 public EachOption apply(ThinAppRuntime thinAppRuntime) {
                    return new SingleOption(String.valueOf(thinAppRuntime.getBuild()), thinAppRuntime.getVersion());
                 }
              });
      return new ArrayList<EachOption>(rt);
   }

   // TODO Use a client whose url can be changed at runtime.
   public List<ThinAppRuntime> getRuntimes() {
      ThinAppRuntimeClient runtimeClient = new ThinAppRuntimeClient(getString(CWS_CONVERSIONS_URL));
      // Make a copy so that we can sort it;
      List<ThinAppRuntime> origList = Collections.emptyList();
      try {
         origList = runtimeClient.list();
      } catch (HttpClientErrorException e) {
         _log.error("Could not get list of runtime options, ignoring: " + e);
      }
      List<ThinAppRuntime> runtimes = new ArrayList<ThinAppRuntime>(origList);
      Collections.sort(runtimes, ThinAppRuntime.getBuildComparator());
      return runtimes;
   }

   /**
    * Set the default configuration values from the specified properties
    * file, which should exist in the class path.
    *
    * @param propsFileName Base name of the configuration file.
    */
   public void setDefaults(String propsFileName) {

      initDbMapIfNecessary();

      try {
         /* Convert name=value entries into configuration defaults */
         final ResourceBundle bundle = ResourceBundle.getBundle(propsFileName);
         for (String key : bundle.keySet()) {
            ConfigParam param = findParam(key);
            if (param == null) {
               _log.warn("Configuration file {} contains invalid key {}",
                         propsFileName,
                         key);
            } else {
               setValue(param, bundle.getString(key), true);
            }
         }
      } catch (MissingResourceException ex) {
         /* If file is not found, ignore it */
         _log.warn("Configuration file {} not found",
                   propsFileName);
      }
   }

   /**
    * Set a configuration parameter value.
    *
    * @param key
    * @param value
    */
   public void setValue(String key, String value) {
      ConfigParam param = findParam(key);
      if (param == null) {
         throw new IllegalArgumentException("Unknown parameter value=" + key);
      }
      setValue(param, value, false);
   }


   /**
    * Get all parameters plus their current or default values,
    * grouped together by parameter group.
    *
    * @param editable - filter data by editable only.
    * @return All params, mapped by group name.
    */
   public synchronized ConfigGroupMap getGroupMap(boolean editable) {
      ConfigGroupMap map = new ConfigGroupMap();

      for (ConfigParam param : _PARAM_MAP.values()) {
         // if editable only is requested, add only those.
         if (!editable || (editable && param.isUserEditable())) {
            map.add(param, getValue(param));
         }
      }

      return map;
   }


   /**
    * Get the value of a configuration parameter as a boolean.
    *
    * @param key
    * @return Boolean value of 'key'
    */
   public boolean getBool(String key) {
      return _booleanCache.getUnchecked(key);
   }

   private boolean getBoolInternal(String key) {
      String s = getValue(getParam(key));
      return Boolean.valueOf(s);
   }


   /**
    * Get the value of a configuration parameter as a string.
    *
    * @param key
    * @return String value of 'key'
    */
   public String getString(String key) {
      return _stringCache.getUnchecked(key);
   }

   private String getStringInternal(String key) {
      return getValue(getParam(key));
   }

   /**
    * Get the value of a configuration parameter as a long.
    * If the value is not set or is invalid, return 0L
    *
    * @param key
    * @return Long value of 'key' / 0L
    */
   public long getLong(String key) {
      return getLong(key, 0L);
   }

   /**
    * Get the value of a configuration parameter as a long.
    * If the value is not set or is not a long datatype, return
    * the default value thats set.
    *
    * @param key
    * @param defaultIfInvalidValue
    * @return Long value of 'key' / defaultIfInvalidValue
    */
   public long getLong(String key, long defaultIfInvalidValue) {
      Long result = null;
      try {
         result = _longCache.getUnchecked(key);
      } catch (CacheLoader.InvalidCacheLoadException e) {
         // this will happen if the value is not set, so ignore.
      }
      if (null == result) {
         result = defaultIfInvalidValue;
      }
      return result;
   }

   @Nullable
   public Long getLongInternal(String key) {
      String s = null;
      try {
         s = getValue(getParam(key));
         if (StringUtils.hasLength(s)) {
            return Long.valueOf(s).longValue();
         }
      } catch (NumberFormatException e) {
         // Since the input value was invalid, use the default value passed.
         _log.debug("Invalid long value for key[{}] : {} ", key, s);
      } catch (Exception e) {
         // If database access failed, this setting cannot be loaded, use default as below.
         _log.warn("Value lookup failed for key[{}] " + e.getMessage(), e);
      }

      return null;
   }


   /**
    * Get the value of a configuration parameter as an integer.
    * If the value is not set or is invalid, return 0
    *
    * @param key
    * @return Integer value of 'key' / 0
    */
   public int getInteger(String key) {
      return getInteger(key, 0);
   }


   /**
    * Get the value of a configuration parameter as an integer.
    * If the value is not set or is not a integer datatype, return
    * the default value thats set.
    *
    * @param key
    * @param defaultIfInvalidValue
    * @return integer value of 'key' / defaultIfInvalidValue
    */
   public int getInteger(String key, int defaultIfInvalidValue) {
      Integer result = null;
      try {
         result = _intCache.getUnchecked(key);
      } catch (CacheLoader.InvalidCacheLoadException e) {
         // this will happen if the value is not set, so ignore.
      }
      if (null == result) {
         result = defaultIfInvalidValue;
      }
      return result;
   }

   private Integer getIntegerInternal(String key) {
      String s = null;
      try {
         s = getValue(getParam(key));
         if (StringUtils.hasLength(s)) {
            return Integer.valueOf(s).intValue();
         }
      } catch (NumberFormatException e) {
         // Since the input value was invalid, use the default value passed.
         _log.debug("Invalid int value for key[{}] : {} ", key, s);
      } catch (Exception e) {
         // If database access failed, this setting cannot be loaded, use default as below.
         _log.warn("Value lookup failed for key[{}] " + e.getMessage(), e);
      }
      return null;
   }


   /**
    * Get the parameter matching the given key.
    * If the key is invalid, an IllegalArgumentException is thrown.
    *
    * @param key
    * @return Parameter with the given key
    * @throws IllegalArgumentException
    */
   public ConfigParam getParam(String key)
         throws IllegalArgumentException {
      ConfigParam param = findParam(key);
      if (param == null) {
         throw new IllegalArgumentException("Invalid configuration key " + key);
      }

      return param;
   }


   /**
    * Find the parameter matching the given key.
    * If the key is invalid, return null.
    *
    * @param key
    * @return Parameter with the given key, or null if none.
    */
   public ConfigParam findParam(String key) {
      return _PARAM_MAP.get(key);
   }


   /**
    * Find a value for the named configuration parameter. If no custom value is
    * found, the built-in default is returned.
    *
    * @param param Parameter to query.
    * @return Current value, or a default.
    */
   @Nonnull
   private synchronized String getValue(ConfigParam param) {
      initDbMapIfNecessary();

      ConfigSetting setting = _settingsInDBMap.get(param.getKey());
      if (setting == null) {
         throw new IllegalArgumentException("Unknown param " + param.getKey());
      }
      String value = setting.getValue();
      if (null != value) {
         return value;
      }

      // if value is "null", that means that no value has been saved
      // to the database yet.  Instead of just returning this, we compute
      // a default value here.  Note that attempting to compute this default
      // value has been a source of bugs, as outside of this function we
      // can no longer distinguish between a properly recorded value which
      // is changed only by the result of a controller action (with appropriate
      // change events fired) and a computed value (which may have been returned
      // from a web service) which may change from minute-to-minute with no one
      // noticing.

      switch (param.getType()) {
         case BOOLEAN:
            return "false";
         case INTEGER:
            return "0";
         case LONG:
            return "0";
         case STRING:
            return "";
         case SINGLE_SELECT:
            List<EachOption> vals = param.getOptions().getValues(this);
            if (vals.isEmpty() || vals.get(0) instanceof GroupOption) {
               return "";
            }
            return ((SingleOption) vals.get(0)).getKey();
      }

      throw new RuntimeException(
            String.format("Parameter %s has an unknown type %s",
                          param.getKey(),
                          param.getType().toString()));
   }

   /**
    * Set the value for a given configuration parameter.
    *
    * Due to the fact that default values can change dynamically
    * (as in the case under a fresh install for runtimes, workpools,
    * and datastores), this method will always save the desired
    * value to the database, even if it appears to the the same as
    * the default.  For more info, see bug 837469.
    *
    * @param param a config parameter instance.
    * @param value a new or updated value.
    * @param onlyIfUnset when true, the new value will only be set if the
    *                existing value is null
    */
   private synchronized void setValue(@Nonnull ConfigParam param,
                         @Nonnull String value,
                         boolean onlyIfUnset) {

      initDbMapIfNecessary();

      String key = param.getKey();
      ConfigSetting setting = _settingsInDBMap.get(key);
      if (setting == null) {
         // the setting must have already been created!
         throw new IllegalArgumentException("Unknown setting " + param);
      }

      // we're setting a default value.  Don't overwrite a value
      // that is already present in the db, and log our actions
      // in either case.
      if (onlyIfUnset) {
         if (null == setting.getValue()) {
            _log.debug("Setting default for {}", key);
         } else {
            // don't change what's already there
            _log.debug("Ignoring default for {}, value already defined", key);
            return;
         }
      }

      // note: it is important to save the value passed in, even
      // if we think it is the same as what we already have recorded,
      // because sometimes the value from getValue() is NOT actually
      // what was retrieved from the database.  If the user has made
      // a selection, we want to now store that value in the database,
      // so that the default can't change out from under him or her.
      // Please see the note in setValue() for more info.

      /* Change existing setting */
      setting.setValue(value);
      _configDao.update(setting);
      _settingsInDBMap.get(key).setValue(value);

      _booleanCache.invalidate(key);
      _stringCache.invalidate(key);
      _longCache.invalidate(key);
      _intCache.invalidate(key);
   }


   /**
    * This public method helps with consolidating all calls to 1 place.
    * This will be removed shortly, and hence adding this exception.
    * @return
    */
   public boolean isNewUI() {
      return true;
   }


   /**
    * Returns a ThinAppRuntimeClient
    *
    * We need to create this object each time, rather than use spring injection,
    * because its constructor takes the URL to the conversions web service.  This
    * means that if the URL changes during the lifetime of the app, the old client
    * object will still be calling into the old URL.
    *
    * @return
    * a ThinAppRuntimeClient instance pointing at the CURRENT web service url
    */
   public ThinAppRuntimeClient newThinappRuntimeClient() {
      return new ThinAppRuntimeClient(
            getString(CWS_CONVERSIONS_URL)
      );
   }

   public long getDefaultRuntime()
   {
      Long result = getLong(THINAPP_RUNTIME_ID, -1L);

      // that's awful.  We can't start the app without this!
      // in order to continue (to even get to the settings page),
      // try to get the list of available runtime and just pick the
      // most recent one.
      //
      if (-1L == result) {
         // take the last one in the server's list
         List<ThinAppRuntime> runtimes = getRuntimes();
         if (null != runtimes && !runtimes.isEmpty()) {
            ThinAppRuntime volunteer = runtimes.get(runtimes.size() - 1);
            result = volunteer.getId();
         }
      }
      return result;
   }

   /**
    * Get maximum projects allowed for a batch capture request.
    *
    * @return maximum number of projects
    */
   public int getMaxProjectsPerBatch() {
      int defaultValueIfInvalid = 500;
      return getInteger(TASKQ_MAX_PROJECTS_PER_BATCH, defaultValueIfInvalid);
   }

   /**
    * Determine if publishing to Horizon is enabled.
    *
    * @return
    */
   public boolean publishToHorizonEnabled() {
      return getBool(HORIZON_ENABLED);
   }

   /**
    * Get the Horizon URL if Horizon publishing is enabled, otherwise null.
    *
    * @return
    */
   public String getHorizonUrlIfEnabled() {
      return getBool(HORIZON_ENABLED) ? getString(HORIZON_URL) : null;
   }

   /**
    * Implementation of AfConfigParamValues which returns a list of
    * datastore names for the DATASTORE_DEFAULT_OUTPUT configuration parameter.
    */
   public static final class DefaultDatastoreValues
         implements ConfigParamOptions {
      @Override
      public List<EachOption> getValues(ConfigRegistry reg) {
         List<EachOption> names = new ArrayList<EachOption>();
         try {
            DatastoreClientService dsClient = new DatastoreClientService(reg);
            for (DsDatastore ds : dsClient.getAllDatastores()) {
               if (ds.getStatus() == Datastore.Status.online) {
                  names.add(new SingleOption(ds.getId().toString(), ds.getName()));
               }
            }
         } catch (Exception ex) {
            // Ignore
         }
         return names;
      }
   }


   /**
    * Implementation of AfConfigParamValues which returns a list of
    * known UI skins
    */
   public static final class SkinValues
         implements ConfigParamOptions {
      private static final List<EachOption> SKIN_NAMES = new ArrayList<EachOption>();

      static {
         SKIN_NAMES.add(new SingleOption("base"));
      }

      @Override
      public List<EachOption> getValues(ConfigRegistry reg) {
         return SKIN_NAMES;
      }
   }


   /**
    * Implementation of AfConfigParamValues which returns a list of
    * known virtual infrastructure types that TAF workpool uses.
    */
   public static final class viTypes
         implements ConfigParamOptions {
      public enum VIType {
         vc,
         vcNoClone,
         wsNoClone
      }

      private static final List<EachOption> VI_TYPES = new ArrayList<EachOption>();

      static {
         VI_TYPES.add(new SingleOption(VIType.vc.name(),
                                       "M.CONFIG.VI.TYPE." + VIType.vc.name(), true));
         VI_TYPES.add(new SingleOption(VIType.vcNoClone.name(),
                                       "M.CONFIG.VI.TYPE." + VIType.vcNoClone.name(), true));
         VI_TYPES.add(new SingleOption(VIType.wsNoClone.name(),
                                       "M.CONFIG.VI.TYPE." + VIType.wsNoClone.name(), true));
      }

      @Override
      public List<EachOption> getValues(ConfigRegistry reg) {
         return VI_TYPES;
      }
   }


   /**
    * Implementation of AfConfigParamValues which returns a list of
    * available workpools
    */
   public static final class WorkpoolValues
         implements ConfigParamOptions {
      @Override
      public List<EachOption> getValues(ConfigRegistry reg) {
         List<EachOption> names = new ArrayList<EachOption>();
         WorkpoolClientService client = new WorkpoolClientService(reg);
         try {
            List<Workpool> workpools = client.getAllWorkpools();
            for (Workpool workpool : workpools) {
               names.add(new SingleOption(
                     workpool.getId().toString(),
                     workpool.getName()));
            }
         } catch (WpException ex) {
            // Failed to get workpools; ignore
         }
         return names;
      }
   }

   /**
    * Implementation of AfConfigParamValues which returns a list of
    * known file share directory layouts.
    */
   public static final class DirLayoutValues
         implements ConfigParamOptions {
      private static final List<EachOption> DIR_LAYOUTS = new ArrayList<EachOption>();

      static {
         DIR_LAYOUTS.add(new SingleOption("vendor/name/version/locale/revision"));
         DIR_LAYOUTS.add(new SingleOption("vendor/name/version/revision/locale"));
         DIR_LAYOUTS.add(new SingleOption("name/vendor/version/locale/revision"));
         DIR_LAYOUTS.add(new SingleOption("name/vendor/version/revision/locale"));
         DIR_LAYOUTS.add(new SingleOption("name/version/locale/revision"));
         DIR_LAYOUTS.add(new SingleOption("name/version/revision/locale"));
         DIR_LAYOUTS.add(new SingleOption("name/locale/version/revision"));
         DIR_LAYOUTS.add(new SingleOption("name/version"));
      }

      @Override
      public List<EachOption> getValues(ConfigRegistry reg) {
         return DIR_LAYOUTS;
      }
   }
}
