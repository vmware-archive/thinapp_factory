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

package com.vmware.appfactory.common.dao;

import java.util.List;

import com.vmware.appfactory.common.base.AbstractRecord;


/**
 * Meta interface used to describe all the basic data operations for any
 * record class.
 * @param <T> The record type
 */
public interface AfDao<T extends AbstractRecord>
{
   /**
    * Returns a timestamp of when the table was most recently modified.
    *
    * @return  a long timestamp if there was a recent modification, 0 if there
    *    are only new entries in the table, and -1 if the table is empty.
    */
   public long lastModified();

   /**
    * Count all instances.
    * @return
    */
   public long countAll();

   /**
    * Find record by its ID.
    * @param id
    * @return
    */
	public T find(Long id);

	/**
	 * Find multiple records all at once.
	 * @param ids IDs of records to get
	 * @param includeMisses If true, result contains a null for an ID that was
	 * not found. If false, result only contains found IDs.
	 * @return List of records matching the requested IDs.
	 */
	public List<T> findAll(List<Long> ids, boolean includeMisses);

	/**
	 * Get all instances.
	 * @return
	 */
	public List<T> findAll();

	/**
	 * Create a new record.
	 * @param record
	 * @return
	 */
	public Long create(T record);

	/**
	 * Create a new record if necessary, else update an existing one.
	 * @param record
	 */
	public void createOrUpdate(T record);

	/**
	 * Update an existing record.
	 * @param record
	 */
	public void update(T record);

	/**
	 * Delete an exising record.
	 * @param record
	 */
	public void delete(T record);

	/**
	 * Delete multiple existing records.
	 * @param records
	 */
	public void delete(List<T> records);

	/**
	 * Delete all records.
	 */
	public void deleteAll();

	/**
	 * Flush any pending database operations.
	 */
	public void flush();
}
