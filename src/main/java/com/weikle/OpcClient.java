package com.weikle;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;


public class OpcClient {
    private static final Logger log = LoggerFactory.getLogger(OpcClient.class);
    private boolean initialize;
    private OpcUaClient opcUaClient;

    private OpcClient() {
    }

    public static OpcClient getInstance() {
        return Holder.INSTANCE;
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

    public Boolean readNodeValue(String browseName) throws Exception {
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }
        //创建变量节点
        NodeId nodeId = new NodeId(2, browseName);
        DataValue value = opcUaClient.readValue(0.0, TimestampsToReturn.Both, nodeId).get();
        System.out.println(value);
        return (Boolean) value.getValue().getValue();
    }

    public void readValue() throws ExecutionException, InterruptedException {
        //创建连接
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }

        NodeId nodeId = new NodeId(3,"\"test_value\"");

        DataValue value = opcUaClient.readValue(0.0, TimestampsToReturn.Both, nodeId).get();

        System.out.println((Integer)value.getValue().getValue());
    }

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

    /**
     * 浏览节点
     */
    public void browseNode() throws Exception {
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }
       opcUaClient.getAddressSpace().browse(Identifiers.RootFolder).forEach(node->{
            System.out.println("Node= " + node.getBrowseName().getName());
       });

    }

    public void subscription() throws ExecutionException, InterruptedException {
        OpcUaSession opcUaSession = opcUaClient.getSession().get();
        if (null == opcUaSession) {
            //创建连接
            opcUaClient.connect().get();
        }

    }

    private static class Holder {
        private static OpcClient INSTANCE = new OpcClient();
    }
}
