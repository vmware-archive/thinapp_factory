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

import com.vmware.thinapp.common.util.AfCalendar;

/**
 * This immutable class set type, associated component and created timestamp
 * for a notification event.
 *
 * @author saung
 * @since M8 8/19/2011
 */
public class Event
{
   /**
    * Notification type - 'info' is default.
    */
   public enum Type
   {
      error,
      warn,
      info,
   }

   /**
    * Enum of TAF components
    */
   public enum Component
   {
      autoCapture,
      manualCapture,
      builds,
      publishing,
      recipes,
      workpool,
      config,
      feeds,
      other
   }

   /** Type of the event */
   private final Type _type;

   /** Created time stamp of the event */
   private final long _timeStamp;

   /** Event description */
   private final String _description;

   /** Component that this event is occurred. */
   private final Component _component;

   /**
    * A constructor with description and component.
    * It sets the event type to 'info'. If the given
    * component is null, it will set 'other' as default.
    * @param description
    * @param component
    */
   public Event(String description, Component component)
   {
      super();
      _type = Type.info;
      _timeStamp = AfCalendar.Now();
      _description = description;
      _component = (component == null) ? Component.other : component;
   }

   /**
    * A constructor with type, description and component
    * If the given component is null, it will set 'other' as default.
    * @param type
    * @param description
    * @param component
    */
   public Event(Type type, String description, Component component)
   {
      super();
      _type = type;
      _timeStamp = AfCalendar.Now();
      _description = description;
      _component = (component == null) ? Component.other : component;
   }

   /**
    * @return the type
    */
   public Type getType()
   {
      return _type;
   }

   /**
    * @return the description
    */
   public String getDescription()
   {
      return _description;
   }

   /**
    * @return the component
    */
   public Component getComponent()
   {
      return _component;
   }

   /**
    * @return the timeStamp
    */
   public long getTimeStamp()
   {
      return _timeStamp;
   }

   /**
    * Create an info-type event.
    *
    * @param description - a description of the event.
    * @param component - a component name.
    * @return an Event instance.
    */
   public static Event infoEvent(String description, Component component)
   {
      return new Event(Type.info, description, component);
   }

   /**
    * Create a warn-type event.
    *
    * @param description - a description of the event.
    * @param component - a component name.
    * @return an Event instance.
    */
   public static Event warnEvent(String description, Component component)
   {
      return new Event(Type.warn, description, component);
   }

   /**
    * Create an error-type event.
    *
    * @param description - a description of the event.
    * @param component - a component name.
    * @return an Event instance.
    */
   public static Event errorEvent(String description, Component component)
   {
      return new Event(Type.error, description, component);
   }
}
