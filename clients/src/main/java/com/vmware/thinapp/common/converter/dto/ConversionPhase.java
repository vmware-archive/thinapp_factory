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

package com.vmware.thinapp.common.converter.dto;

/**
 * Each recipe step has commands that can be executed at certain
 * phases of the conversion process. The supported phases are enumerated
 * here.
 */
public enum ConversionPhase
{
   /**
    * Before capture is started.
    * Useful for installing prerequisites that you do not want captured.
    */
   precapture,

   /**
    * Before the main application is installed.
    * Anything done here is captured into the final package.
    */
   preinstall,

   /**
    * Main application installation.
    * This will override install commands from the feed (if any).
    * Anything done here is captured into the final package.
    */
   install,

   /**
    * After the main application is installed.
    * Anything done here is captured into the final package.
    */
   postinstall,

   /**
    * After the capture, but before the build.
    * This can be used to modify the project before the package is built.
    */
   prebuild
}
