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

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Contains various JSON utility functions and helper classes.
 */
public class AfJson {
   private static final ObjectMapper _JSON_MAPPER;

   static {
      /* Create and configure a JSON object mapper */
      _JSON_MAPPER = new ObjectMapper();
      _JSON_MAPPER.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
      _JSON_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
   }

   /**
    * Get a singleton instance of a JSON object mapper.
    *
    * @return An instance of a JSON object mapper.
    */
   public static ObjectMapper ObjectMapper() {
      return _JSON_MAPPER;
   }

   /**
    * Class serializing dates into the AppFactory format.
    */
   public static class CalendarSerializer
      extends JsonSerializer<Long> {
      @Override
      public void serialize(Long epochMsUtc, JsonGenerator gen, SerializerProvider provider)
         throws IOException
      {
         if (epochMsUtc != 0) {
            String str = AfCalendar.Format(epochMsUtc, false);
            gen.writeString(str);
         }
         else {
            /*
             * TODO: Don't really want to write a null, but Jackson thinks
             * the value is non-null (since cal is non-null) so has already
             * written the key, so some kind of value is needed.
             */
            gen.writeNull();
         }
      }
   }

   /**
    * Class deserializing dates from the AppFactory format.
    */
   public static class CalendarDeserializer
      extends JsonDeserializer<Long> {
      @Override
      public Long deserialize(JsonParser parser, DeserializationContext context)
         throws IOException
      {
         ObjectMapper m = new ObjectMapper();
         String s = m.readValue(parser, String.class);

         try {
            return AfCalendar.Parse(s);
         }
         catch(IllegalArgumentException ex) {
            throw new IOException("Invalid date \"" + s + '"');
         }
      }
   }
}
