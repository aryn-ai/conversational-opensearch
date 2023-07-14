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

    /**
     * Convtructor
     * @param in input stream to create this from
     * @throws IOException if something breaks
     */
    public ListConversationsResponse(StreamInput in) throws IOException {
        super(in);
        conversations = in.readList(ConvoMeta::fromStream);
    }

    /**
     * Constructor
     * @param conversations list of conversations in this response
     */
    public ListConversationsResponse(List<ConvoMeta> conversations) {
        this.conversations = conversations;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(conversations);
    }

    /**
     * Returns the list of conversations in this response
     * @return the list of conversations returned by this action
     */
    public List<ConvoMeta> getConversations() {
        return conversations;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.startArray(ActionConstants.RESPONSE_CONVO_LIST_FIELD);
        for(ConvoMeta convo : conversations) {
            convo.toXContent(builder, params);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

}