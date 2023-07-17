/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.interaction;

import java.io.IOException;
import java.util.List;

import org.opensearch.client.node.NodeClient;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

/**
 * Rest action for adding a new interaction to a conversation
 */
public class PutInteractionRestAction extends BaseRestHandler {
    private final static String PUT_INTERACTION_NAME = "conversational_create_interaction";

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(RestRequest.Method.POST, ActionConstants.CREATE_INTERACTION_PATH)
        );
    }

    @Override
    public String getName() {
        return PUT_INTERACTION_NAME;
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)  throws IOException {
        PutInteractionRequest piRequest = PutInteractionRequest.fromRestRequest(request);
        return channel -> client.execute(PutInteractionAction.INSTANCE, piRequest, new RestToXContentListener<>(channel));
    }



}