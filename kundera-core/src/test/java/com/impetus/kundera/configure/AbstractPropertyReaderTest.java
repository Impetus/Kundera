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
package com.impetus.kundera.configure;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.impetus.kundera.configure.ClientProperties.DataStore;

/**
 * Abstract property reader test
 * 
 * @author Kuldeep Mishra
 * 
 */
public class AbstractPropertyReaderTest
{

    private PropertyReader reader;

    private final String pu = "PropertyTest";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        reader = new DummyPropertyReader();
        new PersistenceUnitConfiguration(pu).configure(null);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    /**
     * Test method for
     * {@link com.impetus.kundera.configure.AbstractPropertyReader#onParseXML(java.lang.String)}
     * .
     */
    @Test
    public void testParseXML()
    {
        ClientProperties cp = null;
        reader.read(pu);
        cp = DummyPropertyReader.dsmd.getClientProperties();
        Assert.assertNotNull(cp);
        Assert.assertNotNull(cp.getDatastores());
        Assert.assertEquals(3, cp.getDatastores().size());
        for (DataStore store : cp.getDatastores())
        {
            Assert.assertNotNull(store);
            Assert.assertNotNull(store.getName());
            // Test for hbase properties.
            if (store.getName().equalsIgnoreCase("HBase"))
            {
                Assert.assertNotNull(store.getSchemas());
                Assert.assertEquals(1, store.getSchemas().size());
                Assert.assertEquals("USERXYZ", store.getSchemas().get(0).getName());
                Assert.assertNotNull(store.getSchemas().get(0).getTables());
                Assert.assertEquals(2, store.getSchemas().get(0).getTables().size());
                Assert.assertEquals("age", store.getSchemas().get(0).getTables().get(0).getName());
                Assert.assertEquals("address", store.getSchemas().get(0).getTables().get(1).getName());
                Assert.assertNotNull(store.getSchemas().get(0).getTables().get(0).getProperties());
                Assert.assertNotNull(store.getSchemas().get(0).getTables().get(1).getProperties());
                Assert.assertEquals(5, store.getSchemas().get(0).getTables().get(0).getProperties().size());
                Assert.assertEquals(5, store.getSchemas().get(0).getTables().get(1).getProperties().size());
                Assert.assertNull(store.getSchemas().get(0).getDataCenters());
                Assert.assertNull(store.getSchemas().get(0).getSchemaProperties());
                Assert.assertNotNull(store.getConnection());
                Assert.assertNotNull(store.getConnection().getProperties());
                Assert.assertNull(store.getConnection().getServers());
                Assert.assertEquals(2, store.getConnection().getProperties().size());

            }
            // Test for mongo properties.
            else if (store.getName().equalsIgnoreCase("Mongo"))
            {
                Assert.assertNull(store.getSchemas());
                Assert.assertNotNull(store.getConnection());
                Assert.assertNotNull(store.getConnection().getProperties());
                Assert.assertEquals(2, store.getConnection().getProperties().size());
                Assert.assertNotNull(store.getConnection().getServers());
                Assert.assertEquals(2, store.getConnection().getServers().size());
            }
            // Test for cassandra properties.
            else if (store.getName().equalsIgnoreCase("Cassandra"))
            {
                Assert.assertNotNull(store.getSchemas());
                Assert.assertEquals(1, store.getSchemas().size());
                Assert.assertEquals("KunderaCassandraXmlTest", store.getSchemas().get(0).getName());
                Assert.assertNotNull(store.getSchemas().get(0).getTables());
                Assert.assertEquals(1, store.getSchemas().get(0).getTables().size());
                Assert.assertEquals("USERXYZ", store.getSchemas().get(0).getTables().get(0).getName());
                Assert.assertNotNull(store.getSchemas().get(0).getTables().get(0).getProperties());
                Assert.assertEquals(7, store.getSchemas().get(0).getTables().get(0).getProperties().size());
                Assert.assertNotNull(store.getSchemas().get(0).getDataCenters());
                Assert.assertEquals(2, store.getSchemas().get(0).getDataCenters().size());
                Assert.assertNotNull(store.getSchemas().get(0).getSchemaProperties());
                Assert.assertEquals(3, store.getSchemas().get(0).getSchemaProperties().size());
            }
        }
    }
}
