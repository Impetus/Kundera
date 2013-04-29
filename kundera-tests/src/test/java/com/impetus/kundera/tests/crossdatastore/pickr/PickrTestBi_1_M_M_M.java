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
package com.impetus.kundera.tests.crossdatastore.pickr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.ColumnDef;
import org.apache.cassandra.thrift.IndexType;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KsDef;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SchemaDisagreementException;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.impetus.kundera.tests.cli.CassandraCli;
import com.impetus.kundera.tests.crossdatastore.pickr.entities.album.AlbumBi_1_M_M_M;
import com.impetus.kundera.tests.crossdatastore.pickr.entities.photo.PhotoBi_1_M_M_M;
import com.impetus.kundera.tests.crossdatastore.pickr.entities.photographer.PhotographerBi_1_M_M_M;

/**
 * @author amresh.singh
 * 
 */
public class PickrTestBi_1_M_M_M extends PickrBaseTest
{
    private static Log log = LogFactory.getLog(PickrTestBi_1_M_M_M.class);

    @Before
    public void setUp() throws Exception
    {
        log.info("Executing PICKR Test: " + this.getClass().getSimpleName() + "\n======"
                + "==========================================================");
        super.setUp();
    }

    /**
     * Tear down.
     * 
     * @throws Exception
     *             the exception
     */
    @After
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Test
    public void test()
    {
        executeTests();
    }

    @Override
    public void addPhotographer()
    {
        PhotographerBi_1_M_M_M p = populatePhotographer();
        pickr.addPhotographer(p);
    }

    @Override
    protected void getPhotographer()
    {
        PhotographerBi_1_M_M_M p = (PhotographerBi_1_M_M_M) pickr.getPhotographer(PhotographerBi_1_M_M_M.class,
                photographerId);
        assertPhotographer(p);

    }

    @Override
    protected void updatePhotographer()
    {
        PhotographerBi_1_M_M_M p = (PhotographerBi_1_M_M_M) pickr.getPhotographer(PhotographerBi_1_M_M_M.class,
                photographerId);
        assertPhotographer(p);
        p.setPhotographerName("Vivek");

        pickr.mergePhotographer(p);

        PhotographerBi_1_M_M_M p2 = (PhotographerBi_1_M_M_M) pickr.getPhotographer(PhotographerBi_1_M_M_M.class,
                photographerId);
        assertModifiedPhotographer(p2);
    }

    @Override
    protected void getAllPhotographers()
    {
        List<Object> ps = pickr.getAllPhotographers(PhotographerBi_1_M_M_M.class.getSimpleName());
        PhotographerBi_1_M_M_M p = (PhotographerBi_1_M_M_M) ps.get(0);

        assertModifiedPhotographer(p);

    }

    @Override
    protected void deletePhotographer()
    {
        PhotographerBi_1_M_M_M p = (PhotographerBi_1_M_M_M) pickr.getPhotographer(PhotographerBi_1_M_M_M.class,
                photographerId);
        assertModifiedPhotographer(p);
        pickr.deletePhotographer(p);
        PhotographerBi_1_M_M_M p2 = (PhotographerBi_1_M_M_M) pickr.getPhotographer(PhotographerBi_1_M_M_M.class,
                photographerId);
        Assert.assertNull(p2);

    }

    private PhotographerBi_1_M_M_M populatePhotographer()
    {
        PhotographerBi_1_M_M_M p = new PhotographerBi_1_M_M_M();
        p.setPhotographerId(photographerId);
        p.setPhotographerName("Amresh");

        AlbumBi_1_M_M_M album1 = new AlbumBi_1_M_M_M("album_1", "My Phuket Vacation", "Went Phuket with friends");
        AlbumBi_1_M_M_M album2 = new AlbumBi_1_M_M_M("album_2", "Office Pics", "Annual office party photos");

        PhotoBi_1_M_M_M photo1 = new PhotoBi_1_M_M_M("photo_1", "One beach", "On beach with friends");
        PhotoBi_1_M_M_M photo2 = new PhotoBi_1_M_M_M("photo_2", "In Hotel", "Chilling out in room");
        PhotoBi_1_M_M_M photo3 = new PhotoBi_1_M_M_M("photo_3", "At Airport", "So tired");
        PhotoBi_1_M_M_M photo4 = new PhotoBi_1_M_M_M("photo_4", "Office Team event", "Shot at Fun park");
        PhotoBi_1_M_M_M photo5 = new PhotoBi_1_M_M_M("photo_5", "My Team", "My team is the best");

        album1.addPhoto(photo1);
        album1.addPhoto(photo2);
        album1.addPhoto(photo3);
        album1.addPhoto(photo4);

        album2.addPhoto(photo2);
        album2.addPhoto(photo3);
        album2.addPhoto(photo4);
        album2.addPhoto(photo5);

        photo1.addAlbum(album1);
        photo2.addAlbum(album1);
        photo2.addAlbum(album2);
        photo3.addAlbum(album1);
        photo3.addAlbum(album2);
        photo4.addAlbum(album1);
        photo4.addAlbum(album2);
        photo5.addAlbum(album2);

        album1.setPhotographer(p);
        album2.setPhotographer(p);

        p.addAlbum(album1);
        p.addAlbum(album2);

        return p;

    }

    private void assertPhotographer(PhotographerBi_1_M_M_M p)
    {
        Assert.assertNotNull(p);
        Assert.assertEquals(1, p.getPhotographerId());
        Assert.assertEquals("Amresh", p.getPhotographerName());

        Assert.assertNotNull(p.getAlbums());
        Assert.assertFalse(p.getAlbums().isEmpty());
        Assert.assertEquals(2, p.getAlbums().size());

        // Album 1
        AlbumBi_1_M_M_M album1 = p.getAlbums().get(0);
        Assert.assertNotNull(album1);
        Assert.assertTrue(album1.getAlbumId().equals("album_1") || album1.getAlbumId().equals("album_2"));

        Assert.assertFalse(album1.getAlbumName().length() == 0);
        Assert.assertFalse(album1.getAlbumDescription().length() == 0);

        PhotographerBi_1_M_M_M revP = album1.getPhotographer();
        Assert.assertNotNull(revP);
        Assert.assertEquals(photographerId, revP.getPhotographerId());

        List<PhotoBi_1_M_M_M> album1Photos = album1.getPhotos();
        Assert.assertNotNull(album1Photos);
        Assert.assertFalse(album1Photos.isEmpty());
        Assert.assertFalse(album1Photos.size() < 2);

        PhotoBi_1_M_M_M album1Photo1 = album1Photos.get(0);
        Assert.assertNotNull(album1Photo1);
        Assert.assertEquals(7, album1Photo1.getPhotoId().length());

        Set<AlbumBi_1_M_M_M> revAlbums1 = album1Photo1.getAlbums();
        Assert.assertNotNull(revAlbums1);
        Assert.assertFalse(revAlbums1.isEmpty());
        Assert.assertFalse(revAlbums1.size() < 1);

        // Album 2
        AlbumBi_1_M_M_M album2 = p.getAlbums().get(1);
        Assert.assertNotNull(album2);
        Assert.assertTrue(album2.getAlbumId().equals("album_1") || album2.getAlbumId().equals("album_2"));

        Assert.assertFalse(album2.getAlbumName().length() == 0);
        Assert.assertFalse(album2.getAlbumDescription().length() == 0);

        PhotographerBi_1_M_M_M revP2 = album1.getPhotographer();
        Assert.assertNotNull(revP2);
        Assert.assertEquals(photographerId, revP2.getPhotographerId());

        List<PhotoBi_1_M_M_M> album2Photos = album2.getPhotos();
        Assert.assertNotNull(album2Photos);
    }

    private void assertModifiedPhotographer(PhotographerBi_1_M_M_M p)
    {
        Assert.assertNotNull(p);
        Assert.assertEquals(1, p.getPhotographerId());
        Assert.assertEquals("Vivek", p.getPhotographerName());

        Assert.assertNotNull(p.getAlbums());
        Assert.assertFalse(p.getAlbums().isEmpty());
        Assert.assertEquals(2, p.getAlbums().size());

        // Album 1
        AlbumBi_1_M_M_M album1 = p.getAlbums().get(0);
        Assert.assertNotNull(album1);
        Assert.assertTrue(album1.getAlbumId().equals("album_1") || album1.getAlbumId().equals("album_2"));

        Assert.assertFalse(album1.getAlbumName().length() == 0);
        Assert.assertFalse(album1.getAlbumDescription().length() == 0);

        PhotographerBi_1_M_M_M revP = album1.getPhotographer();
        Assert.assertNotNull(revP);
        Assert.assertEquals(photographerId, revP.getPhotographerId());

        List<PhotoBi_1_M_M_M> album1Photos = album1.getPhotos();
        Assert.assertNotNull(album1Photos);
        Assert.assertFalse(album1Photos.isEmpty());
        Assert.assertFalse(album1Photos.size() < 2);

        PhotoBi_1_M_M_M album1Photo1 = album1Photos.get(0);
        Assert.assertNotNull(album1Photo1);
        Assert.assertEquals(7, album1Photo1.getPhotoId().length());

        Set<AlbumBi_1_M_M_M> revAlbums1 = album1Photo1.getAlbums();
        Assert.assertNotNull(revAlbums1);
        Assert.assertFalse(revAlbums1.isEmpty());
        Assert.assertFalse(revAlbums1.size() < 1);

        // Album 2
        AlbumBi_1_M_M_M album2 = p.getAlbums().get(1);
        Assert.assertNotNull(album2);
        Assert.assertTrue(album2.getAlbumId().equals("album_1") || album2.getAlbumId().equals("album_2"));

        Assert.assertFalse(album2.getAlbumName().length() == 0);
        Assert.assertFalse(album2.getAlbumDescription().length() == 0);

        PhotographerBi_1_M_M_M revP2 = album1.getPhotographer();
        Assert.assertNotNull(revP2);
        Assert.assertEquals(photographerId, revP2.getPhotographerId());

        List<PhotoBi_1_M_M_M> album2Photos = album2.getPhotos();
        Assert.assertNotNull(album2Photos);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.kundera.tests.crossdatastore.pickr.PickrBaseTest#startServer
     * ()
     */
    @Override
    protected void createCassandraSchema() throws IOException, TException, InvalidRequestException,
            UnavailableException, TimedOutException, SchemaDisagreementException
    {

        /**
         * schema generation for cassandra.
         * */

        KsDef ksDef = null;

        CfDef pCfDef = new CfDef();
        pCfDef.name = "PHOTOGRAPHER";
        pCfDef.keyspace = "Pickr";
        pCfDef.setComparator_type("UTF8Type");
        pCfDef.setDefault_validation_class("UTF8Type");
        ColumnDef pColumnDef2 = new ColumnDef(ByteBuffer.wrap("PHOTOGRAPHER_NAME".getBytes()), "UTF8Type");
        pColumnDef2.index_type = IndexType.KEYS;
        pCfDef.addToColumn_metadata(pColumnDef2);

        CfDef aCfDef = new CfDef();
        aCfDef.name = "ALBUM";
        aCfDef.keyspace = "Pickr";
        aCfDef.setComparator_type("UTF8Type");
        aCfDef.setDefault_validation_class("UTF8Type");
        ColumnDef columnDef = new ColumnDef(ByteBuffer.wrap("ALBUM_NAME".getBytes()), "UTF8Type");
        columnDef.index_type = IndexType.KEYS;
        ColumnDef columnDef3 = new ColumnDef(ByteBuffer.wrap("ALBUM_DESC".getBytes()), "UTF8Type");
        columnDef3.index_type = IndexType.KEYS;
        ColumnDef columnDef5 = new ColumnDef(ByteBuffer.wrap("PHOTOGRAPHER_ID".getBytes()), "UTF8Type");
        columnDef5.index_type = IndexType.KEYS;
        aCfDef.addToColumn_metadata(columnDef);
        aCfDef.addToColumn_metadata(columnDef5);
        aCfDef.addToColumn_metadata(columnDef3);

        CfDef photoLinkCfDef = new CfDef();
        photoLinkCfDef.name = "PHOTO";
        photoLinkCfDef.keyspace = "Pickr";
        photoLinkCfDef.setComparator_type("UTF8Type");
        photoLinkCfDef.setDefault_validation_class("UTF8Type");
        ColumnDef columnDef1 = new ColumnDef(ByteBuffer.wrap("PHOTO_CAPTION".getBytes()), "UTF8Type");
        columnDef1.index_type = IndexType.KEYS;
        ColumnDef columnDef2 = new ColumnDef(ByteBuffer.wrap("PHOTO_DESC".getBytes()), "UTF8Type");
        columnDef2.index_type = IndexType.KEYS;
        photoLinkCfDef.addToColumn_metadata(columnDef1);
        photoLinkCfDef.addToColumn_metadata(columnDef2);

        CfDef cfDef = new CfDef();
        cfDef.name = "ALBUM_PHOTO";
        cfDef.keyspace = "Pickr";
        cfDef.setComparator_type("UTF8Type");
        cfDef.setDefault_validation_class("UTF8Type");
        ColumnDef columnDef4 = new ColumnDef(ByteBuffer.wrap("PHOTO_ID".getBytes()), "UTF8Type");
        columnDef1.index_type = IndexType.KEYS;
        ColumnDef columnDef6 = new ColumnDef(ByteBuffer.wrap("ALBUM_ID".getBytes()), "UTF8Type");
        columnDef2.index_type = IndexType.KEYS;
        cfDef.addToColumn_metadata(columnDef4);
        cfDef.addToColumn_metadata(columnDef6);

        List<CfDef> cfDefs = new ArrayList<CfDef>();
        cfDefs.add(pCfDef);
        cfDefs.add(aCfDef);
        cfDefs.add(cfDef);
        cfDefs.add(photoLinkCfDef);
        try
        {
            ksDef = CassandraCli.client.describe_keyspace("Pickr");
            CassandraCli.client.set_keyspace("Pickr");
            List<CfDef> cfDefn = ksDef.getCf_defs();

            for (CfDef cfDef1 : cfDefn)
            {

                if (cfDef1.getName().equalsIgnoreCase("PHOTOGRAPHER"))
                {
                    CassandraCli.client.system_drop_column_family("PHOTOGRAPHER");
                }
                if (cfDef1.getName().equalsIgnoreCase("ALBUM"))
                {
                    CassandraCli.client.system_drop_column_family("ALBUM");
                }
                if (cfDef1.getName().equalsIgnoreCase("PHOTO"))
                {
                    CassandraCli.client.system_drop_column_family("PHOTO");
                }
                if (cfDef1.getName().equalsIgnoreCase("ALBUM_PHOTO"))
                {
                    CassandraCli.client.system_drop_column_family("ALBUM_PHOTO");
                }
            }
            CassandraCli.client.system_add_column_family(pCfDef);
            CassandraCli.client.system_add_column_family(aCfDef);
            CassandraCli.client.system_add_column_family(photoLinkCfDef);
            CassandraCli.client.system_add_column_family(cfDef);
        }
        catch (NotFoundException e)
        {
            addKeyspace(ksDef, cfDefs);
        }
        catch (InvalidRequestException e)
        {
            log.error(e.getMessage());
        }
        catch (TException e)
        {
            log.error(e.getMessage());
        }

        /**
         * schema generation for cassandra.
         * */

        // HBaseCli.createTable("PHOTOGRAPHER");
        // HBaseCli.addColumnFamily("PHOTOGRAPHER", "PHOTOGRAPHER_NAME");
        //
        // HBaseCli.createTable("PHOTO");
        // HBaseCli.addColumnFamily("PHOTO", "PHOTO_CAPTION");
        // HBaseCli.addColumnFamily("PHOTO", "PHOTO_DESC");
        //
        // HBaseCli.createTable("ALBUM");
        // HBaseCli.addColumnFamily("ALBUM", "ALBUM_NAME");
        // HBaseCli.addColumnFamily("ALBUM", "ALBUM_DESC");
        // HBaseCli.addColumnFamily("ALBUM", "PHOTOGRAPHER_ID");
        //
        // HBaseCli.createTable("ALBUM_PHOTO");
        // HBaseCli.addColumnFamily("ALBUM_PHOTO", "PHOTO_ID");
        // HBaseCli.addColumnFamily("ALBUM_PHOTO", "ALBUM_ID");

    }
}
