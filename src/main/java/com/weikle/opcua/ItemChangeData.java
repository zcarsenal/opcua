package com.weikle.opcua;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class ItemChangeData {

    private NodeId nodeId;

    private Object value;

    public NodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
