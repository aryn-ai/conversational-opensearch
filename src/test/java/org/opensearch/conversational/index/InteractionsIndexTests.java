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

import org.junit.Before;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.test.OpenSearchIntegTestCase;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 2)
public class InteractionsIndexTests extends OpenSearchIntegTestCase {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConvoMetaIndexTests.class);

    private Client client;
    private ClusterService clusterService;
    private InteractionsIndex index;

    private void refreshIndex() {
        client.admin().indices().refresh(Requests.refreshRequest(ConvoIndexConstants.INTERACTIONS_INDEX_NAME)).actionGet();
    }

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
        assert(index.initInteractionsIndexIfAbsent());
        assert(!index.initInteractionsIndexIfAbsent());
        InteractionsIndex otherIndex = new InteractionsIndex(client, clusterService);
        assert(!otherIndex.initInteractionsIndexIfAbsent());
    }

    /**
     * Make sure nothing breaks when I add an interaction, with and without timestamp
     */
    public void testCanAddNewInteraction() {
        String id1 = index.addInteraction("test", "test input", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        String id2 = index.addInteraction("test", "test input", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}", Instant.now());
        assert(!id1.equals(id2));
    }

    /**
     * Make sure I can get interactions out related to a conversation
     */
    public void testGetInteractions() {
        final String convo = "test-convo";
        String id1 = index.addInteraction(convo, "test input", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        String id2 = index.addInteraction(convo, "test input", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}", Instant.now().plus(3, ChronoUnit.MINUTES));
        refreshIndex();
        List<Interaction> interactions = index.getInteractions(convo);
        assert(interactions.size() == 2);
        assert(interactions.get(0).getId().equals(id2));
        assert(interactions.get(1).getId().equals(id1));
    }
}