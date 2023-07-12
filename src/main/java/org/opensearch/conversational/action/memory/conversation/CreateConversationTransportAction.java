/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.conversation;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.conversational.ConversationalMemoryHandler;
import org.opensearch.conversational.index.ConvoMetaIndex;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * The CreateConversationAction that actually does all of the work
 */
public class CreateConversationTransportAction extends HandledTransportAction<CreateConversationRequest, CreateConversationResponse> {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConvoMetaIndex.class);

    private ConversationalMemoryHandler cmHandler;

    /**
     * Constructor
     * @param transportService for inter-node communications
     * @param actionFilters not sure what this is for tbh
     * @param cmHandler Handler for conversational memory operations
     */
    @Inject
    public CreateConversationTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        ConversationalMemoryHandler cmHandler
    ) {
        super(CreateConversationAction.NAME, transportService, actionFilters, CreateConversationRequest::new);
        this.cmHandler = cmHandler;
    }

    @Override
    protected void doExecute(Task task, CreateConversationRequest request, ActionListener<CreateConversationResponse> actionListener) {
        try {
            String name = request.getName();
            String convoId;
            if(name == null) {
                convoId = cmHandler.createConversation();
            } else {
                convoId = cmHandler.createConversation(name);
            }
            CreateConversationResponse response = new CreateConversationResponse(convoId);
            actionListener.onResponse(response);
        } catch(Exception e) {
            log.error("Failed to create new conversation with name " + request.getName(), e);
            actionListener.onFailure(e);
        }
    }


}