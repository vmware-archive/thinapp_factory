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

package com.vmware.thinapp.workpool.web.controllers;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.thinapp.workpool.NotConnectedException;
import com.vmware.thinapp.workpool.VCManager;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.thinapp.workpool.model.VmLocationModel;

/**
 * VC config REST controller.
 */
@Controller
@RequestMapping("/config")
public class ConfigController {
   @Autowired
   private VCManager vcManager;

   /**
    * Update the workpool VC configuration.
    *
    * @param config
    */
   @ResponseBody
   @RequestMapping(method = RequestMethod.POST)
   public void update(@RequestBody VCConfig config) {
      VCConfigModel vcConfig = new VCConfigModel();
      vcConfig.setVmLocation(new VmLocationModel());
      // XXX: Need to do this safer since this does all properties blindly.
      BeanUtils.copyProperties(config, vcConfig, new String[]{"vmLocation", "cloneSupport"});
      BeanUtils.copyProperties(config.getVmLocation(), vcConfig.getVmLocation());
      vcManager.update(vcConfig);
   }

   /**
    * Get the workpool VC configuration
    *
    * @return
    */
   @ResponseBody
   @RequestMapping(method = RequestMethod.GET)
   public
   VCConfig get() {
      VCConfig config = new VCConfig();
      VCConfigModel currentConfig = vcManager.getConfig();
      config.setVmLocation(new com.vmware.thinapp.common.workpool.dto.VmLocation());
      BeanUtils.copyProperties(currentConfig, config, new String[]{"vmLocation"});
      BeanUtils.copyProperties(currentConfig.getVmLocation(), config.getVmLocation());

      VCConfig.CloneSupport cloneSupport;

      try {
         cloneSupport = vcManager.isCloningSupported() ? VCConfig.CloneSupport.available : VCConfig.CloneSupport.unavailable;
      } catch (NotConnectedException e) {
         cloneSupport = VCConfig.CloneSupport.indeterminable;
      }

      config.setCloneSupport(cloneSupport);

      VCConfig.ApiType apiType;

      try {
         apiType = vcManager.getApiType();
      } catch (NotConnectedException e) {
         apiType = VCConfig.ApiType.indeterminable;
      }

      config.setApiType(apiType);
      return config;
   }
}
