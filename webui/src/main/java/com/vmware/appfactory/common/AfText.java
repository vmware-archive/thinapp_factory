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

package com.vmware.appfactory.common;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * Representation of text which might come in different formats.
 */
@Embeddable
public class AfText
{
   /** Length of the 'type' database column */
   public static final int TYPE_LEN = 128;

   /** Length of the 'content' database column */
   public static final int CONTENT_LEN = 4096;

   /* Referenced by embedding classes: do not rename */
   @NotNull
   @Column(length=TYPE_LEN)
   private String _contentType = "";

   /* Referenced by embedding classes: do not rename */
   @NotNull
   @Column(length=CONTENT_LEN)
   private String _content = "";


   /**
    * Create a plain text instance
    * @param content Plain text.
    * @return
    */
   public static final AfText plainTextInstance(String content)
   {
      AfText text = new AfText();
      text.setContentType("text/plain");
      text.setContent(AbstractRecord.truncate(content, CONTENT_LEN));
      return text;
   }


   /**
    * Create a new AfText instance.
    */
   public AfText()
   {
      /* Nothing to do */
   }


   /**
    * Get the text content.
    *
    * @return
    */
   public String getContent()
   {
      return _content;
   }


   /**
    * Set the text content.
    *
    * @param content
    */
   public void setContent(String content)
   {
      if (content == null) {
         throw new IllegalArgumentException();
      }

      _content = AbstractRecord.truncate(content, CONTENT_LEN);
   }


   /**
    * Get the content type.
    * This should be one of the MIME types supported by AppFactory.
    *
    * @return
    */
   public String getContentType()
   {
      return _contentType;
   }


   /**
    * Set the content type.
    * This should be one of the MIME types supported by AppFactory.
    *
    * @param contentType
    */
   public void setContentType(String contentType)
   {
      if (contentType == null) {
         throw new IllegalArgumentException();
      }

      _contentType = contentType;
   }


   @Override
   public AfText clone()
   {
      AfText clone = new AfText();
      clone._contentType = _contentType;
      clone._content = _content;
      return clone;
   }


   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AfText)) {
         return false;
      }

      AfText afText = (AfText) o;

      if (_content != null ? !_content.equals(afText._content) : afText._content != null) {
         return false;
      }
      if (_contentType != null ? !_contentType.equals(afText._contentType) : afText._contentType != null) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {
      int result = _contentType != null ? _contentType.hashCode() : 0;
      result = 31 * result + (_content != null ? _content.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
