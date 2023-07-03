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
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
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
        client.admin().indices().refresh(Requests.refreshRequest(ConvoIndexConstants.META_INDEX_NAME)).actionGet();
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
        assert(index.initConvoMetaIndexIfAbsent());
    }

    /**
     * If the index tries to be initialized more than once does it break something
     * Also make sure that only one initialization happens
     */
    public void testConvoMetaIndexCanBeInitializedTwice() {
        assert(index.initConvoMetaIndexIfAbsent());
        assert(!index.initConvoMetaIndexIfAbsent());
    }

    /**
     * If the index tries to be initialized by different objects does it break anything
     * Also make sure that only one initialization happens
     */
    public void testConvoMetaIndexCanBeInitializedByDifferentObjects() {
        assert(index.initConvoMetaIndexIfAbsent());
        ConvoMetaIndex otherIndex = new ConvoMetaIndex(client, clusterService);
        assert(!otherIndex.initConvoMetaIndexIfAbsent());
    }

    /**
     * Can I add a new conversation to the index without crashong?
     */
    public void testCanAddNewConversation() {
        index.addNewConversation();
    }

    /**
     * Are conversation ids unique?
     */
    public void testConversationIDsAreUnique() {
        int numTries = 1000;
        HashSet<String> seenIDs = new HashSet<String>(numTries);
        for(int i = 0; i < numTries; i++){
            String id = index.addNewConversation();
            if(seenIDs.contains(id)) {
                throw new RuntimeException("Duplicate id found");
            }
            seenIDs.add(id);
        }
    }

    /**
     * If I add a conversation, that id shows up in the list of conversations
     */
    public void testConversationsCanBeListed() {
        String id = index.addNewConversation();
        refreshIndex();
        List<ConvoMeta> convos = index.listConversations();
        boolean foundConvo = false;
        for(ConvoMeta c: convos) {
            if(c.getId().equals(id)) {
                foundConvo = true;
            }
        }
        assert(foundConvo);
    }

    /**
     * When I touch a conversation, it should update the metadata
     */
    public void testConversationsCanGetHit() {
        String id = index.addNewConversation();
        refreshIndex();
        Instant pit = Instant.now().plus(432, ChronoUnit.MINUTES);
        index.hitConversation(id, pit);
        refreshIndex();
        ConvoMeta convo = index.listConversations().get(0);
        assert(convo.getId().equals(id));
        assert(convo.getLastHit().equals(pit));
        assert(convo.getLength() == 1);
    }


}