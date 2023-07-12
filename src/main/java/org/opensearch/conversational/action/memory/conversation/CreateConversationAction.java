/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.conversation;

import org.opensearch.action.ActionType;

/**
 * Action for creating a new conversation in the index
 */
public class CreateConversationAction extends ActionType<CreateConversationResponse> {
    /** Instance of this */
    public static final CreateConversationAction INSTANCE = new CreateConversationAction();
    /** Name of this action. Has something to do with permissions I think??? */
    public static final String NAME = "cluster:admin/opensearch/conversational/conversation/create";

    private CreateConversationAction() { super(NAME, CreateConversationResponse::new); }
}