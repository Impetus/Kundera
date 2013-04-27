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
package com.impetus.client.oraclenosql;

/**
 * Utility class for handling read/ write of data from/ to Oracle NoSQL database
 * @author amresh.singh
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.persistence.PersistenceException;

import oracle.kv.DurabilityException;
import oracle.kv.FaultException;
import oracle.kv.KVStore;
import oracle.kv.Key;
import oracle.kv.KeyValueVersion;
import oracle.kv.Operation;
import oracle.kv.OperationExecutionException;
import oracle.kv.Version;
import oracle.kv.lob.InputStreamVersion;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.impetus.kundera.client.Client;


/**
 * Provides utility methods for handling data held in Oracle NoSQL KVstore.
 * @author amresh.singh
 */
public class OracleNoSQLDataHandler
{
    /** The client. */
    private Client client;
    
    private KVStore kvStore;

    /** The persistence unit. */
    private String persistenceUnit;

    /** The log. */
    private static Log log = LogFactory.getLog(OracleNoSQLDataHandler.class);
    
    
    /**
     * Instantiates a new mongo db data handler.
     *
     * @param client the client
     * @param persistenceUnit the persistence unit
     */
    public OracleNoSQLDataHandler(Client client, KVStore kvStore, String persistenceUnit)
    {
        super();
        this.client = client;
        this.kvStore = kvStore;
        this.persistenceUnit = persistenceUnit;
    }
    
    public void execute(List<Operation> operations)
    {
        if(operations != null && ! operations.isEmpty())
        {            
            try
            {
                kvStore.execute(operations);
            }
            catch (DurabilityException e)
            {
                log.error(e);
                throw new PersistenceException("Error while Persisting data using batch", e);
            }
            catch (OperationExecutionException e)
            {
                log.error(e);
                throw new PersistenceException("Error while Persisting data using batch", e);
            }
            catch (FaultException e)
            {
                log.error(e);
                throw new PersistenceException("Error while Persisting data using batch", e);
            }
            finally
            {
                operations.clear();
            }
        }        
    }
    
    /**
     * @param keyValueVersion
     * @param fileName
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public File getLOBFile(KeyValueVersion keyValueVersion, String fileName) throws FileNotFoundException, IOException
    {
        InputStreamVersion istreamVersion = kvStore.getLOB(keyValueVersion.getKey(), OracleNOSQLConstants.DEFAULT_CONSISTENCY, 5,
                OracleNOSQLConstants.DEFAULT_TIME_UNIT);
        InputStream is = istreamVersion.getInputStream();

        File lobFile = new File(fileName);
        OutputStream os = new FileOutputStream(lobFile);
        int read = 0;
        byte[] bytes = new byte[OracleNOSQLConstants.OUTPUT_BUFFER_SIZE];
        while ((read = is.read(bytes)) != -1)
        {
            os.write(bytes, 0, read);
        }
        return lobFile;
    }

    /**
     * @param minorKeyFirstPart
     * @return
     */
    public String removeLOBSuffix(String minorKeyFirstPart)
    {
        if (minorKeyFirstPart.endsWith(OracleNOSQLConstants.LOB_SUFFIX))
        {
            minorKeyFirstPart = minorKeyFirstPart.substring(0, minorKeyFirstPart.length() - OracleNOSQLConstants.LOB_SUFFIX.length());
        }
        return minorKeyFirstPart;
    }
    
    /**
     * Saves LOB file to Oracle KV Store
     * 
     * @param key
     * @param lobFile
     */
    public void saveLOBFile(Key key, File lobFile)
    {
        try
        {
            FileInputStream fis = new FileInputStream(lobFile);
            Version version = kvStore.putLOB(key, fis, OracleNOSQLConstants.DEFAULT_DURABILITY, OracleNOSQLConstants.DEFAULT_WRITE_TIMEOUT_SECONDS, OracleNOSQLConstants.DEFAULT_TIME_UNIT);
        }
        catch (FileNotFoundException e)
        {
            log.warn("Unable to find file " + lobFile + ". This is being omitted. Details:" + e.getMessage());
        }
        catch (IOException e)
        {
            log.warn("IOException while writing file " + lobFile + ". This is being omitted. Details:" + e.getMessage());
        }
    }
    
    /**
     * @param idString
     * @return
     */
    private String getNextString(String idString)
    {
        char lastChar = (char)idString.charAt(idString.length() - 1);
        String idString2 = idString.substring(0, idString.length() - 1) + (char)((int)lastChar + 1);
        return idString2;
    }
    
}