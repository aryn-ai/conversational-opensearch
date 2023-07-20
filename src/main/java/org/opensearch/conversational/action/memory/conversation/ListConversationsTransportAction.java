/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.conversation;

import java.util.List;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.conversational.ConversationalMemoryHandler;
import org.opensearch.conversational.index.ConvoMeta;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * ListConversationsAction that does the work of asking for them from the ConversationalMemoryHandler
 */
public class ListConversationsTransportAction extends HandledTransportAction<ListConversationsRequest, ListConversationsResponse> {
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
    public ListConversationsTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        ConversationalMemoryHandler cmHandler, 
        Client client
    ) {
        super(ListConversationsAction.NAME, transportService, actionFilters, ListConversationsRequest::new);
        this.client = client;
        this.cmHandler = cmHandler;
    }

    @Override
    public void doExecute(Task task, ListConversationsRequest request, ActionListener<ListConversationsResponse> actionListener) {
        int maxResults = request.getMaxResults();
        int from = request.getFrom();
        try (ThreadContext.StoredContext context = client.threadPool().getThreadContext().stashContext()) {
            ActionListener<ListConversationsResponse> internalListener = ActionListener.runBefore(actionListener, () -> context.restore());
            ActionListener<List<ConvoMeta>> al = ActionListener.wrap(conversations -> {
                internalListener.onResponse(new ListConversationsResponse(conversations, from + maxResults, conversations.size() == maxResults));
            }, e -> {
                log.error(e.toString());
                internalListener.onFailure(e);
            });
            cmHandler.listConversations(from, maxResults, al);
        } catch (Exception e) {
            log.error(e.toString());
            actionListener.onFailure(e);
        }
    }
}