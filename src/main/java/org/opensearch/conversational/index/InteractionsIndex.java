/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.index;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.opensearch.OpenSearchWrapperException;
import org.opensearch.ResourceAlreadyExistsException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.util.concurrent.ThreadContext;
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
     * @param listener gets whether the index needed to be initialized. Throws error if it fails to init
     */
    public void initInteractionsIndexIfAbsent(ActionListener<Boolean> listener) {
        if(!clusterService.state().metadata().hasIndex(indexName)){
            log.debug("No interactions index found. Adding it");
            CreateIndexRequest request = Requests.createIndexRequest(indexName).mapping(ConvoIndexConstants.INTERACTIONS_MAPPINGS);
            try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().stashContext()) {
                ActionListener<Boolean> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
                ActionListener<CreateIndexResponse> al = ActionListener.wrap(r -> {
                    if(r.equals(new CreateIndexResponse(true, true, indexName))) {
                        log.info("created index [" + indexName + "]");
                        internalListener.onResponse(true);
                    } else {
                        internalListener.onResponse(false);
                    }
                }, e-> {
                    if(e instanceof ResourceAlreadyExistsException ||
                        (e instanceof OpenSearchWrapperException &&
                        e.getCause() instanceof ResourceAlreadyExistsException)) {
                        internalListener.onResponse(true);
                    } else {
                        log.error("failed to create index [" + indexName + "]");
                        internalListener.onFailure(e);
                    }
                });
                client.admin().indices().create(request, al);
            } catch (Exception e) {
                if(e instanceof ResourceAlreadyExistsException ||
                    (e instanceof OpenSearchWrapperException &&
                    e.getCause() instanceof ResourceAlreadyExistsException)) {
                    listener.onResponse(true);
                } else {
                    log.error("failed to create index [" + indexName + "]");
                    listener.onFailure(e);
                }
            }
        } else {
            listener.onResponse(true);
        }
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
     * @param listener gets the id of the newly created interaction record
     */ 
    public void addInteraction(
        String convoId, 
        String input, 
        String prompt, 
        String response, 
        String agent, 
        String metadata, 
        Instant timestamp,
        ActionListener<String> listener) 
    {
        initInteractionsIndexIfAbsent(ActionListener.wrap(
            b -> {
                if(b) {
                    IndexRequest request = Requests.indexRequest(indexName).source(
                        ConvoIndexConstants.INTERACTIONS_AGENT_FIELD, agent,
                        ConvoIndexConstants.INTERACTIONS_CONVO_ID_FIELD, convoId,
                        ConvoIndexConstants.INTERACTIONS_INPUT_FIELD, input,
                        ConvoIndexConstants.INTERACTIONS_METADATA_FIELD, metadata,
                        ConvoIndexConstants.INTERACTIONS_PROMPT_FIELD, prompt,
                        ConvoIndexConstants.INTERACTIONS_RESPONSE_FIELD, response,
                        ConvoIndexConstants.INTERACTIONS_TIMESTAMP_FIELD, timestamp
                    );
                    try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().stashContext()) {
                        ActionListener<String> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
                        ActionListener<IndexResponse> al = ActionListener.wrap(resp -> {
                            if(resp.status() == RestStatus.CREATED) {
                                internalListener.onResponse(resp.getId());
                            } else {
                                internalListener.onFailure(new IOException("failed to create conversation"));
                            }
                        }, e -> {
                            internalListener.onFailure(e);
                        });
                        client.index(request, al);
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                } else {
                    listener.onFailure(new IOException("no index to add conversation to"));
                }
            }, e -> {
                listener.onFailure(e);
            }
        ));
    }

    /**
     * Add an interaction to this index, timestamped now. Return the id of the newly created interaction
     * @param convoId The id of the converation this interaction belongs to
     * @param input the user (human) input into this interaction
     * @param prompt the prompt template usd for this interaction
     * @param response the GenAI response for this interaction
     * @param agent the name of the GenAI agent this interaction belongs to
     * @param metadata arbitrary JSON blob of extra info
     * @param listener gets the id of the newly created interaction record
     */
    public void addInteraction(
        String convoId, 
        String input, 
        String prompt, 
        String response, 
        String agent, 
        String metadata,
        ActionListener<String> listener
    ) {
        addInteraction(
            convoId,
            input,
            prompt,
            response,
            agent,
            metadata,
            Instant.now(),
            listener
        );
    }

    /**
     * Gets a list of interactions belonging to a conversation
     * @param convoId the conversation to read from
     * @param from where to start in the reading
     * @param maxResults how many interactions to return
     * @param listener gets the list, sorted by recency, of interactions
     */
    public void getInteractions(String convoId, int from, int maxResults, ActionListener<List<Interaction>> listener) {
        if(! clusterService.state().metadata().hasIndex(indexName)) {
            listener.onResponse(List.of());
        }
        SearchRequest request = Requests.searchRequest(indexName);
        TermQueryBuilder builder = new TermQueryBuilder(ConvoIndexConstants.INTERACTIONS_CONVO_ID_FIELD, convoId);
        request.source().query(builder);
        request.source().from(from).size(maxResults);
        request.source().sort(ConvoIndexConstants.INTERACTIONS_TIMESTAMP_FIELD, SortOrder.DESC);
        try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().stashContext()) {
            ActionListener<List<Interaction>> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
            ActionListener<SearchResponse> al = ActionListener.wrap(response -> {
                List<Interaction> result = new LinkedList<Interaction>();
                for(SearchHit hit : response.getHits()) {
                    result.add(Interaction.fromSearchHit(hit));
                }
                internalListener.onResponse(result);
            }, e -> {
                internalListener.onFailure(e);
            });
            client.admin().indices().refresh(Requests.refreshRequest(indexName), ActionListener.wrap(
                r -> {
                    client.search(request, al);
                }, e -> {
                    internalListener.onFailure(e);
                }
            ));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}