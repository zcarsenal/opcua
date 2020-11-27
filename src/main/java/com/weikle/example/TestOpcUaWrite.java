package com.weikle.example;

import java.util.concurrent.ExecutionException;

public class TestOpcUaWrite {

  public static void main(String[] args) throws Exception {
      OpcUAClientService opcUAClientService = new OpcUAClientServiceImpl();
      OpcUAClientRunner opcUAClientRunner = new OpcUAClientRunner(opcUAClientService);
      opcUAClientRunner.initialize();
    for (int i = 0; i < 100; i++) {
      if (i % 2 == 0) {
        opcUAClientRunner.writeNodeValue(Boolean.TRUE, "D1ShelvesSet.IOMdl.BpsButton_4_1");
        continue;
      }
      opcUAClientRunner.writeNodeValue(Boolean.FALSE, "D1ShelvesSet.IOMdl.BpsButton_4_1");
      Thread.sleep(30);
    }
  }
}
