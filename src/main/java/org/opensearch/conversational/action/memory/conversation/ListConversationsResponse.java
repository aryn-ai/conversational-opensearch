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

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.conversational.index.ConvoMeta;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

/**
 * Action Response for CreateConversation
 */
public class ListConversationsResponse extends ActionResponse implements ToXContentObject {
    
    private List<ConvoMeta> conversations;
    private int nextToken;
    private boolean hasMoreTokens;

    /**
     * Convtructor
     * @param in input stream to create this from
     * @throws IOException if something breaks
     */
    public ListConversationsResponse(StreamInput in) throws IOException {
        super(in);
        conversations = in.readList(ConvoMeta::fromStream);
        this.nextToken = in.readInt();
        this.hasMoreTokens = in.readBoolean();
    }

    /**
     * Constructor
     * @param conversations list of conversations in this response
     * @param nextToken the position of the next conversation after these
     * @param hasMoreTokens whether there are more conversations after this set of results
     */
    public ListConversationsResponse(List<ConvoMeta> conversations, int nextToken, boolean hasMoreTokens) {
        this.conversations = conversations;
        this.nextToken = nextToken;
        this.hasMoreTokens = hasMoreTokens;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(conversations);
        out.writeInt(nextToken);
        out.writeBoolean(hasMoreTokens);
    }

    /**
     * Returns the list of conversations in this response
     * @return the list of conversations returned by this action
     */
    public List<ConvoMeta> getConversations() {
        return conversations;
    }

    /**
     * the token for the next page in the pagination 
     * @return the token (position) for the next page in the pagination
     */
    public int getNextToken() {
        return nextToken;
    }

    /**
     * are there more pages of results in this search
     * @return whether there are more pages of results in this search
     */
    public boolean hasMorePages() {
        return hasMoreTokens;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.startArray(ActionConstants.RESPONSE_CONVO_LIST_FIELD);
        for(ConvoMeta convo : conversations) {
            convo.toXContent(builder, params);
        }
        builder.endArray();
        if(hasMoreTokens) {
            builder.field(ActionConstants.NEXT_TOKEN_FIELD, nextToken);
        }
        builder.endObject();
        return builder;
    }

}