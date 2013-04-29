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
package com.impetus.client.cassandra.thrift;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.KeySlice;
import org.scale7.cassandra.pelops.Bytes;

import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.EntityMetadata.Type;
import com.impetus.kundera.property.PropertyAccessor;
import com.impetus.kundera.property.PropertyAccessorFactory;

/**
 * Provides utility methods for extracting useful data from Thrift result
 * retrieved from database (usually in the form of {@link ColumnOrSuperColumn}
 * 
 * @author amresh.singh
 */
public class ThriftDataResultHelper
{
    public enum ColumnFamilyType
    {
        COLUMN, /* Column family */
        SUPER_COLUMN, /* Super Column family */
        COUNTER_COLUMN, /* Counter Column family */
        COUNTER_SUPER_COLUMN
        /* Super Counter Column family */
    }

    /**
     * Transforms data retrieved as result via thrift to a List whose content
     * type is determined by {@link ColumnFamilyType}
     */
    public static <T> List<T> transformThriftResult(List<ColumnOrSuperColumn> coscList,
            ColumnFamilyType columnFamilyType, ThriftRow row)
    {
        List result = new ArrayList(coscList.size());
        for (ColumnOrSuperColumn cosc : coscList)
        {
            result.add(transformThriftResult(cosc, columnFamilyType, row));
        }
        return result;
    }

    public static <T> T transformThriftResult(ColumnOrSuperColumn cosc, ColumnFamilyType columnFamilyType, ThriftRow row)
    {
        Object output = null;
        switch (columnFamilyType)
        {
        case COLUMN:
            output = cosc.column;
            if (row != null)
            {
                row.addColumn(cosc.column);
            }
            break;

        case SUPER_COLUMN:
            output = cosc.super_column;
            if (row != null)
            {
                row.addSuperColumn(cosc.super_column);
            }
            break;

        case COUNTER_COLUMN:
            output = cosc.counter_column;
            if (row != null)
            {
                row.addCounterColumn(cosc.counter_column);
            }
            break;

        case COUNTER_SUPER_COLUMN:
            output = cosc.counter_super_column;
            if (row != null)
            {
                row.addCounterSuperColumn(cosc.counter_super_column);
            }
            break;
        }
        return (T) output;
    }

    /**
     * Transforms data retrieved as result via thrift to a Map whose content
     * type is determined by {@link ColumnFamilyType}
     */
    public static <T> Map<ByteBuffer, List<T>> transformThriftResult(
            Map<ByteBuffer, List<ColumnOrSuperColumn>> coscResultMap, ColumnFamilyType columnFamilyType, ThriftRow row)
    {

        Map<ByteBuffer, List<T>> output = new HashMap<ByteBuffer, List<T>>();

        for (ByteBuffer key : coscResultMap.keySet())
        {
            output.put(key, (List<T>) transformThriftResult(coscResultMap.get(key), columnFamilyType, row));
        }

        return output;
    }

    public static <T> Map<Bytes, List<T>> transformThriftResult(ColumnFamilyType columnFamilyType,
            List<KeySlice> keySlices, ThriftRow row)
    {

        Map<Bytes, List<T>> output = new HashMap<Bytes, List<T>>();

        for (KeySlice keySlice : keySlices)
        {
            output.put(Bytes.fromByteArray(keySlice.getKey()),
                    (List<T>) transformThriftResult(keySlice.getColumns(), columnFamilyType, row));
        }

        return output;
    }

    /**
     * Transforms data retrieved as result via thrift to a List whose content
     * type is determined by {@link ColumnFamilyType}
     */
    public <T> List<T> transformThriftResultAndAddToList(Map<ByteBuffer, List<ColumnOrSuperColumn>> coscResultMap,
            ColumnFamilyType columnFamilyType, ThriftRow row)
    {

        List<ColumnOrSuperColumn> coscList = new ArrayList<ColumnOrSuperColumn>();

        for (List<ColumnOrSuperColumn> list : coscResultMap.values())
        {
            coscList.addAll(list);
        }

        return transformThriftResult(coscList, columnFamilyType, row);
    }

    /**
     * Translates into thrift row.
     * 
     * @param coscResultMap
     * @param isCounterType
     * @param columnFamilyType
     * @param row
     * @return
     */
    public ThriftRow translateToThriftRow(Map<ByteBuffer, List<ColumnOrSuperColumn>> coscResultMap,
            boolean isCounterType, Type columnFamilyType, ThriftRow row)
    {
        ColumnFamilyType columnType = ColumnFamilyType.COLUMN;

        if (isCounterType)
        {
            if (columnFamilyType.equals(Type.SUPER_COLUMN_FAMILY))
            {
                columnType = ColumnFamilyType.COUNTER_SUPER_COLUMN;
            }
            else
            {
                columnType = ColumnFamilyType.COUNTER_COLUMN;
            }
        }
        else if (columnFamilyType.equals(Type.SUPER_COLUMN_FAMILY))
        {
            columnType = ColumnFamilyType.SUPER_COLUMN;
        }
        transformThriftResultAndAddToList(coscResultMap, columnType, row);
        return row;
    }

    /**
     * Fetches Row keys from a {@link List} of {@link KeySlice}
     */
    public static List<Object> getRowKeys(List<KeySlice> keySlices, EntityMetadata metadata)
    {
        PropertyAccessor<?> accessor = PropertyAccessorFactory.getPropertyAccessor((Field) metadata.getIdAttribute()
                .getJavaMember());
        List<Object> rowKeys = new ArrayList<Object>();
        for (KeySlice keySlice : keySlices)
        {
            byte[] key = keySlice.getKey();
            Object rowKey = accessor.fromBytes(metadata.getIdAttribute().getJavaType(), key);
            rowKeys.add(rowKey);
        }
        return rowKeys;
    }

}
