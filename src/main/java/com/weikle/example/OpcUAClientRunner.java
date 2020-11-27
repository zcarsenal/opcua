package com.weikle.example;

import com.google.common.collect.Lists;
import com.weikle.AndonConstants;
import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUAClientRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CompletableFuture<OpcUaClient> future = new CompletableFuture<>();

    private final OpcUAClientService opcUAClientService;

    public OpcUAClientRunner(OpcUAClientService opcUAClientService) {
        this.opcUAClientService = opcUAClientService;
    }

    private boolean initialize;

    private OpcUaClient opcUaClient;

    /**
     * 写入节点数据
     */
    public Boolean writeNodeValue(Boolean value, String browseName) throws Exception {
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }

        //创建变量节点
        NodeId nodeId = new NodeId(2, browseName);

        //创建Variant对象和DataValue对象
        Variant v = new Variant(value);
        DataValue dataValue = new DataValue(v, null, null);
        StatusCode statusCode = opcUaClient.writeValue(nodeId, dataValue).get();
        if (statusCode.isBad()) {
            nodeId = new NodeId(2, browseName);
            statusCode = opcUaClient.writeValue(nodeId, dataValue).get();
        }
        return statusCode.isGood();
    }

    public void createSubscription(List<String> itemNames) throws ExecutionException, InterruptedException {
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }

        //创建发布间隔1000ms的订阅对象
        UaSubscription subscription = opcUaClient.getSubscriptionManager().createSubscription(1).get();

        //创建监控的参数
        MonitoringParameters parameters = new MonitoringParameters(
                uint(1),
                1.0,     // sampling interval
                null,       // filter, null means use default
                uint(10),   // queue size
                true        // discard oldest
        );

        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        for (String item : itemNames ) {
            //创建订阅的变量
            ReadValueId readValueId = new ReadValueId(new NodeId(2, item), AttributeId.Value.uid(),null,null);
            //创建监控项请求
            //该请求最后用于创建订阅。
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
            requests.add(request);
        }

        //创建监控项，并且注册变量值改变时候的回调函数。
        subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item,id)->{
                    item.setValueConsumer((item1, value)->{
                        System.out.println("nodeid :"+item1.getReadValueId().getNodeId());
                        System.out.println("value :"+value.getValue().getValue());
                    });
                }
        );

    }




    public void createSubscription2(List<String> itemNames) throws ExecutionException, InterruptedException, UaException {
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }

        //创建发布间隔1000ms的订阅对象
        //UaSubscription subscription = opcUaClient.getSubscriptionManager().createSubscription(1).get();
        OpcUaSubscription subscription = (OpcUaSubscription) opcUaClient.getSubscriptionManager().createSubscription(1).get();

        UInteger clientHandle = subscription.nextClientHandle();
        MonitoringParameters parameters = new MonitoringParameters(
                clientHandle,
                1.0,     // sampling interval
                null,       // filter, null means use default
                uint(10),   // queue size
                true        // discard oldest
        );

        List<ReadValueId> list = Lists.newArrayList();

        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        for (String item : itemNames ) {
            //创建订阅的变量
            ReadValueId readValueId = new ReadValueId(new NodeId(2, item), AttributeId.Value.uid(),null,null);
            //创建监控项请求
            //该请求最后用于创建订阅。
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
            requests.add(request);

            list.add(readValueId);
        }
        ManagedSubscription managedSubscription = new ManagedSubscription(opcUaClient,subscription);

    // 创建监控项，并且注册变量值改变时候的回调函数。
        //创建监控项，并且注册变量值改变时候的回调函数。
        subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item,id)->{
                    item.setValueConsumer((item1, value)->{
                        System.out.println(item1.getReadValueId().getNodeId() +" "+value.getValue().getValue());
                    });
                }
        );

    ManagedSubscription.StatusListener statusListener =
        new ManagedSubscription.StatusListener() {
          @Override
          public void onNotificationDataLost(ManagedSubscription subscription) {
            System.out.println(2222222);
          }

          @Override
          public void onSubscriptionStatusChanged(
              ManagedSubscription subscription, StatusCode statusCode) {
            System.out.println(111111);
          }

          @Override
          public void onSubscriptionTransferFailed(
              ManagedSubscription subscription, StatusCode statusCode) {
            System.out.println(333333);
          }
        };

        managedSubscription.addStatusListener(statusListener);

    }


    public void createSubscription() throws ExecutionException, InterruptedException {
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }



        //创建发布间隔1000ms的订阅对象
        UaSubscription subscription = opcUaClient.getSubscriptionManager().createSubscription(1).get();
        UInteger clientHandle = subscription.nextClientHandle();
        MonitoringParameters parameters = new MonitoringParameters(
                clientHandle,
                1000.0,     // sampling interval
                null,       // filter, null means use default
                uint(10),   // queue size
                true        // discard oldest
        );
        //创建订阅的变量
        NodeId nodeId_Tag1 = new NodeId(2, "D1ShelvesSet.IOMdl.BpsButton_4_1");
        NodeId nodeId_Tag2 = new NodeId(2, "D1ShelvesSet.IOMdl.CfmButton_5_1");
        ReadValueId readValueId = new ReadValueId(nodeId_Tag1, AttributeId.Value.uid(),null,null);
        ReadValueId readValueId2 = new ReadValueId(nodeId_Tag2, AttributeId.Value.uid(),null,null);

        //创建监控项请求
        //该请求最后用于创建订阅。
        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
        MonitoredItemCreateRequest request2 = new MonitoredItemCreateRequest(readValueId2, MonitoringMode.Reporting, parameters);

        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        requests.add(request);
        requests.add(request2);

        //创建监控项，并且注册变量值改变时候的回调函数。
        List<UaMonitoredItem> items = subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item,id)->{
                    item.setValueConsumer((item1, value)->{
                        System.out.println("nodeid :"+item1.getReadValueId().getNodeId());
                        System.out.println("value :"+value.getValue().getValue());
                    });
                }
        ).get();
    }


    public void initialize() {
        if (initialize) {
            return;
        }

        //JdbcTemplate jdbcTemplate = SpringContextUtil.getBean(JdbcTemplate.class);
        //if (StrUtil.isBlank(AndonConstants.opcEndPoint)) {
        //String deptId = SpringContextUtil.getProperty("andon.dept.id");
        //AndonConstants.opcEndPoint = jdbcTemplate.queryForObject("select server_address from andon.andon_opc_server where dept_id=" + deptId, String.class);
        AndonConstants.opcEndPoint = "opc.tcp://192.168.150.243:49320";
        //}
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        try {
            Files.createDirectories(securityTempDir);
            if (!Files.exists(securityTempDir)) {
                throw new Exception("没有创建安全目录: " + securityTempDir);
            }
            log.info("安全目录: {}", securityTempDir.toAbsolutePath());

            //安全策略 None、Basic256、Basic128Rsa15、Basic256Sha256
            SecurityPolicy securityPolicy = SecurityPolicy.None;

            List<EndpointDescription> endpoints;

            try {
                endpoints = DiscoveryClient.getEndpoints(AndonConstants.opcEndPoint).get();
            } catch (Throwable ex) {
                // 发现服务
                String discoveryUrl = AndonConstants.opcEndPoint;

                if (!discoveryUrl.endsWith("/")) {
                    discoveryUrl += "/";
                }
                discoveryUrl += "discovery";

                log.info("开始连接 URL: {}", discoveryUrl);
                endpoints = DiscoveryClient.getEndpoints(discoveryUrl).get();
            }

            EndpointDescription endpoint = endpoints.stream()
                    .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
                    .filter(e -> true)
                    .findFirst()
                    .orElseThrow(() -> new Exception("没有连接上端点"));

            log.info("使用端点: {} [{}/{}]", endpoint.getEndpointUrl(), securityPolicy, endpoint.getSecurityMode());

            OpcUaClientConfig config = OpcUaClientConfig.builder()
                    .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                    .setApplicationUri("urn:eclipse:milo:examples:client")
                    .setEndpoint(endpoint)
                    //根据匿名验证和第三个用户名验证方式设置传入对象 AnonymousProvider（匿名方式）UsernameProvider（账户密码）
                    //new UsernameProvider("admin","123456")
                    .setIdentityProvider(new AnonymousProvider())
                    .setRequestTimeout(uint(5000))
                    .build();
            opcUaClient = OpcUaClient.create(config);
            //创建连接
            opcUaClient.connect().get();
            initialize = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化OPC客户端异常", e);
        }
    }

    /**
     * OPC UA的运行入口程序
     */
    public void run() {
        try {
            // 创建OPC UA客户端
            initialize();
            // future执行完毕后，异步判断状态
            future.whenCompleteAsync((c, ex) -> {
                if (ex != null) {
                    log.error("连接OPC UA服务错误: {}", ex.getMessage(), ex);
                }
                // 关闭OPC UA客户端
                try {
                    opcUaClient.disconnect().get();
                    Stack.releaseSharedResources();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("OPC UA服务关闭错误: {}", e.getMessage(), e);
                }
            });

            try {
                // 获取OPC UA服务器的数据
                opcUAClientService.run(opcUaClient, future);
                future.get(1, TimeUnit.SECONDS);
            } catch (Throwable t) {
                log.error("OPC UA客户端运行错误: {}", t.getMessage(), t);
                future.completeExceptionally(t);
            }
        } catch (Throwable t) {
            log.error("OPC UA客户端创建错误: {}", t.getMessage(), t);
            future.completeExceptionally(t);
        }
    }

    public AddressSpace getAddressSpace() {
        return opcUaClient.getAddressSpace();
    }

    /**
     * 创建OPC UA的服务连接对象
     */
/*    private OpcUaClient createClient() throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("不能够创建安全路径: " + securityTempDir);
        }
        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);
        // 获取OPC UA的服务器端节点
        EndpointDescription[] endpoints =
                UaTcpStackClient.getEndpoints(opcUAClientService.getEndpointUrl()).get();
        EndpointDescription endpoint = Arrays.stream(endpoints)
                .filter(e -> e.getEndpointUrl().equals(opcUAClientService.getEndpointUrl()))
                .findFirst().orElseThrow(() -> new Exception("没有节点返回"));

        // 设置OPC UA的配置信息
        OpcUaClientConfig config =
                OpcUaClientConfig.builder()
                        .setApplicationName(LocalizedText.english("OPC UA SCREEN"))
                        .setApplicationUri("urn:DATA-TRANSFER:OPC UA SCREEN")
                        .setCertificate(loader.getClientCertificate())
                        .setKeyPair(loader.getClientKeyPair())
                        .setEndpoint(endpoint)
                        .setIdentityProvider(new UsernameProvider("Administrator", "123456"))
                        .setRequestTimeout(uint(5000))
                        .build();
        // 创建OPC UA客户端
        return new OpcUaClient(config);
    }*/
}
