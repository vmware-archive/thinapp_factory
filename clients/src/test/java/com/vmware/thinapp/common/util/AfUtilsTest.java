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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.google.common.collect.ImmutableList;


/**
 * A catch-all class for various utility functions used in AppFactory.
 */
public class AfUtilsTest {
   private static URI relToAbsBase;
   private static Map<String,String> parentUriTestCases;
   private static Map<String,String> relToAbsTestCases;
   private static Map<String,Boolean> isAbsTestCases;

   static {
      parentUriTestCases = new HashMap<String,String>();
      parentUriTestCases.put("http://test.com", "http://test.com");
      parentUriTestCases.put("http://test.com/", "http://test.com");
      parentUriTestCases.put("ftp://test.com/", "ftp://test.com");
      parentUriTestCases.put("http://test.com:5000", "http://test.com:5000");
      parentUriTestCases.put("http://test.com:5000/", "http://test.com:5000");
      parentUriTestCases.put("http://test.com/dir1", "http://test.com");
      parentUriTestCases.put("http://test.com/dir1/", "http://test.com");
      parentUriTestCases.put("http://test.com/dir1/index.html", "http://test.com/dir1");
      parentUriTestCases.put("http://test.com/dir1/dir2/index.html", "http://test.com/dir1/dir2");
      parentUriTestCases.put("http://test.com:5123/dir1/dir2/index.html", "http://test.com:5123/dir1/dir2");
      parentUriTestCases.put("http://test.com/feeds/feed.json", "http://test.com/feeds");
      parentUriTestCases.put("http://test.com/feeds/feed.json#testing_hashes", "http://test.com/feeds");
      parentUriTestCases.put("http://test.com/feeds/feed.json#testing_hashes?and=queries", "http://test.com/feeds");
      parentUriTestCases.put("http://test.com/feeds/feed.json#testing_hashes?and=queries&with=two", "http://test.com/feeds");
      parentUriTestCases.put("datastore://test.com/dir1/index.html", "datastore://test.com/dir1");
      parentUriTestCases.put("datastore://test.com/dir1/dir2/index.html", "datastore://test.com/dir1/dir2");

      try {
         relToAbsBase = new URI("http://base.com/blargh");

         relToAbsTestCases = new HashMap<String,String>();
         relToAbsTestCases.put("http://test.com/file.dat", "http://test.com/file.dat");
         relToAbsTestCases.put("test/file.dat", "http://base.com/blargh/test/file.dat");

         relToAbsTestCases.put("/test.jpg", "http://base.com/test.jpg");
         relToAbsTestCases.put("/folder1/test.dat", "http://base.com/folder1/test.dat");
         relToAbsTestCases.put("/folder1/f2/test.dat", "http://base.com/folder1/f2/test.dat");

         relToAbsTestCases.put("test.jpg", "http://base.com/blargh/test.jpg");
         relToAbsTestCases.put("folder1/test.dat", "http://base.com/blargh/folder1/test.dat");
         relToAbsTestCases.put("folder1/f2/test.dat", "http://base.com/blargh/folder1/f2/test.dat");

         relToAbsTestCases.put("http://base.com/blargh/test.jpg", "http://base.com/blargh/test.jpg");
         relToAbsTestCases.put("ftp://base.com/blargh/test.jpg", "ftp://base.com/blargh/test.jpg");
         relToAbsTestCases.put("datastore://base.com/blargh/test.jpg", "datastore://base.com/blargh/test.jpg");
      } catch(URISyntaxException ex) {
         fail("Invalid base URI: " + ex.getMessage());
      }

      isAbsTestCases = new HashMap<String,Boolean>();
      isAbsTestCases.put("", Boolean.FALSE);
      isAbsTestCases.put("test.jpg", Boolean.FALSE);
      isAbsTestCases.put("folder1/test.dat", Boolean.FALSE);
      isAbsTestCases.put("folder1/f2/test.dat", Boolean.FALSE);
      isAbsTestCases.put("http://base.com/blargh/test.jpg", Boolean.TRUE);
      isAbsTestCases.put("ftp://base.com/blargh/test.jpg", Boolean.TRUE);
      isAbsTestCases.put("datastore://base.com/blargh/test.jpg", Boolean.TRUE);
   }


   @Test
   public void anyEmpty() {
      assertTrue(true == AfUtil.anyEmpty((String[])null));
      assertTrue(true == AfUtil.anyEmpty(""));
      assertTrue(true == AfUtil.anyEmpty(null, null));
      assertTrue(true == AfUtil.anyEmpty("", null));
      assertTrue(true == AfUtil.anyEmpty("abcdef", ""));
      assertTrue(true == AfUtil.anyEmpty("abcdef", null));
      assertTrue(true == AfUtil.anyEmpty("abcdef", null, null));
      assertTrue(true == AfUtil.anyEmpty("", "abcd", "blargh"));
      assertTrue(true == AfUtil.anyEmpty(null, "abcd", "blargh"));

      assertTrue(false == AfUtil.anyEmpty());
      assertTrue(false == AfUtil.anyEmpty(" "));
      assertTrue(false == AfUtil.anyEmpty("123lkj*SDFLKJ"));
      assertTrue(false == AfUtil.anyEmpty("abcdef"));
      assertTrue(false == AfUtil.anyEmpty("abcdef", " "));
      assertTrue(false == AfUtil.anyEmpty("abcdef", " ", "abc"));
   }

   @Test
   public void testToURL() throws MalformedURLException {
      URL expected = new URL("http://127.0.0.1:8080/index.html");
      URL actual = AfUtil.toURL("http://127.0.0.1:8080/index.html");
      assertEquals(expected, actual);

      expected = null;
      actual = AfUtil.toURL("http://127.0.0.1:invalid/index.html");
      assertEquals(expected, actual);
   }

   @Test
   public void testParseDateStr_ISO_DATE_FORMAT() {
      Date actual = AfUtil.parseIsoDate("in-va-lid");
      assertEquals(null, actual);

      Calendar expected = Calendar.getInstance();
      expected.set(2011, Calendar.JUNE, 30);
      actual = AfUtil.parseIsoDate("2011-06-30");
      Calendar actualCalendar = Calendar.getInstance();
      actualCalendar.setTime(actual);
      assertTrue(expected.get(Calendar.YEAR) == actualCalendar.get(Calendar.YEAR) &&
            expected.get(Calendar.MONTH) == actualCalendar.get(Calendar.MONTH) &&
            expected.get(Calendar.DATE) == actualCalendar.get(Calendar.DATE));
   }

   @Test
   public void testRelToAbs() throws URISyntaxException {
      for (String rel : relToAbsTestCases.keySet()) {
         URI abs = AfUtil.relToAbs(rel, relToAbsBase);

         String target = relToAbsTestCases.get(rel);
         String actual = abs.toASCIIString();
         System.out.println("absToRel(" + rel + ") = " + actual);

         assertEquals(target, actual);
      }
   }

   @Test
   public void testParentUri() throws URISyntaxException {
      for (String uri : parentUriTestCases.keySet()) {
         URI parent = AfUtil.parentUri(new URI(uri));
         String target = parentUriTestCases.get(uri);
         String actual = parent.toASCIIString();
         System.out.println("base(" + uri + ") = " + actual);

         assertEquals(target, actual);
      }
   }

   @Test
   public void testAbsoluteUri()
   {
      for (String uri : isAbsTestCases.keySet()) {
         boolean target = isAbsTestCases.get(uri).booleanValue();
         boolean actual = AfUtil.isAbsoluteUri(uri);
         System.out.println("isAbsolute(" + uri + ") = " + actual);

         assertEquals(target, actual);
      }

      /* Try null pointer */
      try {

         System.out.println("isAbsolute(null)");
         AfUtil.isAbsoluteUri(null);
         org.junit.Assert.fail();
      }
      catch(NullPointerException ex) {
         /* OK */
      }
      catch (Throwable th) {
         /* Not OK */
         org.junit.Assert.fail();
      }
   }

   @Test
   public void testNowUtc() throws IOException {
      // only run these tests if the machine is not set to UTC
      Assume.assumeTrue(AfCalendar.LOCAL_TZONE.getRawOffset() != 0);

      long nowUtc = AfCalendar.Now();
      long nowLocal = System.currentTimeMillis();

      // the difference between nowUtc and now should be
      // very small.
      // They may differ since we measured the values at
      // different times, but they should be within 2 minutes
      // of each other.
      long allowedSkewMS = 2 * 60 * 60 * 1000;
      long difference = nowUtc - nowLocal;
      assertTrue("Now() not properly returning UTC time",
                 Math.abs(difference) < allowedSkewMS);
   }

   @Test
   public void testCalendarSerializerRoundTrip() throws IOException {
      for (long time : ImmutableList.of(System.currentTimeMillis(),0L)) {
         SimpleDateBean testBean = new SimpleDateBean(time);

         String jsonString = AfJson.ObjectMapper().writeValueAsString(testBean);

         SimpleDateBean rehydratedBean = AfJson.ObjectMapper().readValue(jsonString,  SimpleDateBean.class);

         assertEquals("Round-trip through JSON did not match original object.  Source JSON=" + jsonString,
               testBean, rehydratedBean);
         assertEquals(testBean.getTime(), rehydratedBean.getTime());
      }
   }

   @Test
   public void testCalendarSerializerDirectly() throws IOException, ParseException {
      // only run these tests if the machine is not set to UTC
      Assume.assumeTrue(AfCalendar.LOCAL_TZONE.getRawOffset() != 0);

      {
         long someTime = 1326835281356L;
         String utcTime = "2012-01-17 21:21:21 UTC";
         SimpleDateBean testBean = new SimpleDateBean(someTime);
         String jsonString = AfJson.ObjectMapper().writeValueAsString(testBean);
         assertNotNull(jsonString);
         assertTrue("Generated JSON string did not contain expected UTC time: "
                    + jsonString
                    + " expected " + utcTime,
                    jsonString.contains(utcTime));

         // we should get the same string when calling AfCalendar.Format
         // directly with the "local" flag to false
         String rawUtcTime = AfCalendar.Format(someTime,false);

         assertEquals(utcTime, rawUtcTime);

         // and if we pass the "local" flag to true, we should
         // get a different string
         String rawLocalTime = AfCalendar.Format(someTime,true);
         assertThat(rawLocalTime,is(not(equalTo(utcTime))));
      }

      // also try starting with JSON, and going to a known time
      {
         String knownUtcTimeString = "2012-11-10 13:14:15";
         long knownTimestamp = 1352553255000L;

         long computedTimestamp = AfCalendar.Parse(knownUtcTimeString);
         assertEquals(knownTimestamp, computedTimestamp);
      }
   }

   @Test
   /**
    * Tests comparison of version numbers separated by . and containing extra characters.
    */
   public void testAlnumComparision() {
      System.out.println("Testing: AfUtil.alnumCompare(src, dest)");
      Assert.assertTrue(AfUtil.alnumCompare("3.2.1", "10.4.4") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("3.2a.1", "3.3.1") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("3.2.1", "3.2a") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("3.2.1 SP1", "3.2.1 sp0") > 0);
      Assert.assertTrue(AfUtil.alnumCompare("3.2.1 SP1", "3.2.1 Sp2") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("3.2.1 SP1", "3.2.1 sP4") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("1a2.2.1", "1a3.4.4") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("1a9.2.1", "1a13.4.4") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("A13.2.001", "B3.2.1") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("A13.2.1", "b3.2.1") < 0);
      Assert.assertTrue(AfUtil.alnumCompare("A13.2.1", "B3.2.1") < 0);

      Assert.assertTrue(AfUtil.alnumCompare("1A-1BC", "1A-1BC") == 0);
      Assert.assertTrue(AfUtil.alnumCompare("3.2.1 abc23", "3.2.1 ABc23") == 0);
      Assert.assertTrue(AfUtil.alnumCompare("3.35462.1-2340", "3.35462.1-2340") == 0);
   }

   @Test
   public void testDigitOrNonDigitChunking() {
      String src = "ABcD1.98723-4564#1ABC.ex-ZAy0934BeY";
      System.out.println("Testing: AfUtil.getDigitOrNonDigitChunk(src, src.length(), index)");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 0), "ABcD");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 4), "1");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 5), ".");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 6), "98723");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 12), "4564");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 16), "#");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 17), "1");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 18), "ABC.ex-ZAy");
      Assert.assertEquals(AfUtil.getDigitOrNonDigitChunk(src, src.length(), 28), "0934");
   }
}
