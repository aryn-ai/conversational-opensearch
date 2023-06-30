/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.conversational.index;

import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;

public class ConvoMetaIndex {
    private Client client;
    private ClusterService clusterService;

    public ConvoMetaIndex(Client client, ClusterService clusterService){
        this.client = client;
        this.clusterService = clusterService;
    }

    public void initConvoMetaIndexIfAbsent() {
        
    }

}