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

package com.vmware.thinapp.workpool;

import akka.dispatch.Future;

/**
 * Dummy wrapper around Futures so that Akka doesn't think it's actually a future.
 *
 * TODO: Dump this when Akka 2.0 is released with support for typed actors to
 * return futures.
 *
 * @param <T>
 */
public class FutureWrapper<T> {
   private Future<T> realFuture;

   public FutureWrapper(Future<T> realFuture) {
      this.realFuture = realFuture;
   }

   public Future<T> get() {
      return realFuture;
   }
}
