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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import com.vmware.appfactory.notification.Event.Component;

/**
 * This enum class implements all operations of server-side notification system.
 * It's lightweight notification service and uses bounded double-ended queue. It
 * purges old events (FIFO) when the queue size reaches the limit.
 *
 *      oldest <-------------> newest
 * <- [ head | x | x | x | x | tail ] <- new events....
 *
 * @author saung
 * @since M8 8/19/2011
 */
public enum NotificationService
{
   /** Get singleton instance */
   INSTANCE;

   /** Maximum queue size */
   private static final int QUEUE_LIMIT = 50;

   /** Double-ended queue to store events. */
   private final LinkedBlockingDeque<Event> _deque = new LinkedBlockingDeque<Event>(QUEUE_LIMIT);

   /**
    * Create a new 'info' event.
    *
    * @param description - a description of the event.
    * @param component - a component name of the event.
    */
   public void newInfoEvent(String description, Component component)
   {
      addEvent(Event.infoEvent(description, component));
   }

   /**
    * Create a new 'warn' event.
    *
    * @param description - a description of the event.
    * @param component - a component name of the event.
    */
   public void newWarnEvent(String description, Component component)
   {
      addEvent(Event.warnEvent(description, component));
   }

   /**
    * Create a new 'error' event.
    *
    * @param description - a description of the event.
    * @param component - a component name of the event.
    */
   public void newErrorEvent(String description, Component component)
   {
      addEvent(Event.errorEvent(description, component));
   }

   /**
    * Get all the events from the queue.
    * @return an event iterator.
    */
   public Iterator<Event> iterator()
   {
      return _deque.iterator();
   }

   /**
    * Get most recent event from the queue.
    * @return an event if the queue is not empty; otherwise, return null.
    */
   public Event getMostRecentEvent()
   {
      return (_deque.size() > 0) ? _deque.getLast() : null;
   }

   /**
    * Get all the events created after a given time stamp.
    * @return an event iterator.
    */
   public Iterator<Event> iteratorAfter(long timeStamp)
   {
      final List<Event> newEvents = new ArrayList<Event>();
      final Iterator<Event> it = _deque.iterator();

      while (it.hasNext()) {
         final Event e = it.next();
         if (e.getTimeStamp() > timeStamp) {
            newEvents.add(e);
         }
      }

      return newEvents.iterator();
   }

   /**
    * Add a new event to the end of the queue. It also
    * pop out old event when new event inserting failed.
    *
    * @param e - an event to be added to the queue.
    */
   private void addEvent(Event e)
   {
      if ( !_deque.offer(e) ) {
         _deque.poll();
         _deque.offer(e);
      }
   }


}
