/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.internet.MimeMessage;
import javax.transaction.xa.XAException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;

import static org.ofbiz.base.util.UtilGenerics.checkList;
import static org.ofbiz.base.util.UtilGenerics.checkMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.ServiceXaWrapper;
import org.ofbiz.service.mail.MimeMessageWrapper;

/**
 * Common Services
 */
public class CommonServices {

    public final static String module = CommonServices.class.getName();

    /**
     * Generic Test Service
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> testService(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();

        if (context.size() > 0) {
            for (Map.Entry<String, ?> entry: context.entrySet()) {
                Object cKey = entry.getKey();
                Object value = entry.getValue();

                System.out.println("---- SVC-CONTEXT: " + cKey + " => " + value);
            }
        }
        if (!context.containsKey("message")) {
            response.put("resp", "no message found");
        } else {
            System.out.println("-----SERVICE TEST----- : " + (String) context.get("message"));
            response.put("resp", "service done");
        }

        System.out.println("----- SVC: " + dctx.getName() + " -----");
        return response;
    }

    /**
     * Generic Test SOAP Service
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> testSOAPService(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> response = ServiceUtil.returnSuccess();

        GenericValue testing = (GenericValue) context.get("testing");
        List<GenericValue> testingNodes = FastList.newInstance();
        for (int i = 0; i < 3; i ++) {
            GenericValue testingNode = delegator.makeValue("TestingNode");
            testingNode.put("testingNodeId", "TESTING_NODE" + i);
            testingNode.put("description", "Testing Node " + i);
            testingNode.put("createdStamp", UtilDateTime.nowTimestamp());
            testingNodes.add(testingNode);
        }
        response.put("testingNodes", testingNodes);
        return response;
    }

    public static Map<String, Object> blockingTestService(DispatchContext dctx, Map<String, ?> context) {
        Long duration = (Long) context.get("duration");
        if (duration == null) {
            duration = 30000l;
        }
        System.out.println("-----SERVICE BLOCKING----- : " + duration/1000d +" seconds");
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
        }
        return CommonServices.testService(dctx, context);
    }

    public static Map<String, Object> testRollbackListener(DispatchContext dctx, Map<String, ?> context) {
        ServiceXaWrapper xar = new ServiceXaWrapper(dctx);
        xar.setRollbackService("testScv", context);
        try {
            xar.enlist();
        } catch (XAException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnError("Rolling back!");
    }

    public static Map<String, Object> testCommitListener(DispatchContext dctx, Map<String, ?> context) {
        ServiceXaWrapper xar = new ServiceXaWrapper(dctx);
        xar.setCommitService("testScv", context);
        try {
            xar.enlist();
        } catch (XAException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Create Note Record
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createNote(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp noteDate = (Timestamp) context.get("noteDate");
        String partyId = (String) context.get("partyId");
        String noteName = (String) context.get("noteName");
        String note = (String) context.get("note");
        String noteId = delegator.getNextSeqId("NoteData");
        if (noteDate == null) {
            noteDate = UtilDateTime.nowTimestamp();
        }


        // check for a party id
        if (partyId == null) {
            if (userLogin != null && userLogin.get("partyId") != null)
                partyId = userLogin.getString("partyId");
        }

        Map<String, String> fields = UtilMisc.toMap("noteId", noteId, "noteName", noteName, "noteInfo", note,
                "noteParty", partyId, "noteDateTime", noteDate);

        try {
            GenericValue newValue = delegator.makeValue("NoteData", fields);

            delegator.create(newValue);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Could update note data (write failure): " + e.getMessage());
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put("noteId", noteId);
        result.put("partyId", partyId);
        return result;
    }

    /**
     * Service for setting debugging levels.
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> adjustDebugLevels(DispatchContext dctc, Map<String, ?> context) {
        Debug.set(Debug.FATAL, "Y".equalsIgnoreCase((String) context.get("fatal")));
        Debug.set(Debug.ERROR, "Y".equalsIgnoreCase((String) context.get("error")));
        Debug.set(Debug.WARNING, "Y".equalsIgnoreCase((String) context.get("warning")));
        Debug.set(Debug.IMPORTANT, "Y".equalsIgnoreCase((String) context.get("important")));
        Debug.set(Debug.INFO, "Y".equalsIgnoreCase((String) context.get("info")));
        Debug.set(Debug.TIMING, "Y".equalsIgnoreCase((String) context.get("timing")));
        Debug.set(Debug.VERBOSE, "Y".equalsIgnoreCase((String) context.get("verbose")));

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> addOrUpdateLogger(DispatchContext dctc, Map<String, ?> context) {
        String name = (String) context.get("name");
        String level = (String) context.get("level");
        boolean additivity = "Y".equalsIgnoreCase((String) context.get("additivity"));

        Logger logger = null;
        if ("root".equals(name)) {
            logger = Logger.getRootLogger();
        } else {
            logger = Logger.getLogger(name);
        }
        logger.setLevel(Level.toLevel(level));
        logger.setAdditivity(additivity);

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> forceGc(DispatchContext dctx, Map<String, ?> context) {
        System.gc();
        return ServiceUtil.returnSuccess();
    }

    /**
     * Echo service; returns exactly what was sent.
     * This service does not have required parameters and does not validate
     */
     public static Map<String, Object> echoService(DispatchContext dctx, Map<String, ?> context) {
         Map<String, Object> result = FastMap.newInstance();
         result.putAll(context);
         result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
         return result;
     }

    /**
     * Return Error Service; Used for testing error handling
     */
    public static Map<String, Object> returnErrorService(DispatchContext dctx, Map<String, ?> context) {
        return ServiceUtil.returnError("Return Error Service : Returning Error");
    }

    /**
     * Return TRUE Service; ECA Condition Service
     */
    public static Map<String, Object> conditionTrueService(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("conditionReply", Boolean.TRUE);
        return result;
    }

    /**
     * Return FALSE Service; ECA Condition Service
     */
    public static Map<String, Object> conditionFalseService(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("conditionReply", Boolean.FALSE);
        return result;
    }

    /** Cause a Referential Integrity Error */
    public static Map<String, Object> entityFailTest(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();

        // attempt to create a DataSource entity w/ an invalid dataSourceTypeId
        GenericValue newEntity = delegator.makeValue("DataSource");
        newEntity.set("dataSourceId", "ENTITY_FAIL_TEST");
        newEntity.set("dataSourceTypeId", "ENTITY_FAIL_TEST");
        newEntity.set("description", "Entity Fail Test - Delete me if I am here");
        try {
            delegator.create(newEntity);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to create test entity");
        }

        /*
        try {
            newEntity.remove();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        */

        return ServiceUtil.returnSuccess();
    }

    /** Test entity sorting */
    public static Map<String, Object> entitySortTest(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Set<ModelEntity> set = new TreeSet<ModelEntity>();

        set.add(delegator.getModelEntity("Person"));
        set.add(delegator.getModelEntity("PartyRole"));
        set.add(delegator.getModelEntity("Party"));
        set.add(delegator.getModelEntity("ContactMech"));
        set.add(delegator.getModelEntity("PartyContactMech"));
        set.add(delegator.getModelEntity("OrderHeader"));
        set.add(delegator.getModelEntity("OrderItem"));
        set.add(delegator.getModelEntity("OrderContactMech"));
        set.add(delegator.getModelEntity("OrderRole"));
        set.add(delegator.getModelEntity("Product"));
        set.add(delegator.getModelEntity("RoleType"));

        for (ModelEntity modelEntity: set) {
            Debug.log(modelEntity.getEntityName(), module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> makeALotOfVisits(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        int count = ((Integer) context.get("count")).intValue();

        for (int i = 0; i < count; i++) {
            GenericValue v = delegator.makeValue("Visit");
            String seqId = delegator.getNextSeqId("Visit");

            v.set("visitId", seqId);
            v.set("userCreated", "N");
            v.set("sessionId", "NA-" + seqId);
            v.set("serverIpAddress", "127.0.0.1");
            v.set("serverHostName", "localhost");
            v.set("webappName", "webtools");
            v.set("initialLocale", "en_US");
            v.set("initialRequest", "http://localhost:8080/webtools/control/main");
            v.set("initialReferrer", "http://localhost:8080/webtools/control/main");
            v.set("initialUserAgent", "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-us) AppleWebKit/124 (KHTML, like Gecko) Safari/125.1");
            v.set("clientIpAddress", "127.0.0.1");
            v.set("clientHostName", "localhost");
            v.set("fromDate", UtilDateTime.nowTimestamp());

            try {
                delegator.create(v);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> displayXaDebugInfo(DispatchContext dctx, Map<String, ?> context) {
        if (TransactionUtil.debugResources) {
            if (UtilValidate.isNotEmpty(TransactionUtil.debugResMap)) {
                TransactionUtil.logRunningTx();
            } else {
                Debug.log("No running transaction to display.", module);
            }
        } else {
            Debug.log("Debug resources is disabled.", module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> byteBufferTest(DispatchContext dctx, Map<String, ?> context) {
        ByteBuffer buffer1 = (ByteBuffer) context.get("byteBuffer1");
        ByteBuffer buffer2 = (ByteBuffer) context.get("byteBuffer2");
        String fileName1 = (String) context.get("saveAsFileName1");
        String fileName2 = (String) context.get("saveAsFileName2");
        String ofbizHome = System.getProperty("ofbiz.home");
        String outputPath1 = ofbizHome + (fileName1.startsWith("/") ? fileName1 : "/" + fileName1);
        String outputPath2 = ofbizHome + (fileName2.startsWith("/") ? fileName2 : "/" + fileName2);

        try {
            RandomAccessFile file1 = new RandomAccessFile(outputPath1, "rw");
            RandomAccessFile file2 = new RandomAccessFile(outputPath2, "rw");
            file1.write(buffer1.array());
            file2.write(buffer2.array());
        } catch (FileNotFoundException e) {
            Debug.logError(e, module);
        } catch (IOException e) {
            Debug.logError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> uploadTest(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        byte[] array = (byte[]) context.get("uploadFile");
        String fileName = (String) context.get("_uploadFile_fileName");
        String contentType = (String) context.get("_uploadFile_contentType");

        Map<String, Object> createCtx = FastMap.newInstance();
        createCtx.put("binData", array);
        createCtx.put("dataResourceTypeId", "OFBIZ_FILE");
        createCtx.put("dataResourceName", fileName);
        createCtx.put("dataCategoryId", "PERSONAL");
        createCtx.put("statusId", "CTNT_PUBLISHED");
        createCtx.put("mimeTypeId", contentType);
        createCtx.put("userLogin", userLogin);

        Map<String, Object> createResp = null;
        try {
            createResp = dispatcher.runSync("createFile", createCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResp));
        }

        GenericValue dataResource = (GenericValue) createResp.get("dataResource");
        if (dataResource != null) {
            Map<String, Object> contentCtx = FastMap.newInstance();
            contentCtx.put("dataResourceId", dataResource.getString("dataResourceId"));
            contentCtx.put("localeString", ((Locale) context.get("locale")).toString());
            contentCtx.put("contentTypeId", "DOCUMENT");
            contentCtx.put("mimeTypeId", contentType);
            contentCtx.put("contentName", fileName);
            contentCtx.put("statusId", "CTNT_PUBLISHED");
            contentCtx.put("userLogin", userLogin);

            Map<String, Object> contentResp = null;
            try {
                contentResp = dispatcher.runSync("createContent", contentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(contentResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentResp));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> simpleMapListTest(DispatchContext dctx, Map<String, ?> context) {
        List<String> listOfStrings = checkList(context.get("listOfStrings"), String.class);
        Map<String, String> mapOfStrings = checkMap(context.get("mapOfStrings"), String.class, String.class);

        for (String str: listOfStrings) {
            String v = (String) mapOfStrings.get(str);
            Debug.log("SimpleMapListTest: " + str + " -> " + v, module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> mcaTest(DispatchContext dctx, Map<String, ?> context) {
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");
        MimeMessage message = wrapper.getMessage();
        try {
            if (message.getAllRecipients() != null) {
               Debug.log("To: " + UtilMisc.toListArray(message.getAllRecipients()), module);
            }
            if (message.getFrom() != null) {
               Debug.log("From: " + UtilMisc.toListArray(message.getFrom()), module);
            }
            Debug.log("Subject: " + message.getSubject(), module);
            if (message.getSentDate() != null) {
                Debug.log("Sent: " + message.getSentDate().toString(), module);
            }
            if (message.getReceivedDate() != null) {
                Debug.log("Received: " + message.getReceivedDate().toString(), module);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> streamTest(DispatchContext dctx, Map<String, ?> context) {
        InputStream in = (InputStream) context.get("inputStream");
        OutputStream out = (OutputStream) context.get("outputStream");

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        Writer writer = new OutputStreamWriter(out);
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                Debug.log("Read line: " + line, module);
                writer.write(line);
            }
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("contentType", "text/plain");
        return result;
    }

    public static Map<String, Object> ping(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        String message = (String) context.get("message");
        if (message == null) {
            message = "PONG";
        }

        long count = -1;
        try {
            count = delegator.findCountByCondition("SequenceValueItem", null, null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return ServiceUtil.returnError("Unable to connect to datasource!");
        }

        if (count > 0) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("message", message);
            return result;
        } else {
            return ServiceUtil.returnError("Invalid count returned from database");
        }
    }

}
