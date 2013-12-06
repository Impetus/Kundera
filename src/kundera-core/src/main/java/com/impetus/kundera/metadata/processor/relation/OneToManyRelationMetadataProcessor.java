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
package com.impetus.kundera.metadata.processor.relation;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import com.impetus.kundera.loader.MetamodelLoaderException;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.Relation;
import com.impetus.kundera.metadata.model.attributes.AbstractAttribute;
import com.impetus.kundera.metadata.processor.AbstractEntityFieldProcessor;
import com.impetus.kundera.metadata.validator.EntityValidatorImpl;
import com.impetus.kundera.property.PropertyAccessorHelper;

/**
 * The Class OneToManyRelationMetadataProcessor.
 * 
 * @author Amresh Singh
 */
public class OneToManyRelationMetadataProcessor extends AbstractEntityFieldProcessor implements
        RelationMetadataProcessor
{

    /**
     * Instantiates a new one to many relation metadata processor.
     */
    public OneToManyRelationMetadataProcessor()
    {
        validator = new EntityValidatorImpl();
    }

    @Override
    public void process(Class<?> clazz, EntityMetadata metadata)
    {
        throw new MetamodelLoaderException("Method call not applicable for Relation processors");
    }

    @Override
    public void addRelationIntoMetadata(Field relationField, EntityMetadata metadata)
    {

        OneToMany ann = relationField.getAnnotation(OneToMany.class);
        Class<?> targetEntity = PropertyAccessorHelper.getGenericClass(relationField);

        // now, check annotations
        if (null != ann.targetEntity() && !ann.targetEntity().getSimpleName().equals("void"))
        {
            targetEntity = ann.targetEntity();
        }

        validate(targetEntity);

        Relation relation = new Relation(relationField, targetEntity, relationField.getType(), ann.fetch(),
                Arrays.asList(ann.cascade()), Boolean.TRUE, ann.mappedBy(), Relation.ForeignKey.ONE_TO_MANY);

        boolean isJoinedByFK = relationField.isAnnotationPresent(JoinColumn.class);
        boolean isJoinedByTable = relationField.isAnnotationPresent(JoinTable.class);

        if (isJoinedByFK)
        {
            JoinColumn joinColumnAnn = relationField.getAnnotation(JoinColumn.class);
            relation.setJoinColumnName(joinColumnAnn.name());
        }
        else if (isJoinedByTable)
        {
            throw new UnsupportedOperationException("@JoinTable not supported for one to many association");
/*            JoinTableMetadata jtMetadata = new JoinTableMetadata(relationField);

            relation.setRelatedViaJoinTable(true);
            relation.setJoinTableMetadata(jtMetadata);
*/        }
        else
        {   
            String joinColumnName = null;
            if (metadata.getIdAttribute() != null)
            {
                joinColumnName = ((AbstractAttribute) metadata.getIdAttribute()).getJPAColumnName();
            }
            if (relation.getMappedBy() != null)
            {
                try
                {
                    Field mappedField = metadata.getEntityClazz().getDeclaredField(relation.getMappedBy());
                    if (mappedField != null && mappedField.isAnnotationPresent(JoinColumn.class))
                    {
                        joinColumnName = mappedField.getAnnotation(JoinColumn.class).name();
                    }
                }
                catch (NoSuchFieldException e)
                {
                    // do nothing, it means not a case of self association
                }
                catch (SecurityException e)
                {
                    // do nothing, it means not a case of self association
                }
            }
            relation.setJoinColumnName(joinColumnName);
        }

        relation.setBiDirectionalField(metadata.getEntityClazz());
        metadata.addRelation(relationField.getName(), relation);
        metadata.setParent(true);

    }

}
