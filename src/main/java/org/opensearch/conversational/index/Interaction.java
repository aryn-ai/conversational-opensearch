/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.index;

import java.time.Instant;
import java.util.Map;

import org.opensearch.search.SearchHit;

/**
 * Class for dealing with Interactions
 */
public class Interaction {

    private String id;
    private Instant timestamp;
    private String convoId;
    private String input;
    private String prompt;
    private String response;
    private String agent;
    private String metadata;

    /**
     * Constructor
     * @param id id of THIS interaction object in the index
     * @param timestamp when this interaction happened
     * @param convoId id of the conversation this interaction belongs to
     * @param input human input in this interaction
     * @param prompt prompt template used in this interaction
     * @param response GenAI response from this interaction
     * @param agent name of the agent used in this interaction
     * @param metadata arbitrary metadata
     */
    public Interaction(
        String id,
        Instant timestamp,
        String convoId,
        String input,
        String prompt,
        String response,
        String agent,
        String metadata
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.convoId = convoId;
        this.input = input;
        this.prompt = prompt;
        this.response = response;
        this.agent = agent;
        this.metadata = metadata;
    }

    /**
     * Creates an Interaction object from a map of fields in the OS index
     * @param id the Interaction id
     * @param fields the field mapping from the OS document
     * @return a new Interaction object representing the OS document
     */
    public static Interaction fromMap(String id, Map<String, Object> fields) {
        Instant timestamp = Instant.parse((String) fields.get(ConvoIndexConstants.INTERACTIONS_TIMESTAMP_FIELD));
        String convoId   = (String) fields.get(ConvoIndexConstants.INTERACTIONS_CONVO_ID_FIELD);
        String input     = (String) fields.get(ConvoIndexConstants.INTERACTIONS_INPUT_FIELD);
        String prompt    = (String) fields.get(ConvoIndexConstants.INTERACTIONS_PROMPT_FIELD);
        String response  = (String) fields.get(ConvoIndexConstants.INTERACTIONS_RESPONSE_FIELD);
        String agent     = (String) fields.get(ConvoIndexConstants.INTERACTIONS_AGENT_FIELD);
        String metadata  = (String) fields.get(ConvoIndexConstants.INTERACTIONS_METADATA_FIELD);
        return new Interaction(id, timestamp, convoId, input, prompt, response, agent, metadata);
    }

    /**
     * Creates an Interaction object from a search hit
     * @param hit the search hit from the interactions index
     * @return a new Interaction object representing the search hit
     */
    public static Interaction fromSearchHit(SearchHit hit) {
        String id = hit.getId();
        return fromMap(id, hit.getSourceAsMap());
    }

    /**
     * @return this interaction's unique ID
     */
    public String getId() {
        return id;
    }
    /**
     * @return this interaction's timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    /**
     * @return the id of the conversation this interaction belongs to
     */
    public String getConvoId() {
        return convoId;
    }
    /**
     * @return the humna input from this interaction
     */
    public String getInput() {
        return input;
    }
    /**
     * @return the prompt template used in this interaction
     */
    public String getPrompt() {
        return prompt;
    }
    /**
     * @return the GenAI response from this interaction
     */
    public String getResponse() {
        return response;
    }
    /**
     * @return the name of the agent used in this interaction
     */
    public String getAgent() {
        return agent;
    }
    /**
     * @return an arbitrary JSON blob stored as part of this interaction
     */
    public String getMetadata() {
        return metadata;
    }
    

}