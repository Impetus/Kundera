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
package com.impetus.client.neo4j.index;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;

import com.impetus.kundera.PersistenceProperties;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.KunderaMetadata;
import com.impetus.kundera.metadata.model.MetamodelImpl;
import com.impetus.kundera.metadata.model.PersistenceUnitMetadata;
import com.impetus.kundera.metadata.model.attributes.AbstractAttribute;

/**
 * Provides functionalities specific to Auto/ Manual Indexing
 * 
 * @author amresh.singh
 */
public class Neo4JIndexManager
{
    // Auto-Indexing related methods
    public boolean isNodeAutoIndexingEnabled(GraphDatabaseService graphDb)
    {
        return graphDb.index().getNodeAutoIndexer().isEnabled();
    }

    public boolean isRelationshipAutoIndexingEnabled(GraphDatabaseService graphDb)
    {
        return graphDb.index().getRelationshipAutoIndexer().isEnabled();
    }

    public Set<String> getNodeIndexedProperties(GraphDatabaseService graphDb)
    {
        return graphDb.index().getNodeAutoIndexer().getAutoIndexedProperties();
    }

    public Set<String> getRelationsipIndexedProperties(GraphDatabaseService graphDb)
    {
        return graphDb.index().getRelationshipAutoIndexer().getAutoIndexedProperties();
    }

    protected boolean isEntityForNeo4J(EntityMetadata entityMetadata)
    {
        String persistenceUnit = entityMetadata.getPersistenceUnit();
        PersistenceUnitMetadata puMetadata = KunderaMetadataManager.getPersistenceUnitMetadata(persistenceUnit);
        String clientFactory = puMetadata.getProperty(PersistenceProperties.KUNDERA_CLIENT_FACTORY);
        if (clientFactory.indexOf("com.impetus.client.neo4j") >= 0)
        {
            return true;
        }
        return false;
    }

    /**************** Manual Indexing related methods *********************/

    /**
     * If node auto-indexing is disabled,manually index this Node
     * 
     * @param entityMetadata
     * @param graphDb
     * @param autoIndexing
     * @param node
     */
    public void indexNode(EntityMetadata entityMetadata, GraphDatabaseService graphDb, Node node)
    {
        if (!isNodeAutoIndexingEnabled(graphDb) && entityMetadata.isIndexable())
        {
            Index<Node> nodeIndex = graphDb.index().forNodes(entityMetadata.getIndexName());
            addNodeIndex(entityMetadata, node, nodeIndex);
        }
    }

    /**
     * If relationship auto-indexing is disabled, manually index this
     * relationship
     * 
     * @param entityMetadata
     * @param graphDb
     * @param autoIndexing
     * @param node
     */
    public void indexRelationship(EntityMetadata entityMetadata, GraphDatabaseService graphDb, Relationship relationship)
    {
        if (!isRelationshipAutoIndexingEnabled(graphDb) && entityMetadata.isIndexable())
        {
            Index<Relationship> relationshipIndex = graphDb.index().forRelationships(entityMetadata.getIndexName());
            addRelationshipIndex(entityMetadata, relationship, relationshipIndex);
        }
    }

    public void indexNodeUsingBatchIndexer(BatchInserterIndexProvider indexProvider,
            EntityMetadata entityMetadata, long nodeId, Map<String, Object> nodeProperties, boolean nodeAutoIndexingEnabled)
    {
        BatchInserterIndex nodeIndex = null;       

        if (!nodeAutoIndexingEnabled && entityMetadata.isIndexable())
        {
            nodeIndex = indexProvider.nodeIndex(entityMetadata.getIndexName(), MapUtil.stringMap("type", "exact"));

        }
        else
        {
            nodeIndex = indexProvider.nodeIndex("node_auto_index", MapUtil.stringMap("type", "exact"));
        }

        if (nodeIndex != null)
            nodeIndex.add(nodeId, nodeProperties);        
    }
    
    
    public void indexRelationshipUsingBatchIndexer(BatchInserterIndexProvider indexProvider,
            EntityMetadata entityMetadata, long relationshipId, Map<String, Object> relationshipProperties, boolean relationshipAutoIndexingEnabled)
    {       
        BatchInserterIndex relationshipIndex = null;

        if (!relationshipAutoIndexingEnabled && entityMetadata.isIndexable())
        {
            relationshipIndex = indexProvider.relationshipIndex(entityMetadata.getIndexName(), MapUtil.stringMap( "type", "exact"));
        }
        else
        {
            relationshipIndex = indexProvider.relationshipIndex("relationship_auto_index", MapUtil.stringMap( "type", "exact"));            
        }

        if (relationshipIndex != null)
            relationshipIndex.add(relationshipId, relationshipProperties);
    }
    
    
    

    /**
     * If node auto-indexing is disabled, Update index for this node manually
     * 
     * @param entityMetadata
     * @param graphDb
     * @param autoIndexing
     * @param node
     */
    public void updateNodeIndex(EntityMetadata entityMetadata, GraphDatabaseService graphDb, Node node)
    {
        if (!isNodeAutoIndexingEnabled(graphDb) && entityMetadata.isIndexable())
        {
            Index<Node> nodeIndex = graphDb.index().forNodes(entityMetadata.getIndexName());

            // Remove all exiting node entries from Index
            nodeIndex.remove(node);

            // Recreate fresh index on this node
            addNodeIndex(entityMetadata, node, nodeIndex);
        }

    }

    /**
     * If relationship auto-indexing is disabled, Update index for this
     * relationship manually
     * 
     * @param entityMetadata
     * @param graphDb
     * @param autoIndexing
     * @param node
     */
    public void updateRelationshipIndex(EntityMetadata entityMetadata, GraphDatabaseService graphDb,
            Relationship relationship)
    {
        if (!isRelationshipAutoIndexingEnabled(graphDb) && entityMetadata.isIndexable())
        {
            Index<Relationship> relationshipIndex = graphDb.index().forRelationships(entityMetadata.getIndexName());

            // Remove all existing relationship entries from Index
            relationshipIndex.remove(relationship);

            // Recreate fresh index on this relationship
            addRelationshipIndex(entityMetadata, relationship, relationshipIndex);
        }
    }

    /**
     * Deletes a {@link Node} from manually created index if auto-indexing is
     * disabled
     * 
     * @param entityMetadata
     * @param graphDb
     * @param node
     */
    public void deleteNodeIndex(EntityMetadata entityMetadata, GraphDatabaseService graphDb, Node node)
    {
        if (!isNodeAutoIndexingEnabled(graphDb) && entityMetadata.isIndexable())
        {
            Index<Node> nodeIndex = graphDb.index().forNodes(entityMetadata.getIndexName());
            nodeIndex.remove(node);
        }
    }

    /**
     * Deletes a {@link Relationship} from manually created index if
     * auto-indexing is disabled
     * 
     * @param entityMetadata
     * @param graphDb
     * @param relationship
     */
    public void deleteRelationshipIndex(EntityMetadata entityMetadata, GraphDatabaseService graphDb,
            Relationship relationship)
    {
        if (!isRelationshipAutoIndexingEnabled(graphDb) && entityMetadata.isIndexable())
        {
            Index<Relationship> relationshipIndex = graphDb.index().forRelationships(entityMetadata.getIndexName());
            relationshipIndex.remove(relationship);
        }
    }

    /**
     * Adds Node Index for all singular attributes (including ID) of a given
     * entity
     * 
     * @param entityMetadata
     * @param node
     * @param metaModel
     * @param nodeIndex
     */
    private void addNodeIndex(EntityMetadata entityMetadata, Node node, Index<Node> nodeIndex)
    {
        MetamodelImpl metaModel = (MetamodelImpl) KunderaMetadata.INSTANCE.getApplicationMetadata().getMetamodel(
                entityMetadata.getPersistenceUnit());

        // ID attribute has to be indexed necessarily
        String idColumnName = ((AbstractAttribute)entityMetadata.getIdAttribute()).getJPAColumnName();
        nodeIndex.add(node, idColumnName, node.getProperty(idColumnName));

        // Index all other fields, for whom indexing is enabled
        for (Attribute attribute : metaModel.entity(entityMetadata.getEntityClazz()).getSingularAttributes())
        {
            Field field = (Field) attribute.getJavaMember();
            if (!attribute.isCollection() && !attribute.isAssociation()
                    && entityMetadata.getIndexProperties().keySet().contains(field.getName()))
            {
                String columnName = ((AbstractAttribute)attribute).getJPAColumnName();
                nodeIndex.add(node, columnName, node.getProperty(columnName));
            }
        }
    }

    /**
     * Adds Relationship Index for all singular attributes (including ID) of a
     * given relationship entity
     * 
     * @param entityMetadata
     * @param graphDb
     * @param relationship
     */
    private void addRelationshipIndex(EntityMetadata entityMetadata, Relationship relationship,
            Index<Relationship> relationshipIndex)
    {
        MetamodelImpl metaModel = (MetamodelImpl) KunderaMetadata.INSTANCE.getApplicationMetadata().getMetamodel(
                entityMetadata.getPersistenceUnit());

        // ID attribute has to be indexed first
        String idColumnName = ((AbstractAttribute)entityMetadata.getIdAttribute()).getJPAColumnName();
        relationshipIndex.add(relationship, idColumnName, relationship.getProperty(idColumnName));

        // Index all other fields, for whom indexing is enabled
        for (Attribute attribute : metaModel.entity(entityMetadata.getEntityClazz()).getSingularAttributes())
        {
            Field field = (Field) attribute.getJavaMember();
            if (!attribute.isCollection() && !attribute.isAssociation()
                    && entityMetadata.getIndexProperties().keySet().contains(field.getName()))
            {
                String columnName = ((AbstractAttribute)attribute).getJPAColumnName();
                relationshipIndex.add(relationship, columnName, relationship.getProperty(columnName));
            }
        }
    }

}
