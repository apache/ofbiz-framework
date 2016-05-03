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


package org.ofbiz.accounting.thirdparty.ideal;


import java.sql.Timestamp;
import java.util.Random;

import junit.framework.TestCase;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.service.testtools.OFBizTestCase;
import com.ing.ideal.connector.IdealConnector;
import com.ing.ideal.connector.IdealException;
import com.ing.ideal.connector.Transaction;


public class IdealPaymentServiceTest extends OFBizTestCase{

    public IdealPaymentServiceTest(String name) {
        super(name);
    }
    
    public static final String module = IdealPaymentServiceTest.class.getName();

    // test data
    protected String orderId = null;
    protected String orderDiscription = null;
    protected String issuerId = null;
    protected String merchantReturnURL = null;
    protected String configFile = null;

    @Override
    protected void setUp() throws Exception {
        // populate test data
        configFile = "paymentTest";
        orderId = "testOrder1000";
        issuerId = "0151";
        orderDiscription = "Test Order Description";
        merchantReturnURL = "https://localhost:8443/ecommerce";
    }
    
    public void testDirectoryRequest() throws Exception{
        try {
            IdealConnector connector = new IdealConnector(configFile);
            connector.getIssuerList();
        } catch (IdealException ex){
            TestCase.fail(ex.getMessage());
        }
    }
    
    public void testOrderSuccuess() throws Exception{
        try {
            IdealConnector connector = new IdealConnector(configFile);
            int amount = 1;
            int amountFormat = amount * 100;
            
            Transaction transaction = new Transaction();
            transaction.setIssuerID(issuerId);
            transaction.setAmount(Integer.toString(amountFormat));
            transaction.setPurchaseID(orderId);
            transaction.setDescription(orderDiscription);
            
            Random random = new Random();
            String EntranceCode = Long.toString(Math.abs(random.nextLong()), 36);
            transaction.setEntranceCode(EntranceCode);
            transaction.setMerchantReturnURL(merchantReturnURL);
            Transaction trx = connector.requestTransaction(transaction);
            String transactionId = trx.getTransactionID();
            Transaction transactionCustomer = connector.requestTransactionStatus(transactionId);
            transactionCustomer.isSuccess();
            Debug.logInfo("[testOrderSuccuess] IssuerID Messages from iDEAL: " + transactionCustomer.getIssuerID(), module);
            Debug.logInfo("[testOrderSuccuess] Status Messages from iDEAL: " + transactionCustomer.getStatus(), module);
        } catch (IdealException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testOrderCancelled() throws Exception{
        try {
            IdealConnector connector = new IdealConnector(configFile);
            int amount = 2;
            int amountFormat = amount * 100;
            
            Transaction transaction = new Transaction();
            transaction.setIssuerID(issuerId);
            transaction.setAmount(Integer.toString(amountFormat));
            transaction.setPurchaseID(orderId);
            transaction.setDescription(orderDiscription);
            
            Random random = new Random();
            String EntranceCode = Long.toString(Math.abs(random.nextLong()), 36);
            transaction.setEntranceCode(EntranceCode);
            transaction.setMerchantReturnURL(merchantReturnURL);
            Transaction trx = connector.requestTransaction(transaction);
            String transactionId = trx.getTransactionID();
            Transaction transactionCustomer = connector.requestTransactionStatus(transactionId);
            transactionCustomer.isCancelled();
            Debug.logInfo("[testOrderCancelled] IssuerID Messages from iDEAL: " + transactionCustomer.getIssuerID(), module);
            Debug.logInfo("[testOrderCancelled] Status Messages from iDEAL: " + transactionCustomer.getStatus(), module);
        } catch (IdealException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testOrderExpired() throws Exception{
        try {
            IdealConnector connector = new IdealConnector(configFile);
            int amount = 3;
            int amountFormat = amount * 100;
            
            Transaction transaction = new Transaction();
            transaction.setIssuerID(issuerId);
            transaction.setAmount(Integer.toString(amountFormat));
            transaction.setPurchaseID(orderId);
            transaction.setDescription(orderDiscription);
            
            Random random = new Random();
            String EntranceCode = Long.toString(Math.abs(random.nextLong()), 36);
            transaction.setEntranceCode(EntranceCode);
            transaction.setMerchantReturnURL(merchantReturnURL);
            Transaction trx = connector.requestTransaction(transaction);
            String transactionId = trx.getTransactionID();
            Transaction transactionCustomer = connector.requestTransactionStatus(transactionId);
            transactionCustomer.isExpired();
            Debug.logInfo("[testOrderExpired] IssuerID Messages from iDEAL: " + transactionCustomer.getIssuerID(), module);
            Debug.logInfo("[testOrderExpired] Status Messages from iDEAL: " + transactionCustomer.getStatus(), module);
        } catch (IdealException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testOrderOpen() throws Exception{
        try {
            IdealConnector connector = new IdealConnector(configFile);
            int amount = 4;
            int amountFormat = amount * 100;
            
            Transaction transaction = new Transaction();
            transaction.setIssuerID(issuerId);
            transaction.setAmount(Integer.toString(amountFormat));
            transaction.setPurchaseID(orderId);
            transaction.setDescription(orderDiscription);
            
            Random random = new Random();
            String EntranceCode = Long.toString(Math.abs(random.nextLong()), 36);
            transaction.setEntranceCode(EntranceCode);
            transaction.setMerchantReturnURL(merchantReturnURL);
            Transaction trx = connector.requestTransaction(transaction);
            String transactionId = trx.getTransactionID();
            Transaction transactionCustomer = connector.requestTransactionStatus(transactionId);
            transactionCustomer.isOpen();
            Debug.logInfo("[testOrderOpen] IssuerID Messages from iDEAL: " + transactionCustomer.getIssuerID(), module);
            Debug.logInfo("[testOrderOpen] Status Messages from iDEAL: " + transactionCustomer.getStatus(), module);
        } catch (IdealException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testOrderFailure() throws Exception{
        try {
            IdealConnector connector = new IdealConnector(configFile);
            int amount = 5;
            int amountFormat = amount * 100;
            
            Transaction transaction = new Transaction();
            transaction.setIssuerID(issuerId);
            transaction.setAmount(Integer.toString(amountFormat));
            transaction.setPurchaseID(orderId);
            transaction.setDescription(orderDiscription);
            
            Random random = new Random();
            String EntranceCode = Long.toString(Math.abs(random.nextLong()), 36);
            transaction.setEntranceCode(EntranceCode);
            transaction.setMerchantReturnURL(merchantReturnURL);
            Transaction trx = connector.requestTransaction(transaction);
            String transactionId = trx.getTransactionID();
            Transaction transactionCustomer = connector.requestTransactionStatus(transactionId);
            transactionCustomer.isFailure();
            Debug.logInfo("[testOrderFailure] IssuerID Messages from iDEAL: " + transactionCustomer.getIssuerID(), module);
            Debug.logInfo("[testOrderFailure] Status Messages from iDEAL: " + transactionCustomer.getStatus(), module);
        } catch (IdealException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
    
    public void testOrderError() throws Exception{
        try {
            IdealConnector connector = new IdealConnector(configFile);
            int amount = 7;
            int amountFormat = amount * 100;
            
            Transaction transaction = new Transaction();
            transaction.setIssuerID(issuerId);
            transaction.setAmount(Integer.toString(amountFormat));
            transaction.setPurchaseID(orderId);
            transaction.setDescription(orderDiscription);
            
            Random random = new Random();
            String EntranceCode = Long.toString(Math.abs(random.nextLong()), 36);
            transaction.setEntranceCode(EntranceCode);
            transaction.setMerchantReturnURL(merchantReturnURL);
            IdealException ex = new IdealException("");
            ex.setErrorCode("SO1000");
            Transaction transactionCustomer = connector.requestTransaction(transaction);
            Debug.logInfo("[testOrderError] IssuerID Messages from iDEAL: " + transactionCustomer.getIssuerID(), module);
            Debug.logInfo("[testOrderError] Status Messages from iDEAL: " + transactionCustomer.getStatus(), module);
        } catch (IdealException ex){
            TestCase.fail(ex.getMessage());
        }
    }
}
