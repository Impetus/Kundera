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
package com.impetus.client.hbase.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.util.Bytes;

import com.impetus.client.hbase.HBaseData;
import com.impetus.client.hbase.Reader;
import com.impetus.client.hbase.utils.HBaseUtils;

/**
 * Implmentation class for HBase for <code>Reader</code> interface.
 * 
 * @author vivek.mishra
 */
public class HBaseReader implements Reader
{

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.client.hbase.Reader#LoadData(org.apache.hadoop.hbase.client
     * .HTable, java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unused")
    @Override
    public List<HBaseData> LoadData(HTableInterface hTable, String columnFamily, Object rowKey, Filter filter,
            String... columns) throws IOException
    {
        List<HBaseData> results = null;

        // Get g = prepareGet(rowKey, filter);

        ResultScanner scanner = null;

        // only in case of find by id
        Scan scan = null;
        if (rowKey != null)
        {
            byte[] rowKeyBytes = HBaseUtils.getBytes(rowKey);
            Get g = new Get(rowKeyBytes);
            scan = new Scan(g);
        }
        else
        {
            scan = new Scan();
        }
        setScanCriteria(filter, columnFamily, null, scan, columns);
        scanner = hTable.getScanner(scan);

        return scanResults(columnFamily, results, scanner);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.client.hbase.Reader#LoadData(org.apache.hadoop.hbase.client
     * .HTable, java.lang.String)
     */
    @Override
    public List<HBaseData> LoadData(HTableInterface hTable, Object rowKey, Filter filter, String... columns)
            throws IOException
    {
        return LoadData(hTable, Bytes.toString(hTable.getTableName()), rowKey, filter, columns);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.client.hbase.Reader#loadAll(org.apache.hadoop.hbase.client
     * .HTable, org.apache.hadoop.hbase.filter.Filter, byte[], byte[])
     */
    @Override
    public List<HBaseData> loadAll(HTableInterface hTable, Filter filter, byte[] startRow, byte[] endRow,
            String columnFamily, String qualifier, String[] columns) throws IOException
    {
        List<HBaseData> results = null;
        Scan s = null;
        if (startRow != null && endRow != null && startRow.equals(endRow))
        {
            Get g = new Get(startRow);
            s = new Scan(g);
        }
        else if (startRow != null && endRow != null)
        {
            s = new Scan(startRow, endRow);
        }
        else if (startRow != null)
        {
            s = new Scan(startRow);
        }
        else if (endRow != null)
        {
            s = new Scan();
            s.setStopRow(endRow);
        }
        else
        {
            s = new Scan();
        }

        setScanCriteria(filter, columnFamily, qualifier, s, columns);

        ResultScanner scanner = hTable.getScanner(s);
        return scanResults(null, results, scanner);
    }

    /**
     * @param filter
     * @param columnFamily
     * @param s
     */
    private void setScanCriteria(Filter filter, String columnFamily, String qualifier, Scan s, String[] columns)
    {
        if (filter != null)
        {
            s.setFilter(filter);
        }
        if (columnFamily != null)
        {
            // s.addFamily(Bytes.toBytes(columnFamily));
        }
        if (columnFamily != null && qualifier != null)
        {
            s.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier));
        }
        if (columns != null && columns.length > 0)
        {
            for (String columnName : columns)
            {
                if (columnFamily != null && columnName != null)
                {
                    s.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnName));
                }
            }
        }
    }

    /**
     * Scan and populate {@link HBaseData} collection using scanned results.
     * 
     * @param columnFamily
     *            column family.
     * @param results
     *            results.
     * @param scanner
     *            result scanner.
     * @return collection of scanned results.
     */
    private List<HBaseData> scanResults(String columnFamily, List<HBaseData> results, ResultScanner scanner)
    {
        HBaseData data = null;
        for (Result result : scanner)
        {
            List<KeyValue> values = result.list();
            for (KeyValue value : values)
            {
                data = new HBaseData(columnFamily != null ? columnFamily : new String(value.getFamily()),
                        value.getRow());
                break;
            }
            data.setColumns(values);
            if (results == null)
            {
                results = new ArrayList<HBaseData>();
            }
            results.add(data);
        }

        return results;
    }

    @Override
    public Object[] scanRowKeys(final HTableInterface hTable, final Filter filter, final String columnFamilyName,
            final String columnName, final Class rowKeyClazz) throws IOException
    {
        List<Object> rowKeys = new ArrayList<Object>();
        Scan s = new Scan();
        s.setFilter(filter);
        s.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName));

        ResultScanner scanner = hTable.getScanner(s);

        for (Result result : scanner)
        {

            for (KeyValue keyValue : result.list())
            {
                rowKeys.add(HBaseUtils.fromBytes(keyValue.getRow(), rowKeyClazz));
            }
        }

        if (rowKeys != null && !rowKeys.isEmpty())
        {
            return rowKeys.toArray(new Object[0]);
        }
        return null;
    }

    public List<HBaseData> loadAll(HTableInterface hTable, List<Object> rows, String columnFamily, String[] columns)
            throws IOException
    {
        List<HBaseData> results = null;

        HBaseData data = null;

        List<Get> getRequest = new ArrayList<Get>();
        for (Object rowKey : rows)
        {
            if (rowKey != null)
            {
                byte[] rowKeyBytes = HBaseUtils.getBytes(rowKey);
                Get request = new Get(rowKeyBytes);
                getRequest.add(request);
            }
        }
        Result[] rawResult = hTable.get(getRequest);

        for (Result result : rawResult)
        {
            List<KeyValue> values = result.list();

            if (values != null)
            {
                for (KeyValue value : values)
                {
                    data = new HBaseData(columnFamily != null ? columnFamily : new String(value.getFamily()),
                            value.getRow());
                    break;
                }

                data.setColumns(values);
                if (results == null)
                {
                    results = new ArrayList<HBaseData>();
                }
                results.add(data);
            }
        }
        return results;

    }

}
