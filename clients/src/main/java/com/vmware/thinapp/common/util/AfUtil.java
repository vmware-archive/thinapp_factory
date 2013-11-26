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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;

/**
 * A catch-all class for various utility functions used in AppFactory.
 */
public class AfUtil {
   /** An HTTP header that stores attachment metadata of the resource. */
   public static final String CONTENT_DISPOSITION = "Content-Disposition";

   /** An HTTP header that defines the type of content in the message body */
   public static final String CONTENT_TYPE = "Content-Type";

   /** An HTTP header that defines the length of the message body */
   public static final String CONTENT_LENGTH = "Content-Length";

   /** A regex pattern to extract file name from the Content-Disposition header. */
   public static final Pattern FILENAME_PATTERN = Pattern
         .compile(".*filename=.+");

   /** A-Z alphabet used for generating random data */
   private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

   private static final int MAX_ICON_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

   private static final Random RANDOM = new Random();

   private static final Logger log = LoggerFactory.getLogger(AfUtil.class);

   private AfUtil() {
      // Do nothing, just hide it.
   }
   /**
    * Search a binary file for a given string, and at the location where it is
    * found, overwrite with the new string. This assumes there is enough
    * 'buffer' space in the file to accommodate the new string without going
    * past the EOF or overwriting any critical data.
    *
    * @param file File to change.
    * @param oldString Text to replace.
    * @param newString New text.
    * @param encoding Character encoding.
    * @return
    * @throws IOException
    */
   public static long binaryReplace(
         File file,
         String oldString,
         String newString,
         String encoding)
      throws IOException {
      boolean success = false;

      /* Convert old string into encoded bytes */
      byte[] oldBytes = oldString.getBytes(encoding);

      /* Read the file */
      byte[] fileBytes = FileCopyUtils.copyToByteArray(file);

      /* Search for the old string */
      int pos = indexOf(fileBytes, oldBytes);
      if (pos >= 0) {
         /* Replace file bytes with new string */
         byte[] newBytes = newString.getBytes(encoding);
         System.arraycopy(newBytes, 0, fileBytes, pos, newBytes.length);

         /* Rewrite the file */
         FileCopyUtils.copy(fileBytes, file);
         success = true;
      }

      return (success ? fileBytes.length : -1);
   }

   /**
    * Pluralize a noun.
    *
    * @param noun Word like "fish" or "broom"
    * @param count How many?
    * @return Word like "fishes" or "brooms" if count > 1.
    */
   public static String plural(String noun, int count) {
      return (
            count == 1 ? noun :
            noun.endsWith("s") ? noun + "es" :
            noun + "s");
   }

   /**
    * Convert into lowercase with uppercase initial.
    *
    * @param string Word like "hello".
    * @return Word like "Hello"
    */
   public static String toInitialCase(String string) {
      if (string.isEmpty()) {
         return string;
      }

      StringBuffer lower = new StringBuffer(string.toLowerCase());
      lower.setCharAt(0, Character.toUpperCase(lower.charAt(0)));
      return lower.toString();
   }

   /**
    * Return true if any of the given strings are null or empty.
    *
    * @param strings
    * @return True if any string is null or empty.
    */
   public static boolean anyEmpty(String... strings) {
      /* strings is null -> true */
      if (strings == null) {
         return true;
      }

      for (String string : strings) {
         if (!StringUtils.hasLength(string)) {
            return true;
         }
      }

      return false;
   }

   /**
    * Create the directory with the given name, including any necessary but
    * nonexistent parent directories.
    *
    * @param dir a directory name with or without path.
    * @return true if and only if the directory was created, along with all
    *    necessary parent directories; false otherwise.
    */
   public static boolean mkdirsIfNotExist(String dir) {
      if(anyEmpty(dir)) {
         throw new IllegalArgumentException("Invalid dir -> " + dir);
      }
      final File file = new File(dir);
      if(file.isDirectory()) {
         return false;
      }
      return file.mkdirs();
   }

   /**
    * Encode a URL path and optional query string according to RFC 2396.
    *
    * @param path Path to resource
    * @param query Query string
    * @return
    */
   public static String escapeUrlPath(String path, String query) {
      try {
         URI uri;
         uri = new URI("http", "authority", path, query, null);
         return uri.getRawPath() + "?" + uri.getRawQuery();
      }
      catch (URISyntaxException e) {
         /* This should not happen */
         return null;
      }
   }

   /**
    * Added prefix and postfix to a given input string if missing.
    *
    * @param prefix a prefix to append.
    * @param input an input string.
    * @param postfix a postfix to append.
    * @return a new string with prefix and postfix. If the input is empty or null,
    *    it then return just prefix and postfix.
    */
   public static String appendIfNotExist(String prefix, String input, String postfix) {
      if(anyEmpty(input)) {
         return String.valueOf(prefix + postfix);
      }
      final StringBuilder output = new StringBuilder();
      if(!anyEmpty(prefix) && !input.startsWith(prefix)) {
         output.append(prefix);
      }
      output.append(input);
      if(!anyEmpty(postfix) && !input.endsWith(postfix)) {
         output.append(postfix);
      }
      return output.toString();
   }

   /**
    * Convert to a java.net.URL from string url.
    *
    * @param urlStr a string url
    * @return an instance of URL if the url conversion succeeded; return null otherwise.
    */
   public static final URL toURL(String urlStr) {
      try {
         return new URL(urlStr);
      } catch(MalformedURLException ex) {
         return null;
      }
   }

   /**
    * Parse ISO date format date string to java.util.Date.
    *
    * @param dateStr a date string in yyyy-MM-dd format
    * @return an instance of java.util.Date if parsing was successful; otherwise, return null.
    */
   public static Date parseIsoDate(String dateStr) {
      String pattern = DateFormatUtils.ISO_DATE_FORMAT.getPattern();
      Date date = null;

      try {
         date = DateUtils.parseDate(dateStr, new String[] { pattern });
      } catch (ParseException e) {
         /* Eat the exception */
      }
      return date;
   }

   /**
    * Create a random garbage data value for testing.
    * @return A string of 5 chars with random A-Z letters in it.
    */
   public static final String randomString() {
      StringBuilder s = new StringBuilder("     ");
      for (int i = 0; i < s.length(); i++) {
         s.setCharAt(i, ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
      }

      return s.toString();
   }

   /**
    * Search data for a given pattern. This uses the Knuth-Morris-Pratt (KMP)
    * algorithm.
    */
   private static int indexOf(byte[] data, byte[] pattern) {
      int[] failure = computeFailure(pattern);
      int j = 0;

      if (data.length == 0) {
         return -1;
      }

      for (int i = 0; i < data.length; i++) {
         while (j > 0 && pattern[j] != data[i]) {
            j = failure[j - 1];
         }

         if (pattern[j] == data[i]) {
            j++;
         }

         if (j == pattern.length) {
            return i - pattern.length + 1;
         }
      }

      return -1;
   }

   /**
    * Compute the KMP failure function for a given byte pattern.
    */
   private static int[] computeFailure(byte[] pattern) {
      int[] failure = new int[pattern.length];
      int j = 0;

      for (int i = 1; i < pattern.length; i++) {
         while (j > 0 && pattern[j] != pattern[i]) {
            j = failure[j - 1];
         }
         if (pattern[j] == pattern[i]) {
            j++;
         }
         failure[i] = j;
      }

      return failure;
   }

   /**
    * Return true if the given string can be parsed as a URI and
    * begins with a URI scheme (e.g. "http://something"), false otherwise.
    *
    * @param uriString
    * @throws NullPointerException If uriString is null.
    * @return
    */
   public static boolean isAbsoluteUri(String uriString) {
      try {
         URI uri = new URI(uriString);
         return uri.getScheme() != null;
      }
      catch(URISyntaxException ex) {
         return false;
      }
   }

   /**
    * Checks whether the given URL string begins with a protocol (http://,
    * ftp://, etc.)  If it does, the string is returned unchanged.  If it does
    * not, full URL is returned and is constructed as parentUrl "/" url.
    *
    * @param url input URL string in absolute or relative form
    * @param parentUrl base URL to use if the given URL is in relative form
    * @return an absolute URL
    */
   public static URI relToAbs(String url, URI parentUrl)
      throws URISyntaxException {
      if (!StringUtils.hasLength(url)) {
         throw new URISyntaxException(url, "The input url was empty!");
      }
      URI parent2 = new URI(
            parentUrl.getScheme(),
            parentUrl.getUserInfo(),
            parentUrl.getAuthority(),
            parentUrl.getPort(),
            parentUrl.getPath() + "/", // Parent URL path must end with "/" for
                                       // this to work. resolve() removes any
                                       // duplicates.
            parentUrl.getQuery(),
            parentUrl.getFragment());

      return parent2.resolve(url.trim());
   }

   /**
    * Given a URI, return the last uri token.
    *
    * @param uri Last token
    * @return
    */
   public static String extractLastURIToken(URI uri) {
      if (uri == null || !StringUtils.hasLength(uri.getPath())) {
         return null;
      }
      String tokens[] = uri.getPath().split("/");
      return tokens[tokens.length - 1];
   }

   /**
    * Given a URI, return a new URI with the query, fragment, and top level of
    * the path removed.
    *
    * @param uri the input URI
    * @return the base URI
    */
   public static URI parentUri(URI uri) throws URISyntaxException {
      if (uri == null) {
         return null;
      }
      String protocol = uri.getScheme();
      String host = uri.getHost();

      // Process the port number, if any
      int port = uri.getPort();

      // Process the path, if any
      String path = uri.getPath();
      if (!StringUtils.hasLength(path)) {
         path = "";
      } else {
         if (path.endsWith("/")) {
            path = path.substring(0,path.length() - 1);
         }
         if (!path.contains("/")) {
            path = "";
         } else {
            int lastSlash = path.lastIndexOf('/');
            path = path.substring(0, lastSlash);
         }
      }

      // Build the final URL and return it
      return new URI(protocol, null, host, port, path, null, null);
   }

   /**
    * Convert enumeration values into string values.
    *
    * @param values
    * @return
    */
   public static String[] toNames(Enum<?>... values) {
      List<String> names = new ArrayList<String>();

      for (Enum<?> e : values) {
         names.add(e.name());
      }

      return names.toArray(new String[names.size()]);
   }

   /**
    * Convert a string url into a java.net.URI instance.
    *
    * @param url a url string.
    * @return an instance of URI.
    */
   public static URI toURI(String url) {
      if (url == null) {
         throw new IllegalArgumentException("Null url cannot be converted to a URI");
      }
      try {
         return new URI(url);
      } catch (URISyntaxException e) {
         throw new IllegalArgumentException(e);
      }
   }

   /**
    * Convert to a UNC path.
    *
    * @param server a file share server hostname or ip.
    * @param share a share
    * @param path a path
    * @return a UNC formatted string.
    */
   public static String toUNC(String server, String share, String path) {
      if (StringUtils.hasLength(path)) {
         return String.format("//%s/%s/%s", server, share, path).replace("/",
               "\\");
      }
      return String.format("//%s/%s", server, share).replace("/", "\\");
   }

   /**
    * Attempt to extract a filename from the HTTP headers when accessing the
    * given URL.
    *
    * @param url URL to access
    * @return filename extracted from the Content-Disposition HTTP header, null
    *         if extraction fails.
    */
   public static String getFilenameFromUrl(URL url) {
      String filename = null;

      if (url == null) {
         return null;
      }

      try {
         URLConnection connection = url.openConnection();
         connection.connect();

         // Pull out the Content-Disposition header if there is one
         String contentDisp = connection.getHeaderField(AfUtil.CONTENT_DISPOSITION);

         // Attempt to close the associated stream as we don't need it
         Closeables.closeQuietly(connection.getInputStream());

         // Attempt to extract the filename from the HTTP header
         filename = AfUtil.getFilenameFromContentDisposition(contentDisp);
      } catch (IOException ex) {
         // Unable to make the HTTP request to get the filename from the
         // message headers.
         // Do nothing, null will be returned.
      }
      return filename;
   }

   /**
    * Attempt to extract the filename from the HTTP response header.  If it is
    * not possible, attempt to pull it out of the given URI.  If that is not
    * possible, default to the given default filename.
    *
    * @param response HTTP response to process.
    * @param uri URI to process if HTTP response processing fails.
    * @param defaultFilename default filename to use if
    * @return the filename given the result of processing the given HTTP
    *         response, URI, and default filename.
    */
   public static String getFilenameFromHttpResponse(
         ClientHttpResponse response,
         URI uri,
         String defaultFilename) {
      String filename = AfUtil.getFilenameFromResponseHeader(response);

      // If filename not found in the HTTP response header, try to get it from
      // the given URI.
      if (filename == null) {
         filename = AfUtil.getFilenameFromURI(uri);
         // If it's still not available, then use the given default.
         if (filename == null) {
            filename = defaultFilename;
         }
      }

      return filename;
   }

   /**
    * Extract filename for the HTTP response header.
    *
    * @param response a ClientHttpResponse.
    * @return a filename if found or null.
    */
   public static final String getFilenameFromResponseHeader(
         ClientHttpResponse response) {
      String contentDisposition = response.getHeaders().getFirst(
            AfUtil.CONTENT_DISPOSITION);
      if (StringUtils.hasLength(contentDisposition)) {
         return AfUtil.getFilenameFromContentDisposition(contentDisposition);
      }
      return null;
   }

   /**
    * Attempt to extract the filename from the given Content-Disposition header
    * string.  Example of this header string:
    *
    * "attachment; filename=name_of_file.txt"
    *
    * @param contentDisposition string from a HTTP Content-Disposition header
    * @return the filename extracted from the given string, null if parsing the
    *         string fails.
    */
   public static final String getFilenameFromContentDisposition(
         String contentDisposition) {
      if (contentDisposition == null) {
         return null;
      }
      final Matcher matcher = AfUtil.FILENAME_PATTERN.matcher(contentDisposition);
      if (matcher.matches()) {
         String[] temp = contentDisposition.split("filename=");
         if (temp != null && temp.length > 1) {
            String filename = temp[1];
            /** filename could be in double quotes. */
            filename = StringUtils.deleteAny(filename, "\"");
            return filename;
         }
      }
      return null;
   }

   /**
    * Parse a filename from the given URI.
    *
    * @param uri a uri
    * @return a filename if the uri ends with just a resource, null otherwise
    */
   public static final String getFilenameFromURI(URI uri) {
      String path = uri.getPath();
      int lastSlash = path.lastIndexOf('/');

      // Is the last slash at the end?
      if (lastSlash == path.length() - 1) {
         return null;
      }
      return path.substring(lastSlash + 1);
   }

   /**
    * Simple container for an icon's binary data and content type.
    */
   public static class IconInfo {
      public byte[] data;
      public String contentType;

      public IconInfo(byte[] data, String contentType) {
         this.data = data;
         this.contentType = contentType;
      }
   }

   /**
    * Attempt to download the icon specified by the given URL.  If the resource at the URL
    * has a content type of image/*, the binary data for this resource will be downloaded.
    *
    * @param iconUrlStr URL of the image resource to access
    * @return the binary data and content type of the image resource at the given URL, null
    * if the URL is invalid, the resource does not have a content type starting with image/, or
    * on some other failure.
    */
   public static final @Nullable IconInfo getIconInfo(String iconUrlStr) {
      if (!StringUtils.hasLength(iconUrlStr)) {
         log.debug("No icon url exists.");
         return null;
      }
      URL iconUrl = null;
      try {
         // Need to encode any invalid characters before creating the URL object
         iconUrl = new URL(UriUtils.encodeHttpUrl(iconUrlStr, "UTF-8"));
      } catch (MalformedURLException ex) {
         log.debug("Malformed icon URL string: {}", iconUrlStr, ex);
         return null;
      } catch (UnsupportedEncodingException ex) {
         log.debug("Unable to encode icon URL string: {}", iconUrlStr, ex);
         return null;
      }

      // Open a connection with the given URL
      final URLConnection conn;
      final InputStream inputStream;
      try {
         conn = iconUrl.openConnection();
         inputStream = conn.getInputStream();
      } catch (IOException ex) {
         log.debug("Unable to open connection to URL: {}", iconUrlStr, ex);
         return null;
      }

      String contentType = conn.getContentType();
      int sizeBytes = conn.getContentLength();

      try {
         // Make sure the resource has an appropriate content type
         if (!conn.getContentType().startsWith("image/")) {
            log.debug("Resource at URL {} does not have a content type of image/*.", iconUrlStr);
            return null;
         // Make sure the resource is not too large
         } else if(sizeBytes > MAX_ICON_SIZE_BYTES) {
            log.debug("Image resource at URL {} is too large: {}", iconUrlStr, sizeBytes);
            return null;
         } else {
            // Convert the resource to a byte array
            byte[] iconBytes = ByteStreams.toByteArray(new InputSupplier<InputStream>() {
               @Override
               public InputStream getInput() throws IOException {
                  return inputStream;
               }
            });
            return new IconInfo(iconBytes, contentType);
         }
      } catch (IOException e) {
         log.debug("Error reading resource data.", e);
         return null;
      } finally {
         Closeables.closeQuietly(inputStream);
      }
   }

   /**
    * Chunk digits or non-digits from a string starting at an index.
    *
    * @param s
    * @param length
    * @param index
    * @return
    */
   public static String getDigitOrNonDigitChunk(String s, int length, int index) {
      StringBuilder sb = new StringBuilder();
      char c = s.charAt(index);
      sb.append(c);
      index++;
      boolean digitOrNot = Character.isDigit(c);
      while (index < length) {
         c = s.charAt(index);
         if (digitOrNot != Character.isDigit(c)) {
            break;
         }
         sb.append(c);
         index++;
      }
      return sb.toString();
   }

   /**
    * Compare two string by chuning digits and non-digits separately and compare between the two.
    * This way, the version strings (Ex: 10.2c.4.24546 sp1) can be properly compared.
    *
    * @param str1
    * @param str2
    * @return
    */
   public static int alnumCompare(String str1, String str2) {
      // Ensure null cases are handled for both s1 and s2.
      if (str1 == str2) {
         return 0;
      } else if (str1 == null) {
         return -1;
      } else if (str2 == null) {
         return 1;
      }
      int s1Length = str1.length();
      int s2Length = str2.length();

      for (int s1Index =0, s2Index = 0; s1Index < s1Length && s2Index < s2Length;) {
         String thisStr = getDigitOrNonDigitChunk(str1, s1Length, s1Index);
         s1Index += thisStr.length();

         String thatStr = getDigitOrNonDigitChunk(str2, s2Length, s2Index);
         s2Index += thatStr.length();

         // If both strs contain numeric characters, sort them numerically
         int result = 0;
         if (Character.isDigit(thisStr.charAt(0)) && Character.isDigit(thatStr.charAt(0))) {
            // Simple str comparison by length.
            int thisStrLength = thisStr.length();
            result = thisStrLength - thatStr.length();
            // If equal, the first different number counts
            if (result == 0) {
               for (int i = 0; i < thisStrLength; i++){
                  result = thisStr.charAt(i) - thatStr.charAt(i);
                  if (result != 0) {
                     return result;
                  }
               }
            }
         } else {
            result = thisStr.compareToIgnoreCase(thatStr);
         }

         if (result != 0) {
            return result;
         }
      }
      return s1Length - s2Length;
   }
}
