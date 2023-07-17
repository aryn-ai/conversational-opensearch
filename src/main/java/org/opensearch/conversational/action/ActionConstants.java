/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.conversational.action;

/**
 * Constants for conversational actions
 */
public class ActionConstants {

    /** name of conversation Id field in all responses */
    public final static String CONVO_ID_FIELD = "conversationId";

    /** name of list of conversations in all responses */
    public final static String RESPONSE_CONVO_LIST_FIELD = "conversations";
    /** name of list on interactions in all responses */
    public final static String RESPONSE_INTER_LIST_FIELD = "interactions";
    /** name of interaction Id field in all responses */
    public final static String RESPONSE_INTER_ID_FIELD = "interactionId";

    /** name of conversation name in all requests */
    public final static String REQUEST_CONVO_NAME_FIELD = "name";
    /** name of maxResults field name in all requests */
    public final static String REQUEST_MAX_RESULTS_FIELD = "max_results";
    /** name of input field in all requests */
    public final static String INPUT_FIELD = "input";
    /** name of prompt field in all requests */
    public final static String PROMPT_FIELD = "prompt";
    /** name of AI response field in all respopnses */
    public final static String AI_RESPONSE_FIELD = "response";
    /** name of agent field in all requests */
    public final static String AI_AGENT_FIELD = "agent";
    /** name of interaction attributes field in all requests */
    public final static String INTER_ATTRIBUTES_FIELD = "attributes";

    /** path for create conversation */
    public final static String CREATE_CONVERSATION_PATH = "/_plugins/conversational/memory";
    /** path for list conversations */
    public final static String LIST_CONVERSATIONS_PATH  = "/_plugins/conversational/memory";
    /** path for put interaction */
    public final static String CREATE_INTERACTION_PATH = "/_plugins/conversational/memory/{conversationId}";
}