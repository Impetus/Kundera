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
package com.impetus.client.twitter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SchemaDisagreementException;
import org.apache.thrift.TException;
import org.junit.Assert;

import com.impetus.client.persistence.CassandraCli;
import com.impetus.client.twitter.dao.TwitterCassandra;
import com.impetus.client.twitter.dao.TwitterServiceCassandra;
import com.impetus.client.twitter.entities.ExternalLinkCassandra;
import com.impetus.client.twitter.entities.PersonalDetailCassandra;
import com.impetus.client.twitter.entities.PreferenceCassandra;
import com.impetus.client.twitter.entities.ProfessionalDetailCassandra;
import com.impetus.client.twitter.entities.TweetCassandra;
import com.impetus.client.twitter.entities.UserCassandra;

/**
 * Test case for Cassandra Cassandra-cli commands for running with standalone
 * cassandra server: ********** Cassandra-cli commands ************* drop
 * keyspace KunderaExamples; create keyspace KunderaExamples; use
 * KunderaExamples; create column family USER with column_type=Super and
 * comparator=UTF8Type and subcomparator = AsciiType and
 * key_validation_class=UTF8Type; create column family USER_INVRTD_IDX with
 * column_type=Super and key_validation_class=UTF8Type; create column family
 * PREFERENCE; create column family EXTERNAL_LINK with comparator=UTF8Type and
 * default_validation_class=UTF8Type and key_validation_class=UTF8Type and
 * column_metadata=[{column_name: USER_ID, validation_class:UTF8Type,
 * index_type: KEYS}]; describe KunderaExamples;
 * 
 * @author amresh.singh
 */
public abstract class TwitterTestBaseCassandra
{
    public static final boolean RUN_IN_EMBEDDED_MODE = false;

    public static final boolean AUTO_MANAGE_SCHEMA = true;

    public static final String persistenceUnit = "twissandraTest";

    public static final String keyspace = "KunderaExamples";

    /** The user id1. */
    String userId1;

    /** The user id2. */
    String userId2;

    /** The twitter. */
    protected TwitterCassandra twitter;

    /**
     * Sets the up internal.
     * 
     * @param persistenceUnitName
     *            the new up internal
     * @throws Exception
     *             the exception
     */
    protected void setUpInternal(String persistenceUnitName) throws Exception
    {
        userId1 = "0001";
        userId2 = "0002";

        // Start Cassandra Server
        if (RUN_IN_EMBEDDED_MODE)
        {
            startServer();
        }

        // Create Schema
        if (AUTO_MANAGE_SCHEMA)
        {
            CassandraCli.initClient();
            createSchema();
        }
        twitter = new TwitterServiceCassandra(persistenceUnitName);

    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    /**
     * Tear down internal.
     * 
     * @throws Exception
     *             the exception
     */
    protected void tearDownInternal() throws Exception
    {
        if (twitter != null)
        {
            twitter.close();
        }

        if (AUTO_MANAGE_SCHEMA)
        {
            deleteSchema();
        }

        // Stop Server
        if (RUN_IN_EMBEDDED_MODE)
        {
            stopServer();
        }
    }

    /**
     * Execute suite.
     */
    protected void executeTwissandraTest()
    {
        // Insert, Find and Update
        addAllUserInfo();
        getUserById();
        updateUser();

        // Queries
        getPersonalDetailByName();
        getAllUsers();
        getTweetsByDevice();
        getTweetsByRelationshipAndDevice();
        getTweetsByUserIdAndDevice();

        // Remove Users
        removeUsers();
    }

    protected void addAllUserInfo()
    {
        UserCassandra user1 = buildUser1();
        UserCassandra user2 = buildUser2();

        twitter.createEntityManager();
        twitter.addUser(user1);
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addUser(user2);
        twitter.closeEntityManager();
    }

    protected void getUserById()
    {
        twitter.createEntityManager();
        UserCassandra user1 = twitter.findUserById(userId1);
        assertUser1(user1);

        UserCassandra user2 = twitter.findUserById(userId2);
        assertUser2(user2);
    }

    protected void updateUser()
    {
        twitter.createEntityManager();
        UserCassandra user1 = twitter.findUserById(userId1);
        assertUser1(user1);

        user1.setPersonalDetail(new PersonalDetailCassandra("Vivek", "unknown", "Married"));
        user1.addTweet(new TweetCassandra("My Third Tweet", "iPhone"));
        twitter.mergeUser(user1);

        UserCassandra user1AfterMerge = twitter.findUserById(userId1);

        assertUpdatedUser1(user1AfterMerge);

        twitter.closeEntityManager();
    }

    protected void removeUsers()
    {
        twitter.createEntityManager();
        UserCassandra user1 = twitter.findUserById(userId1);
        assertUpdatedUser1(user1);

        twitter.removeUser(user1);

        UserCassandra user1AfterRemoval = twitter.findUserById(userId1);
        Assert.assertNull(user1AfterRemoval);

        UserCassandra user2 = twitter.findUserById(userId2);
        assertUser2(user2);

        twitter.removeUser(user2);

        UserCassandra user2AfterRemoval = twitter.findUserById(userId2);
        Assert.assertNull(user2AfterRemoval);

        twitter.closeEntityManager();

    }

    protected void getAllUsers()
    {
        twitter.createEntityManager();
        List<UserCassandra> users = twitter.getAllUsers();
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertEquals(2, users.size());

        for (UserCassandra u : users)
        {
            Assert.assertNotNull(u);
            if (u.getUserId().equals(userId1))
            {
                assertUpdatedUser1(u);
            }
            else if (u.getUserId().equals(userId2))
            {
                assertUser2(u);
            }
        }
        twitter.closeEntityManager();
    }

    /**
     * Adds the users.
     */
    protected void addUsers()
    {
        twitter.createEntityManager();
        twitter.addUser(userId1, "Amresh", "password1", "married");
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addUser(userId2, "Saurabh", "password2", "single");
        twitter.closeEntityManager();
    }

    /**
     * Save preference.
     */
    protected void savePreference()
    {
        twitter.createEntityManager();
        twitter.savePreference(userId1, new PreferenceCassandra("P1", "Motif", "2"));
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.savePreference(userId2, new PreferenceCassandra("P2", "High Contrast", "3"));
        twitter.closeEntityManager();
    }

    /**
     * Adds the external links.
     */
    protected void addExternalLinks()
    {
        twitter.createEntityManager();
        twitter.addExternalLink(userId1, "L1", "Facebook", "http://facebook.com/coolnerd");
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addExternalLink(userId1, "L2", "LinkedIn", "http://linkedin.com/in/devilmate");
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addExternalLink(userId2, "L3", "GooglePlus", "http://plus.google.com/inviteme");
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addExternalLink(userId2, "L4", "Yahoo", "http://yahoo.com/profiles/itsmeamry");
        twitter.closeEntityManager();
    }

    /**
     * Adds the tweets.
     */
    protected void addTweets()
    {
        twitter.createEntityManager();
        twitter.addTweet(userId1, "Here is my first tweet", "Web");
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addTweet(userId1, "Second Tweet from me", "Mobile");
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addTweet(userId2, "Saurabh tweets for the first time", "Phone");
        twitter.closeEntityManager();

        twitter.createEntityManager();
        twitter.addTweet(userId2, "Another tweet from Saurabh", "text");
        twitter.closeEntityManager();
    }

    /**
     * User1 follows user2.
     */
    protected void user1FollowsUser2()
    {
        twitter.createEntityManager();
        twitter.startFollowing(userId1, userId2);
        twitter.closeEntityManager();
    }

    /**
     * User1 adds user2 as follower.
     */
    protected void user1AddsUser2AsFollower()
    {
        twitter.createEntityManager();
        twitter.addFollower(userId1, userId2);
        twitter.closeEntityManager();
    }

    /**
     * Gets the all tweets.
     * 
     * @return the all tweets
     */
    protected void getAllTweets()
    {
        twitter.createEntityManager();

        List<TweetCassandra> tweetsUser1 = twitter.getAllTweets(userId1);
        List<TweetCassandra> tweetsUser2 = twitter.getAllTweets(userId2);

        twitter.closeEntityManager();

        Assert.assertNotNull(tweetsUser1);
        Assert.assertNotNull(tweetsUser2);

        Assert.assertFalse(tweetsUser1.isEmpty());
        Assert.assertFalse(tweetsUser2.isEmpty());

        Assert.assertEquals(3, tweetsUser1.size());
        Assert.assertEquals(2, tweetsUser2.size());
    }

    public void getPersonalDetailByName()
    {
        twitter.createEntityManager();
        List<UserCassandra> users = twitter.findPersonalDetailByName("Saurabh");
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertTrue(users.size() == 1);

        UserCassandra user = users.get(0);
        Assert.assertNotNull(user);
        PersonalDetailCassandra pd = user.getPersonalDetail();
        Assert.assertNotNull(pd);
        Assert.assertTrue(pd.getPersonalDetailId() != null && !pd.getPersonalDetailId().trim().equals(""));
        Assert.assertTrue("Saurabh", pd.getName() != null);
        Assert.assertEquals("password2", pd.getPassword());
        Assert.assertEquals("single", pd.getRelationshipStatus());

        twitter.closeEntityManager();
    }

    /**
     * Gets the tweets by body.
     * 
     * @return the tweets by body
     */
    public void getTweetsByBody()
    {
        twitter.createEntityManager();
        List<TweetCassandra> user1Tweet = twitter.findTweetByBody("Here");
        List<TweetCassandra> user2Tweet = twitter.findTweetByBody("Saurabh");

        twitter.closeEntityManager();

        Assert.assertNotNull(user1Tweet);
        Assert.assertNotNull(user2Tweet);
        Assert.assertEquals(1, user1Tweet.size());
        Assert.assertEquals(1, user2Tweet.size());
    }

    /**
     * Gets the tweet by device.
     * 
     * @return the tweet by device
     */
    public void getTweetsByDevice()
    {
        twitter.createEntityManager();
        List<TweetCassandra> webTweets = twitter.findTweetByDevice("Web");
        List<TweetCassandra> mobileTweets = twitter.findTweetByDevice("Mobile");

        twitter.closeEntityManager();

        Assert.assertNotNull(webTweets);
        Assert.assertNotNull(mobileTweets);
        Assert.assertEquals(1, webTweets.size());
        Assert.assertEquals(1, mobileTweets.size());

    }

    public void getTweetsByRelationshipAndDevice()
    {
        // Positive scenario, record exists for user who is married and tweeted
        // from Web
        twitter.createEntityManager();
        List<UserCassandra> users = twitter.findByRelationshipAndDevice("married", "Web");
        twitter.closeEntityManager();

        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertTrue(users.size() == 1);
        UserCassandra user = users.get(0);

        Assert.assertFalse(user == null);
        Assert.assertEquals("0001", user.getUserId());
        List<TweetCassandra> tweets = user.getTweets();
        Assert.assertFalse(tweets == null);
        Assert.assertTrue(tweets.size() == 1);
        TweetCassandra tweet = tweets.get(0);
        Assert.assertNotNull(tweet);
        Assert.assertEquals("Web", tweet.getDevice());

        // Negative scenario, record doesn't exist for user who is single and
        // tweeted from mobile
        twitter.createEntityManager();
        List<UserCassandra> users2 = twitter.findByRelationshipAndDevice("single", "Mobile");
        twitter.closeEntityManager();
        Assert.assertTrue(users2 == null || users2.isEmpty());
    }

    public void getTweetsByUserIdAndDevice()
    {
        // Positive scenario, record exists for user1 who tweeted from Web
        twitter.createEntityManager();
        UserCassandra user = twitter.findByUserIdAndTweetDevice(userId1, "Web");
        twitter.closeEntityManager();
        Assert.assertNotNull(user);
        Assert.assertEquals(userId1, user.getUserId());
        List<TweetCassandra> tweets = user.getTweets();
        Assert.assertNotNull(tweets);
        Assert.assertFalse(tweets.isEmpty());
        Assert.assertTrue(tweets.size() == 1);
        Assert.assertEquals("Web", tweets.get(0).getDevice());

        // Negative scenario, User 2 never tweeted from Mobile
        twitter.createEntityManager();
        UserCassandra user2 = twitter.findByUserIdAndTweetDevice(userId2, "Mobile");
        twitter.closeEntityManager();
        Assert.assertNull(user2);
    }

    /**
     * Gets the all followers.
     * 
     * @return the all followers
     */
    protected void getAllFollowers()
    {
        twitter.createEntityManager();
        List<UserCassandra> follower1 = twitter.getFollowers(userId1);
        twitter.closeEntityManager();

        twitter.createEntityManager();
        List<UserCassandra> follower2 = twitter.getFollowers(userId2);
        twitter.closeEntityManager();

        Assert.assertNull(follower1);
        Assert.assertNotNull(follower2);
    }

    /**
     * @return
     */
    private UserCassandra buildUser1()
    {
        UserCassandra user1 = new UserCassandra(userId1, "Amresh", "password1", "married");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(1344079067777l));
        user1.setProfessionalDetail(new ProfessionalDetailCassandra(1234567, "Labs", false, 31, 'C', (byte) 8,
                (short) 5, (float) 10.0, 163.12, new Date(Long.parseLong("1344079065781")), new Date(Long
                        .parseLong("1344079067623")), new Date(Long.parseLong("1344079069105")), 2, new Long(
                        3634521523423L), new Double(0.23452342343), new java.sql.Date(new Date(Long
                        .parseLong("1344079061111")).getTime()), new java.sql.Time(new Date(Long
                        .parseLong("1344079062222")).getTime()), new java.sql.Timestamp(new Date(Long
                        .parseLong("13440790653333")).getTime()), new BigInteger("123456789"),
                new BigDecimal(123456789), cal));

        user1.setPreference(new PreferenceCassandra("P1", "Motif", "2"));

        user1.addExternalLink(new ExternalLinkCassandra("L1", "Facebook", "http://facebook.com/coolnerd"));
        user1.addExternalLink(new ExternalLinkCassandra("L2", "LinkedIn", "http://linkedin.com/in/devilmate"));

        user1.addTweet(new TweetCassandra("Here is my first tweet", "Web"));
        user1.addTweet(new TweetCassandra("Second Tweet from me", "Mobile"));
        return user1;
    }

    /**
     * @return
     */
    private UserCassandra buildUser2()
    {
        UserCassandra user2 = new UserCassandra(userId2, "Saurabh", "password2", "single");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(1344079068888l));
        user2.setProfessionalDetail(new ProfessionalDetailCassandra(1234568, "ODC", true, 32, 'A', (byte) 10,
                (short) 8, (float) 9.80, 323.3, new Date(Long.parseLong("1344079063412")), new Date(Long
                        .parseLong("1344079068266")), new Date(Long.parseLong("1344079061078")), 5, new Long(
                        25423452343L), new Double(0.76452343), new java.sql.Date(new Date(Long
                        .parseLong("1344079064444")).getTime()), new java.sql.Time(new Date(Long
                        .parseLong("1344079065555")).getTime()), new java.sql.Timestamp(new Date(Long
                        .parseLong("1344079066666")).getTime()), new BigInteger("123456790"),
                new BigDecimal(123456790), cal));

        user2.setPreference(new PreferenceCassandra("P2", "High Contrast", "3"));

        user2.addExternalLink(new ExternalLinkCassandra("L3", "GooglePlus", "http://plus.google.com/inviteme"));
        user2.addExternalLink(new ExternalLinkCassandra("L4", "Yahoo", "http://yahoo.com/profiles/itsmeamry"));

        user2.addTweet(new TweetCassandra("Saurabh tweets for the first time", "Phone"));
        user2.addTweet(new TweetCassandra("Another tweet from Saurabh", "text"));
        return user2;
    }

    private void assertUser1(UserCassandra user1)
    {
        Assert.assertNotNull(user1);
        Assert.assertEquals(userId1, user1.getUserId());
        Assert.assertNotNull(user1.getPersonalDetail());
        Assert.assertEquals("Amresh", user1.getPersonalDetail().getName());
        Assert.assertNotNull(user1.getPreference());
        Assert.assertEquals("2", user1.getPreference().getPrivacyLevel());
        Assert.assertNotNull(user1.getTweets());
        Assert.assertFalse(user1.getTweets().isEmpty());
        Assert.assertEquals(2, user1.getTweets().size());
        Assert.assertNotNull(user1.getExternalLinks());
        Assert.assertFalse(user1.getExternalLinks().isEmpty());
        Assert.assertEquals(2, user1.getExternalLinks().size());
    }

    private void assertUser2(UserCassandra user2)
    {
        Assert.assertNotNull(user2);
        Assert.assertEquals(userId2, user2.getUserId());
        Assert.assertNotNull(user2.getPersonalDetail());
        Assert.assertEquals("Saurabh", user2.getPersonalDetail().getName());
        Assert.assertNotNull(user2.getPreference());
        Assert.assertEquals("3", user2.getPreference().getPrivacyLevel());
        Assert.assertNotNull(user2.getTweets());
        Assert.assertFalse(user2.getTweets().isEmpty());
        Assert.assertEquals(2, user2.getTweets().size());
        Assert.assertNotNull(user2.getExternalLinks());
        Assert.assertFalse(user2.getExternalLinks().isEmpty());
        Assert.assertEquals(2, user2.getExternalLinks().size());
    }

    private void assertUpdatedUser1(UserCassandra user1)
    {
        Assert.assertNotNull(user1);
        Assert.assertEquals(userId1, user1.getUserId());
        Assert.assertNotNull(user1.getPersonalDetail());
        Assert.assertEquals("Vivek", user1.getPersonalDetail().getName());
        Assert.assertEquals("unknown", user1.getPersonalDetail().getPassword());
        Assert.assertNotNull(user1.getPreference());
        Assert.assertEquals("2", user1.getPreference().getPrivacyLevel());
        Assert.assertNotNull(user1.getTweets());
        Assert.assertFalse(user1.getTweets().isEmpty());
        Assert.assertEquals(3, user1.getTweets().size());
        Assert.assertNotNull(user1.getExternalLinks());
        Assert.assertFalse(user1.getExternalLinks().isEmpty());
        Assert.assertEquals(2, user1.getExternalLinks().size());
    }

    abstract void startServer();

    abstract void stopServer();

    abstract void deleteSchema();

    abstract void createSchema() throws NotFoundException, InvalidRequestException, TException,
            SchemaDisagreementException;

}
