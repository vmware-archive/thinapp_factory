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


package com.vmware.thinapp.common.workpool.client;

import org.springframework.web.client.RestTemplate;

import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.common.workpool.dto.VCConfig;

public class WorkpoolService {
   private static final RestTemplate template = new RestTemplate();

   private String url;
   private String configUrl;

   public WorkpoolService(String url) {
      this.url = url;
      this.configUrl = url + "/config";
   }

   public VCConfig getConfig() {
      return template.getForObject(configUrl, VCConfig.class);
   }

   public void setConfig(VCConfig vcConfig) {
      template.postForEntity(configUrl, vcConfig, null);
   }

   // @TODO What should be the URL?
   //Can the InventoryBrowser be a common/clients API that consumes getConfig()?
   public VINode getInventoryTree() {
      return template.getForObject(url + "/vi/tree", VINode.class);
   }

   public VmImageClient getVmImages() {
      return new VmImageClient(url + "/vmimages");
   }

   public WorkpoolClient getWorkpools() {
      return new WorkpoolClient(url + "/workpools");
   }
}
