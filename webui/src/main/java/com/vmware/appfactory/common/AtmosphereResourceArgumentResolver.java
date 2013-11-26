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

package com.vmware.appfactory.common;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResource;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Enables Spring to pass arguments of type AtmosphereResource
 * to annotated controllers.
 *
 * The AtmosphereResource class is needed to write a controller
 * which uses the Atmosphere library to return COMET responses to
 * the client.  COMET responses are long-running HTTP requests
 * which asynchronously push data to the browser as events happen,
 * rather than relying on client-side polling.
 */
public class AtmosphereResourceArgumentResolver implements WebArgumentResolver {

   /*
   * @see org.springframework.web.bind.support.WebArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.context.request.NativeWebRequest)
   */
   @Override
   public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception {

      if (AtmosphereResource.class.isAssignableFrom(methodParameter.getParameterType())) {
         return getAtmosphereResource(webRequest.getNativeRequest(ServletRequest.class));
      }
      return WebArgumentResolver.UNRESOLVED;
   }

   @SuppressWarnings("unchecked")
   public static AtmosphereResource<HttpServletRequest, HttpServletResponse> getAtmosphereResource(ServletRequest request) {
      String name = AtmosphereResource.class.getName();
      AtmosphereResource<HttpServletRequest, HttpServletResponse> attribute = (AtmosphereResource<HttpServletRequest, HttpServletResponse>)request.getAttribute(
            name);
      if (null == attribute) {
         throw new IllegalStateException("Atmosphere-implemented controller not called through Atomsphere servlet!");
      }
      return attribute;
   }
}
