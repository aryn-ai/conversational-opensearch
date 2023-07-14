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
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
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
    private Client client;

    /**
     * Constructor
     * @param transportService for inter-node communications
     * @param actionFilters not sure what this is for tbh
     * @param cmHandler Handler for conversational memory operations
     * @param client OS Client for dealing with OS
     */
    @Inject
    public CreateConversationTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        ConversationalMemoryHandler cmHandler, 
        Client client
    ) {
        super(CreateConversationAction.NAME, transportService, actionFilters, CreateConversationRequest::new);
        this.cmHandler = cmHandler;
        this.client = client;
    }

    @Override
    protected void doExecute(Task task, CreateConversationRequest request, ActionListener<CreateConversationResponse> actionListener) {
        String name = request.getName();
        try (ThreadContext.StoredContext context = client.threadPool().getThreadContext().stashContext()) {
            ActionListener<CreateConversationResponse> internalListener = ActionListener.runBefore(actionListener, () -> context.restore());
            ActionListener<String> al = ActionListener.wrap(r -> {
                log.info(r.toString());
                internalListener.onResponse(new CreateConversationResponse(r));
            }, e -> {
                log.error(e.toString());
                internalListener.onFailure(e);
            });

            if(name == null) {
                cmHandler.createConversation(al);
            } else {
                cmHandler.createConversation(name, al);
            }
        } catch(Exception e) {
            log.error("Failed to create new conversation with name " + request.getName(), e);
            actionListener.onFailure(e);
        }
    }


}