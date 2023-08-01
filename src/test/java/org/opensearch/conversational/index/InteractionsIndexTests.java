/*
 * Copyright Aryn, Inc 2023
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.conversational.index;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.opensearch.action.ActionListener;
import org.opensearch.action.LatchedActionListener;
import org.opensearch.action.StepListener;
import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.test.OpenSearchIntegTestCase;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 2)
public class InteractionsIndexTests extends OpenSearchIntegTestCase {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConvoMetaIndexTests.class);

    private Client client;
    private ClusterService clusterService;
    private InteractionsIndex index;


    @Before
    public void setup() {
        client = client();
        clusterService = clusterService();
        index = new InteractionsIndex(client, clusterService);
    }

    /**
     * Test the index intialization logic - can I create the index exactly once (while trying 3 times)
     */
    public void testInteractionsIndexCanBeInitialized() {
        log.info("testing index creation logic of the index object");
        CountDownLatch cdl = new CountDownLatch(3);
        index.initInteractionsIndexIfAbsent(new LatchedActionListener<>(ActionListener.wrap(
            r -> {assert(r);}, e -> {assert(false);}
        ), cdl));
        index.initInteractionsIndexIfAbsent(new LatchedActionListener<>(ActionListener.wrap(
            r -> {assert(r);}, e -> {assert(false);}
        ), cdl));
        InteractionsIndex otherIndex = new InteractionsIndex(client, clusterService);
        otherIndex.initInteractionsIndexIfAbsent(new LatchedActionListener<>(ActionListener.wrap(
            r -> {assert(r);}, e -> {assert(false);}
        ), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Make sure nothing breaks when I add an interaction, with and without timestamp,
     * and that the ids are different
     */
    public void testCanAddNewInteraction() {
        CountDownLatch cdl = new CountDownLatch(2);
        String[] ids = new String[2];
        index.addInteraction("test", "test input", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}",
            new LatchedActionListener<>(ActionListener.wrap(
                id -> {ids[0] = id;}, e -> {assert(false);}
            ), cdl));

        index.addInteraction("test", "test input", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}",
            new LatchedActionListener<>(ActionListener.wrap(
                id -> {ids[1] = id;}, e -> {assert(false);}
            ), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
        assert(!ids[0].equals(ids[1]));
    }

    /**
     * Make sure I can get interactions out related to a conversation
     */
    public void testGetInteractions() {
        final String convo = "test-convo";
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> id1Listener = new StepListener<>();
        index.addInteraction(convo, "test input", "test prompt", "test response", 
            "test agent", "{\"test\":\"metadata\"}", id1Listener);

        StepListener<String> id2Listener = new StepListener<>();
        id1Listener.whenComplete(
            id -> {
                index.addInteraction(convo, "test input", "test prompt", "test response", "test agent", 
                "{\"test\":\"metadata\"}", Instant.now().plus(3, ChronoUnit.MINUTES), id2Listener);
            }, e -> {cdl.countDown(); assert(false);});

        StepListener<List<Interaction>> getListener = new StepListener<>();
        id2Listener.whenComplete(
            r -> {index.getInteractions(convo, 0, 2, getListener);}, 
            e -> {cdl.countDown(); assert(false);}
        );

        LatchedActionListener<List<Interaction>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            interactions -> {
                assert(interactions.size() == 2);
                assert(interactions.get(0).getId().equals(id2Listener.result()));
                assert(interactions.get(1).getId().equals(id1Listener.result()));
            }, e -> {assert(false);}
        ), cdl);
        getListener.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);
        
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    public void testGetInteractionPages() {
        final String convo = "test-convo";
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> id1Listener = new StepListener<>();
        index.addInteraction(convo, "test input", "test prompt", "test response", 
            "test agent", "{\"test\":\"metadata\"}", id1Listener);

        StepListener<String> id2Listener = new StepListener<>();
        id1Listener.whenComplete(
            id -> {
                index.addInteraction(convo, "test input1", "test prompt", "test response", "test agent", 
                "{\"test\":\"metadata\"}", Instant.now().plus(3, ChronoUnit.MINUTES), id2Listener);
            }, e -> {cdl.countDown(); assert(false);}
        );

        StepListener<String> id3Listener = new StepListener<>();
        id2Listener.whenComplete(
            id -> {
                index.addInteraction(convo, "test input2", "test prompt", "test response", "test agent", 
                "{\"test\":\"metadata\"}", Instant.now().plus(4, ChronoUnit.MINUTES), id3Listener);
            }, e -> {cdl.countDown(); assert(false);}
        );

        StepListener<List<Interaction>> getListener1 = new StepListener<>();
        id3Listener.whenComplete(
            r -> {index.getInteractions(convo, 0, 2, getListener1);}, 
            e -> {cdl.countDown(); assert(false);}
        );

        StepListener<List<Interaction>> getListener2 = new StepListener<>();
        getListener1.whenComplete(
            r -> {index.getInteractions(convo, 2, 2, getListener2);}, 
            e -> {cdl.countDown(); assert(false);}
        );

        LatchedActionListener<List<Interaction>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            interactions2 -> {
                List<Interaction> interactions1 = getListener1.result();
                String id1 = id1Listener.result();
                String id2 = id2Listener.result();
                String id3 = id3Listener.result();
                assert(interactions2.size() == 1);
                assert(interactions1.size() == 2);
                assert(interactions1.get(0).getId().equals(id3));
                assert(interactions1.get(1).getId().equals(id2));
                assert(interactions2.get(0).getId().equals(id1));
            }, e -> { assert(false); }
        ), cdl);
        getListener2.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);

        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    public void testDeleteConversation() {
        final String convo1 = "convo1";
        final String convo2 = "convo2";
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> iid1 = new StepListener<>();
        index.addInteraction(convo1, "test input", "test prompt", "test response", 
            "test agent", "{\"test\":\"metadata\"}", iid1);
        
        StepListener<String> iid2 = new StepListener<>();
        iid1.whenComplete(r -> {
            index.addInteraction(convo1, "test input", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2);
        }, e -> { cdl.countDown(); assert(false); });

        StepListener<String> iid3 = new StepListener<>();
        iid2.whenComplete(r -> {
            index.addInteraction(convo2, "test input", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid3);
        }, e -> { cdl.countDown(); assert(false); });

        StepListener<String> iid4 = new StepListener<>();
        iid3.whenComplete(r -> {
            index.addInteraction(convo1, "test input", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid4);
        }, e -> { cdl.countDown(); assert(false); });

        StepListener<Boolean> deleteListener = new StepListener<>();
        iid4.whenComplete(r -> {
            index.deleteConversation(convo1, deleteListener);
        }, e -> { cdl.countDown(); assert(false); });

        StepListener<List<Interaction>> interactions1 = new StepListener<>();
        deleteListener.whenComplete(success -> {
            if(success) {
                index.getInteractions(convo1, 0, 10, interactions1);
            } else {
                cdl.countDown();
                assert(false);
            }
        }, e -> {cdl.countDown(); assert(false); });

        StepListener<List<Interaction>> interactions2 = new StepListener<>();
        interactions1.whenComplete(interactions -> {
            index.getInteractions(convo2, 0, 10, interactions2); 
        }, e -> { cdl.countDown(); assert(false); });

        LatchedActionListener<List<Interaction>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            interactions -> {
                log.info("FINDME");
                log.info(interactions.toString());
                assert(interactions.size() == 1);
                assert(interactions.get(0).getId().equals(iid3.result()));
                assert(interactions1.result().size() == 0);
            }, e -> {assert(false);}
        ), cdl);
        interactions2.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);

        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }
}