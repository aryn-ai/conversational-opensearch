/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.interaction;

import java.io.IOException;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

/**
 * Action Response for create interaction
 */
public class PutInteractionResponse extends ActionResponse implements ToXContentObject {
    private String interactionId;

    /**
     * Convtructor
     * @param in input stream to create this from
     * @throws IOException if something breaks
     */
    public PutInteractionResponse(StreamInput in) throws IOException {
        super(in);
        this.interactionId = in.readString();
    }

    /**
     * Constructor
     * @param interactionId id of the newly created interaction
     */
    public PutInteractionResponse(String interactionId) {
        this.interactionId = interactionId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(this.interactionId);
    }

    /**
     * @return the id of the newly created interaction
     */
    public String getId() {
        return this.interactionId;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContentObject.Params params) throws IOException {
        builder.startObject();
        builder.field(ActionConstants.RESPONSE_INTER_ID_FIELD, this.interactionId);
        builder.endObject();
        return builder;
    }
}