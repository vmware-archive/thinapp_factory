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

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/*
 * A log4j Logger Appender that redirect log messages to per-thread
 * file appenders.
 */
public class ThreadLocalFileAppender extends AppenderSkeleton {
    private static final Logger log = Logger.getLogger(ThreadLocalFileAppender.class);
    private static ThreadLocal<SharedFileAppender> threadLocal
                   = new ThreadLocal<SharedFileAppender>();

    private static class SharedFileAppender extends FileAppender {
       private int refcount;
       private final String key;
       SharedFileAppender(Layout layout, String filename)
                   throws IOException {
          super(layout, filename);
          this.refcount = 1;
          this.key = filename;
       }
       public synchronized void addRef() {
          this.refcount++;
       }
       public synchronized int deRef() {
          return --this.refcount;
       }
       public String getKey() {
          // we don't trust the getFile() of FileAppender
          return key;
       }
    }

    private static Map<String, SharedFileAppender> appenderMap
          = new HashMap<String, SharedFileAppender>();

    public ThreadLocalFileAppender() {
       super();
    }

    public ThreadLocalFileAppender(boolean isActive) {
       super(isActive);
    }

    /*
     *  Setup a file appender specific to the current thread.
     *
     *  @param filename the full path of the log file.
     */
    public static void set(String filename) throws Exception {
       if (filename == null) {
          return;
       }

       SharedFileAppender appender = null;
       synchronized (appenderMap) {
          appender = appenderMap.get(filename);
          if (appender != null) {
             appender.addRef();
             log.debug(String.format("Appender to %s increased refcount.",
                                     appender.getKey()));
          }
          else {
             Enumeration iter = Logger.getRootLogger().getAllAppenders();
             Layout layout = null;

             while (iter.hasMoreElements()) {
                Appender rootAppender = (Appender) iter.nextElement();
                if (rootAppender instanceof ThreadLocalFileAppender) {
                   layout = rootAppender.getLayout();
                   break;
                }
             }

             if (layout == null) {
                log.error("ThreadLocalFileAppender is not configured");
                return;
             }

             appender = new SharedFileAppender(layout, filename);
             appenderMap.put(filename, appender);
             log.debug(String.format("Appender to %s is opened.",
                                     appender.getKey()));
          }
       }
       threadLocal.set(appender);
    }

    /*
     * Remove and close/dispose the file appender specific to
     * the current thread.
     */
    public static void remove() {
       SharedFileAppender appender = threadLocal.get();
       if (appender == null) {
          return;
       }

       threadLocal.remove();

       synchronized (appenderMap) {
          if (appender.deRef() == 0) {
             appenderMap.remove(appender.getKey());
             appender.close();
             log.debug(String.format("Appender to %s is closed.",
                                     appender.getKey()));
          }
          else {
             log.debug(String.format("Appender to %s decreased refcount.",
                                     appender.getKey()));
          }
       }
    }

    @Override
    public void append(LoggingEvent event) {
       FileAppender appender = threadLocal.get();
       if (appender == null) {
          return;
       }
       appender.append(event);
    }

    @Override
    public boolean requiresLayout() {
       return true;
    }

    @Override
    public void close() {
       // Empty
    }
}
