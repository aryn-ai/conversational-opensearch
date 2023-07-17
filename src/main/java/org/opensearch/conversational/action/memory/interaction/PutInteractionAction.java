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
 * Action for adding and interaction to a conversation
 */
public class PutInteractionAction extends ActionType<PutInteractionResponse> {
    /** Instance of this */
    public static PutInteractionAction INSTANCE = new PutInteractionAction();
    /** Name of this */
    public static final String NAME = "cluster:admin/opensearch/conversational/interaction/create";

    private PutInteractionAction() { super(NAME, PutInteractionResponse::new); }
}