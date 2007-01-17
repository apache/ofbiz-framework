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
package org.ofbiz.product.store;

import java.io.Writer;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.survey.SurveyWrapper;
import org.ofbiz.entity.GenericValue;

/**
 * Product Store Survey Wrapper
 */
public class ProductStoreSurveyWrapper extends SurveyWrapper {

    public static final String module = ProductStoreSurveyWrapper.class.getName();

    protected GenericValue productStoreSurveyAppl = null;
    protected String surveyTemplate = null;
    protected String resultTemplate = null;
    protected boolean callResult = false;

    protected ProductStoreSurveyWrapper() {}

    public ProductStoreSurveyWrapper(GenericValue productStoreSurveyAppl, String partyId, Map passThru) {
        this.productStoreSurveyAppl = productStoreSurveyAppl;

        this.passThru = passThru;
        if (this.productStoreSurveyAppl != null) {
            this.partyId = partyId;
            this.delegator = productStoreSurveyAppl.getDelegator();
            this.surveyId = productStoreSurveyAppl.getString("surveyId");
            this.surveyTemplate = productStoreSurveyAppl.getString("surveyTemplate");
            this.resultTemplate = productStoreSurveyAppl.getString("resultTemplate");
        } else {
            throw new IllegalArgumentException("Required parameter productStoreSurveyAppl missing");
        }
        this.checkParameters();
    }

    public void callResult(boolean b) {
        this.callResult = b;
    }

    public Writer render() throws SurveyWrapperException {
        if (canRespond() && !callResult) {
            return renderSurvey();
        } else if (!UtilValidate.isEmpty(resultTemplate)) {
            return renderResult();
        } else {
            throw new SurveyWrapperException("Error template not implemented yet; cannot update survey; no result template defined!");
        }
    }

    public Writer renderSurvey() throws SurveyWrapperException {
        return this.render(surveyTemplate);
    }

    public Writer renderResult() throws SurveyWrapperException {
        return this.render(resultTemplate);
    }
}
