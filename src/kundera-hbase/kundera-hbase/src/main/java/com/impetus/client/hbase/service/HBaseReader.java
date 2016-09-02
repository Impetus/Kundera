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
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Table;
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
    private ResultScanner scanner = null;

    private Iterator<Result> resultsIter;

    private Integer fetchSize;

    private Integer counter = 0;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.client.hbase.Reader#LoadData(org.apache.hadoop.hbase.client
     * .HTable, java.lang.String, java.lang.String)
     */

    @Override
    public List<HBaseData> LoadData(Table hTable, String columnFamily, Object rowKey, Filter filter,
            String... columns) throws IOException
    {
        List<HBaseData> results = new ArrayList<HBaseData>();
        if (scanner == null)
        {

            // only in case of find by id
            Scan scan = null;
            if (rowKey != null)
            {
                byte[] rowKeyBytes = HBaseUtils.getBytes(rowKey);
                Get g = new Get(rowKeyBytes);

                // TODO: After more than one column family for 1 table. this
                // should work. currently failing for embeddable entities.
                if (columnFamily != null)
                {
                    g.addFamily(Bytes.toBytes(columnFamily));
                }

                if (filter != null)
                {
                    g.setFilter(filter);
                }
                Result result = hTable.get(g);

                if (result != null && result.list() != null)
                {
                    HBaseData data = null;
                    for (KeyValue value : result.list())
                    {
                        data = new HBaseData(columnFamily != null ? columnFamily : new String(value.getFamily()),
                                value.getRow());
                        break;
                    }

                    if (data != null)
                    {
                        data.setColumns(result.list());
                        results.add(data);
                    }

                }
                return results;

                // scan = new Scan(g);
            }
            else
            {
                scan = new Scan();
            }
            setScanCriteria(filter, columnFamily, null, scan, columns);
            scanner = hTable.getScanner(scan);
            resultsIter = scanner.iterator();
        }
        return scanResults(columnFamily, results);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.client.hbase.Reader#LoadData(org.apache.hadoop.hbase.client
     * .HTable, java.lang.String)
     */
    @Override
    public List<HBaseData> LoadData(Table hTable, Object rowKey, Filter filter, String... columns)
            throws IOException
    {
        return LoadData(hTable, Bytes.toString(hTable.getName().getName()), rowKey, filter, columns);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.client.hbase.Reader#loadAll(org.apache.hadoop.hbase.client
     * .HTable, org.apache.hadoop.hbase.filter.Filter, byte[], byte[])
     */
    @Override
    public List<HBaseData> loadAll(Table hTable, Filter filter, byte[] startRow, byte[] endRow,
            String columnFamily, String qualifier, String[] columns) throws IOException
    {
        List<HBaseData> results = null;
        if (scanner == null)
        {
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
            scanner = hTable.getScanner(s);
            resultsIter = scanner.iterator();
        }
        return scanResults(null, results);
    }

    /**
     * @param filter
     * @param columnFamily
     * @param s
     */
    private void setScanCriteria(Filter filter, String columnFamily, String qualifier, Scan s, String[] columns)
    {

        if (filter != null && !filter.toString().equals("FilterList AND (0/0): []"))
        {
            s.setFilter(filter);
        }
        if (columnFamily != null && qualifier != null)
        {
            s.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier));
        }
        else if (columnFamily != null)
        {
            s.addFamily(Bytes.toBytes(columnFamily));
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

     * @return collection of scanned results.
     * @throws IOException
     */
    private List<HBaseData> scanResults(final String columnFamily, List<HBaseData> results) throws IOException
    {
        HBaseData data = null;

        if (fetchSize == null)
        {
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

            scanner = null;
            resultsIter = null;
        }
        return results;
    }

    @Override
    public Object[] scanRowKeys(final Table hTable, final Filter filter, final String columnFamilyName,
            final String columnName, final Class rowKeyClazz) throws IOException
    {
        List<Object> rowKeys = new ArrayList<Object>();

        if (scanner == null)
        {
            Scan s = new Scan();
            s.setFilter(filter);
            s.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName));
            scanner = hTable.getScanner(s);
            resultsIter = scanner.iterator();
        }

        if (fetchSize == null)
        {
            for (Result result : scanner)
            {
                for (KeyValue keyValue : result.list())
                {
                    rowKeys.add(HBaseUtils.fromBytes(keyValue.getRow(), rowKeyClazz));
                }
            }
        }
        if (rowKeys != null && !rowKeys.isEmpty())
        {
            return rowKeys.toArray(new Object[0]);
        }
        return null;
    }

    public List<HBaseData> loadAll(final Table hTable, final List<Object> rows, final String columnFamily,
            final String[] columns) throws IOException
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

    public void setFetchSize(final int fetchSize)
    {
        this.fetchSize = fetchSize;
    }

    /**
     * 
     * @return next element of HbaseData.
     */
    public HBaseData next()
    {
        HBaseData data = null;
        Result result = resultsIter.next();
        counter++;
        List<KeyValue> values = result.list();
        for (KeyValue value : values)
        {
            data = new HBaseData(new String(value.getFamily()), value.getRow());
            break;
        }
        data.setColumns(values);
        return data;
    }

    public boolean hasNext()
    {
        if (scanner == null)
        {
            return false;
        }
        else
        {
            if (fetchSize != null)
            {
                if (counter < fetchSize)
                {
                    return resultsIter.hasNext();
                }
            }
            else
            {
                return resultsIter.hasNext();
            }
        }
        return false;
    }

    public void reset()
    {
        scanner = null;
        fetchSize = null;
        resultsIter = null;
        counter = 0;
    }
}
