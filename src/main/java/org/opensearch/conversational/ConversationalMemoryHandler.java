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

import org.opensearch.action.ActionListener;
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
     * @param listener listener to wait for this op to finish, gets unique id of new conversation
     */
    public void createConversation(ActionListener<String> listener) {
        convoMetaIndex.addNewConversation(listener);
    }

    /**
     * Create a new conversation
     * @param name the name of the new conversation
     * @param listener listener to wait for this op to finish, gets unique id of new conversation
     */
    public void createConversation(String name, ActionListener<String> listener) {
        convoMetaIndex.addNewConversation(name, listener);
    }

    /**
     * Adds an interaction to the conversation indicated, updating the conversational metadata
     * @param conversationId the conversation to add the interaction to
     * @param input the human input for the interaction
     * @param prompt the prompt template used in this interaction
     * @param response the Gen AI response for this interaction
     * @param agent the name of the GenAI agent in this interaction
     * @param metadata arbitrary JSON string of extra stuff
     * @param listener gets the ID of the new interaction
     */
    public void putInteraction(
        String conversationId, 
        String input,
        String prompt,
        String response,
        String agent,
        String metadata,
        ActionListener<String> listener
    ) {
        Instant time = Instant.now();
        convoMetaIndex.hitConversation(conversationId, time, ActionListener.wrap(r->{}, e->{}));
        interactionsIndex.addInteraction(
            conversationId, input, prompt, 
            response, agent, metadata, time, listener
        );
    }

    /**
     * Get the interactions associate with this conversation, sorted by recency
     * @param conversationId the conversation whose interactions to get
     * @param from where to start listiing from
     * @param maxResults how many interactions to get
     * @param listener gets the list of interactions in this conversation, sorted by recency
     */
    public void getInteractions(String conversationId, int from, int maxResults, ActionListener<List<Interaction>> listener) {
        interactionsIndex.getInteractions(conversationId, from, maxResults, listener);
    }

    /**
     * Get all conversations (not the interactions in them, just the headers)
     * @param from where to start listing from
     * @param maxResults how many conversations to list
     * @param listener gets the list of all conversations, sorted by recency
     */
    public void listConversations(int from, int maxResults, ActionListener<List<ConvoMeta>> listener) {
        convoMetaIndex.listConversations(from, maxResults, listener);
    }

    /**
     * Get all conversations (not the interactions in them, just the headers)
     * @param maxResults how many conversations to get
     * @param listener receives the list of conversations, sorted by recency
     */
    public void listConversations(int maxResults, ActionListener<List<ConvoMeta>> listener) {
        convoMetaIndex.listConversations(maxResults, listener);
    }




}