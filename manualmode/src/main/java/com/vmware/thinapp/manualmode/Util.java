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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

@Component
public class Util {
   @Autowired
   ApplicationContext appCtxt;

   public <T> T autowire(T bean) {
      appCtxt.getAutowireCapableBeanFactory().autowireBean(bean);
      return bean;
   }

   @SuppressWarnings("unchecked")
   public <T> T getAutowiredBean(Class<T> beanType) {
      return (T) appCtxt.getAutowireCapableBeanFactory().createBean(beanType,
            AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true /* not really sure what this does */);
   }

   /**
    * Load bean from app context using its unique bean name or id.
    * @param name a name/id of the bean
    * @return an instance of a bean.
    */
   public Object getBean(String name) {
      return appCtxt.getBean(name);
   }

   /**
    * Load a resource from the Spring application context.
    * @param resourceName a resource name.
    *    For example:
    *       "classpath:file.txt"
    *       "http://myhost.com/path/file.txt"
    *       "file:/some/path/file.txt"
    * @return a Resource instance.
    */
   public Resource loadResource(String resourceName) {
      return appCtxt.getResource(resourceName);
   }

   /**
    * Extract a file from a Jar--it must be in the classpath-- and
    * return its absolute path.
    * @param resourceName a resource name.
    * @param tempFileName temporary file name which will get deleted when JVM terminates.
    * @return an absolute path of the given resource.
    * @throws Exception if any error raised while extracting a file from the jar.
    */
   public String extractFromJar(String resourceName, String tempFileName) throws Exception {
      if (!StringUtils.hasLength(resourceName)) {
         throw new IllegalArgumentException("resourceName is empty!");
      }
      final InputStream input = loadResource(String.format("classpath:%s", resourceName))
                  .getInputStream();
      final File outputFile = File.createTempFile("tmp-manualmode-" + tempFileName, null);
      outputFile.deleteOnExit();
      final OutputStream output = new FileOutputStream(outputFile);
      FileCopyUtils.copy(input, output);

      return outputFile.getAbsolutePath();
   }
}
