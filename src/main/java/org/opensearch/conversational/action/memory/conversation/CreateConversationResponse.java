/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.conversation;

import java.io.IOException;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

/**
 * Action Response for CreateConversation
 */
public class CreateConversationResponse extends ActionResponse implements ToXContentObject {

    String conversationId;

    /**
     * Convtructor
     * @param in input stream to create this from
     * @throws IOException if something breaks
     */
    public CreateConversationResponse(StreamInput in) throws IOException {
        super(in);
        this.conversationId = in.readString();
    }

    /**
     * Constructor
     * @param conversationId unique id of the newly-created conversation
     */
    public CreateConversationResponse(String conversationId) {
        super();
        this.conversationId = conversationId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(this.conversationId);
    }

    /**
     * @return the unique id of the newly created conversation
     */
    public String getId() {
        return conversationId;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field(ActionConstants.RESPONSE_CONVO_ID_FIELD, this.conversationId);
        builder.endObject();
        return builder;
    }

}