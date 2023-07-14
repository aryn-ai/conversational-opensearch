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
 * Rest Handler for list conversations
 */
public class ListConversationsRestAction extends BaseRestHandler {
    private final static String LIST_CONVERSATION_NAME = "conversational_list_conversations";

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(RestRequest.Method.GET, ActionConstants.LIST_CONVERSATIONS_PATH)
        );
    }

    @Override
    public String getName() {
        return LIST_CONVERSATION_NAME;
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        ListConversationsRequest lcRequest = ListConversationsRequest.fromRestRequest(request);
        return channel -> client.execute(ListConversationsAction.INSTANCE, lcRequest, new RestToXContentListener<>(channel));
    }
}