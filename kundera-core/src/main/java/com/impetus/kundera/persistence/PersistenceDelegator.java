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

package com.impetus.kundera.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.FlushModeType;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.impetus.kundera.KunderaException;
import com.impetus.kundera.PersistenceProperties;
import com.impetus.kundera.client.Client;
import com.impetus.kundera.client.ClientPropertiesSetter;
import com.impetus.kundera.client.ClientResolver;
import com.impetus.kundera.client.ClientResolverException;
import com.impetus.kundera.graph.Node;
import com.impetus.kundera.graph.ObjectGraph;
import com.impetus.kundera.graph.ObjectGraphBuilder;
import com.impetus.kundera.graph.ObjectGraphUtils;
import com.impetus.kundera.lifecycle.states.ManagedState;
import com.impetus.kundera.lifecycle.states.RemovedState;
import com.impetus.kundera.lifecycle.states.TransientState;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.MetadataUtils;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.PersistenceUnitMetadata;
import com.impetus.kundera.metadata.model.attributes.AbstractAttribute;
import com.impetus.kundera.persistence.api.Batcher;
import com.impetus.kundera.persistence.context.CacheBase;
import com.impetus.kundera.persistence.context.EventLog.EventType;
import com.impetus.kundera.persistence.context.FlushManager;
import com.impetus.kundera.persistence.context.MainCache;
import com.impetus.kundera.persistence.context.PersistenceCache;
import com.impetus.kundera.persistence.context.jointable.JoinTableData;
import com.impetus.kundera.persistence.context.jointable.JoinTableData.OPERATION;
import com.impetus.kundera.persistence.event.EntityEventDispatcher;
import com.impetus.kundera.query.QueryResolver;
import com.impetus.kundera.utils.ObjectUtils;

/**
 * The Class PersistenceDelegator.
 */
public final class PersistenceDelegator
{

    /** The Constant log. */
    private static final Log log = LogFactory.getLog(PersistenceDelegator.class);

    /** The closed. */
    private boolean closed;

    /** The client map. */
    private Map<String, Client> clientMap;

    /** The event dispatcher. */
    private EntityEventDispatcher eventDispatcher;

    // /** The is relation via join table. */
    // private boolean isRelationViaJoinTable;

    private FlushModeType flushMode = FlushModeType.AUTO;

    private ObjectGraphBuilder graphBuilder;

    private PersistenceValidator validator;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Whether a transaction is in progress
    private boolean isTransactionInProgress;

    private PersistenceCache persistenceCache;

    private FlushManager flushManager = new FlushManager();

    private boolean enableFlush;

    private Coordinator coordinator;

    private IdGenerator idGenerator;

    /**
     * Instantiates a new persistence delegator.
     * 
     * @param session
     *            the session
     * @param persistenceUnits
     *            the persistence units
     */
    PersistenceDelegator(final PersistenceCache pc)
    {
        this.validator = new PersistenceValidator();
        this.eventDispatcher = new EntityEventDispatcher();
        this.graphBuilder = new ObjectGraphBuilder(pc, this);
        this.persistenceCache = pc;
        this.idGenerator = new IdGenerator();
    }

    /***********************************************************************/
    /***************** CRUD Methods ****************************************/
    /***********************************************************************/

    /**
     * Writes an entity into Persistence cache. (Actual database write is done
     * while flushing)
     */
    public void persist(Object e)
    {
        if (e == null)
        {
            throw new IllegalArgumentException(
                    "Entity object is invalid, operation failed. Please check previous log message for details");
        }
        EntityMetadata metadata = getMetadata(e.getClass());

        idGenerator.setGeneratedIdIfApplicable(e, metadata, getClient(metadata));

        if (!validator.isValidEntityObject(e))
        {
            throw new IllegalArgumentException(
                    "Entity object is invalid, operation failed. Please check previous log message for details");
        }

        // Get entity metadata.

        // Invoke Pre-Persist Events.
        try
        {
            getEventDispatcher().fireEventListeners(metadata, e, PrePersist.class);
        }
        catch (Exception es)
        {
            throw new KunderaException(es);
        }
        // Create an object graph of the entity object.
        ObjectGraph graph = graphBuilder.getObjectGraph(e, new TransientState());

        // Call persist on each node in object graph.
        Node node = graph.getHeadNode();

        // Get write lock before writing object required for transaction.
        lock.writeLock().lock();

        node.persist();

        // build flush stack.
        flushManager.buildFlushStack(node, com.impetus.kundera.persistence.context.EventLog.EventType.INSERT);

        // Flushing data.
        flush();

        // Add node to persistence context after successful flush.
        getPersistenceCache().getMainCache().addHeadNode(node);

        // Unlocking object.
        lock.writeLock().unlock();
        graph.clear();
        graph = null;

        // Invoke Post Persist Events
        getEventDispatcher().fireEventListeners(metadata, e, PostPersist.class);

        if (log.isDebugEnabled())
        {
            log.debug("Data persisted successfully for entity : " + e.getClass());
        }
    }

    /**
     * Find object based on primary key either form persistence cache or from
     * database
     * 
     * @param entityClass
     * @param primaryKey
     * @return
     */
    public <E> E findById(Class<E> entityClass, Object primaryKey)
    {
        E e = find(entityClass, primaryKey);

        if (e == null)
            return null;

        // Set this returned entity as head node if applicable
        String nodeId = ObjectGraphUtils.getNodeId(primaryKey, entityClass);
        CacheBase mainCache = getPersistenceCache().getMainCache();
        Node node = mainCache.getNodeFromCache(nodeId);
        if (node != null && node.getParents() == null && !mainCache.getHeadNodes().contains(node))
        {
            mainCache.addHeadNode(node);
        }

        // Return a deep copy of this entity
        return (E) ObjectUtils.deepCopy(e);
    }

    /**
     * Finds an entity from persistence cache, if not there, fetches from
     * database. Nodes are added into persistence cache (if not already there)
     * as and when they are found from DB. While adding nodes to persistence
     * cache, a deep copy is added, so that found object doesn't refer to
     * managed entity in persistence cache.
     * 
     * @param entityClass
     *            Entity Class
     * @param primaryKey
     *            Primary Key
     * @return Entity Object for the given primary key
     * 
     */
    <E> E find(Class<E> entityClass, Object primaryKey)
    {
        if (primaryKey == null)
        {
            throw new IllegalArgumentException("PrimaryKey value must not be null for object you want to find.");
        }
        // Locking as it might read from persistence context.

        EntityMetadata entityMetadata = getMetadata(entityClass);

        String nodeId = ObjectGraphUtils.getNodeId(primaryKey, entityClass);

        // TODO all the scrap should go from here.
        MainCache mainCache = (MainCache) getPersistenceCache().getMainCache();
        Node node = mainCache.getNodeFromCache(nodeId);

        // if node is not in persistence cache or is dirty, fetch from database
        if (node == null || node.isDirty())
        {
            node = new Node(nodeId, entityClass, new ManagedState(), getPersistenceCache(), primaryKey);
            node.setClient(getClient(entityMetadata));
            // TODO ManagedState.java require serious attention.
            node.setPersistenceDelegator(this);

            lock.readLock().lock();
            node.find();
            lock.readLock().unlock();
        }
        Object nodeData = node.getData();
        if (nodeData == null)
        {
            return null;
        }
        else
        {
            return (E) ObjectUtils.deepCopy(nodeData);
        }

    }

    /**
     * Retrieves a {@link List} of Entities for given Primary Keys
     * 
     * @param entityClass
     *            Entity Class
     * @param primaryKeys
     *            Array of Primary Keys
     * @see {@link PersistenceDelegator#find(Class, Object)}
     * @return List of found entities
     */
    // TODO Is it possible to pass all primary keys directly to database client.
    public <E> List<E> find(Class<E> entityClass, Object... primaryKeys)
    {
        List<E> entities = new ArrayList<E>();
        if (primaryKeys == null)
        {
            return entities;
        }
        Set pKeys = new HashSet(Arrays.asList(primaryKeys));
        for (Object primaryKey : pKeys)
        {
            entities.add(find(entityClass, primaryKey));
        }
        return entities;
    }

    /**
     * Retrieves {@link List} of entities for a given {@link Map} of embedded
     * column values. Purpose of this method is to provide functionality of
     * search based on columns inside embedded objects.
     * 
     * @param entityClass
     *            Entity Class
     * @param embeddedColumnMap
     *            Embedded column map values
     * @return List of found entities.
     */
    public <E> List<E> find(Class<E> entityClass, Map<String, String> embeddedColumnMap)
    {
        EntityMetadata entityMetadata = getMetadata(entityClass);

        // TODO Why returning entities are not added into cache we should not
        // iterate here but client should i think.
        List<E> entities = new ArrayList<E>();
        entities = getClient(entityMetadata).find(entityClass, embeddedColumnMap);

        return entities;
    }

    /**
     * Finds {@link List} of child entities who contain given
     * <code>entityId</code> as <code>joinColumnName</code>
     * 
     * @param childClass
     *            Class of child entity
     * @param entityId
     *            Entity ID of parent entity
     * @param joinColumnName
     *            Join Column Name
     * @return
     */
    List<?> find(Class<?> childClass, Object entityId, String joinColumnName)
    {
        EntityMetadata childMetadata = getMetadata(childClass);
        List<?> entities = new ArrayList();
        Client childClient = getClient(childMetadata);

        entities = childClient.findByRelation(joinColumnName, entityId, childClass);

        if (entities == null)
            return null;

        return entities;
    }

    /**
     * Removes an entity object from persistence cache.
     * 
     */
    public void remove(Object e)
    {
        // Invoke Pre Remove Events

        // TODO Check for validity also as per JPA
        if (e == null)
        {
            throw new IllegalArgumentException("Entity to be removed must not be null.");
        }

        EntityMetadata metadata = getMetadata(e.getClass());
        getEventDispatcher().fireEventListeners(metadata, e, PreRemove.class);

        // Create an object graph of the entity object
        ObjectGraph graph = graphBuilder.getObjectGraph(e, new ManagedState());

        Node node = graph.getHeadNode();

        lock.writeLock().lock();

        // TODO : push into action queue, get original end-point from
        // persistenceContext first!

        // Action/ExecutionQueue/ActivityQueue :-> id, name, EndPoint, changed
        // state

        // Change state of node, after successful flush processing.
        node.remove();

        // build flush stack.

        flushManager.buildFlushStack(node, EventType.DELETE);

        // Flush node.
        flush();

        lock.writeLock().unlock();

        // clear out graph
        graph.clear();
        graph = null;

        getEventDispatcher().fireEventListeners(metadata, e, PostRemove.class);

        if (log.isDebugEnabled())
            log.debug("Data removed successfully for entity : " + e.getClass());
    }

    /**
     * Flushes Dirty objects in {@link PersistenceCache} to databases.
     * 
     */
    private void flush()
    {
        // Get flush stack from Flush Manager
        Deque<Node> fs = flushManager.getFlushStack();

        // Flush each node in flush stack from top to bottom unit it's empty

        if (log.isDebugEnabled())
        {
            log.debug("Flushing following flush stack to database(s) (showing stack objects from top to bottom):\n"
                    + fs);
        }
        if (fs != null)
        {
            boolean isBatch = false;
            while (!fs.isEmpty())
            {
                Node node = fs.pop();

                // Only nodes in Managed and Removed state are flushed, rest
                // are ignored
                if (node.isInState(ManagedState.class) || node.isInState(RemovedState.class))
                {
                    EntityMetadata metadata = getMetadata(node.getDataClass());
                    node.setClient(getClient(metadata));

                    // if batch size is defined.
                    if ((node.getClient() instanceof Batcher) && ((Batcher) (node.getClient())).getBatchSize() > 0)
                    {
                        isBatch = true;
                        ((Batcher) (node.getClient())).addBatch(node);
                    }
                    else if (isTransactionInProgress
                            && MetadataUtils.defaultTransactionSupported(metadata.getPersistenceUnit()))
                    {
                        onSynchronization(node, metadata);
                    }
                    else
                    {
                        node.flush();
                        
                    }
                }
            }

            if (!isBatch)
            {
                // TODO : This needs to be look for different
                // permutation/combination
                // Flush Join Table data into database
                flushJoinTableData();
                // performed,
            }
        }
    }

    public <E> E merge(E e)
    {
        if (log.isDebugEnabled())
            log.debug("Merging Entity : " + e);

        if (e == null)
        {
            throw new IllegalArgumentException("Entity to be merged must not be null.");
        }

        EntityMetadata m = getMetadata(e.getClass());

        // Fire PreUpdate events
        getEventDispatcher().fireEventListeners(m, e, PreUpdate.class);

        // Create an object graph of the entity object to be merged
        ObjectGraph graph = graphBuilder.getObjectGraph(e, new ManagedState());

        // Call merge on each node in object graph
        Node node = graph.getHeadNode();

        lock.writeLock().lock();
        // Change node's state after successful flush.

        // TODO : push into action queue, get original end-point from
        // persistenceContext first!

        // Action/ExecutionQueue/ActivityQueue :-> id, name, EndPoint, changed
        // state

        node.merge();

        // build flush stack.

        flushManager.buildFlushStack(node, EventType.UPDATE);

        flush();
        lock.writeLock().unlock();

        graph.clear();
        graph = null;

        // fire PreUpdate events
        getEventDispatcher().fireEventListeners(m, e, PostUpdate.class);

        return (E) node.getData();
    }

    /**
     * Remove the given entity from the persistence context, causing a managed
     * entity to become detached.
     */
    public void detach(Object entity)
    {
        Node node = getPersistenceCache().getMainCache().getNodeFromCache(entity);
        if (node != null)
        {
            node.detach();
        }
    }

    /**
     * Gets the client.
     * 
     * @param m
     *            the m
     * @return the client
     */
    public Client getClient(EntityMetadata m)
    {
        // // Persistence Unit used to retrieve client
        if (m == null)
        {
            throw new KunderaException("Entitymatadata should not be null");
        }
        String persistenceUnit = m.getPersistenceUnit();
        //
        Client client = clientMap.get(persistenceUnit);
        if (client == null)
        {
            throw new ClientResolverException("No client configured for persistenceUnit" + persistenceUnit);
        }
        return client;
    }

    /**
     * Gets the event dispatcher.
     * 
     * @return the event dispatcher
     */
    private EntityEventDispatcher getEventDispatcher()
    {
        return eventDispatcher;
    }

    /**
     * Creates the query.
     * 
     * @param jpaQuery
     *            the jpa query
     * @return the query
     */
    Query createQuery(String jpaQuery)
    {
        Query query = new QueryResolver().getQueryImplementation(jpaQuery, this);
        return query;
    }

    /**
     * Checks if is open.
     * 
     * @return true, if is open
     */
    public boolean isOpen()
    {
        return !closed;
    }

    /**
     * Close.
     */
    void close()
    {
        doFlush();
        eventDispatcher = null;

        // Close all clients created in this session
        if (clientMap != null && !clientMap.isEmpty())
        {
            for (Client client : clientMap.values())
            {
                client.close();
            }
            clientMap.clear();
            clientMap = null;
        }

        // TODO: Move all nodes tied to this EM into detached state, need to
        // discuss with Amresh.

        closed = true;
    }

    void clear()
    {
        // Move all nodes tied to this EM into detached state
        flushManager.clearFlushStack();
        getPersistenceCache().clean();
    }

    /**
     * Check if the instance is a managed entity instance belonging to the
     * current persistence context.
     */
    boolean contains(Object entity)
    {
        Node node = getPersistenceCache().getMainCache().getNodeFromCache(entity);
        return node != null && node.isInState(ManagedState.class);
    }

    /**
     * Refresh the state of the instance from the database, overwriting changes
     * made to the entity, if any.
     */
    public void refresh(Object entity)
    {
        if (contains(entity))
        {
            MainCache mainCache = (MainCache) getPersistenceCache().getMainCache();
            Node node = mainCache.getNodeFromCache(entity);
            // Locking as it might read from persistence context.
            lock.readLock().lock();
            node.refresh();
            lock.readLock().unlock();
        }
        else
        {
            throw new IllegalArgumentException("This is not a valid or managed entity, can't be refreshed");
        }
    }

    /**
     * Gets the metadata.
     * 
     * @param clazz
     *            the clazz
     * @return the metadata
     */
    private EntityMetadata getMetadata(Class<?> clazz)
    {
        return KunderaMetadataManager.getEntityMetadata(clazz);
    }

    /**
     * @param flushMode
     *            the flushMode to set
     */
    void setFlushMode(FlushModeType flushMode)
    {
        // TODO keeping it open for future releases current not using any where.
        this.flushMode = flushMode;
    }

    /**
     * @return the isTransactionInProgress
     */
    public boolean isTransactionInProgress()
    {
        return isTransactionInProgress;
    }

    /**
     * @return the persistenceCache
     */
    public PersistenceCache getPersistenceCache()
    {
        return persistenceCache;
    }

    /******************************* Transaction related methods ***********************************************/

    void begin()
    {
        isTransactionInProgress = true;
    }

    void commit()
    {
        execute();
        flushManager.commit();
        flushManager.clearFlushStack();
        isTransactionInProgress = false;
    }

    /**
     * On explicit call from em.flush().
     */
    void doFlush()
    {
        enableFlush = true;
        flush();
        execute();
        enableFlush = false;
        flushManager.commit();
        flushManager.clearFlushStack();
    }

    void rollback()
    {
        flushManager.rollback(this);
        flushManager.clearFlushStack();
        getPersistenceCache().clean();
        isTransactionInProgress = false;
    }

    /**
     * Populates client specific properties.
     * 
     * @param properties
     *            map of properties.
     */
    void populateClientProperties(Map properties)
    {
        if (properties != null && !properties.isEmpty())
        {
            Map<String, Client> clientMap = getDelegate();
            if (clientMap != null && !clientMap.isEmpty())
            {
                // TODO If we have two pu for same client then? Need to discuss
                // with Amresh.
                for (Client client : clientMap.values())
                {
                    if (client instanceof ClientPropertiesSetter)
                    {
                        ClientPropertiesSetter cps = (ClientPropertiesSetter) client;
                        cps.populateClientProperties(client, properties);
                    }
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Can't set Client properties as None/ Null was supplied");
            }
        }
    }

    /**
     * Pre load client specific to persistence unit.
     * 
     * @param persistenceUnit
     *            persistence unit.
     */

    void loadClient(String persistenceUnit)
    {
        if (clientMap == null)
        {
            clientMap = new HashMap<String, Client>();
        }

        if (!clientMap.containsKey(persistenceUnit))
        {
            clientMap.put(persistenceUnit, ClientResolver.discoverClient(persistenceUnit));
        }
    }

    /**
     * Returns map of client as delegate to entity manager.
     * 
     * @return clientMap client map
     */
    Map<String, Client> getDelegate()
    {
        return clientMap;
    }

    /**
     * Executes batch.
     */
    private void execute()
    {
        if (clientMap != null)
        {
            for (Client client : clientMap.values())
            {
                if (client instanceof Batcher)
                {
                    if (((Batcher) client).executeBatch() > 0)
                    {
                        flushJoinTableData();
                    }
                }
            }
        }
    }

    /**
     * On flushing join table data
     */
    private void flushJoinTableData()
    {
        if (applyFlush())
        {
            for (JoinTableData jtData : flushManager.getJoinTableData())
            {
                if (!jtData.isProcessed())
                {
                    EntityMetadata m = KunderaMetadataManager.getEntityMetadata(jtData.getEntityClass());
                    Client client = getClient(m);
                    if (OPERATION.INSERT.equals(jtData.getOperation()))
                    {
                        client.persistJoinTable(jtData);
                        jtData.setProcessed(true);
                    }
                    else if (OPERATION.DELETE.equals(jtData.getOperation()))
                    {
                        for (Object pk : jtData.getJoinTableRecords().keySet())
                        {
                            client.deleteByColumn(m.getSchema(), jtData.getJoinTableName(),
                                    ((AbstractAttribute) m.getIdAttribute()).getJPAColumnName(), pk);
                        }
                        jtData.setProcessed(true);
                    }
                }

            }
        }
    }

    /**
     * Returns true, if flush mode is AUTO and not running within transaction ||
     * running within transaction and commit is invoked.
     * 
     * @return boolean value.
     */
    private boolean applyFlush()
    {
        return (!isTransactionInProgress && flushMode.equals(FlushModeType.AUTO)) || enableFlush;
    }

    /**
     * Returns transaction coordinator.
     * 
     * @return
     */
    Coordinator getCoordinator()
    {
        coordinator = new Coordinator();
        try
        {
            for (String pu : clientMap.keySet())
            {
                PersistenceUnitMetadata puMetadata = KunderaMetadataManager.getPersistenceUnitMetadata(pu);

                String txResource = puMetadata.getProperty(PersistenceProperties.KUNDERA_TRANSACTION_RESOURCE);

                if (txResource != null)
                {
                    TransactionResource resource = (TransactionResource) Class.forName(txResource).newInstance();
                    coordinator.addResource(resource, pu);
                    Client client = clientMap.get(pu);

                    if (!(client instanceof TransactionBinder))
                    {
                        throw new KunderaTransactionException(
                                "Client : "
                                        + client.getClass()
                                        + " must implement TransactionBinder interface, if {kundera.transaction.resource.class} property provided!");
                    }
                    else
                    {
                        ((TransactionBinder) client).bind(resource);
                    }
                }
                else
                {
                    coordinator.addResource(new DefaultTransactionResource(clientMap.get(pu)), pu);
                }
            }
        }
        catch (InstantiationException e)
        {
            log.error("Error while initializing Transaction Resource:", e);
            throw new KunderaTransactionException(e);
        }
        catch (IllegalAccessException e)
        {
            log.error("Error while initializing Transaction Resource:", e);
            throw new KunderaTransactionException(e);
        }
        catch (ClassNotFoundException e)
        {
            log.error("Error while initializing Transaction Resource:", e);
            throw new KunderaTransactionException(e);
        }
        return coordinator;
    }

    /**
     * If transaction is in progress and user explicitly invokes em.flush()!
     * 
     * @param node
     *            data node
     * @param metadata
     *            entity metadata.
     */
    private void onSynchronization(Node node, EntityMetadata metadata)
    {
        DefaultTransactionResource resource = (DefaultTransactionResource) coordinator.getResource(metadata
                .getPersistenceUnit());
        if (enableFlush)
        {
            resource.onFlush();
        }
        else
        {
            resource.syncNode(node);
        }
    }
}
