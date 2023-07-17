/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.interaction;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.conversational.ConversationalMemoryHandler;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * The put interaction action that does the work (of calling cmHandler)
 */
public class PutInteractionTransportAction extends HandledTransportAction<PutInteractionRequest, PutInteractionResponse> {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(PutInteractionTransportAction.class);

    private ConversationalMemoryHandler cmHandler;
    private Client client;

    /**
     * Constructor
     * @param transportService for doing intra-cluster communication
     * @param actionFilters not sure what this is for
     * @param cmHandler handler for conversational memory
     * @param client client for general opensearch ops
     */
    @Inject
    public PutInteractionTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        ConversationalMemoryHandler cmHandler, 
        Client client
    ) {
        super(PutInteractionAction.NAME, transportService, actionFilters, PutInteractionRequest::new);
        this.client = client;
        this.cmHandler = cmHandler;
    }

    @Override
    protected void doExecute(Task task, PutInteractionRequest request, ActionListener<PutInteractionResponse> actionListener) {
        String cid = request.getConversationId();
        String inp = request.getInput();
        String prp = request.getPrompt();
        String rsp = request.getResponse();
        String agt = request.getAgent();
        String att = request.getAttributes();
        try (ThreadContext.StoredContext context = client.threadPool().getThreadContext().stashContext()) {
            ActionListener<PutInteractionResponse> internalListener = ActionListener.runBefore(actionListener, () -> context.restore());
            ActionListener<String> al = ActionListener.wrap(iid -> {
                internalListener.onResponse(new PutInteractionResponse(iid));
            }, e -> {
                internalListener.onFailure(e);
            });
            cmHandler.putInteraction(cid, inp, prp, rsp, agt, att, al);
        } catch (Exception e) {
            log.error(e.toString());
            actionListener.onFailure(e);
        }
    }


}