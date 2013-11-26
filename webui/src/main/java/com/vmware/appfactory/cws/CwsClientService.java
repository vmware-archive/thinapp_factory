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

package com.vmware.appfactory.cws;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.google.common.collect.ImmutableMap;
import com.vmware.appfactory.build.dto.ProjectImportResponse;
import com.vmware.appfactory.common.base.AbstractRestClient;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.file.FileData;
import com.vmware.thinapp.common.converter.dto.ConversionJobStatus;
import com.vmware.thinapp.common.converter.dto.ConversionRequest;
import com.vmware.thinapp.common.converter.dto.ConversionResponse;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.datastore.dto.CreateRequest;
import com.vmware.thinapp.common.util.AfUtil;


/**
 * This is the main interface to CWS.
 * Instead of using CWS API calls at random locations throughout AppFactory,
 * they are all wrapped in methods inside this class.
 *
 * @author levans
 */
@Service("cwsClient")
public class CwsClientService
   extends AbstractRestClient
{
   /**
    * Create a new client.
    */
   public CwsClientService()
   {
      super(ConfigRegistryConstants.CWS_SERVICE_URL);
   }


   /**
    * Submit an application for conversion.
    *
    * If all goes will, this will return an CwsRequestResponse instance
    * that tells us what CWS did with the request. Otherwise, an exception
    * is thrown.
    *
    * NOTE: The CwsRequestResponse instance returned might indicate a CWS
    * error. This does not result in an exception being thrown here.
    *
    * @param request New job request
    * @return Job response
    *
    * @throws CwsException
    */
   public ConversionResponse submitJobRequest(ConversionRequest request)
      throws CwsException
   {
      debugLogJson("Submitting conversion request:", request);

      try {
         return _rest.postForObject(
               baseConversionsUrl() + "/conversions",
               request,
               ConversionResponse.class);
      }
      catch(RestClientException ex) {
         _log.error(ex.getMessage(), ex);
         throw new CwsException(ex);
      }
   }


   /**
    * Cancel a conversion.
    * There is no data to POST, and no reply expected.
    *
    * @param jobId Job to cancel
    *
    * @throws CwsException
    */
   public void cancelConversion(Long jobId)
      throws CwsException
   {
      try {
         _rest.postForLocation(
            baseConversionsUrl() + "/conversions/{jobId}/cancel",
            EMPTY_REQUEST,
             jobId);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get the status of a conversion job.
    * If the ID is invalid, an exception is thrown.
    *
    * TODO: Request multiple statuses at once from CWS.
    *
    * @param jobId Job to query
    * @return Job status
    *
    * @throws CwsException
    * @throws AfNotFoundException
    */
   public ConversionJobStatus getConversionStatus(Long jobId)
      throws AfNotFoundException, CwsException
   {
      try {
         ConversionJobStatus status = _rest.getForObject(
            baseConversionsUrl() + "/conversions/{jobId}",
            ConversionJobStatus.class,
            jobId);
         return status;
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch (RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get the status of a conversion project.
    * If the ID is invalid, an exception is thrown.
    *
    * TODO: Request multiple statuses at once from CWS.
    *
    * @param projectId Project to query
    * @return Project status
    *
    * @throws CwsException
    * @throws AfNotFoundException
    */
   public Project getProjectStatus(Long projectId)
      throws AfNotFoundException, CwsException
   {
      try {
         return _rest.getForObject(
            baseCWSUrl() + "/projects/{projectId}",
            Project.class,
            projectId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Delete a project.
    *
    * @param projectId
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public void deleteProject(Long projectId)
      throws AfNotFoundException, CwsException
   {
      try {
         _rest.delete(
                 baseCWSUrl() + "/projects/{projectId}",
                 projectId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Import projects from a datastore.
    *
    *
    * @param datastoreId a datastore id.
    * @param runtimeId ThinApp runtime to create the project with
    * @return an array of imported project ids and a detailed error map.
    */
   public ProjectImportResponse importProjects(Long datastoreId, long runtimeId)
   {
      final CreateRequest request = new CreateRequest();
      request.setDatastore(datastoreId);
      request.setRuntimeId(runtimeId);

      final ProjectImportResponse response = _rest.postForObject(baseCWSUrl()
            + "/projects/import",
            request, ProjectImportResponse.class);

      return response;
   }


   /**
    * Rebuild a project.
    *
    * @param projectId
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public void rebuildProject(Long projectId)
      throws AfNotFoundException, CwsException
   {
      try {
         _rest.postForLocation(
            baseCWSUrl() + "/projects/{projectId}/rebuild",
            EMPTY_REQUEST,
            projectId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get a project's 'packageIni' settings.
    * @param projectId
    * @return The INI data for the project
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public CwsSettingsIni getProjectPackageIni(Long projectId)
      throws AfNotFoundException, CwsException
   {
      try {
         return _rest.getForObject(
            baseCWSUrl() + "/projects/{projectId}/packageini",
            CwsSettingsIni.class,
            projectId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Set a project's 'packageIni' data.
    * This is a bulk update operation: the entire packageIni data is
    * transferred and applied in one shot.
    *
    * @param projectId
    * @param newIni
    * @return true if the update request actually made a change, false otherwise
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public boolean updateProjectPackageIni(Long projectId, CwsSettingsIni newIni)
      throws AfNotFoundException, CwsException
   {
      debugLogJson("New INI data: ", newIni);

      try {
         /* Use 'exchange' since we PUT and expect a response */
         @SuppressWarnings("rawtypes")
         ResponseEntity<Map> response = _rest.exchange(
            baseCWSUrl() + "/projects/{projectId}/packageini",
            HttpMethod.PUT,
            new HttpEntity<CwsSettingsIni>(newIni),
            Map.class,
            projectId);

         @SuppressWarnings("unchecked")
         Map<String,Boolean> states = response.getBody();
         return states.get("modified").booleanValue();
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get a project's root directory settings.
    * @param projectId
    * @return The project directory settings
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public CwsSettingsDir getProjectDirectoryRoot(Long projectId)
      throws AfNotFoundException, CwsException
   {
      try {
         return _rest.getForObject(
            baseCWSUrl() + "/projects/{projectId}/directory",
            CwsSettingsDir.class,
            projectId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get a directory from a CWS project.
    *
    * @param projectId
    * @param dirId
    * @return The request directory
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public CwsSettingsDir getProjectDirectory(Long projectId, Long dirId)
      throws AfNotFoundException, CwsException
   {
      try {
         return _rest.getForObject(
               baseCWSUrl() + "/projects/{projectId}/directory/{dirId}",
               CwsSettingsDir.class,
               projectId, dirId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get project's root registry key.
    * @param projectId
    * @return The project registry root
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public CwsSettingsRegKey getProjectRegistryRoot(Long projectId)
      throws AfNotFoundException, CwsException
   {
      try {
         return _rest.getForObject(
            baseCWSUrl() + "/projects/{projectId}/registry",
            CwsSettingsRegKey.class,
            projectId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get project's registry key.
    * @param projectId
    * @param registryId
    * @return The specified project registry key
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public CwsSettingsRegKey getProjectRegistryKey(Long projectId, Long registryId)
      throws AfNotFoundException, CwsException
   {
      try {
         return _rest.getForObject(
            baseCWSUrl() + "/projects/{projectId}/registry/{registryId}",
            CwsSettingsRegKey.class,
            projectId,
            registryId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Create a new registry key for a CWS project's settings.
    * The location of the new key can be inferred from it's "path" property.
    *
    * @param projectId Project to change.
    * @param request
    * @return The URL to the registry resource
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public String createProjectRegistryKey(
         Long projectId,
         CwsRegistryRequest request)
      throws AfNotFoundException, CwsException
   {
      try {
         HashMap<?,?> map = _rest.postForObject(
            baseCWSUrl() + "/projects/{projectId}/registry/new",
            request,
            HashMap.class,
            projectId);

         return (String) map.get("url");
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Update an existing registry key for a CWS project's settings.
    *
    * @param projectId
    * @param registryId
    * @param newRegKey
    * @return true if the update request actually made a change, false otherwise
    *
    * @throws AfNotFoundException
    * @throws CwsException
    * @throws InvalidDataException
    */
   public boolean updateProjectRegistryKey(
         Long projectId,
         Long registryId,
         CwsSettingsRegKey newRegKey)
      throws AfNotFoundException, CwsException, InvalidDataException
   {
      /* Check the input */
      newRegKey.validate(_log);

      try {
         /* Use 'exchange' since we PUT and expect a response */
         @SuppressWarnings("rawtypes")
         ResponseEntity<Map> response = _rest.exchange(
            baseCWSUrl() + "/projects/{projectId}/registry/{registryId}",
            HttpMethod.PUT,
            new HttpEntity<CwsSettingsRegKey>(newRegKey),
            Map.class,
            projectId,
            registryId);

         @SuppressWarnings("unchecked")
         Map<String,Boolean> states = response.getBody();
         return states.get("modified").booleanValue();
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Update an existing directory for a CWS project.
    *
    * @param projectId
    * @param directoryId
    * @param dir
    * @return true if the update request actually made a change, false otherwise
    *
    * @throws AfNotFoundException
    * @throws CwsException
    * @throws InvalidDataException
    */
   public boolean updateProjectDirectory(
         Long projectId,
         Long directoryId,
         CwsSettingsDir dir)
      throws AfNotFoundException, CwsException, InvalidDataException
   {
      try {
         /* Use 'exchange' since we PUT and expect a response */
         @SuppressWarnings("rawtypes")
         ResponseEntity<Map> response = _rest.exchange(
            baseCWSUrl() + "/projects/{projectId}/directory/{directoryId}",
            HttpMethod.PUT,
            new HttpEntity<CwsSettingsDir>(dir),
            Map.class,
            projectId,
            directoryId);

         @SuppressWarnings("unchecked")
         Map<String,Boolean> states = response.getBody();
         return states.get("modified").booleanValue();
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Delete a registry key for a CWS project's settings.
    *
    * @param projectId
    * @param registryId
    *
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public void deleteProjectRegistryKey(
         Long projectId,
         Long registryId)
      throws AfNotFoundException, CwsException
   {
      try {
         _rest.delete(
            baseCWSUrl() + "/projects/{projectId}/registry/{registryId}",
            projectId,
            registryId);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException(ex);
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Force CWS to refresh its internal state of a project's settings.
    * This is necessary, for example, if a user edits the project directory
    * by hand.
    *
    * @param projectId
    * @throws AfNotFoundException
    * @throws CwsException
    */
   public void refreshProjectSettings(Long projectId)
      throws AfNotFoundException, CwsException
   {
      try {
         _rest.postForLocation(
            baseCWSUrl() + "/projects/{projectId}/refresh",
            EMPTY_REQUEST,
            projectId);
      }
      catch (HttpStatusCodeException ex) {
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Reboot the appliance.
    * @throws CwsException
    */
   public void reboot()
      throws CwsException
   {
      try {
         _rest.postForLocation(
            baseCWSUrl() + "/config/reboot",
            EMPTY_REQUEST);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get a FileData object with the input stream and HTTP headers of the ZIP
    * file for the appliance system logs.
    *
    * @return FileData object for the appliance system logs.
    * @throws CwsException
    */
   public FileData getLogs() throws CwsException
   {
      return getLogsFromUrl(baseCWSUrl() + "/config/logs");
   }


   /**
    * Get a FileData object with the input stream and HTTP headers of the ZIP
    * file for the project with the given ID.
    *
    * @param id ID of project
    * @return object for the project logs.
    * @throws CwsException
    */
   public FileData getProjectLogs(Long id) throws CwsException
   {
      return getLogsFromUrl(String.format("%s/%s/%s/logs",
            baseCWSUrl(),
            "projects",
            id));
   }


   /**
    * Helper method for streaming HTTP file data.  Opens a connection to the
    * given URL and returns a FileData object for that URL.
    *
    * @param urlString the URL of the log file to connect to
    * @return a FileData object for the given URL
    * @throws CwsException
    */
   private FileData getLogsFromUrl(String urlString)
      throws CwsException
   {
      URL url = null;
      try {
         url = new URL(urlString);
      }
      catch (MalformedURLException ex) {
         throw new CwsException(ex);
      }

      try {
         URLConnection connection = url.openConnection();
         connection.connect();

         String contentDisp = connection.getHeaderField(AfUtil.CONTENT_DISPOSITION);
         String contentType = connection.getHeaderField(AfUtil.CONTENT_TYPE);
         String contentLen = connection.getHeaderField(AfUtil.CONTENT_LENGTH);
         InputStream is = connection.getInputStream();
         _log.debug("Streaming log file with length={}", contentLen);

         return new FileData(contentDisp, contentType, Integer.parseInt(contentLen), is);
      }
      catch (IOException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get various info about the appliance.
    * @return A map of string -> server info.  CWS defines what will be returned.
    * @throws CwsException
    */
   public CwsServerInfo getServerInfo()
      throws CwsException
   {
      try {
         CwsServerInfo info = _rest.getForObject(
            baseCWSUrl() + "/config/info",
            CwsServerInfo.class);
         return info;
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get the status of host/guest time synchronization.
    * @return True if time sync is set, else False
    * @throws CwsException
    */
   public Boolean getTimeSync()
      throws CwsException
   {
      try {
         return _rest.getForObject(
            baseCWSUrl() + "/config/timesync",
            Boolean.class);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Set the status of host/guest time synchronization.
    * @param state
    * @throws CwsException
    */
   public void setTimeSync(boolean state)
      throws CwsException
   {
      try {
         _rest.postForLocation(
            baseCWSUrl() + "/config/timesync/{set}",
            null,
            (state ? "enable" : "disable"));
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get the license expiration date from CWS.
    * CWS should return an expiration date in yyyy-mm-dd ISO format.
    *
    * @return License expiration date, or null if the server doesn't have one.
    * @throws CwsException if any error raised while invoking CWS '/config/expire' endpoint.
    */
   public Date getLicenseExpirationDate() throws CwsException {
      try {
         @SuppressWarnings("unchecked")
         Map<String, String> resMap = _rest.getForObject(baseCWSUrl() + "/config/expire", Map.class);
         String strDate = resMap.get("date");
         _log.debug("Received expire date: " + strDate);
         return AfUtil.parseIsoDate(strDate);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return null;
         }
         throw new CwsException(ex);
      }
      catch(RestClientException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Construct the full CWS API URL from the configured base.
    *
    * @param path
    * @return
    */
   private String baseCWSUrl()
   {
      return _config.getString(ConfigRegistryConstants.CWS_SERVICE_URL);
   }


   /**
    * Construct the full Conversions API URL from the configured base.
    *
    * @param path
    * @return
    */
   private String baseConversionsUrl()
   {
      return _config.getString(ConfigRegistryConstants.CWS_CONVERSIONS_URL);
   }


   /**
    * Update the ThinApp runtime to a different version.
    *
    * @param projectId
    * @param thinappRuntimeId
    */
   public void updateThinAppRuntime(Long projectId, Long thinappRuntimeId) {
      Map<String, Object> message = ImmutableMap.<String, Object>of("runtimeId", thinappRuntimeId);
      _rest.put(baseCWSUrl() + "/projects/{projectId}", message, projectId);
   }


   /**
    * Update the state of an existing project.
    *
    * @param projectId a project id
    * @param state a valid project state
    */
   public void updateProjectState(Long projectId, String state)
   {
      Map<String, Object> message = ImmutableMap.<String, Object>of("state", state);
      _rest.put(baseCWSUrl() + "/projects/{projectId}", message, projectId);
   }
}
