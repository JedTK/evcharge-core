package com.evcharge.utils;

import com.alibaba.fastjson2.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * 用于将XML格式的银行/支付响应消息转换为JSONObject的工具类
 */
public class XmlToJsonConverter {

    /**
     * 将XML格式的响应消息转换为JSONObject
     *
     * @param xmlStr XML格式的字符串
     * @return JSONObject对象
     */
    public static JSONObject xmlToJsonObject(String xmlStr) throws Exception {
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        Document doc = builder.parse(new ByteArrayInputStream(xmlStr.getBytes("GBK")));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

        doc.getDocumentElement().normalize();

        // 创建顶层JSONObject
        JSONObject jsonObj = new JSONObject();

        // 获取根元素
        Element rootElement = doc.getDocumentElement();

        // 处理所有子节点
        NodeList childNodes = rootElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String nodeName = element.getNodeName();

                switch (nodeName) {
                    case "Message":
                        // 处理Message节点
                        JSONObject messageObj = processElement(element);
                        jsonObj.put("Message", messageObj);
                        break;
                    case "Signature-Algorithm":
                        // 处理签名算法节点
                        jsonObj.put("Signature-Algorithm", element.getTextContent());
                        break;
                    case "Signature":
                        // 处理签名节点
                        jsonObj.put("Signature", element.getTextContent());
                        break;
                    default:
                        // 处理其他节点
                        jsonObj.put(nodeName, element.getTextContent());
                        break;
                }
            }
        }

        return jsonObj;
    }

    /**
     * 递归处理XML元素，转换为JSONObject
     *
     * @param element XML元素
     * @return JSONObject对象
     */
    private static JSONObject processElement(Element element) {
        JSONObject jsonObj = new JSONObject();

        // 如果当前元素没有子元素，直接返回文本内容
        if (!element.hasChildNodes() || (element.getChildNodes().getLength() == 1 && element.getFirstChild().getNodeType() == Node.TEXT_NODE)) {
            return jsonObj;
        }

        // 处理所有子元素
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                String nodeName = childElement.getNodeName();

                // 检查子元素是否有自己的子元素
                if (hasChildElements(childElement)) {
                    // 递归处理
                    JSONObject childObj = processElement(childElement);
                    jsonObj.put(nodeName, childObj);
                } else {
                    // 叶子节点，直接获取文本内容
                    jsonObj.put(nodeName, childElement.getTextContent());
                }
            }
        }

        return jsonObj;
    }

    /**
     * 检查元素是否有子元素（不包括纯文本节点）
     *
     * @param element XML元素
     * @return 是否有子元素
     */
    private static boolean hasChildElements(Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 示例方法：将给定的XML字符串转换为JSONObject并打印
     */
    public static void main(String[] args) {
        try {
            String xmlStr = "<MSG><Message><TrxResponse><ReturnCode>0000</ReturnCode><ErrorMessage>交易成功</ErrorMessage><ECMerchantType>EBUS</ECMerchantType><MerchantID>103882200000958</MerchantID><TrxType>ABCSoftTokenPay</TrxType><OrderNo>ON2a01409240031121ww</OrderNo><Amount>0.01</Amount><BatchNo>001306</BatchNo><VoucherNo>937228</VoucherNo><HostDate>2020/06/18</HostDate><HostTime>19:33:13</HostTime><PayType>EP072</PayType><NotifyType>1</NotifyType><iRspRef>6IECEP01172931989049</iRspRef><AccDate>20200721</AccDate><AcqFee>0.00</AcqFee><IssFee>0.00</IssFee><JrnNo> 123174002</JrnNo></TrxResponse></Message><Signature-Algorithm>SHA1withRSA</Signature-Algorithm><Signature>OXadmwXKRhyT3AidSwobGfvdRH1XzOO/muuCHr+u8fOpvdIEzBEuajNC1hRFqVmImdC858NxON+wO56o1ceOS+Q8HjyzLKQCGIIUb72Pr13shm+JOgo+Vs0pBRY9CdtGt67nncsTgwlCxb85sKNxs7+Sl1IXNUfQla8XKgeexYs=</Signature></MSG>";

            JSONObject jsonObj = xmlToJsonObject(xmlStr);

            // 打印转换后的JSONObject
            System.out.println("转换后的JSONObject:");
            System.out.println(jsonObj.toJSONString());

            // 获取特定字段的示例
            JSONObject message = jsonObj.getJSONObject("Message");
            JSONObject trxResponse = message.getJSONObject("TrxResponse");
            String returnCode = trxResponse.getString("ReturnCode");
            String orderNo = trxResponse.getString("OrderNo");
            String amount = trxResponse.getString("Amount");

            System.out.println("\n获取特定字段示例:");
            System.out.println("返回码: " + returnCode);
            System.out.println("订单号: " + orderNo);
            System.out.println("金额: " + amount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}