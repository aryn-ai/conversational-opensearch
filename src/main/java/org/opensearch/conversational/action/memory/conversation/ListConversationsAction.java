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
 * Action for listing all conversations in the index
 */
public class ListConversationsAction extends ActionType<ListConversationsResponse> {
    /** Instance of this */
    public static final ListConversationsAction INSTANCE = new ListConversationsAction();
    /** Name of this action. Has something to do with permissions I think?? */
    public static final String NAME = "cluster:admin/opensearch/conversational/conversation/list";

    private ListConversationsAction() { super(NAME, ListConversationsResponse::new); }
}
