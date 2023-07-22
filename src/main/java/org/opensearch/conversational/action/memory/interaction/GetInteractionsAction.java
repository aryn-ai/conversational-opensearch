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
package org.opensearch.conversational.action.memory.interaction;

import org.opensearch.action.ActionType;

/**
 * Action to return the interactions associated with a conversation
 */
public class GetInteractionsAction extends ActionType<GetInteractionsResponse> {
    /** Instance of this */
    public static final GetInteractionsAction INSTANCE = new GetInteractionsAction();
    /** Name of this action. Has something to do with permissions I think?? */
    public static final String NAME = "cluste:admin/opensearch/conversational/conversation/get";

    private GetInteractionsAction() { super(NAME, GetInteractionsResponse::new);}

}
