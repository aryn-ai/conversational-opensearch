/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.interaction;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.rest.RestRequest;

import static org.opensearch.action.ValidateActions.addValidationError;

/**
 * ActionRequest for get interactions
 */
public class GetInteractionsRequest extends ActionRequest {

    private int maxResults = 10;
    private String conversationId;

    /**
     * Constructor
     * @param conversationId UID of the conversation to get interactions from
     * @param maxResults number of interactions to retrieve
     */
    public GetInteractionsRequest(String conversationId, int maxResults) {
        this.conversationId = conversationId;
        this.maxResults = maxResults;
    }

    /**
     * Constructor
     * @param conversationId the UID of the conversation to get interactions from
     */
    public GetInteractionsRequest(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * Constructor
     * @param in streaminput to read this from. assumes there was a GetInteractionsRequest.writeTo 
     * @throws IOException if there wasn't a GIR in the stream
     */
    public GetInteractionsRequest(StreamInput in) throws IOException {
        super(in);
        this.conversationId = in.readString();
        this.maxResults = in.readInt();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(conversationId);
        out.writeInt(maxResults);
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException exception = null;
        if(conversationId == null) {
            exception = addValidationError("must get interactions from a conversation", exception);
        }
        if(maxResults <= 0) {
            exception = addValidationError("must retrieve positive interactions", exception);
        }
        return exception;
    }

    /**
     * Get the UID of the conversation to retrieve from
     * @return the UID of the conversation this request is trying to retrieve from
     */
    public String getConversationId() {
        return this.conversationId;
    }

    /**
     * Makes a GetInteractionsRequest out of a RestRequest
     * @param request Rest Request representing a get interactions request
     * @return a new GetInteractionsRequest
     * @throws IOException if something goes wrong
     */
    public static GetInteractionsRequest fromRestRequest(RestRequest request) throws IOException {
        String cid = request.param(ActionConstants.CONVO_ID_FIELD);
        if(request.hasParam(ActionConstants.REQUEST_MAX_RESULTS_FIELD)) {
            int maxResults = Integer.parseInt(request.param(ActionConstants.REQUEST_MAX_RESULTS_FIELD));
            return new GetInteractionsRequest(cid, maxResults);
        } else {
            return new GetInteractionsRequest(cid);
        }
    }

}