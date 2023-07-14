/* 
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.conversation;

import java.io.IOException;
import java.util.Map;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.rest.RestRequest;

import static org.opensearch.action.ValidateActions.addValidationError;

/**
 * ActionRequest for list conversations action
 */
public class ListConversationsRequest extends ActionRequest {

    private int maxResults = 10;

    /**
     * Constructor
     * @param maxResults number of results to return
     */
    public ListConversationsRequest(int maxResults) {
        super();
        this.maxResults = maxResults;
    }

    /**
     * Constructor; defaults to 10 results returned
     */
    public ListConversationsRequest() {
        super();
    }

    /**
     * Constructor
     * @param in Input stream to read from. assumes there was a writeTo
     * @throws IOException if I can't read
     */
    public ListConversationsRequest(StreamInput in) throws IOException {
        super(in);
        this.maxResults = in.readInt();
    }

    /**
     * max results to be returned by this action
     * @return max results
     */
    public int getMaxResults() {
        return maxResults;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(maxResults);
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException exception = null;
        if(this.maxResults == 0) {
            exception = addValidationError("Can't list 0 conversations", exception);
        }
        return exception;
    }

    /**
     * Creates a ListConversationsRequest from a RestRequest
     * @param request a RestRequest for a ListConversations
     * @return a new ListConversationsRequest
     * @throws IOException if something breaks
     */
    public static ListConversationsRequest fromRestRequest(RestRequest request) throws IOException {
        if(!request.hasContent()) {
            return new ListConversationsRequest();
        }
        Map<String, String> payload = request.contentOrSourceParamParser().mapStrings();
        if(payload.containsKey(ActionConstants.REQUEST_MAX_RESULTS_FIELD)) {
            return new ListConversationsRequest(Integer.parseInt(payload.get(ActionConstants.REQUEST_MAX_RESULTS_FIELD)));
        } else {
            return new ListConversationsRequest();
        }
    }
}