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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import com.vmware.thinapp.common.workpool.dto.VmImage;

/**
 * Client for managing VmImages.
 *
 * @see VmImage
 */
public class VmImageClient {
   private static final RestTemplate template = new RestTemplate();
   private final String url;

   public VmImageClient(String url) {
      this.url = url;
   }

   public VmImage create(VmImage body) {
      URI result = template.postForLocation(url, body);
      return template.getForObject(result, VmImage.class);
   }

   public VmImage get(Long id) {
      return template.getForObject(url + "/{id}", VmImage.class, id);
   }

   public List<VmImage> list() {
      return Arrays.asList(template.getForObject(url, VmImage[].class));
   }

   public void delete(VmImage vmImage) {
      template.delete(url + "/{id}", vmImage.getId());
   }
}
