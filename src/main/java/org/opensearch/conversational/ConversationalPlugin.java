/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.conversational.action.memory.conversation.CreateConversationAction;
import org.opensearch.conversational.action.memory.conversation.CreateConversationRestAction;
import org.opensearch.conversational.action.memory.conversation.CreateConversationTransportAction;
import org.opensearch.conversational.action.memory.conversation.ListConversationsAction;
import org.opensearch.conversational.action.memory.conversation.ListConversationsRestAction;
import org.opensearch.conversational.action.memory.conversation.ListConversationsTransportAction;
import org.opensearch.conversational.action.memory.interaction.PutInteractionAction;
import org.opensearch.conversational.action.memory.interaction.PutInteractionRestAction;
import org.opensearch.conversational.action.memory.interaction.PutInteractionTransportAction;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

/**
 * Plugin that implements a conversational memory store and API.
 * Eventually this may also include some conversational 'chains' for lack of a better term
 */
public class ConversationalPlugin extends Plugin implements ActionPlugin {
    

    private ConversationalMemoryHandler cmHandler;

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return List.of(
            new ActionHandler<>(CreateConversationAction.INSTANCE, CreateConversationTransportAction.class),
            new ActionHandler<>(ListConversationsAction.INSTANCE, ListConversationsTransportAction.class),
            new ActionHandler<>(PutInteractionAction.INSTANCE, PutInteractionTransportAction.class)
        );
    }


    @Override
    public Collection<Object> createComponents(
        Client client,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier
    ){
        this.cmHandler = new ConversationalMemoryHandler(client, clusterService);
        
        return List.of(
            this.cmHandler
        );
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        CreateConversationRestAction restCreateConversation = new CreateConversationRestAction();
        ListConversationsRestAction restListConversations = new ListConversationsRestAction();
        PutInteractionRestAction restCreateInteraction = new PutInteractionRestAction();
        return List.of(
            restCreateConversation,
            restListConversations,
            restCreateInteraction
        );
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of();
    }

}
