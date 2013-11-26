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

import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * Client for a workpool.
 */
public class WorkpoolClient {
   private static final RestTemplate template = new RestTemplate();
   private final String url;

   public WorkpoolClient(String url) {
      this.url = url;
   }

   public Workpool get(Long id) {
      return template.getForObject(url + "/{id}", Workpool.class, id);
   }

   public List<Workpool> list() {
      return Arrays.asList(template.getForObject(url, Workpool[].class));
   }

   public Workpool create(Workpool workpool) {
      URI uri = template.postForLocation(url, workpool);
      return template.getForObject(uri, Workpool.class);
   }

   public Lease acquire(Workpool workpool) {
      Long id = workpool.getId();
      return template.postForObject(url + "/acquire", "", Lease.class, id);
   }

   public void release(Lease lease) {
      template.postForObject(url + "/release", lease, null);
   }

   public void delete(Workpool workpool) {
      template.delete(url + "/{id}", workpool.getId());
   }

   public InstanceInfo addInstance(Workpool workpool, InstanceInfo instance) {
      URI uri = template.postForLocation(url + "/{id}/instances", instance, workpool.getId());
      return template.getForObject(uri, InstanceInfo.class);
   }

   public void removeInstance(Workpool workpool, Long instanceId) {
      template.delete(url + "/{id}/instances/{instanceId}", workpool.getId(), instanceId);
   }
}
