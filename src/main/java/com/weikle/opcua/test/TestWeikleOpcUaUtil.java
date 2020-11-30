package com.weikle.opcua.test;

import com.google.common.collect.Lists;
import com.weikle.opcua.DefaultWeikleOpcUaConfig;
import com.weikle.opcua.ItemChangeData;
import com.weikle.opcua.WeikleOpcUaConfig;
import com.weikle.opcua.WeikleOpcUaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class TestWeikleOpcUaUtil {

  private static final Logger logger = LoggerFactory.getLogger(TestWeikleOpcUaUtil.class);

  public static void main(String[] args) throws Exception {
      WeikleOpcUaConfig weikleOpcUaConfig = new DefaultWeikleOpcUaConfig();
      WeikleOpcUaUtil weikleOpcUaUtil = new WeikleOpcUaUtil(weikleOpcUaConfig);
      Object o = weikleOpcUaUtil.asyncReadNodeValue("T14.Shlv.T1.Indicator18_1_2");
    System.out.println(o);
    weikleOpcUaUtil.write("T14.Shlv.T1.Indicator18_1_2",true);
      Object o1 = weikleOpcUaUtil.syncReadNodeValue("T14.Shlv.T1.Indicator18_1_2");
      System.out.println(o1);

      weikleOpcUaUtil.subscription(getItems(), new Consumer<ItemChangeData>() {
          @Override
          public void accept(ItemChangeData itemChangeData) {
              logger.info(
                      "subscription value received: item={}, value={}",itemChangeData.getNodeId() , itemChangeData.getValue());
          }
      });

      while(true){
          Thread.sleep(100000);
      }
  }

    private static List<String> getItems() {
        List<String> items = Lists.newArrayList();
        items.add("D1ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("D2ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("D3ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("D4ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("D5ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("D6ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("T1ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("T2ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("T3ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("T4ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("F1ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("F2ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("F3ShelvesSet.IOMdl.BpsButton_4_1");
        items.add("F4ShelvesSet.IOMdl.BpsButton_4_1");

        items.add("D1ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("D2ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("D3ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("D4ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("D5ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("D6ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("T1ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("T2ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("T3ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("T4ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("F1ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("F2ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("F3ShelvesSet.IOMdl.CfmButton_5_1");
        items.add("F4ShelvesSet.IOMdl.CfmButton_5_1");

        for (int i = 0; i < 63; i++) {
            items.add("T14.Shlv.T1.Indicator"+i+"_1_1");
            items.add("T14.Shlv.T1.Indicator"+i+"_1_2");
            items.add("T14.Shlv.T4.Indicator"+i+"_1_1");
            items.add("T14.Shlv.T4.Indicator"+i+"_1_2");
        }
        for (int i = 0; i < 63; i++) {
            items.add("T23.Shlv.T2.Indicator"+i+"_1_1");
            items.add("T23.Shlv.T2.Indicator"+i+"_1_2");
            items.add("T23.Shlv.T3.Indicator"+i+"_1_1");
            items.add("T23.Shlv.T3.Indicator"+i+"_1_2");
        }
        for (int i = 0; i < 63; i++) {
            items.add("F24.Shlv.F4.Indicator"+i+"_1_1");
            items.add("F24.Shlv.F4.Indicator"+i+"_1_2");
            items.add("F24.Shlv.F2.Indicator"+i+"_1_1");
            items.add("F24.Shlv.F2.Indicator"+i+"_1_2");
        }

        for (int i = 0; i < 63; i++) {
            items.add("D15.Shlv.D1.Indicator"+i+"_1_1");
            items.add("D15.Shlv.D1.Indicator"+i+"_1_2");
            items.add("D15.Shlv.D5.Indicator"+i+"_1_1");
            items.add("D15.Shlv.D5.Indicator"+i+"_1_2");
        }
        for (int i = 0; i < 63; i++) {
            items.add("D24.Shlv.D2.Indicator"+i+"_1_1");
            items.add("D24.Shlv.D2.Indicator"+i+"_1_2");
            items.add("D24.Shlv.D4.Indicator"+i+"_1_1");
            items.add("D24.Shlv.D4.Indicator"+i+"_1_2");
        }
        for (int i = 0; i < 63; i++) {
            items.add("D36.Shlv.D3.Indicator"+i+"_1_1");
            items.add("D36.Shlv.D3.Indicator"+i+"_1_2");
            items.add("D36.Shlv.D6.Indicator"+i+"_1_1");
            items.add("D36.Shlv.D6.Indicator"+i+"_1_2");
        }
        for (int i = 0; i < 63; i++) {
            items.add("F13.Shlv.F1.Indicator"+i+"_1_1");
            items.add("F13.Shlv.F1.Indicator"+i+"_1_2");
            items.add("F13.Shlv.F3.Indicator"+i+"_1_1");
            items.add("F13.Shlv.F3.Indicator"+i+"_1_2");
        }
    return items;
  }
}
