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

package com.vmware.appfactory.datastore.simulator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.thinapp.common.datastore.dto.Datastore;


/**
 * Fake controller which pretends to be the Converter Web Service. This can be
 * used for UI testing; it does not actually "convert" anything.
 *
 * Note: Since we are simulating the back end, we must deal with the Datastore
 * DTO class only, not the enhanced webui-only DsDatastore class.
 */
@Controller
@RequestMapping(value = {"/ds", "/webui/ds"})
public class DsSimulator
   extends AbstractApiController
{
    // note: these constant values need to be longs, otherwise they'll
    // overflow when being computed
    private static final long TWENTY_GIGABYTES = 20l * 1024l * 1024l * 1024l;

   private static Long nextId = 1l;

   private final Map<Long,Datastore> _datastores = new TreeMap<Long,Datastore>();


   /**
    * Creates a new DsSimulator, which is an MVC controller that handles
    * the datastore API that CWS normally implements. This is useful for testing
    * and UI-only development.
    */
   public DsSimulator()
   {
      Datastore ds;

      /* Fake equivalent of appliance datastore */
      ds = new Datastore();
      ds.setId(nextId++);
      ds.setServer("0.0.0.0");
      ds.setShare("packages");
      ds.setName("internal");
      ds.setStatus(Datastore.Status.online);
      ds.setSize(TWENTY_GIGABYTES);
      _datastores.put(ds.getId(), ds);

      /* Fake equivalent of appliance datastore */
      ds = new Datastore();
      ds.setId(nextId++);
      ds.setName("system");
      ds.setStatus(Datastore.Status.online);
      ds.setSize(TWENTY_GIGABYTES);
      _datastores.put(ds.getId(), ds);

      /* I guess everyone should have their own, but for now... */
      ds = new Datastore();
      ds.setId(nextId++);
      ds.setName("levans-xp1");
      ds.setServer("taf.your.company.com");
      ds.setShare("/tafapps");
      ds.setUsername("user");
      ds.setPassword("password");
      ds.setStatus(Datastore.Status.online);
      ds.setSize(10000000);
      ds.setUsed(9000000);
      _datastores.put(ds.getId(), ds);
   }


   /**
    * Create a new datastore.
    *
    * If a datastore with the given name already exists, it will be updated.
    * Else, a new one is created.
    *
    * @param ds
    */
   @ResponseBody
   @RequestMapping(
         value = "/storage",
         method = RequestMethod.POST)
   public void createDatastore(
         @RequestBody Datastore ds,
         HttpServletResponse response)
      throws AfServerErrorException
   {
      ds.setId(nextId++);
      _datastores.put(ds.getId(), ds);

      try {
         /* Must set Location to a URI with ID as the final component */
         URI uri = DsUtil.generateDatastoreURI(ds.getId(), ""+ds.getId());
         response.setHeader("Location", uri.toString());
      }
      catch(URISyntaxException ex) {
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Update a datastore.
    * Not all datastore attributes can be changed: currently, only username,
    * password, and the 'mountAtBoot' flag. A datastore must also be offline
    * before it can be changed.
    *
    * @param id Datastore to modify
    * @param ds New datastore attributes.
    * @throws AfNotFoundException If the ID is not valid
    * @throws AfBadRequestException If the datastore is not offline.
    */
   @ResponseBody
   @RequestMapping(
         value = "/storage/{id}",
         method = RequestMethod.PUT)
   public void updateDatastore(
         Locale locale,
         @PathVariable Long id,
         @RequestBody Datastore ds)
      throws AfNotFoundException, AfBadRequestException
   {
      Datastore current = _datastores.get(id);
      if (current == null) {
         throw new AfNotFoundException("Invalid datastore id \"" + id + "\"");
      }

      /* Mimic CWS's restriction of allowing mods only to offline datastores */
      if (current.getStatus() != Datastore.Status.offline) {
         throw new AfBadRequestException(tr(locale, "M.STORAGE.CANT_UPDATE_IF_NOT_OFFLINE"));
      }

      /* There's only a few things we can change */
      current.setUsername(ds.getUsername());
      current.setPassword(ds.getPassword());
      current.setMountAtBoot(ds.isMountAtBoot());
   }


   /**
    * Get a list of datastores.
    * @return A JSON-formatted list of all datastores.
    */
   @ResponseBody
   @RequestMapping(
         value = "/storage",
         method = RequestMethod.GET)
   public Datastore[] listDatastores()
   {
      return _datastores.values().toArray(new Datastore[_datastores.values().size()]);
   }


   /**
    * Get one datastore.
    *
    * @param id
    * @return The datastore requested.
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/storage/{id}",
         method = RequestMethod.GET)
   public Datastore getDatastore(
         @PathVariable Long id)
      throws AfNotFoundException
   {
      if (!_datastores.containsKey(id)) {
         throw new AfNotFoundException("Invalid datastore id \"" + id + "\"");
      }

      return _datastores.get(id);
   }


   /**
    * Change the status of a datastore.
    *
    * The value for 'status' must match one of values of the enumeration
    * Datastore.Status, else an error (400) is returned.
    *
    * @param id
    * @param status
    * @param response
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/storage/{id}/{status}",
         method = RequestMethod.POST)
   public void setDatastoreStatus(
         @PathVariable Long id,
         @PathVariable Datastore.Status status,
         HttpServletResponse response)
      throws AfNotFoundException, AfBadRequestException
   {
      if (!_datastores.containsKey(id)) {
         throw new AfNotFoundException("Invalid datastore id \"" + id + "\"");
      }
      if (status == null) {
         throw new AfBadRequestException("Invalid or missing datastore status");
      }

      Datastore ds = _datastores.get(id);
      ds.setStatus(status);
   }


   /**
    * Delete one datastore.
    *
    * @param id
    * @param response
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/storage/{id}",
         method = RequestMethod.DELETE)
   public void deleteDatastore(
         @PathVariable Long id,
         HttpServletResponse response)
      throws AfNotFoundException, AfBadRequestException
   {
      Datastore ds = _datastores.get(id);

      if (ds == null) {
         throw new AfNotFoundException("Invalid datastore id \"" + id + "\"");
      }
      if (ds.getName().equals("internal")) {
         throw new AfBadRequestException("Cannot delete internal datastore");
      }

      _datastores.remove(id);
   }
}
