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
    public final static String RESPONSE_CONVO_ID_FIELD = "conversationId";

    /** name of conversation name in all requests */
    public final static String REQUEST_CONVO_NAME_FIELD = "name";

    /** path for create conversation */
    public final static String CREATE_CONVERSATION_PATH = "/_plugins/conversational/memory";
}