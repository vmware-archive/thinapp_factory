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

package com.vmware.appfactory.common.base;

import org.springframework.ui.ModelMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Base class to set model variables for the UI transition:
 *
 *  - newUI
 *    when true,
 *    the javascript code will make adjustments to behave as
 *    if it is running in the "new" UI
 *
 *  - legacyUI
 *    when true,
 *    all of the existing javascript / datatable libraries
 *    and the old CSS will be included on the page.  When
 *    false, they will not be.
 *
 * User: rude
 * Date: 11/11/11
 * Time: 1:39 PM
 */
public class AbstractNewUiController extends AbstractUiController {

    protected ModelMap getBaseModel(@Nonnull  HttpServletRequest request,
                                    @Nullable Locale locale,
                                    @Nullable String titleKey) {
        ModelMap result = super.getBaseModel(request, locale, titleKey);
        result.put("newUI", true);
        result.put("legacyUI", true);
        return result;
    }
}
