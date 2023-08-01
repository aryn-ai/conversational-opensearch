/*
 * Copyright Aryn, Inc 2023
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class GetConversationsRestAction extends BaseRestHandler {
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
        GetConversationsRequest lcRequest = GetConversationsRequest.fromRestRequest(request);
        return channel -> client.execute(GetConversationsAction.INSTANCE, lcRequest, new RestToXContentListener<>(channel));
    }
}