/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.weikle.opcua.test;

import com.google.common.collect.ImmutableList;
import com.weikle.opcua.WeikleOpcUaClient;
import com.weikle.opcua.WeikleOpcUaClientRunner;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WriteExample implements WeikleOpcUaClient {

    public static void main(String[] args) throws Exception {
        WriteExample example = new WriteExample();

        new WeikleOpcUaClientRunner(example).run();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        List<NodeId> nodeIds = ImmutableList.of(new NodeId(2, "T14.Shlv.T1.Indicator18_1_2"));

        while(true){


        for (int i = 0; i < 2; i++) {
            Variant v = null;
            if(i == 0){
                 v = new Variant(false);
            } else if(i ==1){
                 v = new Variant(true);
            }

            // don't write status or timestamps
            DataValue dv = new DataValue(v, null, null);

            // write asynchronously....
            CompletableFuture<List<StatusCode>> f =
                client.writeValues(nodeIds, ImmutableList.of(dv));

            // ...but block for the results so we write in order
            List<StatusCode> statusCodes = f.get();
            StatusCode status = statusCodes.get(0);

            if (status.isGood()) {
                logger.info("Wrote '{}' to nodeId={}", v, nodeIds.get(0));
            }

            Thread.sleep(1000);
        }
        }
        // future.complete(client);
    }

}
