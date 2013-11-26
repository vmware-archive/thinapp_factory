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

package com.vmware.appfactory.notification;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Object used for action alerts that are displayed on the page for alert
 * notification.
 */
public class ActionAlert implements Comparable<ActionAlert>
{
   /** Alert type - 'info' is default. */
   public enum AlertType
   {
      info,
      warn,
      error
   }

   /** Group to classify an ActionAlert */
   public enum Group
   {
      datastore,
      capture,
      manualCapture,
      build,
      workpool,
      image,
      feed
   }

   private AlertType _type;

   private Group _group;

   private int _count = 0;

   private long _timestamp = 0;

   private String[] _info;


   /**
    * Default constructor.
    */
   public ActionAlert()
   {
      _type = AlertType.info;
   }


   /**
    * Constructor that sets the type and group.
    *
    * @param type
    * @param group
    */
   public ActionAlert(AlertType type, Group group)
   {
      this._type = type;
      this._group = group;
   }


   /**
    * Constructor setting all parameters.
    *
    * @param type
    * @param group
    * @param count
    * @param timestamp
    * @param info
    */
   public ActionAlert(
         AlertType type,
         Group group,
         int count,
         long timestamp,
         String... infos)
   {
      this._type = type;
      this._group = group;
      this._count = count;
      this._timestamp = timestamp;
      this._info = infos;
   }


   /**
    * Constructor setting type, group and count parameters.
    *
    * @param type
    * @param group
    * @param count
    * @param timestamp
    */
   public ActionAlert(
         AlertType type,
         Group group,
         int count,
         long timestamp)
   {
      this._type = type;
      this._group = group;
      this._count = count;
      this._timestamp = timestamp;
   }


   /**
    * @return the _type
    */
   public AlertType getType()
   {
      return _type;
   }


   /**
    * @param type the _type to set
    */
   public void setType(AlertType type)
   {
      _type = type;
   }


   /**
    * @return the _group
    */
   public Group getGroup()
   {
      return _group;
   }


   /**
    * @param group the _group to set
    */
   public void setGroup(Group group)
   {
      _group = group;
   }


   /**
    * @return the _count
    */
   public int getCount()
   {
      return _count;
   }


   /**
    * @param count the _count to set
    */
   public void setCount(int count)
   {
      this._count = count;
   }


   /**
    * Increment the count.
    */
   public void incrementCount()
   {
      this._count++;
   }


   /**
    * Sets _timestamp to the highest of the @param timestsamp and _timestamp.
    */
   @JsonIgnore
   public void setIfRecentTimestamp(long timestamp)
   {
      if (this._timestamp < timestamp) {
         this._timestamp = timestamp;
      }
   }

   /**
    * @return the _timestamp
    */
   public long getTimestamp()
   {
      return _timestamp;
   }


   /**
    * @param timestamp the _timestamp to set
    */
   public void setTimestamp(long timestamp)
   {
      _timestamp = timestamp;
   }


   /**
    * @return the _info
    */
   public String[] getInfo()
   {
      return _info;
   }


   /**
    * @param info the _info to set
    */
   public void setInfo(String[] info)
   {
      _info = info;
   }


   /**
    * The default sort order for these ActionAlerts is to have the
    * latest _timestamp entity at the top. If _timestamp match, then
    * use group and then type.
    *
    * @param other
    * @return
    */
   @Override
   public int compareTo(ActionAlert o)
   {
      return new CompareToBuilder()
         .append(o._timestamp, this._timestamp)
         .append(o._group, this._group)
         .append(o._type, this._type)
         .toComparison();
   }
}
