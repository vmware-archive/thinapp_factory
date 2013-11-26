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

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Config setting constants.
 */
public final class ConfigRegistryConstants {
   /**
    * private constructor to prevent instantiation.
    */
   private ConfigRegistryConstants() {
      // Empty
   }

   /*
    * General configuration
    */
   public static final String CONF_GROUP_GENERAL = "GENERAL";
   /** How often the background processor looks for things to do */
   public static final String GEN_REFRESH_PERIOD_SECS = "general.refresh_period_secs";
   /** The skin used in web clients */
   public static final String GEN_SKIN = "general.skin";
   /** Has the EULA been accepted? */
   public static final String GEN_EULA_ACCEPTED = "general.eula_accepted";

   /*
    * Feed configuration
    */
   public static final String CONF_GROUP_FEEDS = "FEEDS";
   /** Rescan a feed if a scan fails? */
   public static final String FEED_RETRY_FAILED_SCAN = "feeds.retry_failed_scan";
   /** Convert apps in a feed if any conversion failed? */
   public static final String FEED_RETRY_FAILED_CONVERT = "feeds.retry_failed_convert";
   /** How long to wait between feed rescans */
   public static final String FEED_RESCAN_PERIOD_MINS = "feeds.rescan_period_mins";

   /*
    * File Share configuration
    */
   public static final String CONF_GROUP_FILESHARES = "FILESHARES";
   /** Maximum directory depth to scan installers in a file share */
   public static final String FILESHARE_MAX_DIR_DEPTH_SCAN = "fileshares.max_dir_depth_scan";
   /** Directory layout for application meta-data mapping */
   public static final String FILESHARE_DIR_LAYOUT = "fileshares.dir_layout";
   /** Directory holding recipes */
   public static final String FILESHARE_RECIPE_DIR = "fileshares.recipe_dir";
   /** Override app info metadata in file share rescan for metadata-modified apps */
   public static final String FILESHARE_OVERRIDE_APP_INFO_IN_RESCAN = "fileshares.override_app_info_in_rescan";

   /*
    * Application configuration
    */
   public static final String CONF_GROUP_APPS = "APPLICATIONS";
   /** How many failed conversion attempts before marking the app as bad? */
   public static final String FEEDS_MAX_CONVERT_ATTEMPTS = "feeds.max_convert_attempts";

   /*
    * Tasks/Task queue configuration
    */
   public static final String CONF_GROUP_TASKS = "TASKS";
   /** Max number of projects per batch request */
   public static final String TASKQ_MAX_PROJECTS_PER_BATCH = "taskq.max_projects_per_batch";
   /** Max number of task threads to run at once */
   public static final String TASKQ_MAX_CONCURRENT = "taskq.max_concurrent";
   /** Number of completed tasks to retain in memory */
   public static final String TASKQ_MAX_FINISHED_COUNT = "taskq.max_finished_count";
   /** Max number of task threads to run at once */
   public static final String TASKQ_MAX_CONCURRENT_SCANS = "taskq.max_concurrent_scans";
   /** Number of completed tasks to retain in memory */
   public static final String TASKQ_MAX_FINISHED_SCANS = "taskq.max_finished_scans";

   /*
    * CWS configuration
    */
   public static final String CONF_GROUP_CWS = "CWS";
   /** URL for the CWS service */
   public static final String CWS_SERVICE_URL = "cws.service_url";
   /** Stop checking CWS for updates */
   public static final String CWS_PAUSED = "cws.paused";
   /** URL for the conversion service */
   public static final String CWS_CONVERSIONS_URL = "cws.conversions_service_url";
   /** Timeout for stalled automatic conversion jobs */
   public static final String CWS_STALL_TIMEOUT = "cws.stall_timeout";
   /** Stall threshold for average CPU usage */
   public static final String CWS_STALL_CPU = "cws.stall_cpu";
   /** Stall threshold for average network usage */
   public static final String CWS_STALL_NET = "cws.stall_net";
   /** Stall threshold for average disk usage */
   public static final String CWS_STALL_DISK = "cws.stall_disk";
   /** Enable Quality Reporting on ThinApp packages */
   public static final String CWS_ENABLE_QUALITY_REPORTING = "cws.enable_quality_reporting";
   /** Tag to use when Quality Reporting is enabled */
   public static final String CWS_QUALITY_REPORTING_TAG = "cws.quality_reporting_tag";

   /*
    * Storage configuration
    */
   public static final String CONF_GROUP_STORAGE = "STORAGE";
   /** URL for the datastore service */
   public static final String DATASTORE_SERVICE_URL = "datastore.service_url";
   /** Default datastore for project files */
   public static final String DATASTORE_DEFAULT_OUTPUT_ID = "datastore.default_output_id";
   /** Default datastore for recipe file uploads */
   public static final String DATASTORE_DEFAULT_RECIPE_ID = "datastore.default_recipe_id";
   /** Show offline datastores in the UI */
   public static final String DATASTORE_SHOW_OFFLINE = "datastore.show_offline";

   /*
    * Workpool configuration
    */
   public static final String CONF_GROUP_WORKPOOL = "WORKPOOL";
   /** URL for the workpool service */
   public static final String WORKPOOL_SERVICE_URL = "workpool.service_url";
   /** Default workpool for build requests */
   public static final String WORKPOOL_DEFAULT_WORKPOOL = "workpool.default_workpool";
   /** Flag to change workpool clone support */
   public static final String WORKPOOL_DEFAULT_MAX_INSTANCE = "workpool.default_max_instance";
   /** Flag to indicate virtual infrastructure is VC. If not, its hostAgent(WS/Host) */
   public static final String WORKPOOL_VI_TYPE = "workpool.vi_type";
   /** Initial workpool setup check */
   public static final String WORKPOOL_SHOW_SETUP_ALERT = "workpool.show_setup_alert";

   /*
    * Runtime configuration.
    */
   public static final String THINAPP_RUNTIME_ID = "cws.thinapp_runtime_id";

   /*
    * Horizon configuration.
    */
   public static final String CONF_GROUP_HORIZON = "HORIZON";
   /** Flag indicating horizon publishing is enabled */
   public static final String HORIZON_ENABLED = "horizon.enabled";
   /** Horizon url for admin api to connect */
   public static final String HORIZON_URL = "horizon.url";
   /** Horizon flag to explicitly ignore the horizon ssl certificate warning/error. */
   public static final String HORIZON_IGNORE_CERT_WARN = "horizon.ignore.cert.warn";
   /** Horizon one time Oauth activation code */
   public static final String HORIZON_IDP_ACTIVATION_TOKEN = "horizon.apiKey";
   /** Horizon idp login clientId */
   public static final String HORIZON_CLIENT_USERNAME = "horizon.apiUsername";
   /** Horizon idp login clientSecret */
   public static final String HORIZON_CLIENT_PASSWORD = "horizon.apiPassword";
   /** Serialized oauth2 token that can be used later on */
   public static final String HORIZON_OAUTH2_TOKEN = "horizon.oauth2.token";

   /*
    * Debug configuration
    */
   public static final String CONF_GROUP_DEBUG = "DEBUG";
   /** Force a delay in all UI responses */
   public static final String DEBUG_WEBUI_UI_DELAY = "debug.webui_ui_delay";
   /** Force a delay in all API responses */
   public static final String DEBUG_WEBUI_API_DELAY = "debug.webui_api_delay";
   /** Force a delay in all simulator actions */
   public static final String DEBUG_WEBUI_SIMS_DELAY = "debug.webui_sims_delay";
   /** Enable Javascript logging */
   public static final String DEBUG_JAVASCRIPT_LOGGING = "debug.javascript_logging";
   /** Details JSON I/O tracing */
   public static final String DEBUG_JSON_LOGGING = "debug.json_logging";

   /** List of config entries hidden from the user on the default config page. */
   public static final Set<String> HIDDEN_CONFIG_KEYS = Sets.newHashSet(
         HORIZON_URL,
         HORIZON_IGNORE_CERT_WARN,
         HORIZON_IDP_ACTIVATION_TOKEN,
         HORIZON_CLIENT_USERNAME,
         HORIZON_CLIENT_PASSWORD,
         HORIZON_OAUTH2_TOKEN
   );
}
