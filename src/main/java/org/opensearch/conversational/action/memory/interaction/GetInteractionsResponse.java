/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.interaction;

import java.io.IOException;
import java.util.List;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.conversational.index.Interaction;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

/**
 * Action Response for get interactions
 */
public class GetInteractionsResponse extends ActionResponse implements ToXContentObject {
    
    private List<Interaction> interactions;
    private int nextToken;
    private boolean hasMoreTokens;

    /**
     * Constructor
     * @param in stream input; assumes GetInteractionsResponse.writeTo was called
     * @throws IOException if theres not a G.I.R. in the stream
     */
    public GetInteractionsResponse(StreamInput in) throws IOException {
        super(in);
        interactions = in.readList(Interaction::fromStream);
        nextToken = in.readInt();
        hasMoreTokens = in.readBoolean();
    }

    /**
     * Constructor
     * @param interactions list of interactions returned by this response
     * @param nextToken token representing the next page of results
     * @param hasMoreTokens whether there are more results after this page
     */
    public GetInteractionsResponse(List<Interaction> interactions, int nextToken, boolean hasMoreTokens) {
        this.interactions = interactions;
        this.nextToken = nextToken;
        this.hasMoreTokens = hasMoreTokens;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(interactions);
        out.writeInt(nextToken);
        out.writeBoolean(hasMoreTokens);
    }

    /**
     * Get the list of interactions
     * @return the list of interactions returned by this response
     */
    public List<Interaction> getInteractions() {
        return interactions;
    }

    /**
     * Are there more pages in this search results
     * @return whether there are more pages in this search
     */
    public boolean hasMorePages() {
        return hasMoreTokens;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.startArray(ActionConstants.RESPONSE_INTER_LIST_FIELD);
        for(Interaction inter : interactions ){
            inter.toXContent(builder, params);
        }
        builder.endArray();
        if(hasMoreTokens) {
            builder.field(ActionConstants.NEXT_TOKEN_FIELD, nextToken);
        }
        builder.endObject();
        return builder;
    }
}