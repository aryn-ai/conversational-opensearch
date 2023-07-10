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
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.SearchHit;
import org.opensearch.search.sort.SortOrder;

/**
 * Class for handling the interactions index
 */
public class InteractionsIndex {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConvoMetaIndex.class);

    private Client client;
    private ClusterService clusterService;
    private final String indexName = ConvoIndexConstants.INTERACTIONS_INDEX_NAME;

    /**
     * Constructor
     * @param client Opensearch client to use for all operations
     * @param clusterService ClusterService object for managing OS
     */
    public InteractionsIndex(Client client, ClusterService clusterService) {
        this.client = client;
        this.clusterService = clusterService;
    }

    /**
     * 'PUT's the index in opensearch if it's not there already
     * @return whether the index needed to be initialized. Throws error if it fails to init
     */
    public boolean initInteractionsIndexIfAbsent() throws AssertionError {
        if(!clusterService.state().metadata().hasIndex(indexName)){
            log.debug("No interactions index found. Adding it");
            CreateIndexRequest request = Requests.createIndexRequest(indexName).mapping(ConvoIndexConstants.INTERACTIONS_MAPPINGS);
            CreateIndexResponse response = client.admin().indices().create(request).actionGet();
            assert(response.equals(new CreateIndexResponse(true, true, indexName)));
            return true;
        }
        return false;
    }

    /**
     * Add an interaction to this index. Return the ID of the newly created interaction
     * @param convoId The id of the conversation this interaction belongs to
     * @param input the user (human) input into this interaction
     * @param prompt the prompt template used for this interaction
     * @param response the GenAI response for this interaction
     * @param agent the name of the GenAI agent this interaction belongs to
     * @param metadata arbitrary JSON blob of extra info
     * @param timestamp when this interaction happened
     * @return the id of the newly created interaction record
     */
    public String addInteraction(
        String convoId, 
        String input, 
        String prompt, 
        String response, 
        String agent, 
        String metadata, 
        Instant timestamp) 
    {
        initInteractionsIndexIfAbsent();
        IndexRequest request = Requests.indexRequest(indexName).source(
            ConvoIndexConstants.INTERACTIONS_AGENT_FIELD, agent,
            ConvoIndexConstants.INTERACTIONS_CONVO_ID_FIELD, convoId,
            ConvoIndexConstants.INTERACTIONS_INPUT_FIELD, input,
            ConvoIndexConstants.INTERACTIONS_METADATA_FIELD, metadata,
            ConvoIndexConstants.INTERACTIONS_PROMPT_FIELD, prompt,
            ConvoIndexConstants.INTERACTIONS_RESPONSE_FIELD, response,
            ConvoIndexConstants.INTERACTIONS_TIMESTAMP_FIELD, timestamp
        );
        IndexResponse indResponse = client.index(request).actionGet();
        assert(indResponse.status() == RestStatus.CREATED);
        return indResponse.getId();
    }

    /**
     * Add an interaction to this index, timestamped now. Return the id of the newly created interaction
     * @param convoId The id of the converation this interaction belongs to
     * @param input the user (human) input into this interaction
     * @param prompt the prompt template usd for this interaction
     * @param response the GenAI response for this interaction
     * @param agent the name of the GenAI agent this interaction belongs to
     * @param metadata arbitrary JSON blob of extra info
     * @return the id of the newly created interaction record
     */
    public String addInteraction(
        String convoId, 
        String input, 
        String prompt, 
        String response, 
        String agent, 
        String metadata
    ) {
        return addInteraction(
            convoId,
            input,
            prompt,
            response,
            agent,
            metadata,
            Instant.now()
        );
    }

    /**
     * Gets a list of all interactions belonging to a conversation
     * @param convoId the conversation to gather all interactions of
     * @return the list, sorted by recency, of interactions belonging to the conversation
     */
    public List<Interaction> getInteractions(String convoId) {
        if(! clusterService.state().metadata().hasIndex(indexName)) {
            return List.of();
        }
        SearchRequest request = Requests.searchRequest(indexName);
        TermQueryBuilder builder = new TermQueryBuilder(ConvoIndexConstants.INTERACTIONS_CONVO_ID_FIELD, convoId);
        request.source().query(builder);
        request.source().sort(ConvoIndexConstants.INTERACTIONS_TIMESTAMP_FIELD, SortOrder.DESC);
        SearchResponse response = client.search(request).actionGet();
        List<Interaction> result = new LinkedList<Interaction>();
        for(SearchHit hit : response.getHits()) {
            result.add(Interaction.fromSearchHit(hit));
        }
        return result;
    }

}