/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
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

public class ListConversationsTransportActionTests extends OpenSearchTestCase {
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
    ActionListener<ListConversationsResponse> actionListener;

    @Mock
    ConversationalMemoryHandler cmHandler;

    ListConversationsRequest request;
    ListConversationsTransportAction action;
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
        ActionListener<ListConversationsResponse> al = (ActionListener<ListConversationsResponse>) Mockito.mock(ActionListener.class);
        this.actionListener = al;
        this.cmHandler = Mockito.mock(ConversationalMemoryHandler.class);

        this.request = new ListConversationsRequest();
        this.action = spy(new ListConversationsTransportAction(transportService, actionFilters, cmHandler, client));

        Settings settings = Settings.builder().build();
        this.threadContext = new ThreadContext(settings);
        when(this.client.threadPool()).thenReturn(this.threadPool);
        when(this.threadPool.getThreadContext()).thenReturn(this.threadContext);
    }

    public void testListConversations() {
        List<ConvoMeta> testResult = List.of(
            new ConvoMeta("testcid1", Instant.now(), Instant.now(), 0, ""),
            new ConvoMeta("testcid2", Instant.now(), Instant.now().minus(2, ChronoUnit.MINUTES), 4, "testname")
        );
        doAnswer(invocation -> {
            ActionListener<List<ConvoMeta>> listener = invocation.getArgument(2);
            listener.onResponse(testResult);
            return null;
        }).when(cmHandler).listConversations(anyInt(), anyInt(), any());
        action.doExecute(null, request, actionListener);
        ArgumentCaptor<ListConversationsResponse> argCaptor = ArgumentCaptor.forClass(ListConversationsResponse.class);
        verify(actionListener).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult));
        assert(!argCaptor.getValue().hasMorePages());
    }

    public void testPagination() {
        List<ConvoMeta> testResult = List.of(
            new ConvoMeta("testcid1", Instant.now(), Instant.now(), 0, ""),
            new ConvoMeta("testcid2", Instant.now(), Instant.now().minus(2, ChronoUnit.MINUTES), 4, "testname"),
            new ConvoMeta("testcid3", Instant.now(), Instant.now().minus(3, ChronoUnit.MINUTES), 4, "testname")
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
        }).when(cmHandler).listConversations(anyInt(), anyInt(), any());
        ListConversationsRequest r0 = new ListConversationsRequest(2);
        action.doExecute(null, r0, actionListener);
        ArgumentCaptor<ListConversationsResponse> argCaptor = ArgumentCaptor.forClass(ListConversationsResponse.class);
        verify(actionListener).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult.subList(0, 2)));
        assert(argCaptor.getValue().hasMorePages());
        assert(argCaptor.getValue().getNextToken() == 2);

        @SuppressWarnings("unchecked")
        ActionListener<ListConversationsResponse> al1 = (ActionListener<ListConversationsResponse>) Mockito.mock(ActionListener.class);
        ListConversationsRequest r1 = new ListConversationsRequest(2, 2);
        action.doExecute(null, r1, al1);
        argCaptor = ArgumentCaptor.forClass(ListConversationsResponse.class);
        verify(al1).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult.subList(0,2)));
        assert(argCaptor.getValue().hasMorePages());
        assert(argCaptor.getValue().getNextToken() == 4);

        @SuppressWarnings("unchecked")
        ActionListener<ListConversationsResponse> al2 = (ActionListener<ListConversationsResponse>) Mockito.mock(ActionListener.class);
        ListConversationsRequest r2 = new ListConversationsRequest(20, 4);
        action.doExecute(null, r2, al2);
        argCaptor = ArgumentCaptor.forClass(ListConversationsResponse.class);
        verify(al2).onResponse(argCaptor.capture());
        assert(argCaptor.getValue().getConversations().equals(testResult));
        assert(!argCaptor.getValue().hasMorePages());
    }
}