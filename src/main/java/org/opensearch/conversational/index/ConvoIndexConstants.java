/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.index;

/**
 * Class containing a bunch of constant defining how the conversational indices are formatted
 */
public class ConvoIndexConstants {
    /** Name of the conversational metadata index */
    public final static String META_INDEX_NAME = ".conversational-meta";
    /** Name of the metadata field for initial timestamp */
    public final static String META_CREATED_FIELD = "createTime";
    /** Name of the metadata field for most recent timestamp */
    public final static String META_ENDED_FIELD = "lastInteractionTime";
    /** Name of the metadata field for number of interactions */
    public final static String META_LENGTH_FIELD = "numInteractions";
    /** Name of the metadata field for name of the conversation */
    public final static String META_NAME_FIELD = "name";
    /** Name of the metadata field for conversation ID */
    public final static String META_ID_FIELD = "conversationId";
    /** Mappings for the conversational metadata index */
    protected final static String META_MAPPING = "{\n"
        + "    \"properties\": {\n"
        + "        \""
        + META_NAME_FIELD
        + "\": {\"type\": \"keyword\"},\n"
        + "        \""
        + META_CREATED_FIELD
        + "\": {\"type\": \"date\", \"format\": \"strict_date_optional_time||epoch_millis\"},\n"
        + "        \""
        + META_ENDED_FIELD
        + "\": {\"type\": \"date\", \"format\": \"strict_date_optional_time||epoch_millis\"},\n"
        + "        \""
        + META_LENGTH_FIELD
        + "\": {\"type\": \"integer\"}\n"
        + "    }\n"
        + "}";

        /** Name of the conversational interactions index */
        public final static String INTERACTIONS_INDEX_NAME = ".conversational-interactions";
        /** Name of the interaction field for the conversation Id */
        public final static String INTERACTIONS_CONVO_ID_FIELD = "conversation_id";
        /** Name of the interaction field for the human input */
        public final static String INTERACTIONS_INPUT_FIELD = "input";
        /** Name of the interaction field for the prompt template */
        public final static String INTERACTIONS_PROMPT_FIELD = "prompt";
        /** Name of the interaction field for the AI response */
        public final static String INTERACTIONS_RESPONSE_FIELD = "response";
        /** Name of the interaction field for the GAI Agent */
        public final static String INTERACTIONS_AGENT_FIELD = "agent";
        /** Name of the interaction field for the timestamp */
        public final static String INTERACTIONS_TIMESTAMP_FIELD = "timestamp";
        /** Name of the interaction field for any excess metadata */
        public final static String INTERACTIONS_METADATA_FIELD = "metadata";
        /** Mappings for the interactions index */
        protected final static String INTERACTIONS_MAPPINGS = "{\n"
        + "    \"properties\": {\n"
        + "        \""
        + INTERACTIONS_CONVO_ID_FIELD
        + "\": {\"type\": \"keyword\"},\n"
        + "        \""
        + INTERACTIONS_TIMESTAMP_FIELD
        + "\": {\"type\": \"date\", \"format\": \"strict_date_optional_time||epoch_millis\"},\n"
        + "        \""
        + INTERACTIONS_INPUT_FIELD
        + "\": {\"type\": \"text\"},\n"
        + "        \""
        + INTERACTIONS_PROMPT_FIELD
        + "\": {\"type\": \"text\"},\n"
        + "        \""
        + INTERACTIONS_RESPONSE_FIELD
        + "\": {\"type\": \"text\"},\n"
        + "        \""
        + INTERACTIONS_AGENT_FIELD
        + "\": {\"type\": \"keyword\"},\n"
        + "        \""
        + INTERACTIONS_METADATA_FIELD
        + "\": {\"type\": \"text\"}\n"
        + "    }\n"
        + "}";

}