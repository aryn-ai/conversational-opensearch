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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.conversational.ConversationalMemoryHandler;
import org.opensearch.conversational.index.ConvoMeta;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

public class GetConversationsTransportActionTests extends OpenSearchTestCase {
    @Mock
    ThreadPool threadPool;

    @Mock
    Client client;

    @Mock
    ClusterService clusterService;

    @Mock
    NamedXContentRegistry xContentRegistry;

    @Mock
    TransportService transportService;

    @Mock
    ActionFilters actionFilters;

    @Mock
    ActionListener<GetConversationsResponse> actionListener;

    @Mock
    ConversationalMemoryHandler cmHandler;

    GetConversationsRequest request;
    GetConversationsTransportAction action;
    ThreadContext threadContext;

    @Before
    public void setup() throws IOException {
        this.threadPool = Mockito.mock(ThreadPool.class);
        this.client = Mockito.mock(Client.class);
        this.clusterService = Mockito.mock(ClusterService.class);
        this.xContentRegistry = Mockito.mock(NamedXContentRegistry.class);
        this.transportService = Mockito.mock(TransportService.class);
        this.actionFilters = Mockito.mock(ActionFilters.class);
        @SuppressWarnings("unchecked")
        ActionListener<GetConversationsResponse> al = (ActionListener<GetConversationsResponse>) Mockito.mock(ActionListener.class);
        this.actionListener = al;
        this.cmHandler = Mockito.mock(ConversationalMemoryHandler.class);

        this.request = new GetConversationsRequest();
        this.action = spy(new GetConversationsTransportAction(transportService, actionFilters, cmHandler, client));

        Settings settings = Settings.builder().build();
        this.threadContext = new ThreadContext(settings);
        when(this.client.threadPool()).thenReturn(this.threadPool);
        when(this.threadPool.getThreadContext()).thenReturn(this.threadContext);
    }

    public void testListConversations() {
        List<ConvoMeta> testResult = List.of(
            new ConvoMeta("testcid1", Instant.now(), Instant.now(), 0, "", null),
            new ConvoMeta("testcid2", Instant.now(), Instant.now().minus(2, ChronoUnit.MINUTES), 4, "testname", null)
        );
        doAnswer(invocation -> {
            ActionListener<List<ConvoMeta>> listener = invocation.getArgument(2);
            listener.onResponse(testResult);
            return null;
        }).when(cmHandler).getConversations(anyInt(), anyInt(), any());
        action.doExecute(null, request, actionListener);
        ArgumentCaptor<GetConversationsResponse> argCaptor = ArgumentCaptor.forClass(GetConversationsResponse.class);
        verify(actionListener).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult));
        assert(!argCaptor.getValue().hasMorePages());
    }

    public void testPagination() {
        List<ConvoMeta> testResult = List.of(
            new ConvoMeta("testcid1", Instant.now(), Instant.now(), 0, "", null),
            new ConvoMeta("testcid2", Instant.now(), Instant.now().minus(2, ChronoUnit.MINUTES), 4, "testname", null),
            new ConvoMeta("testcid3", Instant.now(), Instant.now().minus(3, ChronoUnit.MINUTES), 4, "testname", null)
        );
        doAnswer(invocation -> {
            ActionListener<List<ConvoMeta>> listener = invocation.getArgument(2);
            int maxResults = invocation.getArgument(1);
            if(maxResults <= 3) {
                listener.onResponse(testResult.subList(0, maxResults));
            } else {
                listener.onResponse(testResult);
            }
            return null;
        }).when(cmHandler).getConversations(anyInt(), anyInt(), any());
        GetConversationsRequest r0 = new GetConversationsRequest(2);
        action.doExecute(null, r0, actionListener);
        ArgumentCaptor<GetConversationsResponse> argCaptor = ArgumentCaptor.forClass(GetConversationsResponse.class);
        verify(actionListener).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult.subList(0, 2)));
        assert(argCaptor.getValue().hasMorePages());
        assert(argCaptor.getValue().getNextToken() == 2);

        @SuppressWarnings("unchecked")
        ActionListener<GetConversationsResponse> al1 = (ActionListener<GetConversationsResponse>) Mockito.mock(ActionListener.class);
        GetConversationsRequest r1 = new GetConversationsRequest(2, 2);
        action.doExecute(null, r1, al1);
        argCaptor = ArgumentCaptor.forClass(GetConversationsResponse.class);
        verify(al1).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult.subList(0,2)));
        assert(argCaptor.getValue().hasMorePages());
        assert(argCaptor.getValue().getNextToken() == 4);

        @SuppressWarnings("unchecked")
        ActionListener<GetConversationsResponse> al2 = (ActionListener<GetConversationsResponse>) Mockito.mock(ActionListener.class);
        GetConversationsRequest r2 = new GetConversationsRequest(20, 4);
        action.doExecute(null, r2, al2);
        argCaptor = ArgumentCaptor.forClass(GetConversationsResponse.class);
        verify(al2).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult));
        assert(!argCaptor.getValue().hasMorePages());
    }
}