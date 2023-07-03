/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.index;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.SearchHit;
import org.opensearch.search.sort.SortOrder;

/**
 * Class for handling the conversational metadata index
 */
public class ConvoMetaIndex {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConvoMetaIndex.class);

    private Client client;
    private ClusterService clusterService;
    private final String indexName = ConvoIndexConstants.META_INDEX_NAME;


    /**
     * Constructor
     * @param client base OpenSearch client to use for dealing with the OS cluster
     * @param clusterService a ClusterService object for managing OS
     */
    public ConvoMetaIndex(Client client, ClusterService clusterService){
        this.client = client;
        this.clusterService = clusterService;
    }

    /**
     * 'PUT's the index in opensearch if it's not there already
     * @return whether the index needed to be initialized. Throws error if it fails to init
     */
    public boolean initConvoMetaIndexIfAbsent() throws AssertionError {
        if(!clusterService.state().metadata().hasIndex(indexName)){
            log.debug("No conversational meta index found. Adding it");
            CreateIndexRequest request = Requests.createIndexRequest(indexName).mapping(ConvoIndexConstants.META_MAPPING);
            CreateIndexResponse response = client.admin().indices().create(request).actionGet();
            assert(response.equals(new CreateIndexResponse(true, true, indexName)));
            return true;
        }
        return false;
    }

    /**
     * Adds a new conversation with the specified name to the index
     * @param name user-specified name of the conversation to be added
     * @return the UID of the new conversation
     */
    public String addNewConversation(String name) throws AssertionError{
        initConvoMetaIndexIfAbsent();
        IndexRequest request = Requests.indexRequest(indexName).source(
            ConvoIndexConstants.META_CREATED_FIELD, Instant.now(),
            ConvoIndexConstants.META_ENDED_FIELD, Instant.now(),
            ConvoIndexConstants.META_LENGTH_FIELD, 0,
            ConvoIndexConstants.META_NAME_FIELD, name
        );
        IndexResponse response = client.index(request).actionGet();
        assert(response.status() == RestStatus.CREATED);
        return response.getId();
    }

    /**
     * Adds a new conversation named ""
     * @return the UID of the new conversation
     */
    public String addNewConversation() throws AssertionError{
        return addNewConversation("");
    }

    /**
     * @return the list of all conversation metadata objects in the index
     */
    public List<ConvoMeta> listConversations() {
        if(!clusterService.state().metadata().hasIndex(indexName)){
            return List.of();
        }
        SearchRequest request = Requests.searchRequest(indexName);
        MatchAllQueryBuilder queryBuilder = new MatchAllQueryBuilder();
        request.source().query(queryBuilder);
        request.source().sort(ConvoIndexConstants.META_ENDED_FIELD, SortOrder.DESC);
        SearchResponse response = client.search(request).actionGet();
        List<ConvoMeta> result = new LinkedList<ConvoMeta>();
        for(SearchHit hit : response.getHits()) {
            result.add(ConvoMeta.fromSearchHit(hit));
        }
        return result;
    }

    /**
     * Update a conversation's metadata with a new hit now
     * @param id the id of the conversation to touch
     * @param hitTime when this conversation got touched
     * @return whether the operation was successful
     */
    public boolean hitConversation(String id, Instant hitTime) {
        GetRequest getRequest = Requests.getRequest(indexName).id(id);
        GetResponse getResponse = client.get(getRequest).actionGet();
        if(!(getResponse.isExists() && getResponse.getId().equals(id))){
            return false;
        }
        ConvoMeta convo = ConvoMeta.fromMap(id, getResponse.getSourceAsMap());
        UpdateRequest update = (new UpdateRequest(indexName, id)).doc(convo.hit(hitTime).toIndexRequest(id));
        UpdateResponse response = client.update(update).actionGet();
        return response.status() == RestStatus.CREATED;
    }

}