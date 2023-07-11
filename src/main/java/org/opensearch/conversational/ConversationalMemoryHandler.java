/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.conversational;

import java.time.Instant;
import java.util.List;

import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.conversational.index.ConvoMeta;
import org.opensearch.conversational.index.ConvoMetaIndex;
import org.opensearch.conversational.index.Interaction;
import org.opensearch.conversational.index.InteractionsIndex;

/**
 * Class for handling all Conversational Memory operactions
 */
public class ConversationalMemoryHandler {
    //private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConversationalMemoryHandler.class);

    private ConvoMetaIndex convoMetaIndex;
    private InteractionsIndex interactionsIndex;

    /**
     * Constructor
     * @param client opensearch client to use for talking to OS
     * @param clusterService ClusterService object for managing OS
     */
    public ConversationalMemoryHandler(Client client, ClusterService clusterService) {
        this.convoMetaIndex = new ConvoMetaIndex(client, clusterService);
        this.interactionsIndex = new InteractionsIndex(client, clusterService);
    }

    /**
     * Create a new conversation
     * @return the unique id of the new conversation
     */
    public String createConversation() {
        return convoMetaIndex.addNewConversation();
    }

    /**
     * Create a new conversation
     * @param name the name of the new conversation
     * @return the unique id of the new conversation
     */
    public String createConversation(String name) {
        return convoMetaIndex.addNewConversation(name);
    }

    /**
     * Adds an interaction to the conversation indicated, updating the conversational metadata
     * @param conversationId the conversation to add the interaction to
     * @param input the human input for the interaction
     * @param prompt the prompt template used in this interaction
     * @param response the Gen AI response for this interaction
     * @param agent the name of the GenAI agent in this interaction
     * @param metadata arbitrary JSON string of extra stuff
     * @return the ID of the new interaction
     */
    public String putInteraction(
        String conversationId, 
        String input,
        String prompt,
        String response,
        String agent,
        String metadata
    ) {
        Instant time = Instant.now();
        convoMetaIndex.hitConversation(conversationId, time);
        return interactionsIndex.addInteraction(
            conversationId, input, prompt, 
            response, agent, metadata, time
        );
    }

    /**
     * Get the interactions associate with this conversation, sorted by recency
     * @param conversationId the conversation whose interactions to get
     * @return the list of interactions in this conversation, sorted by recency
     */
    public List<Interaction> getInteractions(String conversationId) {
        return interactionsIndex.getInteractions(conversationId);
    }

    /**
     * Get all conversations (not the interactions in them, just the headers)
     * @return the list of all conversations, sorted by recency
     */
    public List<ConvoMeta> listConversations() {
        return convoMetaIndex.listConversations();
    }




}