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

package com.vmware.appfactory.file.upload;

import org.apache.commons.fileupload.ProgressListener;

/**
 * Implementing the ProgressListener, used to store values related to the
 * upload progress.
 *
 * @see ProgressListener
 */
public class ProgressListenerImpl implements ProgressListener, Cloneable
{
   private long total = 0;
   private long current = 0;
   private boolean finished = false;

   public ProgressListenerImpl() {
      // Empty
   }

   public ProgressListenerImpl(long current, long total, boolean finished) {
      this.total = total;
      this.current = current;
      this.finished = finished;
   }

   @Override
   public void update(long currentLength, long totalLength, int itemSize)
   {
       this.current = currentLength;
       this.total = totalLength;
   }


   public void setFinished()
   {
       this.finished = true;
   }


   public boolean isFinished()
   {
       return finished;
   }


   public long getTotal()
   {
      return total;
   }


   public long getCurrent()
   {
      return current;
   }


   /**
    * Return the percentage of current over total.
    * @return
    */
   public int getPercentDone()
   {
      if (finished) {
         return 100;
      }

      // The update() is not invoked yet. Hence no upload so far.
      if (total == 0) {
         return 0;
      }

      // If the total size is not known, always show something remaining.
      if (total == -1) {
         return (int) Math.abs(current * 100.0 / (current + 900000));
      }
      return (int) Math.abs(current * 100.0 / total);
   }

   @Override
   public ProgressListenerImpl clone() {
      return new ProgressListenerImpl(current, total, finished);
   }

   @Override
   public String toString() {
      return "ProgressListenerImpl [_total=" + total
            + ", _current=" + current
            + ", _finished=" + finished + "]";
   }
}