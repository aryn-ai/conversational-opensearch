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
 * Rest Handler for get Interactions
 */
public class GetInteractionsRestAction extends BaseRestHandler {
    private final static String GET_INTERACTIONS_NAME = "conversational_get_interactions";

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(RestRequest.Method.GET, ActionConstants.GET_INTERACTIONS_PATH)
        );
    }

    @Override
    public String getName() {
        return GET_INTERACTIONS_NAME;
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        GetInteractionsRequest giRequest = GetInteractionsRequest.fromRestRequest(request);
        return channel -> client.execute(GetInteractionsAction.INSTANCE, giRequest, new RestToXContentListener<>(channel));
    }
}