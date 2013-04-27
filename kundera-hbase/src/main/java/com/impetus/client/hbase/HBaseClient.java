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
package com.impetus.client.hbase;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

import com.impetus.client.hbase.admin.DataHandler;
import com.impetus.client.hbase.admin.HBaseDataHandler;
import com.impetus.client.hbase.admin.HBaseDataHandler.HBaseDataWrapper;
import com.impetus.client.hbase.query.HBaseQuery;
import com.impetus.client.hbase.utils.HBaseUtils;
import com.impetus.kundera.KunderaException;
import com.impetus.kundera.PersistenceProperties;
import com.impetus.kundera.client.Client;
import com.impetus.kundera.client.ClientBase;
import com.impetus.kundera.client.ClientPropertiesSetter;
import com.impetus.kundera.db.RelationHolder;
import com.impetus.kundera.generator.TableGenerator;
import com.impetus.kundera.graph.Node;
import com.impetus.kundera.index.IndexManager;
import com.impetus.kundera.lifecycle.states.RemovedState;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.MetadataUtils;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.KunderaMetadata;
import com.impetus.kundera.metadata.model.MetamodelImpl;
import com.impetus.kundera.metadata.model.PersistenceUnitMetadata;
import com.impetus.kundera.metadata.model.TableGeneratorDiscriptor;
import com.impetus.kundera.metadata.model.attributes.AbstractAttribute;
import com.impetus.kundera.persistence.EntityReader;
import com.impetus.kundera.persistence.api.Batcher;
import com.impetus.kundera.persistence.context.jointable.JoinTableData;
import com.impetus.kundera.property.PropertyAccessorHelper;

/**
 * HBase client.
 * 
 * @author impetus
 */
public class HBaseClient extends ClientBase implements Client<HBaseQuery>, Batcher, ClientPropertiesSetter,
        TableGenerator
{
    /** the log used by this class. */
    private static Log log = LogFactory.getLog(HBaseClient.class);

    /** The handler. */
    private DataHandler handler;

    /** The reader. */
    private EntityReader reader;

    private List<Node> nodes = new ArrayList<Node>();

    private int batchSize;

    private Map<String, Object> puProperties;

    /**
     * Instantiates a new h base client.
     * 
     * @param indexManager
     *            the index manager
     * @param conf
     *            the conf
     * @param hTablePool
     *            the h table pool
     * @param reader
     *            the reader
     * @param persistenceUnit
     *            the persistence unit
     * @param puProperties
     */
    public HBaseClient(IndexManager indexManager, HBaseConfiguration conf, HTablePool hTablePool, EntityReader reader,
            String persistenceUnit, Map<String, Object> puProperties)
    {
        this.indexManager = indexManager;
        this.handler = new HBaseDataHandler(conf, hTablePool);
        this.reader = reader;
        this.persistenceUnit = persistenceUnit;
        this.puProperties = puProperties;

        getBatchSize(persistenceUnit, this.puProperties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#find(java.lang.Class,
     * java.lang.Object, java.util.List)
     */
    @Override
    public Object find(Class entityClass, Object rowId)
    {
        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(entityClass);
        List<String> relationNames = entityMetadata.getRelationNames();
        // columnFamily has a different meaning for HBase, so it won't be used
        // here
        String tableName = entityMetadata.getTableName();
        Object enhancedEntity = null;
        List results = null;
        try
        {
            if (rowId == null)
            {
                return null;
            }
            results = handler
                    .readData(tableName, entityMetadata.getEntityClazz(), entityMetadata, rowId, relationNames);
            if (results != null)
            {
                enhancedEntity = results.get(0);
            }
        }
        catch (IOException e)
        {
            log.error("Error during find by id, Caused by:" + e);
            throw new KunderaException(e);
        }
        return enhancedEntity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#findAll(java.lang.Class,
     * java.lang.Object[])
     */
    @Override
    public <E> List<E> findAll(Class<E> entityClass, String[] columnsToSelect, Object... rowIds)
    {
        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(entityClass);
        List<E> entities = new ArrayList<E>();
        if (rowIds == null)
        {
            return null;
        }
        // for (Object rowKey : rowIds)
        // {
        E e = null;
        /*
         * try { if (rowKey != null) {
         */
        List results;
        try
        {
            results = handler.readAll(entityMetadata.getTableName(), entityMetadata.getEntityClazz(), entityMetadata,
                    Arrays.asList(rowIds), entityMetadata.getRelationNames());
        }
        catch (IOException e1)
        {
            log.error("Error during find All, Caused by:", e1);
            throw new KunderaException(e1);
        }
        /*
         * if (results != null) { // e = (E) results.get(0);
         * entities.addAll(results); } //
         *//* } */
        /*
         * } catch (IOException ioex) {
         * log.error("Error during find All, Caused by:" + ioex); throw new
         * KunderaException(ioex); }
         */

        // }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#find(java.lang.Class,
     * java.util.Map)
     */
    @Override
    public <E> List<E> find(Class<E> entityClass, Map<String, String> col)
    {
        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(getPersistenceUnit(), entityClass);
        List<E> entities = new ArrayList<E>();
        Map<String, Field> columnFamilyNameToFieldMap = MetadataUtils.createSuperColumnsFieldMap(entityMetadata);
        for (String columnFamilyName : col.keySet())
        {
            String entityId = col.get(columnFamilyName);
            if (entityId != null)
            {
                E e = null;
                try
                {
                    List results = handler.readData(entityMetadata.getTableName(), entityMetadata.getEntityClazz(),
                            entityMetadata, entityId, null);
                    if (results != null)
                    {
                        e = (E) results.get(0);
                    }
                }
                catch (IOException ioex)
                {
                    log.error("Error during find for embedded entities, Caused by:" + ioex);

                    throw new KunderaException(ioex);
                }

                Field columnFamilyField = columnFamilyNameToFieldMap.get(columnFamilyName.substring(0,
                        columnFamilyName.indexOf("|")));
                Object columnFamilyValue = PropertyAccessorHelper.getObject(e, columnFamilyField);
                if (Collection.class.isAssignableFrom(columnFamilyField.getType()))
                {
                    entities.addAll((Collection) columnFamilyValue);
                }
                else
                {
                    entities.add((E) columnFamilyValue);
                }
            }
        }
        return entities;
    }

    /**
     * Method to find entities using JPQL(converted into FilterList.)
     * 
     * @param <E>
     *            parameterized entity class.
     * @param entityClass
     *            entity class.
     * @param metadata
     *            entity metadata.
     * @return list of entities.
     */
    public <E> List<E> findByQuery(Class<E> entityClass, EntityMetadata metadata, String... columns)
    {
        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(entityClass);
        List<String> relationNames = entityMetadata.getRelationNames();
        // columnFamily has a different meaning for HBase, so it won't be used
        // here
        String tableName = entityMetadata.getTableName();
        Object enhancedEntity = null;
        List results = null;

        if (isFindKeyOnly(metadata, columns))
        {
            columns = null;
            setFilter(new KeyOnlyFilter());
        }

        try
        {
            results = handler.readData(tableName, entityMetadata.getEntityClazz(), entityMetadata, null, relationNames,
                    columns);
        }
        catch (IOException ioex)
        {
            log.error("Error during find All, Caused by:" + ioex);
            throw new KunderaException(ioex);
        }
        return results != null ? results : new ArrayList();

    }

    /**
     * Handles find by range query for given start and end row key range values.
     * 
     * @param <E>
     *            parameterized entity class.
     * @param entityClass
     *            entity class.
     * @param metadata
     *            entity metadata
     * @param startRow
     *            start row.
     * @param endRow
     *            end row.
     * @return collection holding results.
     */
    public <E> List<E> findByRange(Class<E> entityClass, EntityMetadata metadata, byte[] startRow, byte[] endRow,
            String[] columns)
    {
        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(entityClass);
        // List<String> relationNames = entityMetadata.getRelationNames();
        // columnFamily has a different meaning for HBase, so it won't be used
        // here
        String tableName = entityMetadata.getTableName();
        // Object enhancedEntity = null;
        List results = new ArrayList();

        if (isFindKeyOnly(metadata, columns))
        {
            columns = null;
            setFilter(new KeyOnlyFilter());
        }

        try
        {
            results = handler.readDataByRange(tableName, entityClass, metadata, startRow, endRow, columns);
        }
        catch (IOException ioex)
        {
            log.error("Error during find All, Caused by:" + ioex);
            throw new KunderaException(ioex);
        }
        return results;

    }

    /**
     * @param metadata
     * @param columns
     * @return
     */
    private boolean isFindKeyOnly(EntityMetadata metadata, String[] columns)
    {
        int noOFColumnsToFind = 0;
        boolean findIdOnly = false;
        if (columns != null)
        {
            for (String column : columns)
            {
                if (column != null)
                {
                    if (column.equals(((AbstractAttribute) metadata.getIdAttribute()).getJPAColumnName()))
                    {
                        noOFColumnsToFind++;
                        findIdOnly = true;
                    }
                    else
                    {
                        noOFColumnsToFind++;
                        findIdOnly = false;
                    }
                }
            }
        }
        if (noOFColumnsToFind == 1 && findIdOnly)
        {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#close()
     */
    @Override
    public void close()
    {
        handler.shutdown();
        puProperties = null;
    }

    /**
     * Setter for filter.
     * 
     * @param filter
     *            filter.
     */
    public void setFilter(Filter filter)
    {
        ((HBaseDataHandler) handler).setFilter(filter);
    }

    /**
     * On persist.
     * 
     * @param entityMetadata
     *            the entity metadata
     * @param entity
     *            the entity
     * @param id
     *            the id
     * @param relations
     *            the relations
     */
    @Override
    protected void onPersist(EntityMetadata entityMetadata, Object entity, Object id, List<RelationHolder> relations)
    {
        String tableName = entityMetadata.getTableName();

        try
        {
            // Write data to HBase
            handler.writeData(tableName, entityMetadata, entity, id, relations);
        }
        catch (IOException e)
        {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void persistJoinTable(JoinTableData joinTableData)
    {
        String joinTableName = joinTableData.getJoinTableName();
        String joinColumnName = joinTableData.getJoinColumnName();
        String invJoinColumnName = joinTableData.getInverseJoinColumnName();
        Map<Object, Set<Object>> joinTableRecords = joinTableData.getJoinTableRecords();

        for (Object key : joinTableRecords.keySet())
        {
            Set<Object> values = joinTableRecords.get(key);
            Object joinColumnValue = key;

            Map<String, Object> columns = new HashMap<String, Object>();
            for (Object childValue : values)
            {
                Object invJoinColumnValue = childValue;
                columns.put(invJoinColumnName + "_" + invJoinColumnValue, invJoinColumnValue);
            }

            if (columns != null && !columns.isEmpty())
            {
                try
                {
                    handler.createTableIfDoesNotExist(joinTableName, joinTableName);
                    handler.writeJoinTableData(joinTableName, joinColumnValue, columns);
                }
                catch (IOException e)
                {
                    throw new PersistenceException(e);
                }
            }

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.kundera.client.Client#getForeignKeysFromJoinTable(java.lang
     * .String, java.lang.String, java.lang.String,
     * com.impetus.kundera.metadata.model.EntityMetadata,
     * com.impetus.kundera.persistence.handler.impl.EntitySaveGraph)
     */
    @Override
    public <E> List<E> getColumnsById(String schemaName, String joinTableName, String joinColumnName,
            String inverseJoinColumnName, Object parentId, Class columnJavaType)
    {
        return handler.getForeignKeysFromJoinTable(joinTableName, parentId, inverseJoinColumnName);

    }

    public void deleteByColumn(String schemaName, String tableName, String columnName, Object columnValue)
    {
        try
        {
            handler.deleteRow(columnValue, tableName);

        }
        catch (IOException e)
        {
            log.error("Error during delete by key. Caused by:" + e);
            throw new PersistenceException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#delete(java.lang.Object,
     * java.lang.Object)
     */
    @Override
    public void delete(Object entity, Object pKey)
    {
        EntityMetadata metadata = KunderaMetadataManager.getEntityMetadata(entity.getClass());
        deleteByColumn(metadata.getSchema(), metadata.getTableName(),
                ((AbstractAttribute) metadata.getIdAttribute()).getJPAColumnName(), pKey);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#findByRelation(java.lang.String,
     * java.lang.Object, java.lang.Class)
     */
    @Override
    public List<Object> findByRelation(String colName, Object colValue, Class entityClazz)
    {
        CompareOp operator = HBaseUtils.getOperator("=", false);

        EntityMetadata m = KunderaMetadataManager.getEntityMetadata(entityClazz);

        String columnFamilyName = m.getTableName();

        byte[] valueInBytes = HBaseUtils.getBytes(colValue);
        SingleColumnValueFilter f = null;
        f = new SingleColumnValueFilter(Bytes.toBytes(columnFamilyName), Bytes.toBytes(colName), operator, valueInBytes);

        // f.setFilterIfMissing(true);
        try
        {

            return ((HBaseDataHandler) handler)
                    .scanData(f, m.getTableName(), entityClazz, m, columnFamilyName, colName);
        }
        catch (IOException ioe)
        {
            log.error("Error during find By Relation, Caused by:", ioe);
            throw new KunderaException(ioe);
        }
        catch (InstantiationException ie)
        {
            log.error("Error during find By Relation, Caused by:", ie);
            throw new KunderaException(ie);
        }
        catch (IllegalAccessException iae)
        {
            log.error("Error during find By Relation, Caused by:", iae);
            throw new KunderaException(iae);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#getReader()
     */
    @Override
    public EntityReader getReader()
    {
        return reader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#getQueryImplementor()
     */
    @Override
    public Class<HBaseQuery> getQueryImplementor()
    {
        return HBaseQuery.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.client.Client#findIdsByColumn(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.Object, java.lang.Class)
     */
    @Override
    public Object[] findIdsByColumn(String schemaName, String tableName, String pKeyName, String columnName,
            Object columnValue, Class entityClazz)
    {
        CompareOp operator = HBaseUtils.getOperator("=", false);
        EntityMetadata m = KunderaMetadataManager.getEntityMetadata(entityClazz);

        byte[] valueInBytes = HBaseUtils.getBytes(columnValue);
        Filter f = new SingleColumnValueFilter(Bytes.toBytes(tableName), Bytes.toBytes(columnName), operator,
                valueInBytes);
        KeyOnlyFilter keyFilter = new KeyOnlyFilter();
        FilterList filterList = new FilterList(f, keyFilter);
        try
        {
            return handler.scanRowyKeys(filterList, tableName, tableName, columnName + "_" + columnValue, m
                    .getIdAttribute().getBindableJavaType());
        }
        catch (IOException e)
        {
            log.error("Error while executing findIdsByColumn(), Caused by: " + e);
            throw new KunderaException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.kundera.persistence.api.Batcher#addBatch(com.impetus.kundera
     * .graph.Node)
     */
    public void addBatch(Node node)
    {

        if (node != null)
        {
            nodes.add(node);
        }

        onBatchLimit();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.persistence.api.Batcher#getBatchSize()
     */
    @Override
    public int getBatchSize()
    {
        return batchSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.persistence.api.Batcher#clear()
     */
    @Override
    public void clear()
    {
        if (nodes != null)
        {
            nodes.clear();
            nodes = null;
            nodes = new ArrayList<Node>();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.persistence.api.Batcher#executeBatch()
     */
    @Override
    public int executeBatch()
    {

        Map<HTableInterface, List<HBaseDataWrapper>> data = new HashMap<HTableInterface, List<HBaseDataWrapper>>();

        try
        {
            for (Node node : nodes)
            {
                if (node.isDirty())
                {
                    HTableInterface hTable = null;
                    Object rowKey = node.getEntityId();
                    Object entity = node.getData();
                    if (node.isInState(RemovedState.class))
                    {
                        delete(entity, rowKey);
                    }
                    else
                    {
                        EntityMetadata metadata = KunderaMetadataManager.getEntityMetadata(node.getDataClass());

                        HBaseDataWrapper columnWrapper = new HBaseDataHandler.HBaseDataWrapper(rowKey,
                                new java.util.HashSet<Attribute>(), entity, metadata.getTableName());

                        MetamodelImpl metaModel = (MetamodelImpl) KunderaMetadata.INSTANCE.getApplicationMetadata()
                                .getMetamodel(metadata.getPersistenceUnit());

                        EntityType entityType = metaModel.entity(node.getDataClass());

                        List<HBaseDataWrapper> embeddableData = new ArrayList<HBaseDataHandler.HBaseDataWrapper>();
                        // List<HBaseDataWrapper> columnWrapper = new
                        // ArrayList<HBaseDataHandler.HBaseDataWrapper>();

                        hTable = ((HBaseDataHandler) handler).gethTable(metadata.getTableName());
                        ((HBaseDataHandler) handler).preparePersistentData(metadata.getTableName(), entity, rowKey,
                                metaModel, entityType.getAttributes(), columnWrapper, embeddableData);

                        List<HBaseDataWrapper> dataSet = null;
                        if (data.containsKey(hTable))
                        {
                            dataSet = data.get(metadata.getTableName());
                            addRecords(columnWrapper, embeddableData, dataSet);

                        }
                        else
                        {
                            dataSet = new ArrayList<HBaseDataHandler.HBaseDataWrapper>();
                            addRecords(columnWrapper, embeddableData, dataSet);
                            data.put(hTable, dataSet);
                        }
                    }
                }
            }

            if (!data.isEmpty())
            {
                ((HBaseDataHandler) handler).batch_insert(data);
            }
            return data.size();
        }
        catch (IOException e)
        {
            log.error("Error while executing batch insert/update, Caused by: " + e);
            throw new KunderaException(e);
        }

    }

    /**
     * Add records to data wrapper.
     * 
     * @param columnWrapper
     *            column wrapper
     * @param embeddableData
     *            embeddable data
     * @param dataSet
     *            data collection set
     */
    private void addRecords(HBaseDataWrapper columnWrapper, List<HBaseDataWrapper> embeddableData,
            List<HBaseDataWrapper> dataSet)
    {
        dataSet.add(columnWrapper);

        if (!embeddableData.isEmpty())
        {
            dataSet.addAll(embeddableData);
        }
    }

    /**
     * Check on batch limit.
     */
    private void onBatchLimit()
    {
        if (batchSize > 0 && batchSize == nodes.size())
        {
            executeBatch();
            nodes.clear();
        }
    }

    /**
     * @param persistenceUnit
     * @param puProperties
     */
    private void getBatchSize(String persistenceUnit, Map<String, Object> puProperties)
    {
        String batch_Size = puProperties != null ? (String) puProperties.get(PersistenceProperties.KUNDERA_BATCH_SIZE)
                : null;
        if (batch_Size != null)
        {
            batchSize = Integer.valueOf(batch_Size);
            if (batchSize == 0)
            {
                throw new IllegalArgumentException("kundera.batch.size property must be numeric and > 0");
            }
        }
        else
        {
            PersistenceUnitMetadata puMetadata = KunderaMetadataManager.getPersistenceUnitMetadata(persistenceUnit);
            batchSize = puMetadata.getBatchSize();
        }
    }

    @Override
    public void populateClientProperties(Client client, Map<String, Object> properties)
    {
        new HBaseClientProperties().populateClientProperties(client, properties);
    }

    @Override
    public Long generate(TableGeneratorDiscriptor discriptor)
    {
        try
        {
            HTableInterface hTable = ((HBaseDataHandler) handler).gethTable(discriptor.getTable());
            Long latestCount = hTable.incrementColumnValue(discriptor.getPkColumnValue().getBytes(), discriptor
                    .getTable().getBytes(), discriptor.getValueColumnName().getBytes(), 1);
            if (latestCount == 1)
            {
                return (long) discriptor.getInitialValue();
            }
            else
            {
                return (latestCount - 1) * discriptor.getAllocationSize();
            }
        }
        catch (IOException e)
        {
            log.error("Error while executing batch insert/update, Caused by: " + e);
            throw new KunderaException(e);
        }
    }
}
