/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.conversational;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.opensearch.action.ActionListener;
import org.opensearch.action.LatchedActionListener;
import org.opensearch.action.StepListener;
import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.conversational.index.ConvoMeta;
import org.opensearch.conversational.index.Interaction;
import org.opensearch.test.OpenSearchIntegTestCase;



@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 2)
public class ConversationalMemoryHandlerTests extends OpenSearchIntegTestCase {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConversationalMemoryHandlerTests.class);


    private Client client;
    private ClusterService clusterService;
    private ConversationalMemoryHandler cmHandler;

    @Before
    private void setup() {
        client = client();
        clusterService = clusterService();
        cmHandler = new ConversationalMemoryHandler(client, clusterService);
    }

    public void testCanStartConvos() {
        CountDownLatch cdl = new CountDownLatch(3);
        cmHandler.createConversation("test-1", new LatchedActionListener<String>(ActionListener.wrap(cid0 -> {
            cmHandler.createConversation("test-2", new LatchedActionListener<String>(ActionListener.wrap(cid1 -> {
                cmHandler.createConversation(new LatchedActionListener<String>(ActionListener.wrap(cid2 -> {
                    assert(!cid0.equals(cid1) && !cid0.equals(cid2) && !cid1.equals(cid2));
                }, e -> {
                    log.error(e); assert(false);
                }), cdl));
            }, e -> {
                log.error(e); assert(false);
            }), cdl));
        }, e -> {
            log.error(e); assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    public void testCanAddNewInteractionsToConvo() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> cidListener = new StepListener<>();
        cmHandler.createConversation("test", cidListener);

        StepListener<String> iid1Listener = new StepListener<>();
        cidListener.whenComplete(cid -> {
            cmHandler.putInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid1Listener);
        }, e -> {
            assert(false);
        });

        StepListener<String> iid2Listener = new StepListener<>();
        iid1Listener.whenComplete(iid -> {
            cmHandler.putInteraction(cidListener.result(), "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2Listener);
        }, e -> {
            assert(false);
        });

        LatchedActionListener<String> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            iid2 -> {assert(!iid2.equals(iid1Listener.result()));}, e -> {assert(false);}
        ), cdl);
        iid2Listener.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }


    public void testCanGetInteractionsBackOut() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> cidListener = new StepListener<>();
        cmHandler.createConversation("test", cidListener);

        StepListener<String> iid1Listener = new StepListener<>();
        cidListener.whenComplete(cid -> {
            cmHandler.putInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid1Listener);
        }, e -> {
            assert(false);
        });

        StepListener<String> iid2Listener = new StepListener<>();
        iid1Listener.whenComplete(iid -> {
            cmHandler.putInteraction(cidListener.result(), "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2Listener);
        }, e -> {
            assert(false);
        });

        StepListener<List<Interaction>> interactionsListener = new StepListener<>();
        iid2Listener.whenComplete(
            iid2 -> {cmHandler.getInteractions(cidListener.result(), interactionsListener);}, 
            e -> {assert(false);}
        );

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            conversations -> {
                List<Interaction> interactions = interactionsListener.result();
                String id1 = iid1Listener.result();
                String id2 = iid2Listener.result();
                String cid = cidListener.result();
                assert(interactions.size() == 2);
                assert(interactions.get(0).getId().equals(id2));
                assert(interactions.get(1).getId().equals(id1));
                assert(conversations.size() == 1);
                assert(conversations.get(0).getId().equals(cid));
            }, e -> {
                assert(false);
            }
        ), cdl);
        interactionsListener.whenComplete(r -> {
            cmHandler.listConversations(finishAndAssert);
        }, e -> {assert(false);});

        try { 
            cdl.await(); 
        } catch (InterruptedException e) { 
            log.error(e); 
        }
    }

    public void testConvoMetaIsUpToDateWithHits() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> cidListener = new StepListener<>();
        cmHandler.createConversation("test", cidListener);

        StepListener<String> iid1Listener = new StepListener<>();
        cidListener.whenComplete(cid -> {
            cmHandler.putInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid1Listener);
        }, e -> {
            assert(false);
        });

        StepListener<String> iid2Listener = new StepListener<>();
        iid1Listener.whenComplete(iid -> {
            cmHandler.putInteraction(cidListener.result(), "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2Listener);
        }, e -> {
            assert(false);
        });

        StepListener<List<Interaction>> interactionsListener = new StepListener<>();
        iid2Listener.whenComplete(
            iid2 -> {cmHandler.getInteractions(cidListener.result(), interactionsListener);}, 
            e -> {assert(false);}
        );

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            conversations -> {
                List<Interaction> interactions = interactionsListener.result();
                assert(conversations.size() == 1);
                ConvoMeta convo = conversations.get(0);
                assert(convo.getLastHit().equals(interactions.get(0).getTimestamp()));
                assert(convo.getLength() == 2);
            }, e -> {
                assert(false);
            }
        ), cdl);
        interactionsListener.whenComplete(r -> {
            cmHandler.listConversations(finishAndAssert);
        }, e -> {assert(false);});

        try { 
            cdl.await(); 
        } catch (InterruptedException e) { 
            log.error(e); 
        }
    }
}