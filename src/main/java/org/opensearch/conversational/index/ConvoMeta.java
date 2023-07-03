/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.index;

import java.time.Instant;
import java.util.Map;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.search.SearchHit;

/**
 * Class for holding conversational metadata
 */
public final class ConvoMeta {

    private String id;
    private Instant created;
    private Instant lastHit;
    private int numInteractions;
    private String name;

    /**
     * Most naive constructor
     * @param id the UID of this conversation
     * @param created the timestamp of this conversation's creation
     * @param lastHit the timestamp of the most recent interaction belonging to this conversation 
     *      or the time of creation if there are no interactions
     * @param numInteractions the length of this conversation
     * @param name a user-defined name for the conversation
     */
    public ConvoMeta(
        String id,
        Instant created,
        Instant lastHit,
        int numInteractions,
        String name
    ) {
        this.id = id;
        this.created = created;
        this.lastHit = lastHit;
        this.numInteractions = numInteractions;
        this.name = name;
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
        return new ConvoMeta(id, created, lastHit, numInteractions, name);
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
            + "}";
    }

    
}
