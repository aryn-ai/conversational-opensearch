/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.conversational;


import org.junit.Before;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.conversational.index.ConvoIndexConstants;
import org.opensearch.conversational.index.ConvoMeta;
import org.opensearch.conversational.index.Interaction;
import org.opensearch.test.OpenSearchIntegTestCase;

import java.util.List;



@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 2)
public class ConversationalMemoryHandlerTests extends OpenSearchIntegTestCase {


    private Client client;
    private ClusterService clusterService;
    private ConversationalMemoryHandler cmHandler;

    @Before
    private void setup() {
        client = client();
        clusterService = clusterService();
        cmHandler = new ConversationalMemoryHandler(client, clusterService);
    }

    private void refreshIndices() {
        client.admin().indices().refresh(Requests.refreshRequest(ConvoIndexConstants.INTERACTIONS_INDEX_NAME)).actionGet();
        client.admin().indices().refresh(Requests.refreshRequest(ConvoIndexConstants.META_INDEX_NAME)).actionGet();
    }

    public void testCanStartConvos() {
        String cid0 = cmHandler.createConversation("test-1");
        String cid1 = cmHandler.createConversation("test-2");
        String cid2 = cmHandler.createConversation();
        assert(!cid0.equals(cid1) && !cid0.equals(cid2) && !cid1.equals(cid2));
    }

    public void testCanAddNewInteractionToConvo() {
        String cid = cmHandler.createConversation("test");
        String id1 = cmHandler.putInteraction(cid, "test input1", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        String id2 = cmHandler.putInteraction(cid, "test input2", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        assert(!id1.equals(id2));
    }

    public void testCanGetInteractionsBackOut() {
        String cid = cmHandler.createConversation("test");
        String id1 = cmHandler.putInteraction(cid, "test input1", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        String id2 = cmHandler.putInteraction(cid, "test input2", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        refreshIndices();
        List<Interaction> interactions = cmHandler.getInteractions(cid);
        assert(interactions.size() == 2);
        assert(interactions.get(0).getId().equals(id2));
        assert(interactions.get(1).getId().equals(id1));
        List<ConvoMeta> conversations = cmHandler.listConversations();
        assert(conversations.size() == 1);
        assert(conversations.get(0).getId().equals(cid));
    }

    public void testConvoMetaIsUpToDateWithHits() {
        String cid = cmHandler.createConversation("test");
        cmHandler.putInteraction(cid, "test input1", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        cmHandler.putInteraction(cid, "test input2", "test prompt", 
            "test response", "test agent", "{\"test\":\"metadata\"}");
        refreshIndices();
        List<Interaction> interactions = cmHandler.getInteractions(cid);
        List<ConvoMeta> conversations = cmHandler.listConversations();
        assert(conversations.size() == 1);
        ConvoMeta convo = conversations.get(0);
        assert(convo.getLastHit().equals(interactions.get(0).getTimestamp()));
        assert(convo.getLength() == 2);
    }

}