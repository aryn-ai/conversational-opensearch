/*
 * Copyright Aryn, Inc 2023
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.conversational.index;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.conversational.action.ActionConstants;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.search.SearchHit;

/**
 * Class for holding conversational metadata
 */
public final class ConvoMeta implements Writeable, ToXContentObject {

    private String id;
    private Instant created;
    private Instant lastHit;
    private int numInteractions;
    private String name;
    private String user;

    /**
     * Most naive constructor
     * @param id the UID of this conversation
     * @param created the timestamp of this conversation's creation
     * @param lastHit the timestamp of the most recent interaction belonging to this conversation 
     *      or the time of creation if there are no interactions
     * @param numInteractions the length of this conversation
     * @param name a user-defined name for the conversation
     * @param user the user who owns this conversation
     */
    public ConvoMeta(
        String id,
        Instant created,
        Instant lastHit,
        int numInteractions,
        String name,
        String user
    ) {
        this.id = id;
        this.created = created;
        this.lastHit = lastHit;
        this.numInteractions = numInteractions;
        this.name = name;
        this.user = user;
    }

    /**
     * Creates a ConvoMeta object from a SearchHit object
     * @param hit the search hit to transform into a ConvoMeta object
     * @return a new ConvoMeta object representing the search hit
     */
    public static ConvoMeta fromSearchHit(SearchHit hit) {
        String id = hit.getId();
        return ConvoMeta.fromMap(id, hit.getSourceAsMap());
    }

    /**
     * Creates a ConvoMeta object from a Map of fields in the OS index
     * @param id the conversation's id
     * @param docFields the map of source fields
     * @return a new ConvoMeta object representing the map
     */
    public static ConvoMeta fromMap(String id, Map<String, Object> docFields) {
        Instant created = Instant.parse((String) docFields.get(ConvoIndexConstants.META_CREATED_FIELD));
        Instant lastHit = Instant.parse((String) docFields.get(ConvoIndexConstants.META_ENDED_FIELD));
        int numInteractions = (int) docFields.get(ConvoIndexConstants.META_LENGTH_FIELD);
        String name = (String) docFields.get(ConvoIndexConstants.META_NAME_FIELD);
        String user = (String) docFields.get(ConvoIndexConstants.USER_FIELD);
        return new ConvoMeta(id, created, lastHit, numInteractions, name, user);
    }

    /**
     * Creates a ConvoMeta from a stream, given the stream was written to by 
     * ConvoMeta.writeTo
     * @param in stream to read from
     * @return new ConvoMeta object
     * @throws IOException if you're reading from a stream without a ConvoMeta in it
     */
    public static ConvoMeta fromStream(StreamInput in) throws IOException {
        String id = in.readString();
        Instant created = in.readInstant();
        Instant lastHit = in.readInstant();
        int numInteractions = in.readInt();
        String name = in.readString();
        String user = in.readOptionalString();
        return new ConvoMeta(id, created, lastHit, numInteractions, name, user);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(id);
        out.writeInstant(created);
        out.writeInstant(lastHit);
        out.writeInt(numInteractions);
        out.writeString(name);
        out.writeString(user);
    }

    /**
     * @return the unique id of this ConvoMeta object
     */
    public String getId() {
        return id;
    }

    /**
     * @return the Instant when this conversation was first created
     */
    public Instant getCreated() {
        return created;
    }

    /**
     * @return the Instant when this conversation was last interacted with
     */
    public Instant getLastHit() {
        return lastHit;
    }

    /**
     * @return the number of interactions in this conversation
     */
    public int getLength() {
        return numInteractions;
    }

    /**
     * Hit this ConvoMeta at this time, increasing the converation length
     * @param hitTime the Instant when the new interaction was created
     * @return this ConvoMeta object (fields updated)
     */
    public ConvoMeta hit(Instant hitTime) {
        this.lastHit = hitTime;
        this.numInteractions++;
        return this;
    }

    /**
     * @return the name of this conversation. Could be the empty string.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the user who owns this conversation
     */
    public String getUser() {
        return user;
    }

    /**
     * Convert this ConvoMeta object into an IndexRequest so it can be indexed
     * @param index the index to send this convo to. Should usually be .conversational-meta
     * @return the IndexRequest for the client to send
     */
    public IndexRequest toIndexRequest(String index) {
        IndexRequest request = new IndexRequest(index);
        return request.id(this.id).source(
            ConvoIndexConstants.META_CREATED_FIELD, this.created,
            ConvoIndexConstants.META_ENDED_FIELD, this.lastHit,
            ConvoIndexConstants.META_LENGTH_FIELD, this.numInteractions,
            ConvoIndexConstants.META_NAME_FIELD, this.name
        );
    }

    @Override
    public String toString() {
        return "{id=" + id
            + ", name=" + name
            + ", length=" + numInteractions
            + ", created=" + created.toString()
            + ", lastHit=" + lastHit.toString()
            + ", user=" + user
            + "}";
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContentObject.Params params) throws IOException {
        builder.startObject();
        builder.field(ActionConstants.CONVO_ID_FIELD, this.id);
        builder.field(ConvoIndexConstants.META_CREATED_FIELD, this.created);
        builder.field(ConvoIndexConstants.META_ENDED_FIELD, this.lastHit);
        builder.field(ConvoIndexConstants.META_LENGTH_FIELD, this.numInteractions);
        builder.field(ConvoIndexConstants.META_NAME_FIELD, this.name);
        builder.field(ConvoIndexConstants.USER_FIELD, this.user);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof ConvoMeta)) {
            return false;
        }
        ConvoMeta otherConvo = (ConvoMeta) other;
        if(! otherConvo.id.equals(this.id)) {
            return false;
        } if(! otherConvo.user.equals(this.user)) {
            return false; 
        } if(! otherConvo.created.equals(this.created)) {
            return false;
        } if(! otherConvo.lastHit.equals(this.lastHit)) {
            return false;
        } if(otherConvo.numInteractions != this.numInteractions) {
            return false;
        } return otherConvo.name.equals(this.name);
    }
    
}
