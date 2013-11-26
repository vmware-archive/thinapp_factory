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

package com.vmware.appfactory.application.model;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * Class to describe the install instructions for an application from a feed.
 */
@Entity
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class AppInstall
   extends AbstractRecord
{
   @NotNull
   private String _command;


   /**
    * Create a new AfAppInstall instance.
    */
   public AppInstall()
   {
      /* Nothing to do */
   }


   /**
    * Create an instance with a given command.
    * @param command
    */
   public AppInstall(String command)
   {
      _command = command;
   }


   /**
    * Set the installer command.
    * Cannot be null.
    *
    * @param command
    */
   public void setCommand(String command)
   {
      if (command == null) {
         throw new IllegalArgumentException("AfAppInstall command cannot be null!");
      }

      _command = command.trim();
   }


   /**
    * Get the installer command.
    * Will not be null.
    *
    * @return
    */
   public String getCommand()
   {
      return _command;
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      AppInstall other = (AppInstall) record;
      int numChanges = 0;

      if (!StringUtils.equals(getCommand(), other.getCommand())) {
         setCommand(other.getCommand());
         numChanges++;
      }

      return numChanges;
   }


   @Override
   public AppInstall clone()
   {
      AppInstall clone = new AppInstall();
      clone._command = _command;
      return clone;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) {
         return false;
      }

      if (obj == this) {
         return true;
      }

      if (!(obj instanceof AppInstall)) {
         return false;
      }

      AppInstall other = (AppInstall) obj;
      return _command.equals(other._command);
   }


   @Override
   public int hashCode()
   {
      return _command.hashCode();
   }
}
