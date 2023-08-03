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
import org.opensearch.conversational.action.memory.conversation.DeleteConversationAction;
import org.opensearch.conversational.action.memory.conversation.DeleteConversationRestAction;
import org.opensearch.conversational.action.memory.conversation.DeleteConversationTransportAction;
import org.opensearch.conversational.action.memory.conversation.GetConversationsAction;
import org.opensearch.conversational.action.memory.conversation.GetConversationsRestAction;
import org.opensearch.conversational.action.memory.conversation.GetConversationsTransportAction;
import org.opensearch.conversational.action.memory.interaction.GetInteractionsAction;
import org.opensearch.conversational.action.memory.interaction.GetInteractionsRestAction;
import org.opensearch.conversational.action.memory.interaction.GetInteractionsTransportAction;
import org.opensearch.conversational.action.memory.interaction.CreateInteractionAction;
import org.opensearch.conversational.action.memory.interaction.CreateInteractionRestAction;
import org.opensearch.conversational.action.memory.interaction.CreateInteractionTransportAction;
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
            new ActionHandler<>(GetConversationsAction.INSTANCE, GetConversationsTransportAction.class),
            new ActionHandler<>(CreateInteractionAction.INSTANCE, CreateInteractionTransportAction.class),
            new ActionHandler<>(GetInteractionsAction.INSTANCE, GetInteractionsTransportAction.class),
            new ActionHandler<>(DeleteConversationAction.INSTANCE, DeleteConversationTransportAction.class)
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
        GetConversationsRestAction restListConversations = new GetConversationsRestAction();
        CreateInteractionRestAction restCreateInteraction = new CreateInteractionRestAction();
        GetInteractionsRestAction restListInteractions = new GetInteractionsRestAction();
        DeleteConversationRestAction restDeleteConversation = new DeleteConversationRestAction();
        return List.of(
            restCreateConversation,
            restListConversations,
            restCreateInteraction,
            restListInteractions,
            restDeleteConversation
        );
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of();
    }

}
