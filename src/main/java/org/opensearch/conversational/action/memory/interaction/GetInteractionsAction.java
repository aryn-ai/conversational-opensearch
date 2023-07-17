/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.interaction;

import org.opensearch.action.ActionType;

/**
 * Action to return the interactions associated with a conversation
 */
public class GetInteractionsAction extends ActionType<GetInteractionsResponse> {
    /** Instance of this */
    public static final GetInteractionsAction INSTANCE = new GetInteractionsAction();
    /** Name of this action. Has something to do with permissions I think?? */
    public static final String NAME = "cluste:admin/opensearch/conversational/conversation/get";

    private GetInteractionsAction() { super(NAME, GetInteractionsResponse::new);}

}
