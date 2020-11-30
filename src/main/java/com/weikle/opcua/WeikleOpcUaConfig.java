package com.weikle.opcua;

import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import java.util.function.Predicate;

public interface WeikleOpcUaConfig {

    default String getEndpointUrl() {
        return "opc.tcp://192.168.150.243:49320";
    }

    default Predicate<EndpointDescription> endpointFilter() {
        return e -> true;
    }

    default SecurityPolicy getSecurityPolicy() {
        return SecurityPolicy.None;
    }

    default IdentityProvider getIdentityProvider() {
        return new AnonymousProvider();
    }
}
