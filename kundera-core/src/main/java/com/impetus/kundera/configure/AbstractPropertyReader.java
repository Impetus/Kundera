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

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.impetus.kundera.PersistenceProperties;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.model.PersistenceUnitMetadata;
import com.thoughtworks.xstream.XStream;

/**
 * Abstract property reader parse xml or properties on the basis of
 * {@code PropertyType}
 * 
 * @author Kuldeep Mishra
 * 
 */
public abstract class AbstractPropertyReader
{
    /** The log instance. */
    private Log log = LogFactory.getLog(AbstractPropertyReader.class);

    /** The xStream instance */
    private XStream xStream;

    protected PersistenceUnitMetadata puMetadata;

    /**
     * Reads property file which is given in persistence unit
     * 
     * @param pu
     */
    final public void read(String pu)
    {
        puMetadata = KunderaMetadataManager.getPersistenceUnitMetadata(pu);
        String propertyFileName = puMetadata != null ? puMetadata
                .getProperty(PersistenceProperties.KUNDERA_CLIENT_PROPERTY) : null;

        if (propertyFileName != null && PropertyType.value(propertyFileName).equals(PropertyType.xml))
        {
            onXml(onParseXML(propertyFileName, puMetadata));
        }
    }

    /**
     * If property file is xml.
     * 
     * @param propertyFileName
     * @param puMetadata
     * @return
     */
    private ClientProperties onParseXML(String propertyFileName, PersistenceUnitMetadata puMetadata)
    {
        InputStream inStream = puMetadata.getClassLoader().getResourceAsStream(propertyFileName);
        xStream = getXStreamObject();
        if (inStream != null)
        {
            Object o = xStream.fromXML(inStream);
            return (ClientProperties) o;
        }
        return null;
    }

    /**
     * get XStream Object.
     * 
     * @return XStream object.
     */
    private XStream getXStreamObject()
    {
        if (xStream == null)
        {
            XStream stream = new XStream();
            stream.alias("clientProperties", ClientProperties.class);
            stream.alias("dataStore", ClientProperties.DataStore.class);
            stream.alias("schema", ClientProperties.DataStore.Schema.class);
            stream.alias("table", ClientProperties.DataStore.Schema.Table.class);
            stream.alias("dataCenter", ClientProperties.DataStore.Schema.DataCenter.class);
            stream.alias("connection", ClientProperties.DataStore.Connection.class);
            stream.alias("server", ClientProperties.DataStore.Connection.Server.class);
            return stream;
        }
        else
        {
            return xStream;
        }
    }

    /**
     * property type emun.
     * 
     * @author Kuldeep Mishra
     * 
     */
    private enum PropertyType
    {
        xml, properties;

        private static final String DELIMETER = ".";

        /**
         * Check for allowed property format.
         * 
         * @param propertyFileName
         * @return
         */
        static PropertyType value(String propertyFileName)
        {
            if (isXml(propertyFileName))
            {
                return xml;
            }
            throw new IllegalArgumentException("unsupported property provided format: " + propertyFileName);
        }

        /**
         * Check for property is xml.
         * 
         * @param propertyFileName
         * @return
         */
        private static boolean isXml(String propertyFileName)
        {
            return propertyFileName.endsWith(DELIMETER + xml);
        }
    }

    /**
     * If property is xml.
     * 
     * @param cp
     */
    protected abstract void onXml(ClientProperties cp);
}
