package com.weikle.example;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Node;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import java.util.List;

public class TestBrowserRoot {

  public static void main(String[] args) throws Exception {
      OpcUAClientService opcUAClientService = new OpcUAClientServiceImpl();
      OpcUAClientRunner opcUAClientRunner = new OpcUAClientRunner(opcUAClientService);
      opcUAClientRunner.initialize();

      /*opcUAClientRunner
              .getAddressSpace()
              .browse(nodeId_Tag1)
              .forEach(a -> System.out.println(a.getNodeId().getIdentifier()));*/
      populateNodes(opcUAClientRunner,"D15");
  }

    public static void populateNodes(OpcUAClientRunner opcUAClientRunner, String browseRoot) {
        try {
            NodeId nodeId_Tag1 = new NodeId(2, browseRoot);
            List<ReferenceDescription> nodes = opcUAClientRunner
                    .getAddressSpace()
                    .browse(nodeId_Tag1);
            for (ReferenceDescription node : nodes) {
                System.out.println(node.getNodeId().getIdentifier());
                // recursively browse to children
                populateNodes(opcUAClientRunner, "" + node.getNodeId().getIdentifier());

            }
        } catch (UaException e) {
            e.printStackTrace();
        }
    }
}
