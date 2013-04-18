/**
 * Copyright 2012 Impetus Infotech.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.impetus.kundera.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.MapKeyJoinColumn;

import org.apache.commons.lang.StringUtils;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import com.impetus.kundera.graph.NodeLink.LinkProperty;
import com.impetus.kundera.lifecycle.states.NodeState;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.MetadataUtils;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.Relation;
import com.impetus.kundera.persistence.IdGenerator;
import com.impetus.kundera.persistence.PersistenceDelegator;
import com.impetus.kundera.persistence.context.PersistenceCache;
import com.impetus.kundera.property.PropertyAccessorHelper;
import com.impetus.kundera.utils.DeepEquals;

/**
 * Responsible for generating {@link ObjectGraph} of nodes from a given entity
 * 
 * @author amresh.singh
 */
public class ObjectGraphBuilder
{
    private PersistenceCache persistenceCache;

    private PersistenceDelegator pd;

    private IdGenerator idGenerator;

    public ObjectGraphBuilder(PersistenceCache pcCache, PersistenceDelegator pd)
    {
        this.persistenceCache = pcCache;
        this.pd = pd;
        this.idGenerator = new IdGenerator();
    }

    public ObjectGraph getObjectGraph(Object entity, NodeState initialNodeState)
    {
        // Initialize object graph
        ObjectGraph objectGraph = new ObjectGraph();

        // Recursively build object graph and get head node.
        Node headNode = getNode(entity, objectGraph, initialNodeState);

        // Set head node into object graph
        if (headNode != null)
        {
            objectGraph.setHeadNode(headNode);
        }
        return objectGraph;
    }

    /**
     * Constructs and returns {@link Node} representation for a given entity
     * object. Output is fully constructed graph with relationships embedded.
     * Each node is put into <code>graph</code> once it is constructed.
     * 
     * @param entity
     * @return
     */
    private Node getNode(Object entity, ObjectGraph graph, NodeState initialNodeState)
    {

        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(entity.getClass());

        // entity metadata could be null.
        if (entityMetadata == null)
        {
            return null;
        }
        Object id = PropertyAccessorHelper.getId(entity, entityMetadata);

        String nodeId = ObjectGraphUtils.getNodeId(id, entity.getClass());
        Node node = graph.getNode(nodeId);

        // If this node is already there in graph (may happen for bidirectional
        // relationship, do nothing and return null)
        if (node != null)
        {
            if (node.isGraphCompleted())
            {
                return node;
            }
            return null;
        }

        // Construct this Node first, if one not already there in Persistence
        // Cache
        Node nodeInPersistenceCache = persistenceCache.getMainCache().getNodeFromCache(nodeId);

        // Make a deep copy of entity data

        if (nodeInPersistenceCache == null)
        {
            node = new Node(nodeId, entity, initialNodeState, persistenceCache, id);
        }
        else
        {
            node = nodeInPersistenceCache;

            // Determine whether this node is dirty based on comparison between
            // Node data and entity data
            // If dirty, set the entity data into node and mark it as dirty
            if (!DeepEquals.deepEquals(node.getData(), entity))
            {
                node.setData(entity);
                node.setDirty(true);
            }
            else if (node.isProcessed())
            {
                node.setDirty(false);
            }

            // If node is NOT in managed state, its data needs to be
            // replaced with the one provided in entity object
        }

        // Put this node into object graph
        graph.addNode(nodeId, node);

        // Iterate over relations and construct children nodes
        for (Relation relation : entityMetadata.getRelations())
        {

            // Child Object set in this entity
            Object childObject = PropertyAccessorHelper.getObject(entity, relation.getProperty());

            if (childObject != null && !(childObject instanceof HibernateProxy))
            {
                EntityMetadata metadata = KunderaMetadataManager.getEntityMetadata(childObject.getClass());
                if (metadata != null && relation.isJoinedByPrimaryKey())
                {
                    PropertyAccessorHelper.setId(childObject, metadata,
                            PropertyAccessorHelper.getId(entity, entityMetadata));
                }
                // This child object could be either an entity(1-1 or M-1)
                // or a
                // collection/ Map of entities(1-M or M-M)
                if (Collection.class.isAssignableFrom(childObject.getClass()))
                {
                    // For each entity in the collection, construct a child
                    // node
                    // and add to graph
                    Collection childrenObjects = (Collection) childObject;

                    if (childrenObjects != null && !(childrenObjects instanceof PersistentCollection))

                        for (Object childObj : childrenObjects)
                        {
                            if (childObj != null)
                            {
                                addChildNodesToGraph(graph, node, relation, childObj, initialNodeState);
                            }
                        }
                }
                else if (Map.class.isAssignableFrom(childObject.getClass()))
                {
                    Map childrenObjects = (Map) childObject;
                    if (childrenObjects != null && !(childrenObjects instanceof PersistentCollection))
                    {
                        for (Map.Entry entry : (Set<Map.Entry>) childrenObjects.entrySet())
                        {
                            addChildNodesToGraph(graph, node, relation, entry, initialNodeState);
                        }
                    }
                }
                else
                {
                    // Construct child node and add to graph
                    addChildNodesToGraph(graph, node, relation, childObject, initialNodeState);
                }
            }
        }

        // Means compelte graph is build.
        node.setGraphCompleted(true);
        return node;
    }

    /**
     * @param graph
     * @param node
     * @param relation
     * @param childObject
     */
    private void addChildNodesToGraph(ObjectGraph graph, Node node, Relation relation, Object childObject,
            NodeState initialNodeState)
    {
        if (childObject instanceof Map.Entry)
        {
            Map.Entry entry = (Map.Entry) childObject;
            Object relObject = entry.getKey();
            Object entityObject = entry.getValue();

            Node childNode = getNode(entityObject, graph, initialNodeState);

            if (childNode != null)
            {
                if (!StringUtils.isEmpty(relation.getMappedBy())
                        && relation.getProperty().getAnnotation(MapKeyJoinColumn.class) == null)
                {
                    return;
                }

                NodeLink nodeLink = new NodeLink(node.getNodeId(), childNode.getNodeId());
                nodeLink.setMultiplicity(relation.getType());

                EntityMetadata metadata = KunderaMetadataManager.getEntityMetadata(node.getDataClass());
                nodeLink.setLinkProperties(getLinkProperties(metadata, relation));

                nodeLink.addLinkProperty(LinkProperty.LINK_VALUE, relObject);

                // Add Parent node to this child
                childNode.addParentNode(nodeLink, node);

                // Add child node to this node
                node.addChildNode(nodeLink, childNode);
            }
        }
        else
        {
            // Construct child node for this child object via recursive call
            Node childNode = getNode(childObject, graph, initialNodeState);

            if (childNode != null)
            {
                // Construct Node Link for this relationship
                NodeLink nodeLink = new NodeLink(node.getNodeId(), childNode.getNodeId());
                nodeLink.setMultiplicity(relation.getType());

                EntityMetadata metadata = KunderaMetadataManager.getEntityMetadata(node.getDataClass());
                nodeLink.setLinkProperties(getLinkProperties(metadata, relation));

                // Add Parent node to this child
                childNode.addParentNode(nodeLink, node);

                // Add child node to this node
                node.addChildNode(nodeLink, childNode);
            }
        }
    }

    /**
     * 
     * @param metadata
     *            Entity metadata of the parent node
     * @param relation
     * @return
     */
    private Map<LinkProperty, Object> getLinkProperties(EntityMetadata metadata, Relation relation)
    {
        Map<LinkProperty, Object> linkProperties = new HashMap<NodeLink.LinkProperty, Object>();

        linkProperties.put(LinkProperty.LINK_NAME, MetadataUtils.getMappedName(metadata, relation));
        linkProperties.put(LinkProperty.IS_SHARED_BY_PRIMARY_KEY, relation.isJoinedByPrimaryKey());
        linkProperties.put(LinkProperty.IS_BIDIRECTIONAL, !relation.isUnary());
        linkProperties.put(LinkProperty.IS_RELATED_VIA_JOIN_TABLE, relation.isRelatedViaJoinTable());
        linkProperties.put(LinkProperty.PROPERTY, relation.getProperty());
        linkProperties.put(LinkProperty.CASCADE, relation.getCascades());

        if (relation.isRelatedViaJoinTable())
        {
            linkProperties.put(LinkProperty.JOIN_TABLE_METADATA, relation.getJoinTableMetadata());
        }

        // TODO: Add more link properties as required
        return linkProperties;
    }
}
