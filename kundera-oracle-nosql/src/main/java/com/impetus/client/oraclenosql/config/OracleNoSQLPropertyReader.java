/**
 * Copyright 2013 Impetus Infotech.
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
package com.impetus.client.oraclenosql.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.impetus.kundera.configure.AbstractPropertyReader;
import com.impetus.kundera.configure.ClientProperties;
import com.impetus.kundera.configure.ClientProperties.DataStore;
import com.impetus.kundera.configure.PropertyReader;

/**
 * XML Property reader for OracleNoSQL specific configuration
 * 
 * @author amresh.singh
 */
public class OracleNoSQLPropertyReader extends AbstractPropertyReader implements PropertyReader
{
    /** log instance */
    private static Log log = LogFactory.getLog(OracleNoSQLPropertyReader.class);

    /** OracleNoSQL schema metadata instance */
    public static OracleNoSQLSchemaMetadata osmd;

    public OracleNoSQLPropertyReader()
    {
        osmd = new OracleNoSQLSchemaMetadata();
    }

    /**
     * Sets Client properties from XML configuration file into
     * {@link OracleNoSQLSchemaMetadata}
     */
    @Override
    protected void onXml(ClientProperties cp)
    {
        if (cp != null)
        {
            osmd.setClientProperties(cp);
        }
    }

    /**
     * Holds property related to OracleNoSQL specific configuration file
     * 
     * @author Amresh
     * 
     */
    public class OracleNoSQLSchemaMetadata
    {
        private static final String ORACLE_NOSQL_DATASTORE = "oraclenosql";

        private ClientProperties clientProperties;

        public OracleNoSQLSchemaMetadata()
        {

        }

        /**
         * @return the clientProperties
         */
        public ClientProperties getClientProperties()
        {
            return clientProperties;
        }

        /**
         * @param clientProperties
         *            the clientProperties to set
         */
        private void setClientProperties(ClientProperties clientProperties)
        {
            this.clientProperties = clientProperties;
        }

        /**
         * Returns datastore instance for given {@link ClientProperties} for
         * OracleNoSQL
         * 
         * @return
         */
        public DataStore getDataStore()
        {
            if (getClientProperties() != null && getClientProperties().getDatastores() != null)
            {
                for (DataStore dataStore : getClientProperties().getDatastores())
                {
                    if (dataStore.getName() != null && dataStore.getName().equalsIgnoreCase(ORACLE_NOSQL_DATASTORE))
                    {
                        return dataStore;
                    }
                }
            }
            return null;
        }
    }

}
