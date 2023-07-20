/* 
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.action.memory.conversation;

import java.io.IOException;

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

    private int maxResults = ActionConstants.DEFAULT_MAX_RESULTS;
    private int from = 0;

    /**
     * Constructor; returns from position 0
     * @param maxResults number of results to return
     */
    public ListConversationsRequest(int maxResults) {
        super();
        this.maxResults = maxResults;
    }

    /**
     * Constructor
     * @param maxResults number of results to return
     * @param from where to start from
     */
    public ListConversationsRequest(int maxResults, int from) {
        super();
        this.maxResults = maxResults;
        this.from = from;
    }

    /**
     * Constructor; defaults to 10 results returned from position 0
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
        this.from = in.readInt();
    }

    /**
     * max results to be returned by this action
     * @return max results
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * what position to start at in retrieving conversations
     * @return the position
     */
    public int getFrom() {
        return from;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(maxResults);
        out.writeInt(from);
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
        if(request.hasParam(ActionConstants.NEXT_TOKEN_FIELD)) {
            if(request.hasParam(ActionConstants.REQUEST_MAX_RESULTS_FIELD)) {
                return new ListConversationsRequest(Integer.parseInt(request.param(ActionConstants.REQUEST_MAX_RESULTS_FIELD)),
                                                    Integer.parseInt(request.param(ActionConstants.NEXT_TOKEN_FIELD)));
            } else {
                return new ListConversationsRequest(ActionConstants.DEFAULT_MAX_RESULTS,
                                                    Integer.parseInt(request.param(ActionConstants.NEXT_TOKEN_FIELD)));
            }
        } else {
            if(request.hasParam(ActionConstants.REQUEST_MAX_RESULTS_FIELD)) {
                return new ListConversationsRequest(Integer.parseInt(request.param(ActionConstants.REQUEST_MAX_RESULTS_FIELD)));
            } else {
                return new ListConversationsRequest();
            }
        }
    }
}