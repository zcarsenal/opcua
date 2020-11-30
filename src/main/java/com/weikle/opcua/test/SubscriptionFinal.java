package com.weikle.opcua.test;

import com.google.common.collect.Lists;
import com.weikle.opcua.WeikleOpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class SubscriptionFinal implements WeikleOpcUaClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final Lock lock = new ReentrantLock();
    final Condition notificationArrived = lock.newCondition();

    private List<String> itemNames;

    public List<String> getItemNames() {
        return itemNames;
    }

    public void setItemNames(List<String> itemNames) {
        this.itemNames = itemNames;
    }

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        //connect server
        OpcUaSession opcUaSession = client.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            client.connect().get();
        }
        UaSubscription.NotificationListener notificationListener = new UaSubscription.NotificationListener() {
            @Override
            public void onDataChangeNotification(UaSubscription subscription, List<UaMonitoredItem> items, List<DataValue> values, DateTime publishTime) {
                for (int i = 0; i < values.size(); i++) {
                    logger.info(
                            "subscription value received: item={}, value={}",
                            items.get(i).getReadValueId().getNodeId(), values.get(i).getValue()
                    );
                }
            }
        };

        client.getSubscriptionManager().addSubscriptionListener(new UaSubscriptionManager.SubscriptionListener() {
            @Override
            public void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
                Stack.sharedExecutor().execute(() -> {
                    try {
                        createItemAndWait(client,notificationListener,itemNames);
                        //future.complete(null);
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Error creating Subscription: {}", e.getMessage(), e);

                        //future.completeExceptionally(e);
                    }
                });
            }
        });

        createItemAndWait(client,notificationListener,itemNames);
    }

    private void createItemAndWait(OpcUaClient client, UaSubscription.NotificationListener notificationListener, List<String> itemNames) throws InterruptedException, ExecutionException {

        // create a subscription and a monitored item
        UaSubscription subscription = client.getSubscriptionManager().createSubscription(1.0).get();
        subscription.addNotificationListener(notificationListener);
        List<MonitoredItemCreateRequest> requests = Lists.newArrayList();
        for (String itemName : itemNames) {
            //创建订阅的变量
            ReadValueId readValueId = new ReadValueId(new NodeId(2, itemName), AttributeId.Value.uid(),null,null);
            //创建监控项请求
            //该请求最后用于创建订阅。
            UInteger clientHandle = subscription.nextClientHandle();
            MonitoringParameters parameters = new MonitoringParameters(
                    clientHandle,
                    1.0,     // sampling interval
                    null,       // filter, null means use default
                    uint(10),   // queue size
                    true        // discard oldest
            );
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
            requests.add(request);
        }

        lock.lock();
        try {
            List<UaMonitoredItem> items = subscription.createMonitoredItems(TimestampsToReturn.Both, requests).get();
            for (UaMonitoredItem item : items) {
                if (item.getStatusCode().isGood()) {
                    logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
                } else {
                    logger.warn(
                            "failed to create item for nodeId={} (status={})",
                            item.getReadValueId().getNodeId(), item.getStatusCode());
                }
            }
            notificationArrived.await(5, TimeUnit.SECONDS);
        } finally {
            lock.unlock();
        }
    }


}
