/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.interaction;

import java.util.List;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.conversational.ConversationalMemoryHandler;
import org.opensearch.conversational.action.memory.conversation.ListConversationsTransportAction;
import org.opensearch.conversational.index.Interaction;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Get Interactions action that does the work of calling stuff
 */
public class GetInteractionsTransportAction extends HandledTransportAction<GetInteractionsRequest, GetInteractionsResponse> {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ListConversationsTransportAction.class);

    private Client client;
    private ConversationalMemoryHandler cmHandler;

    /**
     * Constructor
     * @param transportService for inter-node communications
     * @param actionFilters not sure what this is for tbh
     * @param cmHandler Handler for conversational memory operations
     * @param client OS Client for dealing with OS
     */
    @Inject
    public GetInteractionsTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        ConversationalMemoryHandler cmHandler, 
        Client client
    ) {
        super(GetInteractionsAction.NAME, transportService, actionFilters, GetInteractionsRequest::new);
        this.client = client;
        this.cmHandler = cmHandler;
    }

    @Override
    public void doExecute(Task task, GetInteractionsRequest request, ActionListener<GetInteractionsResponse> actionListener) {
        int maxResults = request.getMaxResults();
        int from = request.getFrom();
        try (ThreadContext.StoredContext context = client.threadPool().getThreadContext().stashContext()) {
            ActionListener<GetInteractionsResponse> internalListener = ActionListener.runBefore(actionListener, () -> context.restore());
            ActionListener<List<Interaction>> al = ActionListener.wrap(interactions -> {
                internalListener.onResponse(new GetInteractionsResponse(interactions, from + maxResults, interactions.size() == maxResults));
            }, e -> {
                internalListener.onFailure(e);
            });
            cmHandler.getInteractions(request.getConversationId(), from, maxResults, al);
        } catch(Exception e) {
            log.error(e.toString());
            actionListener.onFailure(e);
        }

    }

}
