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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility functions for handling dates and times, and instances of the
 * Calendar class.
 */
public class AfCalendar {
   /** Single instance representing "never" */
   public static final long NEVER = 0L;

   /** The UTC time zone */
   private static final TimeZone UTC_TZONE;

   /** Server local time zone */
   public static final TimeZone LOCAL_TZONE;

   /** Date formatter for UTC timezone */
   private static final DateFormat UTC_DATE_FORMAT;

   /** Date formatter for local timezone */
   private static final DateFormat LOCAL_DATE_FORMAT;

   private static final DateFormat PARSER_FORMAT;

   static {
      UTC_TZONE = TimeZone.getTimeZone("UTC");
      LOCAL_TZONE = TimeZone.getDefault();

      PARSER_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
      PARSER_FORMAT.setTimeZone(UTC_TZONE);

      UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
      UTC_DATE_FORMAT.setTimeZone(UTC_TZONE);

      LOCAL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
      LOCAL_DATE_FORMAT.setTimeZone(LOCAL_TZONE);
   }

   /**
    * Get the current UTC time.
    *
    * This is for evaluating elapsed times, recording times when actions
    * occur, etc.
    *
    * @return the current time as UTC milliseconds from the epoch
    */
   public static long Now() {
      return System.currentTimeMillis();
   }

   /**
    * Get the current UTC date/time as a Date object.
    *
    * @return the current UTC date/time
    */
   public static Date NowDate() {
      return new Date(Now());
   }

   /**
    * @return the current URC date/time as a string
    */
   public static String NowString() {
      return AfCalendar.formatUtc(NowDate());
   }
   /**
    * Format a Calendar instance into the standard AppFactory format.
    *
    * @param epochMsUtc
    * Milliseconds into the epoch in UTC.  Note that
    * System.currentTimeMillis() returns a local milliseconds value, not
    * necessarily a UTC value.
    *
    * @param local Show local time (else UTC time)
    * @return
    */
   public static String Format(long epochMsUtc, boolean local) {
      Date d;
      if (local) {
         d = new Date(epochMsUtc + LOCAL_TZONE.getRawOffset());
      } else {
         d = new Date(epochMsUtc);
      }
      return (local ? LOCAL_DATE_FORMAT.format(d) : UTC_DATE_FORMAT.format(d));
   }

   /**
    * Create a Calendar instance from the standard AppFactory format.
    *
    * @param text
    * @return
    */
   public static long Parse(String text) {
      try {
         return PARSER_FORMAT.parse(text).getTime();
      } catch(ParseException ex) {
         throw new IllegalArgumentException(ex);
      }
   }

   /**
    * Parse the given date string.
    *
    * @param text string of the date to parse
    * @return a Date representing the date of the given string
    */
   public static Date parseUtc(String text) {
      try {
         return UTC_DATE_FORMAT.parse(text);
      } catch (ParseException ex) {
         throw new IllegalArgumentException(ex);
      }
   }

   /**
    * Format the given date as a UTC date string.
    *
    * @param date the date to format into a string
    * @return a date string based on the given date
    */
   public static String formatUtc(Date date) {
      return UTC_DATE_FORMAT.format(date);
   }
}
