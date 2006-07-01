/*
 * $Id: ProductStoreSurveyWrapper.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.product.store;

import java.io.Writer;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.survey.SurveyWrapper;
import org.ofbiz.entity.GenericValue;

/**
 * Product Store Survey Wrapper
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
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
