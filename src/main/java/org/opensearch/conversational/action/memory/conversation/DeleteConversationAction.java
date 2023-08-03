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

import org.opensearch.action.ActionType;

/**
 * Action for deleting a conversation from conversational memory
 */
public class DeleteConversationAction extends ActionType<DeleteConversationResponse> {
    /** Instance of this */
    public static final DeleteConversationAction INSTANCE = new DeleteConversationAction();
    /** Name of this action - has something to do with security maybe */
    public static final String NAME = "cluster:admin/opensearch/conversational/conversation/delete";

    private DeleteConversationAction() {super(NAME, DeleteConversationResponse::new);}
}