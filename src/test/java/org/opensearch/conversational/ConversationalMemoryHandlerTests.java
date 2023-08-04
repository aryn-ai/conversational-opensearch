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
                    cdl.countDown(); cdl.countDown(); log.error(e); assert(false);
                }), cdl));
            }, e -> {
                cdl.countDown(); cdl.countDown(); log.error(e); assert(false);
            }), cdl));
        }, e -> {
            cdl.countDown(); cdl.countDown(); log.error(e); assert(false);
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
            cmHandler.createInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid1Listener);
        }, e -> {
            cdl.countDown(); assert(false);
        });

        StepListener<String> iid2Listener = new StepListener<>();
        iid1Listener.whenComplete(iid -> {
            cmHandler.createInteraction(cidListener.result(), "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2Listener);
        }, e -> {
            cdl.countDown(); assert(false);
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
            cmHandler.createInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid1Listener);
        }, e -> {
            cdl.countDown(); assert(false);
        });

        StepListener<String> iid2Listener = new StepListener<>();
        iid1Listener.whenComplete(iid -> {
            cmHandler.createInteraction(cidListener.result(), "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2Listener);
        }, e -> {
            cdl.countDown(); assert(false);
        });

        StepListener<List<Interaction>> interactionsListener = new StepListener<>();
        iid2Listener.whenComplete(
            iid2 -> {cmHandler.getInteractions(cidListener.result(), 0, 2, interactionsListener);}, 
            e -> {cdl.countDown(); assert(false);}
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
            cmHandler.getConversations(10, finishAndAssert);
        }, e -> {cdl.countDown(); assert(false);});

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
            cmHandler.createInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid1Listener);
        }, e -> {
            cdl.countDown(); assert(false);
        });

        StepListener<String> iid2Listener = new StepListener<>();
        iid1Listener.whenComplete(iid -> {
            cmHandler.createInteraction(cidListener.result(), "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2Listener);
        }, e -> {
            cdl.countDown(); assert(false);
        });

        StepListener<List<Interaction>> interactionsListener = new StepListener<>();
        iid2Listener.whenComplete(
            iid2 -> {cmHandler.getInteractions(cidListener.result(), 0, 10, interactionsListener);}, 
            e -> {cdl.countDown(); assert(false);}
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
            cmHandler.getConversations(10, finishAndAssert);
        }, e -> {cdl.countDown(); assert(false);});

        try { 
            cdl.await(); 
        } catch (InterruptedException e) { 
            log.error(e); 
        }
    }

    public void testCanDeleteConversations() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> cid1 = new StepListener<>();
        cmHandler.createConversation("test", cid1);

        StepListener<String> iid1 = new StepListener<>();
        cid1.whenComplete(cid -> {
            cmHandler.createInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid1);
        }, e -> {cdl.countDown(); assert(false); });

        StepListener<String> iid2 = new StepListener<>();
        iid1.whenComplete(iid -> {
            cmHandler.createInteraction(cid1.result(), "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid2);
        }, e -> {cdl.countDown(); assert(false); });

        StepListener<String> cid2 = new StepListener<>();
        iid2.whenComplete(iid -> {
            cmHandler.createConversation(cid2);
        }, e -> { cdl.countDown(); assert(false); });

        StepListener<String> iid3 = new StepListener<>();
        cid2.whenComplete(cid -> {
            cmHandler.createInteraction(cid, "test input1", "test prompt", "test response", 
                "test agent", "{\"test\":\"metadata\"}", iid3);
        }, e -> {cdl.countDown(); assert(false); });

        StepListener<Boolean> del = new StepListener<>();
        iid3.whenComplete(iid -> {
            cmHandler.deleteConversation(cid1.result(), del);
        }, e -> {cdl.countDown(); assert(false);});

        StepListener<List<ConvoMeta>> convos = new StepListener<>();
        del.whenComplete(success -> {
            cmHandler.getConversations(10, convos);
        }, e -> {cdl.countDown(); assert(false); });

        StepListener<List<Interaction>> inters1 = new StepListener<>();
        convos.whenComplete(cons -> {
            cmHandler.getInteractions(cid1.result(), 0, 10, inters1);
        }, e -> {cdl.countDown(); assert(false); });

        StepListener<List<Interaction>> inters2 = new StepListener<>();
        inters1.whenComplete(ints -> {
            cmHandler.getInteractions(cid2.result(), 0, 10, inters2);
        }, e -> {cdl.countDown(); assert(false);});

        LatchedActionListener<List<Interaction>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            r -> {
                assert(del.result());
                assert(convos.result().size() == 1);
                assert(convos.result().get(0).getId().equals(cid2.result()));
                assert(inters1.result().size() == 0);
                assert(inters2.result().size() == 1);
                assert(inters2.result().get(0).getId().equals(iid3.result()));
            }, e -> {
                assert(false);
            }
        ), cdl);
        inters2.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);

        try { 
            cdl.await(); 
        } catch (InterruptedException e) { 
            log.error(e); 
        }
    }


}