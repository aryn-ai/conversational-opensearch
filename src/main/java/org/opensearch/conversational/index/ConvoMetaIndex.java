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
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.util.concurrent.ThreadContext;
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
     * Creates the conversational meta index if it doesn't already exist
     * @param listener listener to wait for this to finish
     */
    public void initConvoMetaIndexIfAbsent(ActionListener<Boolean> listener) {
        if(!clusterService.state().metadata().hasIndex(indexName)){
            log.debug("No conversational meta index found. Adding it");
            CreateIndexRequest request = Requests.createIndexRequest(indexName).mapping(ConvoIndexConstants.META_MAPPING);
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
     * Adds a new conversation with the specified name to the index
     * @param name user-specified name of the conversation to be added
     * @param listener listener to wait for this to finish
     */
    public void addNewConversation(String name, ActionListener<String> listener) {
        initConvoMetaIndexIfAbsent(ActionListener.wrap(r -> {
            if(r) {
                IndexRequest request = Requests.indexRequest(indexName).source(
                    ConvoIndexConstants.META_CREATED_FIELD, Instant.now(),
                    ConvoIndexConstants.META_ENDED_FIELD, Instant.now(),
                    ConvoIndexConstants.META_LENGTH_FIELD, 0,
                    ConvoIndexConstants.META_NAME_FIELD, name
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
                        log.error("failed to create conversation", e);
                        internalListener.onFailure(e);
                    });
                    client.index(request, al);
                } catch (Exception e) {
                    log.error(e.toString());
                    listener.onFailure(e);
                }
            } else {
                listener.onFailure(new IOException("no index to add conversation to"));
            }
        }, e -> {
            listener.onFailure(e);
        }));
    }

    /**
     * Adds a new conversation named ""
     * @param listener listener to wait for this to finish
     */
    public void addNewConversation(ActionListener<String> listener) {
        addNewConversation("", listener);
    }
    
    /**
     * list all the conversations in the index
     * @param listener gets the list of all conversation metadata objects in the index
     */
    public void listConversations(ActionListener<List<ConvoMeta>> listener) {
        if(!clusterService.state().metadata().hasIndex(indexName)){
            listener.onResponse(List.of());
        }
        SearchRequest request = Requests.searchRequest(indexName);
        MatchAllQueryBuilder queryBuilder = new MatchAllQueryBuilder();
        request.source().query(queryBuilder);
        request.source().sort(ConvoIndexConstants.META_ENDED_FIELD, SortOrder.DESC);
        try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().stashContext()) {
            ActionListener<List<ConvoMeta>> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
            ActionListener<SearchResponse> al = ActionListener.wrap(r -> {
                List<ConvoMeta> result = new LinkedList<ConvoMeta>();
                for(SearchHit hit : r.getHits()) {
                    result.add(ConvoMeta.fromSearchHit(hit));
                }
                internalListener.onResponse(result);
            }, e -> {
                log.error("failed to list conversations", e);
                internalListener.onFailure(e);
            });
            client.admin().indices().refresh(Requests.refreshRequest(indexName), ActionListener.wrap(
                r -> {
                    client.search(request, al);
                }, e -> {
                    log.error("failed during refresh", e);
                    internalListener.onFailure(e);
                }
            ));
        } catch (Exception e) {
            log.error("failed during list conversations", e);
            listener.onFailure(e);
        }
    }

    /**
     * Update a conversation's metadata with a new hit
     * @param id id of the conversation to touch
     * @param hitTime when this conversation got toushed
     * @param listener gets whether the operation was successful
     */
    public void hitConversation(String id, Instant hitTime, ActionListener<Boolean> listener) {
        GetRequest getRequest = Requests.getRequest(indexName).id(id);
        try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().stashContext()) {
            ActionListener<Boolean> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
            ActionListener<GetResponse> al = ActionListener.wrap(getResponse -> {
                if(!(getResponse.isExists() && getResponse.getId().equals(id))){
                    internalListener.onResponse(false);
                }
                ConvoMeta convo = ConvoMeta.fromMap(id, getResponse.getSourceAsMap());
                UpdateRequest update = (new UpdateRequest(indexName, id)).doc(convo.hit(hitTime).toIndexRequest(id));
                client.update(update, ActionListener.wrap(response -> {
                    internalListener.onResponse(true);
                }, e -> {
                    log.error("failure touching conversation", e);
                    internalListener.onFailure(e);
                }));
            }, e -> {
                log.error("failure touching conversation", e);
                internalListener.onFailure(e);
            });
            client.get(getRequest, al);
        } catch (Exception e) {
            log.error("failed during hit conversation", e);
            listener.onFailure(e);
        }
    }
}