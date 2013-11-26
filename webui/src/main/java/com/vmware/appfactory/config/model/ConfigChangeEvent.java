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

package com.vmware.appfactory.config.model;

import org.springframework.context.ApplicationEvent;

/**
 * Fired when the app's configuration is updated.
 *
 * Different elements of the app can hook this event
 * to reconfigure themselves as necessary.
 */
public class ConfigChangeEvent extends ApplicationEvent {
   public ConfigChangeEvent(Object source) {
      super(source);
   }
}
