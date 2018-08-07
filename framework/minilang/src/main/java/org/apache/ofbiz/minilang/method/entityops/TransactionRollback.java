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
package org.apache.ofbiz.minilang.method.entityops;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;transaction-rollback&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class TransactionRollback extends MethodOperation {

    public static final String module = TransactionRollback.class.getName();

    private final FlexibleMapAccessor<Boolean> beganTransactionFma;

    public TransactionRollback(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "began-transaction-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "began-transaction-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        beganTransactionFma = FlexibleMapAccessor.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("began-transaction-name"), "beganTransaction"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        boolean beganTransaction = false;
        Boolean beganTransactionBoolean = beganTransactionFma.get(methodContext.getEnvMap());
        if (beganTransactionBoolean != null) {
            beganTransaction = beganTransactionBoolean;
        }
        try {
            TransactionUtil.rollback(beganTransaction, "Explicit rollback in simple-method [" + this.simpleMethod.getShortDescription() + "]", null);
        } catch (GenericTransactionException e) {
            String errMsg = "Exception thrown while rolling back transaction: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        beganTransactionFma.remove(methodContext.getEnvMap());
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<transaction-rollback ");
        sb.append("began-transaction-name=\"").append(this.beganTransactionFma).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;transaction-rollback&gt; element.
     */
    public static final class TransactionRollbackFactory implements Factory<TransactionRollback> {
        @Override
        public TransactionRollback createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new TransactionRollback(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "transaction-rollback";
        }
    }
}
