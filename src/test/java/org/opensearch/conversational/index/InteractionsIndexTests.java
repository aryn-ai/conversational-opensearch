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
            }, e -> {assert(false);});

        StepListener<List<Interaction>> getListener = new StepListener<>();
        id2Listener.whenComplete(
            r -> {index.getInteractions(convo, getListener);}, e -> {assert(false);});

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
}