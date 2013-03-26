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
package com.impetus.kundera.index;

import java.util.Map;

/**
 * Indexer interface. Any custom implementation for this interface can be plugged-in by configuring kundera.indexer.class property
 * in persistence.xml. Once this is enabled in persistence provider, Kundera will automatically delegate index related requests to 
 * configure indexer interface implementation but will keep functioning for any database specific requests. 
 * For example, developer may rely upon custom index implementation for inverted indexes(e.g. @Id attributes) but entity data population
 * will be handled by Kundera
 * 
 * @author vivek.mishra
 *
 */
public interface Indexer
{

    /**
     * Index a document for given entity class and collection of values.
     * 
     * @param entityClazz entity class
     * 
     * @param values map of values containing field name as key and it's value.
     */
    void index(final Class entityClazz, final Map<String, Object> values);

    /**
     * Executes lucene query and returns inverted indices as output.
     * 
     * @param queryString  lucene query.
     * @param start        start counter
     * @param end          end counter
     * @return             collection containing stored index value.   
     */
    Map<String, Object> search(final String queryString, int start, int count);

    /**
     * Deletes index for given entity class.
     * 
     * @param entityClazz entity class
     * 
     * @param key  stored index value
     */
    void unIndex(final Class entityClazz, final Object key);

    void close();
}
