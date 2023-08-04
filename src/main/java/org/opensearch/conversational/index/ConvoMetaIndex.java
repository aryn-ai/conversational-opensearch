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
import java.util.LinkedList;
import java.util.List;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.OpenSearchWrapperException;
import org.opensearch.ResourceAlreadyExistsException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.DocWriteResponse.Result;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
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
import org.opensearch.commons.ConfigConstants;
import org.opensearch.commons.authuser.User;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
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
            try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().newStoredContext(true)) {
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
    public void createConversation(String name, ActionListener<String> listener) {
        initConvoMetaIndexIfAbsent(ActionListener.wrap(r -> {
            if(r) {
                String userstr = client.threadPool().getThreadContext().getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
                IndexRequest request = Requests.indexRequest(indexName).source(
                    ConvoIndexConstants.META_CREATED_FIELD, Instant.now(),
                    ConvoIndexConstants.META_ENDED_FIELD, Instant.now(),
                    ConvoIndexConstants.META_LENGTH_FIELD, 0,
                    ConvoIndexConstants.META_NAME_FIELD, name,
                    ConvoIndexConstants.USER_FIELD, userstr==null ? null : User.parse(userstr).getName()
                );
                try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().newStoredContext(true)) {
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
    public void createConversation(ActionListener<String> listener) {
        createConversation("", listener);
    }
    
    
    /**
     * list size conversations in the index
     * @param from where to start listing from
     * @param maxResults how many conversations to list
     * @param listener gets the list of conversation metadata objects in the index
     */
    public void getConversations(int from, int maxResults, ActionListener<List<ConvoMeta>> listener) {
        if(!clusterService.state().metadata().hasIndex(indexName)){
            listener.onResponse(List.of());
        }
        SearchRequest request = Requests.searchRequest(indexName);
        String userstr = client.threadPool().getThreadContext().getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
        QueryBuilder queryBuilder;
        if(userstr == null) 
            queryBuilder = new MatchAllQueryBuilder();  
        else 
            queryBuilder = new TermQueryBuilder(ConvoIndexConstants.USER_FIELD, User.parse(userstr).getName());
        request.source().query(queryBuilder);
        request.source().from(from).size(maxResults);
        request.source().sort(ConvoIndexConstants.META_ENDED_FIELD, SortOrder.DESC);
        try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().newStoredContext(true)) {
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
     * list size conversations in the index
     * @param maxResults how many conversations to list
     * @param listener gets the list of conversation metadata objects in the index
     */
    public void getConversations(int maxResults, ActionListener<List<ConvoMeta>> listener) {
        getConversations(0, maxResults, listener);
    }

    /**
     * Update a conversation's metadata with a new hit
     * @param id id of the conversation to touch
     * @param hitTime when this conversation got toushed
     * @param listener gets whether the operation was successful
     */
    public void hitConversation(String id, Instant hitTime, ActionListener<Boolean> listener) {
        GetRequest getRequest = Requests.getRequest(indexName).id(id);
        String userstr = client.threadPool().getThreadContext().getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
        try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().newStoredContext(true)) {
            ActionListener<Boolean> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
            ActionListener<GetResponse> al = ActionListener.wrap(getResponse -> {
                if(!(getResponse.isExists() && getResponse.getId().equals(id))){
                    internalListener.onResponse(false);
                }
                ConvoMeta convo = ConvoMeta.fromMap(id, getResponse.getSourceAsMap());
                if(userstr != null) {
                    String user = User.parse(userstr).getName();
                    if(! user.equals(convo.getUser())) {
                        throw new OpenSearchSecurityException("User [" + user + "] does not have access to conversation " + id);
                    }
                }
                UpdateRequest update = (new UpdateRequest(indexName, id)).doc(convo.hit(hitTime).toIndexRequest(indexName));
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


    /**
     * Deletes a conversation from the conversation metadata index
     * @param conversationId id of the conversation to delete
     * @param listener gets whether the deletion was successful
     */
    public void deleteConversation(String conversationId, ActionListener<Boolean> listener) {
        if(!clusterService.state().metadata().hasIndex(indexName)) {
            listener.onResponse(true);
        }
        DeleteRequest delRequest = Requests.deleteRequest(indexName).id(conversationId);
        try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().newStoredContext(true)) {
            ActionListener<Boolean> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
            // When we get the delete response, do this:
            ActionListener<DeleteResponse> al = ActionListener.wrap(deleteResponse -> {
                if(deleteResponse.getResult() == Result.DELETED) {
                    internalListener.onResponse(true);
                } else if(deleteResponse.status() == RestStatus.NOT_FOUND) {
                    internalListener.onResponse(true);
                } else {
                    internalListener.onResponse(false);
                }
            }, e -> {
                log.error("failure deleting conversation " + conversationId, e);
                internalListener.onFailure(e);
            });
            this.checkAccess(conversationId, ActionListener.wrap(access -> {
                if(access) {
                    client.delete(delRequest, al);
                } else {
                    String userstr = client.threadPool().getThreadContext().getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
                    if(userstr != null) {
                        String user = User.parse(userstr).getName();
                        throw new OpenSearchSecurityException("User [" + user + "] does not have access to conversation " + conversationId);
                    }
                }
            }, e -> {
                internalListener.onFailure(e);
            }));
        } catch (Exception e) {
            log.error("failed deleting conversation with id=" + conversationId, e);
            listener.onFailure(e);
        }
    }

    /**
     * Checks whether the current requesting user has permission to see this conversation
     * @param conversationId the conversation to check
     * @param listener receives whether access should be granted
     */
    public void checkAccess(String conversationId, ActionListener<Boolean> listener) {
        // If the index doesn't exist, you have permission. Just won't get you anywhere
        if(!clusterService.state().metadata().hasIndex(indexName)) {
            listener.onResponse(true);
            return;
        }
        try (ThreadContext.StoredContext threadContext = client.threadPool().getThreadContext().newStoredContext(true)) {
            ActionListener<Boolean> internalListener = ActionListener.runBefore(listener, () -> threadContext.restore());
            String userstr = client.threadPool().getThreadContext().getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
            log.info("USERSTR: " + userstr);
            // If security is off - User doesn't exist - you have permission
            if(userstr == null || User.parse(userstr) == null) {
                internalListener.onResponse(true);
                return;
            }
            GetRequest getRequest = Requests.getRequest(indexName).id(conversationId);
            ActionListener<GetResponse> al = ActionListener.wrap(getResponse -> {
                // If the conversation doesn't exist, you have permission
                if(!(getResponse.isExists() && getResponse.getId().equals(conversationId))){
                    internalListener.onResponse(true);
                    return;
                }
                ConvoMeta convo = ConvoMeta.fromMap(conversationId, getResponse.getSourceAsMap());
                String user = User.parse(userstr).getName();
                // If you're not the owner of this conversation, you do not have permission
                if(! user.equals(convo.getUser())) {
                    internalListener.onResponse(false);
                    return;
                }
                internalListener.onResponse(true);
            }, e -> {
                internalListener.onFailure(e);
            });
            client.get(getRequest, al);
        } catch(Exception e) {
            listener.onFailure(e);
        }
    }
}