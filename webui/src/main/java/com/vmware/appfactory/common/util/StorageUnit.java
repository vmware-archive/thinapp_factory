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

package com.vmware.appfactory.common.util;

import java.text.MessageFormat;

/**
 * Converts long byte values into strings of appropriate units,
 * such as "12.3 MB".
 */
public enum StorageUnit {
   // note: this list MUST be in order of increasing size
   Bytes(1L,"bytes"),
   KB(1024L,"KB"),
   MB(1024L * 1024L,"MB"),
   GB(1024L * 1024L * 1024L,"GB"),
   TB(1024L * 1024L * 1024L * 1024L,"TB");

   private final long baseVal;
   private final String suffix;

   StorageUnit(long baseVal, String suffix) {
      this.baseVal = baseVal;
      this.suffix = suffix;
   }

   /**
    * Formats a long byte value into a string using common storage units.
    *  e.g.  1,000,000,000  => "1.0 GB"
    *        1,500,000      => "1.5 MB"
    *        216            => "216 bytes"
    *
    * @param bytes    number of bytes.  Should be a nonnegative value.
    *
    * @return A string formatted per the above rules.
    */
   public static String format(long bytes) {
      for (int i = StorageUnit.values().length - 1; i > 0; --i) {
         StorageUnit unit = StorageUnit.values()[i];
         if (bytes >= unit.baseVal) {
            double val = (bytes * 1.0) / unit.baseVal;
            String valFormat = String.format("%1$,.1f", val);
            return MessageFormat.format("{0} {1}", valFormat, unit.suffix);
         }
      }
      return bytes + " " + Bytes.suffix;
   }
}
