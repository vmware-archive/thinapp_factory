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

package com.vmware.appfactory.taskqueue.tasks.state.builder;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractFeedState;

@SuppressWarnings("unchecked")
public class AbstractFeedStateBuilder<
      API extends AbstractFeedState<API, Builder, StatusEnum>,
      Builder extends AbstractFeedStateBuilder<API, Builder, StatusEnum>,
      StatusEnum extends Enum<StatusEnum>>
      extends TaskStateBuilder<API, Builder, StatusEnum> {

   protected AbstractFeedStateBuilder(@Nonnull Class<? extends API> resultClass, @Nonnull String type) {
      super(resultClass, type);
   }

   public Builder withMaxConverisonAttempts(int maxConverisonAttempts) {
      addChange("maxConversionAttempts",maxConverisonAttempts);
      return (Builder)this;
   }

   public Builder withConverisonWorkpoolId(long conversionWorkpoolId) {
      addChange("conversionWorkpoolId",conversionWorkpoolId);
      return (Builder)this;
   }

   public Builder withConversionDatastoreId(long conversionDatastoreId) {
      addChange("conversionDatastoreId",conversionDatastoreId);
      return (Builder)this;
   }

   public Builder withConversionRuntimeId(long conversionRuntimeId) {
      addChange("conversionRuntimeId",conversionRuntimeId);
      return (Builder)this;
   }

   @Override
   public API build() {
      API result = super.build();
      Preconditions.checkArgument(result.getMaxConversionAttempts() > 0,
         "Must have at least one conversion attempt");

      Preconditions.checkArgument(result.getConversionWorkpoolId() != 0,
                                  "Need a converison workpool ID, or -1 for no workpool");

      Preconditions.checkArgument(result.getConversionDatastoreId() != 0,
                                  "Need a conversion datastore ID, or -1 for no datastore");

      Preconditions.checkArgument(result.getConversionRuntimeId() > 0,
                                  "Need a conversion runtime ID");

      return result;
   }


   @Override
   public Builder withOriginal(@Nonnull API original) {
      addChanges(original);
      return (Builder)this;
   }
}
