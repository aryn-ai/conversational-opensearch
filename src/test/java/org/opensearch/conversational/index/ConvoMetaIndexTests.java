/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.index;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.opensearch.action.ActionListener;
import org.opensearch.action.LatchedActionListener;
import org.opensearch.action.StepListener;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.test.OpenSearchIntegTestCase;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 2)
public class ConvoMetaIndexTests extends OpenSearchIntegTestCase {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConvoMetaIndexTests.class);

    private ClusterService clusterService;
    private Client client;
    private ConvoMetaIndex index;

    private void refreshIndex() {
        client.admin().indices().refresh(Requests.refreshRequest(ConvoIndexConstants.META_INDEX_NAME));
    }

    @Before
    public void setup() {
        log.info("Setting up test");
        this.client = client();
        this.clusterService = clusterService();
        this.index = new ConvoMetaIndex(client, clusterService);
    }

    /**
     * Can the index be initialized?
     */
    public void testConvoMetaIndexCanBeInitialized() {
        CountDownLatch cdl = new CountDownLatch(1);
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>( ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * If the index tries to be initialized more than once does it break something
     * Also make sure that only one initialization happens
     */
    public void testConvoMetaIndexCanBeInitializedTwice() {
        CountDownLatch cdl = new CountDownLatch(2);
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * If the index tries to be initialized by different objects does it break anything
     * Also make sure that only one initialization happens
     */
    public void testConvoMetaIndexCanBeInitializedByDifferentObjects() {
        CountDownLatch cdl = new CountDownLatch(2);
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        ConvoMetaIndex otherIndex = new ConvoMetaIndex(client, clusterService);
        otherIndex.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Can I add a new conversation to the index without crashong?
     */
    public void testCanAddNewConversation() {
        CountDownLatch cdl = new CountDownLatch(1);
        index.addNewConversation(new LatchedActionListener<String>(ActionListener.wrap(r->{
            assert(r != null && r.length() > 0);
        }, e->{
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Are conversation ids unique?
     */
    public void testConversationIDsAreUnique() {
        int numTries = 10;
        CountDownLatch cdl = new CountDownLatch(numTries);
        Set<String> seenIds = Collections.synchronizedSet(new HashSet<String>(numTries));
        for(int i = 0; i < numTries; i++){
            index.addNewConversation(new LatchedActionListener<String>(ActionListener.wrap(r-> {
                assert(!seenIds.contains(r));
                seenIds.add(r);
            }, e-> {
                log.error(e);
                assert(false);
            }), cdl));
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * If I add a conversation, that id shows up in the list of conversations
     */
    public void testConversationsCanBeListed() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> addConvoListener = new StepListener<>();
        index.addNewConversation(addConvoListener);

        StepListener<List<ConvoMeta>> listConvoListener = new StepListener<>();
        addConvoListener.whenComplete(cid -> {
            refreshIndex();
            refreshIndex();
            index.listConversations(listConvoListener);
        }, e -> {
            log.error(e);
        });

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            convos -> {
                boolean foundConvo = false;
                log.info("FINDME");
                log.info(addConvoListener.result());
                log.info(convos);
                for(ConvoMeta c: convos) {
                    if(c.getId().equals(addConvoListener.result())) {
                        foundConvo = true;
                    }
                }
                assert(foundConvo);
            }, e -> {
                log.error(e);
            }
        ), cdl);
        listConvoListener.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * When I touch a conversation, it should update the metadata
     */
    public void testConversationsCanGetHitStepped() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> addConvoListener = new StepListener<>();
        index.addNewConversation(addConvoListener);
        
        Instant pit = Instant.now().plus(423, ChronoUnit.MINUTES);

        StepListener<Boolean> hitConvoListener = new StepListener<>();
        addConvoListener.whenComplete(cid -> {
            refreshIndex();
            index.hitConversation(cid, pit, hitConvoListener);
        }, e -> {
            log.error(e);
        });

        StepListener<List<ConvoMeta>> listConvoListener = new StepListener<>();
        hitConvoListener.whenComplete(b -> {
            refreshIndex();
            index.listConversations(listConvoListener);
        }, e -> {
            log.error(e);
        });

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            convos -> {
                ConvoMeta convo = convos.get(0);
                assert(convo.getId().equals(addConvoListener.result()));
                assert(convo.getLastHit().equals(pit));
                assert(convo.getLength() == 1);
            }, e -> {
                log.error(e);
            }
        ), cdl);
        listConvoListener.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }


}