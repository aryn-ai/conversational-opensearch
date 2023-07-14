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

/**
 * Action Request for creating a conversation
 */
public class CreateConversationRequest extends ActionRequest {

    private String name = null;

    /**
     * Constructor
     * @param in input stream to read from
     * @throws IOException if something breaks
     */
    public CreateConversationRequest(StreamInput in) throws IOException {
        super(in);
        this.name = in.readOptionalString();
    }

    /**
     * Constructor
     * @param name name of the conversation
     */
    public CreateConversationRequest(String name) {
        super();
        this.name = name;
    }
    /**
     * Constructor
     * name will be null
     */
    public CreateConversationRequest() {}

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(name);
    }

    /**
     * @return name of the conversation to be created
     */
    public String getName() {
        return name;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException exception = null;

        return exception;
    }

    /**
     * Creates a CreateConversationRequest from a RestRequest
     * @param restRequest a RestRequest for a CreateConversation
     * @return a new CreateConversationRequest
     * @throws IOException if something breaks
     */
    public static CreateConversationRequest fromRestRequest(RestRequest restRequest) throws IOException {
        if(!restRequest.hasContent()) {
            return new CreateConversationRequest();
        }
        Map<String, String> payload = restRequest.contentParser().mapStrings();
        if(payload.containsKey(ActionConstants.REQUEST_CONVO_NAME_FIELD)) {
            return new CreateConversationRequest(payload.get(ActionConstants.REQUEST_CONVO_NAME_FIELD));
        } else {
            return new CreateConversationRequest();
        }
    }

}