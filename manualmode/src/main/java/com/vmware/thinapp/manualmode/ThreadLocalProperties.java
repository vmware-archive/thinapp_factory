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

package com.vmware.thinapp.manualmode;

import java.util.Properties;

public class ThreadLocalProperties {
    private static ThreadLocal<Properties> threadLocal
                   = new ThreadLocal<Properties>();

    public static final String PER_PROJECT_LOGFILE
                               = "manualmode.project.logfile";

    public static String getProperty(String key) {
       Properties props = threadLocal.get();
       if (props == null) {
          return null;
       }

       return props.getProperty(key);
    }

    public static void setProperty(String key, String value) {
       Properties props = threadLocal.get();
       if (props == null) {
          props = new Properties();
          threadLocal.set(props);
       }

       props.setProperty(key, value);
    }
}
