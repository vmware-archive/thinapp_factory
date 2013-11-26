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

package com.vmware.appfactory.inventory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A node in the inventory tree, which could be a top level item or a child.
 * @author levans
 */
public class InventoryItem
{
   private final String _name;

   private final String _view;

   private int _count;

   private final List<InventoryItem> _children;


   /**
    * Create a new item with the given name, that opens the given view.
    * @param name
    * @param view
    */
   public InventoryItem(String name, String view)
   {
      this(name, view, 0);
   }


   /**
    * Create a new item with the given name, that opens the given view.
    * Also defines a count of items, displayed as a badge in the UI if > 0.
    * @param name
    * @param view
    * @param count
    */
   public InventoryItem(String name, String view, int count)
   {
      _name = name;
      _view = view;
      _count = count;
      _children = new ArrayList<InventoryItem>();
   }


   /**
    * Get the item's name.
    * @return
    */
   public String getName()
   {
      return _name;
   }


   /**
    * Get the number of children.
    * @return
    */
   public int getNumChildren()
   {
      return _children.size();
   }


   /**
    * Get one of the children.
    * @param index
    * @return
    */
   public InventoryItem getChild(int index)
   {
      return _children.get(index);
   }


   /**
    * Get an iterator over all children.
    * @return
    */
   public Iterator<InventoryItem> getChildren()
   {
      return _children.iterator();
   }


   /**
    * Add a new child item.
    * @param child
    */
   public void addChild(InventoryItem child)
   {
      _children.add(child);
   }


   /**
    * Get the view that this node opens.
    * Can be null for items that don't have a view of their own but just
    * have children.
    * @return
    */
   public String getView()
   {
      return _view;
   }


   /**
    * Set the item count, displayed as a badge in the UI if > 0.
    * @param count
    */
   public void setCount(int count)
   {
      _count = count;
   }


   /**
    * Get the item count, displayed as a badge in the UI if > 0.
    * @return
    */
   public int getCount()
   {
      return _count;
   }
}
