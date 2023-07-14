/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.conversation;

import java.io.IOException;
import java.util.List;

import org.opensearch.client.node.NodeClient;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

/**
 * Rest Action for creating a conversation
 */
public class CreateConversationRestAction extends BaseRestHandler {
    private final static String CREATE_CONVERSATION_NAME = "conversational_create_conversation";

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(RestRequest.Method.POST, ActionConstants.CREATE_CONVERSATION_PATH)
        );
    }

    @Override
    public String getName() {
        return CREATE_CONVERSATION_NAME;
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        CreateConversationRequest ccRequest = CreateConversationRequest.fromRestRequest(request);
        return channel -> client.execute(CreateConversationAction.INSTANCE, ccRequest, new RestToXContentListener<>(channel));
    }

}