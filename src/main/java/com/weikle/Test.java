package com.weikle;

public class Test {

  public static void main(String[] args) throws Exception {
      OpcClient client = OpcClient.getInstance();
      client.initialize();
      client.browseNode();

      Boolean nodeValue = client.readNodeValue("D1ShelvesSet.IOMdl.BpsButton_4_1");
      System.out.println(nodeValue);
  }
}
