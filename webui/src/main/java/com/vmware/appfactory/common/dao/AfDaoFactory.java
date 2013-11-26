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

package com.vmware.appfactory.common.dao;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.vmware.appfactory.application.dao.AppBuildRequestDao;
import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.build.dao.BuildDao;
import com.vmware.appfactory.config.dao.ConfigDao;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.recipe.dao.RecipeDao;

/**
 * A factory class which can be used to get an instance of any of the
 * various DAO's that are used in AppFactory.
 */
@Service("daoFactory")
public class AfDaoFactory
{
   @Resource
   private ApplicationDao _appDao;

   @Resource
   private BuildDao _buildDao;

   @Resource
   private ConfigDao _configDao;

   @Resource
   private FeedDao _feedDao;

   @Resource
   private FileShareDao _fileShareDao;

   @Resource
   private RecipeDao _recipeDao;

   @Resource
   private AppBuildRequestDao _appBuildRequestDao;

   /**
    *
    *Get the ApplicationDao resource
    * @return
    */
   public ApplicationDao getApplicationDao() {
      return _appDao;
   }

   /**
    * Get the BuildDao resource
    * @return
    */
   public BuildDao getBuildDao() {
      return _buildDao;
   }

   /**
    * Get the ConfigDao resource
    * @return
    */
   public ConfigDao getConfigDao() {
      return _configDao;
   }

   /**
    * Get the FeedDao resource
    * @return
    */
   public FeedDao getFeedDao() {
      return _feedDao;
   }

   /**
    * Get the FileShareDao resource
    * @return
    */
   public FileShareDao getFileShareDao() {
      return _fileShareDao;
   }

   /**
    * Get the RecipeDao resource
    * @return
    */
   public RecipeDao getRecipeDao() {
      return _recipeDao;
   }

   /**
    * Get the AppBuildRequestDao.
    * @return a AppbuildRequestDaoImpl instance.
    */
   public AppBuildRequestDao getAppBuildRequestDao() {
      return _appBuildRequestDao;
   }
}
