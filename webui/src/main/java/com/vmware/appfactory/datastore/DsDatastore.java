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

package com.vmware.appfactory.datastore;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.ProgressListener;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.springframework.web.multipart.MultipartFile;

import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.fileshare.model.FileShare;
import com.vmware.thinapp.common.datastore.dto.Datastore;

/**
 * Represents a datastore.
 *
 * Datastores are used for ThinApp package storage, access to installers
 * on file shares, etc. This is an interface to a specific implementation.
 */
@JsonTypeInfo(
      use=JsonTypeInfo.Id.CLASS,
      include= JsonTypeInfo.As.PROPERTY,
      property="_class")
@JsonSubTypes({@JsonSubTypes.Type(DsDatastoreCifs.class)})
@JsonIgnoreProperties(ignoreUnknown=true)
public interface DsDatastore
{
   // TODO: why does both this and Datastore exist?

   /**
    * Get the type of datastore this is.
    * All instances of an implementation need to return the same value.
    */
   public Datastore.Type getType();


   /**
    * Get a unique datastore id.
    * @return a valid datastore id.
    */
   public Long getId();


   /**
    * Set the unique datastore id.
    * @param id A valid datastore id.
    */
   public void setId(Long id);


   /**
    * Get the datastore's unique name.
    * @return name
    */
   public String getName();


   /**
    * Get the datastore status.
    * @return
    */
   public Datastore.Status getStatus();


   /**
    * Get the total size (bytes) of this datastore.
    * @return
    */
   public long getSize();


   /**
    * Get the amount of space used (bytes) on this datastore.
    * @return
    */
   public long getUsed();


   /**
    * Pass 'true' if this datastore is mounted at appliance boot.
    * @param mountAtBoot
    */
   public void setMountAtBoot(boolean mountAtBoot);


   /**
    * Return 'true' if this datastore is mounted at appliance boot.
    * @return
    */
   public boolean isMountAtBoot();


   /**
    * Check for a modifiable datastore.
    * A datastore that is not modifiable cannot be deleted, edited, or
    * taken offline.
    * @return
    */
   public boolean isModifiable();


   /**
    * Check for a writable datastore.
    * A datastore that is not writable cannot be used for storing conversion
    * job output.
    * @return
    */
   public boolean isWritable();


   /**
    * Returns true if the disk is at least as full as the
    * given value.
    *
    * @return  If the capacity of the disk is known, returns
    *          an integer in the range [0..100] representing
    *          the value of this expression:
    *             (used / size) * 100
    *
    *          If it is not known, returns -1;
    */
   public int getPctFree();

   /**
    * Get the shared path on this datastore's server.
    * @return the share
    */
   public String getShare();


   /**
    * Get the datastore server name.
    * @return the server
    */
   public String getServer();


   /**
    * Get the port used for accessing the datastore server.
    * @return
    */
   public int getPort();


   /**
    * Get the user domain for accessing this datastore.
    * @return
    */
   public String getDomain();


   /**
    * Get the user name for accessing this datastore.
    * @return the username
    */
   public String getUsername();


   /**
    * Set the user name for accessing this datastore.
    * @param username the username to set
    */
   public void setUsername(String username);


   /**
    * Get the password for accessing this datastore.
    * @return the password
    */
   public String getPassword();


   /**
    * Set the password for accessing this datastore.
    * @param password the password to set
    */
   public void setPassword(String password);


   /**
    * Create a path from the given path components using the path
    * separator application to this implementation
    */
   public String buildPath(String... components);


   /**
    * Create the specified directory tree in the root of the datastore,
    * skipping anything that's already present, and return the final path
    * when done
    */
   public String createDirsIfNotExists(String... directories)
      throws IOException;


   /**
    * Copy an upload file to the specified destination file path.
    * TODO: Use a more generic progress interface.
    */
   public void copy(MultipartFile source, String destination, ProgressListener pl)
      throws IOException;


   /**
    * Open an input stream to a file on the datastore.
    */
   public InputStream openStream(String filePath)
      throws IOException;


   /**
    * Get the fileshare that is linked to this datastore, creating it if
    * necessary.
    */
   public FileShare findOrCreateFileshare(FileShareDao fsDao);


   /**
    * Create a fileshare linked to this data source.
    * @see #findOrCreateFileshare(FileShareDao)
    *
    * @return A new fileshare. This is not saved; it is up to the caller to
    * verify it is unique, and then save it.
    */
   public FileShare createFileShare();
}
