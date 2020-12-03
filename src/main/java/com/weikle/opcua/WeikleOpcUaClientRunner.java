package com.weikle.opcua;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class WeikleOpcUaClientRunner {

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CompletableFuture<OpcUaClient> future = new CompletableFuture<>();

    private final WeikleOpcUaClient weikleOpcUaClient;


    public WeikleOpcUaClientRunner(WeikleOpcUaClient weikleOpcUaClient) throws Exception {
        this.weikleOpcUaClient = weikleOpcUaClient;
    }

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
                weikleOpcUaClient.getEndpointUrl(),
                endpoints ->
                        endpoints.stream()
                                .filter(weikleOpcUaClient.endpointFilter())
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                                .setApplicationUri("urn:eclipse:milo:examples:client")
                                .setCertificate(loader.getClientCertificate())
                                .setKeyPair(loader.getClientKeyPair())
                                .setIdentityProvider(weikleOpcUaClient.getIdentityProvider())
                                .setRequestTimeout(uint(5000))
                                .build()
        );
    }

    public void run() {
        try {
            OpcUaClient client = createClient();

            future.whenCompleteAsync((c, ex) -> {
                if (ex != null) {
                    logger.error("Error running example: {}", ex.getMessage(), ex);
                }

                try {
                    client.disconnect().get();
                    Stack.releaseSharedResources();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error disconnecting: {}", e.getMessage(), e);
                }

                try {
                    Thread.sleep(1000);
                    System.exit(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            try {
                client.connect().get();
                weikleOpcUaClient.run(client, future);
                //future.get(150, TimeUnit.SECONDS);
                future.get();
            } catch (Throwable t) {
                logger.error("Error running client example: {}", t.getMessage(), t);
                //future.completeExceptionally(t);
            }
        } catch (Throwable t) {
            logger.error("Error getting client: {}", t.getMessage(), t);

            future.completeExceptionally(t);
        }
    }

}
