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

package com.vmware.thinapp.common.util;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Objects;

/**
 * An extension of Guava's {@link Function}, which can throw a typed exception.
 */
public interface ExceptionalFunction<F,T,E extends Exception> {
   /**
    * Returns the result of applying this function to {@code input}. This method is <i>generally
    * expected</i>, but not absolutely required, to have the following properties:
    *
    * <ul>
    * <li>Its execution does not cause any observable side effects.
    * <li>The computation is <i>consistent with equals</i>; that is, {@link Objects#equal
    *     Objects.equal}{@code (a, b)} implies that {@code Objects.equal(function.apply(a),
    *     function.apply(b))}.
    * </ul>
    *
    * @throws
    *
    * E if the implementing function does
    *
    * NullPointerException if {@code input} is null and this function does not accept null
    *     arguments
    */
   T apply(@Nullable F input) throws E;
}
