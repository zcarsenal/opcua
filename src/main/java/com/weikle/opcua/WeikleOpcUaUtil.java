package com.weikle.opcua;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.*;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class WeikleOpcUaUtil {

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final Lock lock = new ReentrantLock();

    final Condition notificationArrived = lock.newCondition();

    private final CompletableFuture<OpcUaClient> future = new CompletableFuture<>();

    private final WeikleOpcUaConfig weikleOpcUaConfig;

    private OpcUaClient client;


    public WeikleOpcUaUtil(WeikleOpcUaConfig  weikleOpcUaConfig){
        this.weikleOpcUaConfig = weikleOpcUaConfig;
        try{
            client = createClient();
            client.connect().get();
        }catch (Exception e){
            e.printStackTrace();
            logger.error("创建opc连接失败，失败原因{}",e.getMessage());
        }
    }

    /**
     * 建立opc客户端连接
     * @return
     * @throws Exception
     */
    private OpcUaClient createClient() throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }

        LoggerFactory.getLogger(getClass())
                .info("security temp dir: {}", securityTempDir.toAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        return OpcUaClient.create(
                weikleOpcUaConfig.getEndpointUrl(),
                endpoints ->
                        endpoints.stream()
                                .filter(weikleOpcUaConfig.endpointFilter())
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                                .setApplicationUri("urn:eclipse:milo:examples:client")
                                .setCertificate(loader.getClientCertificate())
                                .setKeyPair(loader.getClientKeyPair())
                                .setIdentityProvider(weikleOpcUaConfig.getIdentityProvider())
                                .setRequestTimeout(uint(5000))
                                .build()
        );
    }

    public List<NodeId> obtainAllNodeIds(String path) {
        reconnect();
        // start browsing at root folder
        List<NodeId> nodeIds = Lists.newArrayList();
        populateNodes(path,nodeIds);
        return nodeIds;
    }

    public void subscription(List<String> itemNames, Consumer<ItemChangeData> consumer){
        //connect server
        reconnect();
        UaSubscription.NotificationListener notificationListener = new UaSubscription.NotificationListener() {
            @Override
            public void onDataChangeNotification(UaSubscription subscription, List<UaMonitoredItem> items, List<DataValue> values, DateTime publishTime) {
                for (int i = 0; i < values.size(); i++) {
                    ItemChangeData data = new ItemChangeData();
                    data.setNodeId(items.get(i).getReadValueId().getNodeId());
                    data.setValue(values.get(i).getValue());
                    consumer.accept(data);
                }
            }
        };

        client.getSubscriptionManager().addSubscriptionListener(new UaSubscriptionManager.SubscriptionListener() {
            @Override
            public void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
                Stack.sharedExecutor().execute(() -> createItemAndWait(client,notificationListener,itemNames));
            }
        });

        createItemAndWait(client,notificationListener,itemNames);
    }

    private void createItemAndWait(OpcUaClient client, UaSubscription.NotificationListener notificationListener, List<String> itemNames) {

        // create a subscription and a monitored item
        UaSubscription subscription = null;
        try {
            subscription = client.getSubscriptionManager().createSubscription(weikleOpcUaConfig.requestedPublishingInterval()).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
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
                    weikleOpcUaConfig.requestedPublishingInterval(),     // sampling interval
                    null,       // filter, null means use default
                    uint(10),   // queue size
                    true        // discard oldest
            );
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
            requests.add(request);
        }

        try {
            lock.lock();
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


    public void write(String itemName,Object value) throws ExecutionException, InterruptedException {
        reconnect();
        NodeId nodeId = new NodeId(2, itemName);
        Variant v = new Variant(value);
        DataValue dv = new DataValue(v, null, null);
        // write asynchronously....
        CompletableFuture<StatusCode> f = client.writeValue(nodeId, dv);

        // ...but block for the results so we write in order
        StatusCode status = f.get();

        if (status.isGood()) {
            logger.info("Wrote '{}' to nodeId={} success", v, nodeId);
        } else {
            logger.error("Wrote '{}' to nodeId={} fail", v, nodeId);
        }

        f.complete(status);

    }

    private void reconnect() {
        // synchronous connect
       try{
           OpcUaSession opcUaSession = client.getSession().get();
           if (null == opcUaSession) {
               //创建连接
               client.connect().get();
           }
       } catch (InterruptedException e) {
           e.printStackTrace();
       } catch (ExecutionException e) {
           e.printStackTrace();
       }
    }

    public Object syncReadNodeValue(String itemName) throws Exception {
        reconnect();
        UaVariableNode node = client.getAddressSpace().getVariableNode(new NodeId(2, itemName));
        DataValue value = node.readValue();

        logger.info("node value={}",value.getValue().getValue());
        return value.getValue().getValue();
    }

    public Object asyncReadNodeValue(String itemName) throws Exception {
        reconnect();
        AtomicReference<Object> value = new AtomicReference<>();
        // asynchronous read request
        readServerStateAndTime(client,new NodeId(2, itemName)).thenAccept(values -> {
            DataValue dataValue = values.get(0);
            value.set(dataValue.getValue().getValue());
            future.complete(client);
        });
        future.get();
        return value.get();
    }

    private CompletableFuture<List<DataValue>> readServerStateAndTime(OpcUaClient client,NodeId node) {
        List<NodeId> nodeIds = ImmutableList.of(node);
        return client.readValues(0.0, TimestampsToReturn.Both,nodeIds);
    }

    public void populateNodes(String browse, List<NodeId> nodeIds) {
        try {
            NodeId nodeId = new NodeId(2, browse);
            List<ReferenceDescription> nodes = client
                    .getAddressSpace()
                    .browse(nodeId);
            for (ReferenceDescription node : nodes) {
                // recursively browse to children
                populateNodes(String.valueOf(node.getNodeId().getIdentifier()), nodeIds);
                try {
                    nodeIds.add(node.getNodeId().toNodeIdOrThrow(client.getNamespaceTable()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (UaException e) {
            e.printStackTrace();
        }
    }

}
