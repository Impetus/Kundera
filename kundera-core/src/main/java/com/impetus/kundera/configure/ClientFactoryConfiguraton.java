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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.impetus.kundera.client.ClientResolver;
import com.impetus.kundera.utils.KunderaCoreUtils;

/**
 * The Class ClientFactoryConfiguration load client metadata.
 * 
 * @author kuldeep.mishra
 * 
 */
public class ClientFactoryConfiguraton implements Configuration
{
    /** The log instance. */
    private static Logger log = LoggerFactory.getLogger(ClientFactoryConfiguraton.class);

    /** Holding instance for persistence units. */
    private String[] persistenceUnits;

    /** Holding persistenceUnit properties */
    private Map externalProperties;

    /**
     * Constructor parameterised with persistence units.
     * 
     * @param persistenceUnits
     *            persistence units.
     */
    public ClientFactoryConfiguraton(Map puProperties, String... persistenceUnits)
    {
        this.persistenceUnits = persistenceUnits;
        this.externalProperties = puProperties;
    }

    @Override
    public void configure()
    {
        // Invoke Client Loaders

        for (String pu : persistenceUnits)
        {
            log.info("Loading Client(s) For Persistence Unit(s) " + pu);

            Map<String, Object> puProperty = KunderaCoreUtils.getExternalProperties(pu, externalProperties,
                    persistenceUnits);

            ClientResolver.getClientFactory(pu, puProperty).load(pu, puProperty);
        }
    }
}
