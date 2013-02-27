/*******************************************************************************
 * * Copyright 2012 Impetus Infotech.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 ******************************************************************************/
package com.impetus.kundera.index;

import java.util.Map;

import com.impetus.kundera.Constants;
import com.impetus.kundera.metadata.MetadataUtils;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.property.PropertyAccessException;
import com.impetus.kundera.property.PropertyAccessorHelper;

/**
 * Manager responsible to co-ordinate with an Indexer. It is bound with
 * EntityManager.
 * 
 * @author animesh.kumar
 */
public class IndexManager
{

    /** The indexer. */
    private Indexer indexer;

    /**
     * The Constructor.
     * 
     * @param indexer
     *            the indexer
     */
    @SuppressWarnings("deprecation")
    public IndexManager(Indexer indexer)
    {
        this.indexer = indexer;
    }

    /**
     * Removes an object from Index.
     * 
     * @param metadata
     *            the metadata
     * @param entity
     *            the entity
     * @param key
     *            the key
     */
    public final void remove(EntityMetadata metadata, Object entity, String key)
    {
        if (!MetadataUtils.useSecondryIndex(metadata.getPersistenceUnit()))
        {
            indexer.unindex(metadata, key);
        }

    }

    /**
     * Updates the index for an object.
     * 
     * @param metadata
     *            the metadata
     * @param entity
     *            the entity
     */
    public final void update(EntityMetadata metadata, Object entity, Object parentId, Class<?> clazz)
    {

        try
        {
            if (!MetadataUtils.useSecondryIndex(metadata.getPersistenceUnit()))
            {
                Object id = PropertyAccessorHelper.getId(entity, metadata);

                boolean documentExists = indexer.entityExistsInIndex(entity.getClass());
                if (documentExists)
                {
                    indexer.unindex(metadata, id);
                    indexer.flush();
                }
                indexer.index(metadata, entity, parentId.toString(), clazz);
            }
        }
        catch (PropertyAccessException e)
        {
            throw new IndexingException("Can't access ID from entity class " + metadata.getEntityClazz(), e);
        }
    }

    /**
     * Indexes an object.
     * 
     * @param metadata
     *            the metadata
     * @param entity
     *            the entity
     */
    public final void write(EntityMetadata metadata, Object entity)
    {
        if (!MetadataUtils.useSecondryIndex(metadata.getPersistenceUnit()))
        {
            indexer.index(metadata, entity);
        }
    }

    /**
     * Indexes an object.
     * 
     * @param metadata
     *            the metadata
     * @param entity
     *            the entity
     * @param parentId
     *            parent Id.
     * @param clazz
     *            class name
     */
    public final void write(EntityMetadata metadata, Object entity, String parentId, Class<?> clazz)
    {
        if (!MetadataUtils.useSecondryIndex(metadata.getPersistenceUnit()))
        {
            indexer.index(metadata, entity, parentId, clazz);
        }
    }

    /**
     * Searches on the index. Note: Query must be in Indexer's understandable
     * format
     * 
     * @param query
     *            the query
     * @return the list
     */
    public final Map<String, String> search(String query)
    {
        return search(query, Constants.INVALID, Constants.INVALID, false);
    }

    /**
     * Searches on the index. Note: Query must be in Indexer's understandable
     * format
     * 
     * @param query
     *            the query
     * @return the list
     */
    public final Map<String, String> fetchRelation(String query)
    {
        // TODO: need to return list.
        return search(query, Constants.INVALID, Constants.INVALID, true);
    }

    /**
     * Search.
     * 
     * @param query
     *            the query
     * @param count
     *            the count
     * @return the list
     */
    public final Map<String, String> search(String query, int count)
    {
        return search(query, Constants.INVALID, count, false);
    }

    /**
     * Search.
     * 
     * @param query
     *            the query
     * @param start
     *            the start
     * @param count
     *            the count
     * @return the list
     */
    public final Map<String, String> search(String query, int start, int count)
    {
        return indexer != null ? indexer.search(query, start, count, false):null;
    }

    /**
     * Search.
     * 
     * @param query
     *            the query
     * @param start
     *            the start
     * @param count
     *            the count
     * @param fetchRelation
     *            the fetch relation
     * @return the list
     */
    public final Map<String, String> search(String query, int start, int count, boolean fetchRelation)
    {
        return indexer != null ? indexer.search(query, start, count, fetchRelation):null;
    }

    /**
     * Flushes out the indexes, keeping RAM directory open.
     */
    public void flush() throws IndexingException
    {
        if (indexer != null)
        {
            indexer.flush();
        }
    }

    /**
     * Closes the transaction alongwith RAM directory.
     */
    public void close() throws IndexingException
    {
        if (indexer != null)
        {
            indexer.close();
        }
    }
}
