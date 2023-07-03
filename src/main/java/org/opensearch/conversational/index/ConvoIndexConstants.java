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
    public final static String META_CREATED_FIELD = "created";
    /** Name of the metadata field for most recent timestamp */
    public final static String META_ENDED_FIELD = "last_hit";
    /** Name of the metadata field for number of interactions */
    public final static String META_LENGTH_FIELD = "length";
    /** Name of the metadata field for name of the conversation */
    public final static String META_NAME_FIELD = "name";
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

}