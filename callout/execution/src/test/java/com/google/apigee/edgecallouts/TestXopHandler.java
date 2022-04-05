// Copyright 2018-2021 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ------------------------------------------------------------------

package com.google.apigee.edgecallouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.xml.XmlUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

public class TestXopHandler {
  private static final String testDataDir = "src/test/resources/test-data";

  MessageContext msgCtxt;
  InputStream messageContentStream;
  Message message;
  ExecutionContext exeCtxt;

  private static String stringify(Object value) {
    if (value != null) return value.toString();
    return "-null-";
  }

  @BeforeMethod()
  public void beforeMethod() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map variables;

          public void $init() {
            variables = new HashMap();
          }

          @Mock()
          public <T> T getVariable(final String name) {
            if (variables == null) {
              variables = new HashMap();
            }
            if (name.equals("message")) {
              return (T) message;
            }
            System.out.printf("getVariable(%s) = %s\n", name, stringify(variables.get(name)));
            return (T) variables.get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            if (variables == null) {
              variables = new HashMap();
            }
            if (name.equals("message.content")) {
              if (value instanceof String) {
                messageContentStream =
                    new ByteArrayInputStream(((String) value).getBytes(StandardCharsets.UTF_8));
              } else if (value instanceof InputStream) {
                messageContentStream = (InputStream) value;
              }
            }
            System.out.printf("setVariable(%s) <= %s\n", name, value.toString());
            variables.put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (variables == null) {
              variables = new HashMap();
            }
            if (variables.containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }

          @Mock()
          public Message getMessage() {
            return message;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

    message =
        new MockUp<Message>() {
          @Mock()
          public InputStream getContentAsStream() {
            return messageContentStream;
          }

          @Mock()
          public String getHeader(String name) {
            System.out.printf("\ngetHeader(%s)\n", name);
            return (String) msgCtxt.getVariable("message.header." + name.toLowerCase());
          }

          @Mock()
          public void setContent(InputStream is) {
            // System.out.printf("\n** setContent(Stream)\n");
            messageContentStream = is;
          }

          @Mock()
          public void setContent(String content) {
            // System.out.printf("\n** setContent(String)\n");
            messageContentStream =
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
          }

          @Mock()
          public String getContent() {
            // System.out.printf("\n** getContent()\n");
            try {
              StringWriter writer = new StringWriter();
              IOUtils.copy(messageContentStream, writer, StandardCharsets.UTF_8);
              return writer.toString();
            } catch (Exception ex1) {
              return null;
            }
          }
        }.getMockInstance();
  }

  private static final String msg1 =
      ""
          + "--MIME_boundary\n"
          + "Content-Type: application/soap+xml; charset=UTF-8\n"
          + "Content-Transfer-Encoding: 8bit\n"
          + "Content-ID: <rootpart@soapui.org>\n"
          + "\n"
          + "<S:Envelope xmlns:S='http://schemas.xmlsoap.org/soap/envelope/'>\n"
          + "  <S:Header>\n"
          + "    <wsse:Security\n"
          + "        xmlns:wsse='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd'>\n"
          + "      <wsse:UsernameToken\n"
          + "          xmlns:wsse='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd'>\n"
          + "        <wsse:Username\n"
          + "            xmlns:wsse='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd'>XXXXXX</wsse:Username>\n"
          + "        <wsse:Password\n"
          + "            xmlns:wsse='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd'>XXXXXX</wsse:Password>\n"
          + "      </wsse:UsernameToken>\n"
          + "    </wsse:Security>\n"
          + "  </S:Header>\n"
          + "  <S:Body>\n"
          + "    <GenericRequest\n"
          + "        xmlns='http://www.oracle.com/UCM' webKey='cs'>\n"
          + "      <Service IdcService='CHECKIN_UNIVERSAL'>\n"
          + "        <Document>\n"
          + "          <Field name='UserDateFormat'>iso8601</Field>\n"
          + "          <Field name='UserTimeZone'>UTC</Field>\n"
          + "          <Field name='dDocName'>201807111403445918-1-464</Field>\n"
          + "          <Field name='dSecurityGroup'>FAFusionImportExport</Field>\n"
          + "          <Field name='dDocAccount'>hcm$/dataloader$/import$</Field>\n"
          + "          <Field name='dDocType'>Application</Field>\n"
          + "          <Field name='dDocTitle'>201807111403445918_76_I228_1_ValueSet_Budget_Center_ID_Independent.zip</Field>\n"
          + "          <File name='primaryFile' href='201807111403445918_76_I228_1_ValueSet_Budget_Center_ID_Independent.zip'>\n"
          + "            <Contents>\n"
          + "              <Include\n"
          + "                  xmlns='http://www.w3.org/2004/08/xop/include' href='cid:0b83cd6b-af15-45d2-bbda-23895de2a73d'/>\n"
          + "            </Contents>\n"
          + "          </File>\n"
          + "        </Document>\n"
          + "      </Service>\n"
          + "    </GenericRequest>\n"
          + "  </S:Body>\n"
          + "</S:Envelope>\n"
          + "\n"
          + "--MIME_boundary\n"
          + "Content-Type: application/zip\n"
          + "Content-Transfer-Encoding: binary\n"
          + "Content-ID: <0b83cd6b-af15-45d2-bbda-23895de2a73d>\n"
          + "\n"
          + "...binary zip data...\n"
          + "\n"
          + "--MIME_boundary--\n"
          + "\n";

  private static final String msg2 =
      ""
          + "--MIME_boundary\n"
          + "Content-Type: application/soap+xml; charset=UTF-8\n"
          + "Content-Transfer-Encoding: 8bit\n"
          + "Content-ID: <claim@insurance.com>\n"
          + "\n"
          + "<soap:Envelope\n"
          + " xmlns:soap='http://www.w3.org/2003/05/soap-envelope'\n"
          + " xmlns:xop='http://www.w3.org/2004/08/xop/include'\n"
          + " xmlns:xop-mime='http://www.w3.org/2005/05/xmlmime'>\n"
          + " <soap:Body>\n"
          + " <submitClaim>\n"
          + "  <accountNumber>5XJ45-3B2</accountNumber>\n"
          + "  <eventType>accident</eventType>\n"
          + "  <image xop-mime:content-type='image/bmp'><xop:Include href='cid:image@insurance.com'/></image>\n"
          + " </submitClaim>\n"
          + " </soap:Body>\n"
          + "</soap:Envelope>\n"
          + "\n"
          + "--MIME_boundary\n"
          + "Content-Type: image/bmp\n"
          + "Content-Transfer-Encoding: binary\n"
          + "Content-ID: <image@insurance.com>\n"
          + "\n"
          + "...binary BMP image...\n"
          + "\n"
          + "--MIME_boundary--\n"
          + "\n";

  @Test
  public void parseMessage() throws Exception {
    msgCtxt.setVariable("message.header.mime-version", "1.0");
    msgCtxt.setVariable(
        "message.header.content-type",
        "Multipart/Related; boundary=MIME_boundary; type='application/soap+xml'; start='<rootpart@soapui.org>'");

    msgCtxt.setVariable("message.content", msg1);

    Properties props = new Properties();
    props.put("source", "message");
    props.put("debug", "true");

    XopHandler callout = new XopHandler(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("xop_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("xop_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    // cannot directly reference message.content with the mocked MessageContext
    // Object output = msgCtxt.getVariable("message.content");
    Message msg = msgCtxt.getMessage();
    Object output = msg.getContent();
    Assert.assertNotNull(output, "no output");
  }

  @Test
  public void withBogusAction() throws Exception {
    msgCtxt.setVariable("message.header.mime-version", "1.0");
    msgCtxt.setVariable(
        "message.header.content-type",
        "Multipart/Related; boundary=MIME_boundary; type='application/soap+xml'; start='<rootpart@soapui.org>'");

    msgCtxt.setVariable("message.content", msg1);

    Properties props = new Properties();
    props.put("source", "message");
    props.put("action", "bogus");
    props.put("debug", "true");

    XopHandler callout = new XopHandler(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.ABORT;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("xop_error");
    Assert.assertNotNull(error, "error");
    Assert.assertEquals(error, "specify a valid action.");
  }

  @Test
  public void withExtractAction() throws Exception {
    msgCtxt.setVariable("message.header.mime-version", "1.0");
    msgCtxt.setVariable(
        "message.header.content-type",
        "Multipart/Related; boundary=MIME_boundary; type='application/soap+xml'; start='<rootpart@soapui.org>'");

    msgCtxt.setVariable("message.content", msg1);

    Properties props = new Properties();
    props.put("source", "message");
    props.put("action", "extract_soap");
    props.put("debug", "true");

    XopHandler callout = new XopHandler(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("xop_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("xop_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    String xml = msgCtxt.getVariable("xop_extracted_xml");
    Assert.assertNotNull(xml, "no extracted content");
    Document xmlDoc = XmlUtils.parseXml(xml);
    Assert.assertNotNull(xmlDoc, "cannot instantiate XML document");
  }

  @Test
  public void withEmbedAction() throws Exception {
    msgCtxt.setVariable("message.header.mime-version", "1.0");
    msgCtxt.setVariable(
        "message.header.content-type",
        "Multipart/Related; boundary=MIME_boundary; type='application/soap+xml'; start='<rootpart@soapui.org>'");

    msgCtxt.setVariable("message.content", msg1);

    Properties props = new Properties();
    props.put("source", "message");
    props.put("action", "TRANSFORM_TO_EMBEDDED");
    props.put("debug", "true");

    XopHandler callout = new XopHandler(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("xop_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("xop_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    Message msg = msgCtxt.getMessage();
    Object output = msg.getContent();
    Assert.assertNotNull(output, "no output");

    System.out.printf("Result:\n%s\n", (String) output);

    // String xml = new String(IOUtil.readAllBytes((InputStream)output), StandardCharsets.UTF_8);
    Document xmlDoc = XmlUtils.parseXml((String) output);
    Assert.assertNotNull(xmlDoc, "cannot instantiate XML document");
  }

  @Test
  public void unacceptableContentType() throws Exception {
    msgCtxt.setVariable("message.header.mime-version", "1.0");
    msgCtxt.setVariable(
        "message.header.content-type",
        "Multipart/Related; boundary=MIME_boundary; type='application/soap+xml'; start='<claim@insurance.com>'");

    msgCtxt.setVariable("message.content", msg2);

    Properties props = new Properties();
    props.put("source", "message");
    props.put("debug", "true");

    XopHandler callout = new XopHandler(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.ABORT;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("xop_error");
    Assert.assertNotNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("xop_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    // cannot directly reference message.content with the mocked MessageContext
    // Object output = msgCtxt.getVariable("message.content");
    Message msg = msgCtxt.getMessage();
    Object output = msg.getContent();
    Assert.assertNotNull(output, "no output");
  }

  @Test
  public void acceptableContentType() throws Exception {
    msgCtxt.setVariable("message.header.mime-version", "1.0");
    msgCtxt.setVariable(
        "message.header.content-type",
        "Multipart/Related; boundary=MIME_boundary; type='application/soap+xml'; start='<claim@insurance.com>'");

    msgCtxt.setVariable("message.content", msg2);

    Properties props = new Properties();
    props.put("source", "message");
    props.put("part2-ctypes", "image.jpeg, image/bmp");
    props.put("debug", "true");

    XopHandler callout = new XopHandler(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("xop_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("xop_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");
  }
  private static final String msg3 =
          ""
                  + "--WMBMIME1Boundaryurn_uuid_2E163B8A4F365625E21642980897108\n"
                  + "Content-Type: application/xop+xml; charset=utf-8;\n"
                  + "Content-Transfer-Encoding: binary\n"
                  + "Content-ID: namita\n"
                  + "\n"
                  + "<soap:Envelope\n"
                  + " xmlns:soap='http://www.w3.org/2003/05/soap-envelope'\n"
                  + " xmlns:xop='http://www.w3.org/2004/08/xop/include'\n"
                  + " xmlns:xop-mime='http://www.w3.org/2005/05/xmlmime'>\n"
                  + " <soap:Body>\n"
                  + " <submitClaim>\n"
                  + "  <accountNumber>5XJ45-3B2</accountNumber>\n"
                  + "  <eventType>accident</eventType>\n"
                  + "  <xop:Include\n" +
                  "xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:1.urn:uuid:2E163B8A4F365625E21642980897111@ibm.com\">\n" +
                  "</xop:Include>\n"
                  + " </submitClaim>\n"
                  + " </soap:Body>\n"
                  + "</soap:Envelope>\n"
                  + "\n"
                  + "--WMBMIME1Boundaryurn_uuid_2E163B8A4F365625E21642980897108\n"
                  + "Content-Type: application/octet-stream\n"
                  + "Content-Transfer-Encoding: binary\n"
                  + "Content-ID: <1.urn:uuid:2E163B8A4F365625E21642980897111@ibm.com>\n"
                  + "\n"
                  + "%PDF-1.5 %���� 3 0 obj <\n" +
                  "</ColorSpace/DeviceRGB/Subtype/Image/Height 55/Filter/DCTDecode/Type/XObject/Width 200/BitsPerComponent 8/Length 21777>>stream ���� ExifMM* \f � � � � ( 1 � 2 ԇi � ��' ��' Adobe Photoshop CS6 (Macintosh)2016:02:25 15:27:15 � 0221� ��� Ƞ 7 n v ( ~ �H H ����\fAdobe_CM �� Adobed� ���\f \f \f \f\f \f\f\f\f\f\f \f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f \f\f\f\f\f \f\f\f\f\f\f \f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f�� ,� \" �� �� ? \f3 ! 1 AQa \"q�2 ���B#$ R�b34r��C %�S���cs5 ���&D�TdE£t6 �U�e���u��F'���������������Vfv��������7GWgw�������� 5 !1 AQaq\" 2�� ��B#�R��3$b�r��CS cs4�% ��� &5��D�T� dEU6te����u��F���������������Vfv��������'7GWgw�������\f ?����͇~H ��� �v���. #�wT�� 3���=S ʭ~׀�A� F=����m����穪ݟhV��=�}p���0� �v �q%�5؟g� ��Wk�I�X����S�\\U-u] ( ̵�ج{uk�eY���smP�f�mó��x�8�N �� �>�%�Q�7���fU [i�ܖ�1�0n���@fVV��,��/�ߤ� 髸�Yz��� �u֜�ܛ��U� M�mf�d=�^5N��db���{?��.|ߋ~9��� �S� T EWe�z��B�ëw� m����w.�?��ث>�,���� ��� ^�K��1�7��?I����q նyLS �� c��&�)J'�' ����>��3]���3^ _�Mv���9� s[?�P� ��a����9� � 鍷�S��{ 9��s Ѱ�m�ɇ��PWGO��]���\u05F6�(������e�����z�z~���U��h��*��� �{��b\n" +
                  "<DX�N 7�8�Q�� �>A/�-���,� �xY �� �`ö ͞��}$ �� Ճl� *�{*m����6� �5�{-���c�O}lZk�� ���? � �ricn ���V� �}�������[o�4��F�V�{D��u��u�u\\�Y�� ���ls{�ns ��}��7Uen�c�2ΟV=w�������V�O� 7���{:�_lv� �Z�ͫ~ %�Y�� �� 9��wz��zv{ � w�*���Iy�� }3 �u p����U��s�4�ַ{�߁���ו���]����Kh���u�C>�� �ԟ� ��|.�vVʝ�A��c�;gۙ�6���vS�\u05EB�e�㷂���\\F ��l������^2�L9d�y nF� �z��m���;zw�7���[3����0N��fU�Ӳ _CwT��[(��k�6[�� �ٺ�_mU���=�fU��YG� ���[�C�_� �Eu2�i�� eu�R=?s�ٹ r�� Wq>����cS��c � }J��o����,f�7���g��V�� ]� �I&�I$�J ���� ��>�n���e� ��弲�Ǽ��P�۳���zJ���ٌ�6f�P��Ye��T��.��32�N�S�Y����^ϴbߑ�͘��9 _��G�睏� � 2� j ���m�Zm����m������_G����V�� M�� �= :y���!UU��w�dYQnUl�v[�W� �半�-� ��� ~� #!�?� � �6}_�N�U c8��2�hk�Z�ux̢�� ��S= ��7�хF>.= eaZ1-9���}�d�m4P��n�< ��m���z�S�V�ӫ U��־�~ ]_X02M��gG}l����� Zܛ�]_C����\\�������>�ӚY���Ns1�2-���N�=,}�6�M������ݍ�:������!R� �S��[�FM����>�s:�vuM��}���w�k��z���v�U.�����?Ѯ��# }\"� \\ ��۹�\\c3 K�~��+_�� �;�8��Y�_\"�����N�W�9 ����nc� c?1u�Z?�ʿ�� ���'� ��� ��y̼� dTG� P�N��i���/��̳2�B�qZ\u05FC�k��6�Kv�����~�t�e��ǻ���X�\\�6��� [�V��S����Ԯ����U��`��etF�:�}<��rq��:�z \f� ��ik]���m �����]fmy�dul���o��5���2�o��� j�ƺ��O���/�]_��oU 5�ѹ W�Z:�-Ĺ�4�u �� ��g������~� ���pݟNM ��ǳ҆� �sO��۷�/?����?�u>��l��Y �cd`݉Y}��:���= ��u�T�f=wU�oS�� �GE̩ٖ��eft�s�3]��+�� �{��}]? ��}��z 6��Tّ�?�U]��\"�U�=�z�A�̾��� �&�|�s���!� �U��;�[[� �Q��Э�+�\\j���{s^�6]CMU3 ףG�����&���� U�\\\uE6C5{�~F 1�� ��`�����Fʙc�����̟�W}����f� ~���&�Ӱ�����>�[in8{w�2m�;���ȧ}�g�ޗ�?��F��bz)��~�t��6 �v � �ͦ����_���l�cX��m�}?��zW~��=4k:�F����V^�NM���HcE0 �6��O���M�?G�� �?��G�� T��[ �3 ? �Yml���X3Amn�F�Yk�Ί�?N�Y��!�Ս��c��_�� Z� �e ��U}��5�)̧&����z���61�� T.�J�:,���w�u�����-�u�V��MO� ��{�G}?B�k6} �OS� �} t�M��굖5�{�� ok���o�j���@������~e5��: ��nH�����9?g��Y�E���� �������g/*�q ge?���.��,�f,� ]Y�?g� W�����ڱ�}7��V�xbF���?Z�v u�0�7[�N.[m���r �W��̧ ��e�?E�Z�F+��^��m��j �&ڜ\u070FY���\f��[�[� ����>� վ��_S�w3��#�ӓ�.� �Z��1�[�V & g�Qe,���H � +7���t� �rz�E��>� �j�m����2�=ގE_���� 1���e� u���c����fc�1\\-a ��c��w~�� ��h����.� ����Z_V3��k�7K뤟Q��?�W �~�b _�� ���q��_�N�SS��ߡ�Ǫ�>���� �h C��T���S�^Q~vp����î����{z��SgM O�} ������?����c\u07BC�g����>�_ �?� ���Iꭥ���ik�6�6���]�;�U�����7���= �9��\f�l l��f]�o��k�Ŧ�5�ȩ���z�c?��k�OV:�~���� O�vo�w����}/��?�~��w�m�� A��i�j~����O���Չ�?��� o�����7���7� 잏�^�� h��] ѭ�~����f55}W��uvZ�����mt�\f��SAۿ �}�_�_���Y����� X�K � *�qr�- h`� p����{��:�~��:�����߳z�i�}=�&g ����~��/G���������[��� �W�.��;v�L Nݲ} � ������ �^����J�C��:u������\\������fg�����g�[�^^��췷�U[��� �6�j�~� ɕ �� ���\\�����k�[�w��M�O��z `���}_־��o�/�E�}e��6�� m��13��]�K�I��8߂Fť�ϩ�>����K ��mŹ�J�~��z��X�\fM���c}'��=� ���X_[F7K�\u06DD_�\\��mf �mx�ͪ��w�M�m ��o��?����+�[�� a������{��~��cٿ��� I��^���~ �/�������ߴ� ���Uޯھ�� ��~����_ٿ�}/��S��� �8h_��W�w����R�z~M��v�b�*� �+ ��(}�� Q�#��c-�Y��o�+�}o��*mv-��[� J� C�^D��}�og�V;_��7� ���l���n~���T�~��o��ً������� Og����uz�������/D��g�������}��'���Q#\u00ADS�߮ � ����Σ�˯-�ю�Mwى�dZ�K ���Ʈ����� 7��1�g��r-f M��nuU? >��z ��[�����-�c��_�}_�.C���� I����o}�+�>�߳���y���=Og�~�� ��������Y��Uޟ��h� �� j����Fڿ�ϵ~��ϳ ?�y���ڝ�z-Z��/��gu�:f #�q���s m: ����>�� �Z\u0379�n�_�Ϩ� 湻��L�� ����� f�7� Ge�g������[�OS��7��)4�D�$�H)I$�JRI$������\n"
                  + "\n"
                  + "--WMBMIME1Boundaryurn_uuid_2E163B8A4F365625E21642980897108--\n"
                  + "\n";


  @Test
  public void withExtractActionMyOwn() throws Exception {
    msgCtxt.setVariable("message.header.mime-version", "1.0");
    msgCtxt.setVariable(
            "message.header.content-type",
            "Multipart/Related; boundary=WMBMIME1Boundaryurn_uuid_2E163B8A4F365625E21642980897108; type='application/xop+xml'; start='namita'");

    msgCtxt.setVariable("message.content", msg3);

    Properties props = new Properties();
    props.put("source", "message");
    props.put("action", "extract_soap");
    props.put("debug", "true");

    XopHandler callout = new XopHandler(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("xop_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("xop_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    String xml = msgCtxt.getVariable("xop_extracted_xml");
    Assert.assertNotNull(xml, "no extracted content");
    Document xmlDoc = XmlUtils.parseXml(xml);
    Assert.assertNotNull(xmlDoc, "cannot instantiate XML document");

    String base64encod = msgCtxt.getVariable("xop_base64Encoded");
    Assert.assertNotNull(base64encod, "cannot instantiate XML document");
  }

  private static final String msg4 =
          ""
                  + "%PDF-1.5 %���� 3 0 obj <\n" +
                  "</ColorSpace/DeviceRGB/Subtype/Image/Height 55/Filter/DCTDecode/Type/XObject/Width 200/BitsPerComponent 8/Length 21777>>stream ���� ExifMM* \f � � � � ( 1 � 2 ԇi � ��' ��' Adobe Photoshop CS6 (Macintosh)2016:02:25 15:27:15 � 0221� ��� Ƞ 7 n v ( ~ �H H ����\fAdobe_CM �� Adobed� ���\f \f \f \f\f \f\f\f\f\f\f \f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f \f\f\f\f\f \f\f\f\f\f\f \f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f�� ,� \" �� �� ? \f3 ! 1 AQa \"q�2 ���B#$ R�b34r��C %�S���cs5 ���&D�TdE£t6 �U�e���u��F'���������������Vfv��������7GWgw�������� 5 !1 AQaq\" 2�� ��B#�R��3$b�r��CS cs4�% ��� &5��D�T� dEU6te����u��F���������������Vfv��������'7GWgw�������\f ?����͇~H ��� �v���. #�wT�� 3���=S ʭ~׀�A� F=����m����穪ݟhV��=�}p���0� �v �q%�5؟g� ��Wk�I�X����S�\\U-u] ( ̵�ج{uk�eY���smP�f�mó��x�8�N �� �>�%�Q�7���fU [i�ܖ�1�0n���@fVV��,��/�ߤ� 髸�Yz��� �u֜�ܛ��U� M�mf�d=�^5N��db���{?��.|ߋ~9��� �S� T EWe�z��B�ëw� m����w.�?��ث>�,���� ��� ^�K��1�7��?I����q նyLS �� c��&�)J'�' ����>��3]���3^ _�Mv���9� s[?�P� ��a����9� � 鍷�S��{ 9��s Ѱ�m�ɇ��PWGO��]���\u05F6�(������e�����z�z~���U��h��*��� �{��b\n" +
                  "<DX�N 7�8�Q�� �>A/�-���,� �xY �� �`ö ͞��}$ �� Ճl� *�{*m����6� �5�{-���c�O}lZk�� ���? � �ricn ���V� �}�������[o�4��F�V�{D��u��u�u\\�Y�� ���ls{�ns ��}��7Uen�c�2ΟV=w�������V�O� 7���{:�_lv� �Z�ͫ~ %�Y�� �� 9��wz��zv{ � w�*���Iy�� }3 �u p����U��s�4�ַ{�߁���ו���]����Kh���u�C>�� �ԟ� ��|.�vVʝ�A��c�;gۙ�6���vS�\u05EB�e�㷂���\\F ��l������^2�L9d�y nF� �z��m���;zw�7���[3����0N��fU�Ӳ _CwT��[(��k�6[�� �ٺ�_mU���=�fU��YG� ���[�C�_� �Eu2�i�� eu�R=?s�ٹ r�� Wq>����cS��c � }J��o����,f�7���g��V�� ]� �I&�I$�J ���� ��>�n���e� ��弲�Ǽ��P�۳���zJ���ٌ�6f�P��Ye��T��.��32�N�S�Y����^ϴbߑ�͘��9 _��G�睏� � 2� j ���m�Zm����m������_G����V�� M�� �= :y���!UU��w�dYQnUl�v[�W� �半�-� ��� ~� #!�?� � �6}_�N�U c8��2�hk�Z�ux̢�� ��S= ��7�хF>.= eaZ1-9���}�d�m4P��n�< ��m���z�S�V�ӫ U��־�~ ]_X02M��gG}l����� Zܛ�]_C����\\�������>�ӚY���Ns1�2-���N�=,}�6�M������ݍ�:������!R� �S��[�FM����>�s:�vuM��}���w�k��z���v�U.�����?Ѯ��# }\"� \\ ��۹�\\c3 K�~��+_�� �;�8��Y�_\"�����N�W�9 ����nc� c?1u�Z?�ʿ�� ���'� ��� ��y̼� dTG� P�N��i���/��̳2�B�qZ\u05FC�k��6�Kv�����~�t�e��ǻ���X�\\�6��� [�V��S����Ԯ����U��`��etF�:�}<��rq��:�z \f� ��ik]���m �����]fmy�dul���o��5���2�o��� j�ƺ��O���/�]_��oU 5�ѹ W�Z:�-Ĺ�4�u �� ��g������~� ���pݟNM ��ǳ҆� �sO��۷�/?����?�u>��l��Y �cd`݉Y}��:���= ��u�T�f=wU�oS�� �GE̩ٖ��eft�s�3]��+�� �{��}]? ��}��z 6��Tّ�?�U]��\"�U�=�z�A�̾��� �&�|�s���!� �U��;�[[� �Q��Э�+�\\j���{s^�6]CMU3 ףG�����&���� U�\\\uE6C5{�~F 1�� ��`�����Fʙc�����̟�W}����f� ~���&�Ӱ�����>�[in8{w�2m�;���ȧ}�g�ޗ�?��F��bz)��~�t��6 �v � �ͦ����_���l�cX��m�}?��zW~��=4k:�F����V^�NM���HcE0 �6��O���M�?G�� �?��G�� T��[ �3 ? �Yml���X3Amn�F�Yk�Ί�?N�Y��!�Ս��c��_�� Z� �e ��U}��5�)̧&����z���61�� T.�J�:,���w�u�����-�u�V��MO� ��{�G}?B�k6} �OS� �} t�M��굖5�{�� ok���o�j���@������~e5��: ��nH�����9?g��Y�E���� �������g/*�q ge?���.��,�f,� ]Y�?g� W�����ڱ�}7��V�xbF���?Z�v u�0�7[�N.[m���r �W��̧ ��e�?E�Z�F+��^��m��j �&ڜ\u070FY���\f��[�[� ����>� վ��_S�w3��#�ӓ�.� �Z��1�[�V & g�Qe,���H � +7���t� �rz�E��>� �j�m����2�=ގE_���� 1���e� u���c����fc�1\\-a ��c��w~�� ��h����.� ����Z_V3��k�7K뤟Q��?�W �~�b _�� ���q��_�N�SS��ߡ�Ǫ�>���� �h C��T���S�^Q~vp����î����{z��SgM O�} ������?����c\u07BC�g����>�_ �?� ���Iꭥ���ik�6�6���]�;�U�����7���= �9��\f�l l��f]�o��k�Ŧ�5�ȩ���z�c?��k�OV:�~���� O�vo�w����}/��?�~��w�m�� A��i�j~����O���Չ�?��� o�����7���7� 잏�^�� h��] ѭ�~����f55}W��uvZ�����mt�\f��SAۿ �}�_�_���Y����� X�K � *�qr�- h`� p����{��:�~��:�����߳z�i�}=�&g ����~��/G���������[��� �W�.��;v�L Nݲ} � ������ �^����J�C��:u������\\������fg�����g�[�^^��췷�U[��� �6�j�~� ɕ �� ���\\�����k�[�w��M�O��z `���}_־��o�/�E�}e��6�� m��13��]�K�I��8߂Fť�ϩ�>����K ��mŹ�J�~��z��X�\fM���c}'��=� ���X_[F7K�\u06DD_�\\��mf �mx�ͪ��w�M�m ��o��?����+�[�� a������{��~��cٿ��� I��^���~ �/�������ߴ� ���Uޯھ�� ��~����_ٿ�}/��S��� �8h_��W�w����R�z~M��v�b�*� �+ ��(}�� Q�#��c-�Y��o�+�}o��*mv-��[� J� C�^D��}�og�V;_��7� ���l���n~���T�~��o��ً������� Og����uz�������/D��g�������}��'���Q#\u00ADS�߮ � ����Σ�˯-�ю�Mwى�dZ�K ���Ʈ����� 7��1�g��r-f M��nuU? >��z ��[�����-�c��_�}_�.C���� I����o}�+�>�߳���y���=Og�~�� ��������Y��Uޟ��h� �� j����Fڿ�ϵ~��ϳ ?�y���ڝ�z-Z��/��gu�:f #�q���s m: ����>�� �Z\u0379�n�_�Ϩ� 湻��L�� ����� f�7� Ge�g������[�OS��7��)4�D�$�H)I$�JRI$������\n"
                  + "\n"
                  + "--WMBMIME1Boundaryurn_uuid_2E163B8A4F365625E21642980897108--\n"
                  + "\n";

//  @Test
//  public void justBase64() throws Exception {
//    msgCtxt.setVariable("message.header.mime-version", "1.0");
//    msgCtxt.setVariable(
//            "message.header.content-type",
//            "Multipart/Related; boundary=WMBMIME1Boundaryurn_uuid_2E163B8A4F365625E21642980897108; type='application/xop+xml'; start='namita'");
//
//
//    msgCtxt.setVariable("message.content", msg4);
//    msgCtxt.setVariable("apiKey", "YAZLxVNAL1lZlIbc9XGn7O44Ov0XQp2k");
//    msgCtxt.setVariable("documentId", "00P5P000001dJUc");
//    Properties props = new Properties();
//    props.put("source", "message");
//    props.put("action", "GET_BASE64STR");
//    props.put("debug", "true");
//    props.put("apiKey", "apiKey");
//    props.put("documentId", "documentId");
//
//    XopHandler callout = new XopHandler(props);
//
//    // execute and retrieve output
//    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
//    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
//    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");
//
//    // check result and output
//    Object error = msgCtxt.getVariable("xop_error");
//    Assert.assertNull(error, "error");
//
//    Object stacktrace = msgCtxt.getVariable("xop_stacktrace");
//    Assert.assertNull(stacktrace, "stacktrace");
//
//    String base64encod = msgCtxt.getVariable("xop_base64Encoded");
//    Assert.assertNotNull(base64encod, "cannot instantiate XML document");
//  }

//
}
