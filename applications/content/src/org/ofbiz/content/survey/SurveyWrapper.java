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
package org.ofbiz.content.survey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Survey Wrapper - Class to render survey forms
 */
public class SurveyWrapper {

    public static final String module = SurveyWrapper.class.getName();

    protected GenericDelegator delegator = null;
    protected String responseId = null;
    protected String partyId = null;
    protected String surveyId = null;
    protected Map passThru = null;
    protected boolean edit = false;

    protected SurveyWrapper() {}

    public SurveyWrapper(GenericDelegator delegator, String responseId, String partyId, String surveyId, Map passThru) {
        this.delegator = delegator;
        this.responseId = responseId;
        this.partyId = partyId;
        this.surveyId = surveyId;
        if (passThru != null) {
            this.passThru = new HashMap(passThru);
        }
        this.checkParameters();
    }

    public SurveyWrapper(GenericDelegator delegator, String surveyId) {
        this(delegator, null, null, surveyId, null);
    }

    protected void checkParameters() {
        if (delegator == null || surveyId == null) {
            throw new IllegalArgumentException("Missing one or more required parameters (delegator, surveyId)");
        }
    }

    /**
     * Renders the Survey
     * @return Writer object from the parsed Freemarker Template
     * @throws SurveyWrapperException
     */
    public Writer render(String templatePath) throws SurveyWrapperException {
        URL templateUrl = UtilURL.fromResource(templatePath);
        if (templateUrl == null) {
            String errMsg = "Problem getting the template for Survey from URL: " + templatePath;
            Debug.logError(errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }

        Writer writer = new StringWriter();
        this.render(templateUrl, writer);
        return writer;
    }

    /**
     * Renders the Survey
     * @return Writer object from the parsed Freemarker Template
     * @throws SurveyWrapperException
     */
    public void render(URL templateUrl, Writer writer) throws SurveyWrapperException {
        String responseId = this.getThisResponseId();
        GenericValue survey = this.getSurvey();
        List surveyQuestionAndAppls = this.getSurveyQuestionAndAppls();
        Map results = this.getResults(surveyQuestionAndAppls);
        Map currentAnswers = null;
        if (responseId != null && canUpdate()) {
            currentAnswers = this.getResponseAnswers(responseId);
        }
        
        Map sqaaWithColIdListByMultiRespId = FastMap.newInstance();
        Iterator surveyQuestionAndApplIter = surveyQuestionAndAppls.iterator();
        while (surveyQuestionAndApplIter.hasNext()) {
            GenericValue surveyQuestionAndAppl = (GenericValue) surveyQuestionAndApplIter.next();
            String surveyMultiRespColId = surveyQuestionAndAppl.getString("surveyMultiRespColId");
            if (UtilValidate.isNotEmpty(surveyMultiRespColId)) {
                String surveyMultiRespId = surveyQuestionAndAppl.getString("surveyMultiRespId");
                List surveyQuestionAndApplList = (List) sqaaWithColIdListByMultiRespId.get(surveyMultiRespId);
                if (surveyQuestionAndApplList == null) {
                    surveyQuestionAndApplList = FastList.newInstance();
                    sqaaWithColIdListByMultiRespId.put(surveyMultiRespId, surveyQuestionAndApplList);
                }
                surveyQuestionAndApplList.add(surveyQuestionAndAppl);
            }
        }

        Map templateContext = FastMap.newInstance();
        FreeMarkerWorker.addAllOfbizTransforms(templateContext);
        templateContext.put("partyId", partyId);
        templateContext.put("survey", survey);
        templateContext.put("surveyResults", results);
        templateContext.put("surveyQuestionAndAppls", surveyQuestionAndAppls);
        templateContext.put("sqaaWithColIdListByMultiRespId", sqaaWithColIdListByMultiRespId);
        templateContext.put("alreadyShownSqaaPkWithColId", FastSet.newInstance());
        templateContext.put("surveyAnswers", currentAnswers);
        templateContext.put("surveyResponseId", responseId);
        templateContext.put("sequenceSort", UtilMisc.toList("sequenceNum"));
        templateContext.put("additionalFields", passThru);

        Template template = this.getTemplate(templateUrl);
        try {
            template.process(templateContext, writer);
        } catch (TemplateException e) {
            Debug.logError(e, "Error rendering Survey with template at [" + templateUrl.toExternalForm() + "]", module);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering Survey with template at [" + templateUrl.toExternalForm() + "]", module);
        }
    }

    // returns the FTL Template object
    protected Template getTemplate(URL templateUrl) {
        Configuration config = null;
        try {
            config = FreeMarkerWorker.makeDefaultOfbizConfig();
        } catch (IOException e) {
            Debug.logError(e, "Error creating default OFBiz FreeMarker Configuration", module);
        } catch (TemplateException e) {
            Debug.logError(e, "Error creating default OFBiz FreeMarker Configuration", module);
        }

        Template template = null;
        try {
            InputStream templateStream = templateUrl.openStream();
            InputStreamReader templateReader = new InputStreamReader(templateStream);
            template = new Template(templateUrl.toExternalForm(), templateReader, config);
        } catch (IOException e) {
            Debug.logError(e, "Unable to get template from URL :" + templateUrl.toExternalForm(), module);
        }
        return template;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }

    // returns the GenericValue object for the current Survey
    public GenericValue getSurvey() {
        GenericValue survey = null;
        try {
            survey = delegator.findByPrimaryKey("Survey", UtilMisc.toMap("surveyId", surveyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get Survey : " + surveyId, module);
        }
        return survey;
    }

    public String getSurveyName() {
        GenericValue survey = this.getSurvey();
        if (survey != null) {
            return survey.getString("surveyName");
        }
        return "";
    }

    // true if we can update this survey
    public boolean canUpdate() {
        if (this.edit) {
            return true;
        }
        
        GenericValue survey = this.getSurvey();
        if (!"Y".equals(survey.getString("allowMultiple")) || !"Y".equals(survey.getString("allowUpdate"))) {
            return false;
        }
        return true;
    }

    public boolean canRespond() {
        String responseId = this.getThisResponseId();
        if (responseId == null) {
            return true;
        } else {
            GenericValue survey = this.getSurvey();
            if ("Y".equals(survey.getString("allowMultiple"))) {
                return true;
            }
        }
        return false;
    }

    // returns a list of SurveyQuestions (in order by sequence number) for the current Survey
    public List getSurveyQuestionAndAppls() {
        List questions = new LinkedList();

        try {
            Map fields = UtilMisc.toMap("surveyId", surveyId);
            List order = UtilMisc.toList("sequenceNum", "surveyMultiRespColId");
            questions = delegator.findByAnd("SurveyQuestionAndAppl", fields, order);
            if (questions != null) {
                questions = EntityUtil.filterByDate(questions);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get questions for survey : " + surveyId, module);
        }

        return questions;
    }

    // returns the most current SurveyResponse ID for a survey; null if no party is found
    protected String getThisResponseId() {
        if (responseId != null) {
            return responseId;
        }

        if (partyId == null) {
            return null;
        }

        String responseId = null;
        List responses = null;
        try {
            responses = delegator.findByAnd("SurveyResponse", UtilMisc.toMap("surveyId", surveyId, "partyId", partyId), UtilMisc.toList("-lastModifiedDate"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (responses != null && responses.size() > 0) {
            GenericValue response = EntityUtil.getFirst(responses);
            responseId = response.getString("surveyResponseId");
            if (responses.size() > 1) {
                Debug.logWarning("More then one response found for survey : " + surveyId + " by party : " + partyId + " using most current", module);
            }
        }

        return responseId;
    }

    protected void setThisResponseId(String responseId) {
        this.responseId = responseId;
    }

    public long getNumberResponses() throws SurveyWrapperException {
        long responses = 0;
        try {
            responses = delegator.findCountByAnd("SurveyResponse", UtilMisc.toMap("surveyId", surveyId));
        } catch (GenericEntityException e) {
            throw new SurveyWrapperException(e);
        }
        return responses;
    }

    public List getSurveyResponses(GenericValue question) throws SurveyWrapperException {
        List responses = null;
        try {
            responses = delegator.findByAnd("SurveyResponse", UtilMisc.toMap("surveyQuestionId", question.getString("surveyQuestionId")));
        } catch (GenericEntityException e) {
            throw new SurveyWrapperException(e);
        }
        return responses;
    }

    // returns a Map of answers keyed on SurveyQuestion ID from the most current SurveyResponse ID
    public Map getResponseAnswers(String responseId) throws SurveyWrapperException {
        if (responseId == null) {
            throw new SurveyWrapperException("Null response ID is not supported at this time");
        }

        Map answerMap = new HashMap();

        if (responseId != null) {
            List answers = null;
            try {
                answers = delegator.findByAnd("SurveyResponseAnswer", UtilMisc.toMap("surveyResponseId", responseId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            if (answers != null && answers.size() > 0) {
                Iterator i = answers.iterator();
                while (i.hasNext()) {
                    GenericValue answer = (GenericValue) i.next();
                    answerMap.put(answer.get("surveyQuestionId"), answer);
                }
            }
        }

        // get the pass-thru (posted form data)
        if (passThru != null && passThru.size() > 0) {
            Iterator i = passThru.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                if (key.toUpperCase().startsWith("ANSWERS_")) {
                    int splitIndex = key.indexOf('_');
                    String questionId = key.substring(splitIndex+1);
                    Map thisAnswer = new HashMap();
                    String answer = (String) passThru.remove(key);
                    thisAnswer.put("booleanResponse", answer);
                    thisAnswer.put("currencyResponse", answer);
                    thisAnswer.put("floatResponse", answer);
                    thisAnswer.put("numericResponse", answer);
                    thisAnswer.put("textResponse", answer);
                    thisAnswer.put("surveyOptionSeqId", answer);
                    // this is okay since only one will be looked at
                    answerMap.put(questionId, thisAnswer);
                }
            }
        }

        return answerMap;
    }

    public List getQuestionResponses(GenericValue question, int startIndex, int number) throws SurveyWrapperException {
        List resp = null;
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
            
            EntityListIterator eli = this.getEli(question);
            if (startIndex > 0 && number > 0) {
                resp = eli.getPartialList(startIndex, number);
            } else {
                resp = eli.getCompleteList();
            }

            eli.close();
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error getting survey question responses", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }

            throw new SurveyWrapperException(e);
        } finally {
            try {
                // only commit the transaction if we started one...
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                throw new SurveyWrapperException(e);
                //Debug.logError(e, "Could not commit transaction: " + e.toString(), module);
            }
        }
        return resp;
    }

    public Map getResults(List questions) throws SurveyWrapperException {
        Map questionResults = new HashMap();
        if (questions != null) {
            Iterator i = questions.iterator();
            while (i.hasNext()) {
                GenericValue question = (GenericValue) i.next();
                Map results = getResultInfo(question);
                if (results != null) {
                    questionResults.put(question.getString("surveyQuestionId"), results);
                }
            }
        }
        return questionResults;
    }

    // returns a map of question reqsults
    public Map getResultInfo(GenericValue question) throws SurveyWrapperException {
        Map resultMap = new HashMap();

        // special keys in the result:
        // "_q_type"      - question type (SurveyQuestionTypeId)
        // "_a_type"      - answer type ("boolean", "option", "long", "double", "text")
        // "_total"       - number of total responses (all types)
        // "_tally"       - tally of all response values (number types)
        // "_average"     - average of all response values (number types)
        // "_yes_total"   - number of 'Y' (true) reponses (boolean type)
        // "_no_total"    - number of 'N' (false) responses (boolean type)
        // "_yes_percent" - number of 'Y' (true) reponses (boolean type)
        // "_no_percent"  - number of 'N' (false) responses (boolean type)
        // [optionId]     - Map containing '_total, _percent' keys (option type)

        String questionType = question.getString("surveyQuestionTypeId");
        resultMap.put("_q_type", questionType);

        // call the proper method based on the question type
        // note this will need to be updated as new types are added
        if ("OPTION".equals(questionType)) {
            Map thisResult = getOptionResult(question);
            if (thisResult != null) {
                Long questionTotal = (Long) thisResult.remove("_total");
                if (questionTotal == null) questionTotal = new Long(0);
                // set the total responses
                resultMap.put("_total", questionTotal);

                // create the map of option info ("_total", "_percent")
                Iterator i = thisResult.keySet().iterator();
                while (i.hasNext()) {
                    Map optMap = new HashMap();
                    String optId = (String) i.next();
                    Long optTotal = (Long) thisResult.get(optId);
                    if (optTotal == null) optTotal = new Long(0);
                    Long percent = new Long((long)(((double)optTotal.longValue() / (double)questionTotal.longValue()) * 100));
                    optMap.put("_total", optTotal);
                    optMap.put("_percent", percent);
                    resultMap.put(optId, optMap);
                }
                resultMap.put("_a_type", "option");
            }
        } else if ("BOOLEAN".equals(questionType)) {
            long[] thisResult = getBooleanResult(question);
            long yesPercent = thisResult[1] > 0 ? (long)(((double)thisResult[1] / (double)thisResult[0]) * 100) : 0;
            long noPercent = thisResult[2] > 0 ? (long)(((double)thisResult[2] / (double)thisResult[0]) * 100) : 0;

            resultMap.put("_total", new Long(thisResult[0]));
            resultMap.put("_yes_total", new Long(thisResult[1]));
            resultMap.put("_no_total", new Long(thisResult[2]));
            resultMap.put("_yes_percent", new Long(yesPercent));
            resultMap.put("_no_percent", new Long(noPercent));
            resultMap.put("_a_type", "boolean");
        } else if ("NUMBER_LONG".equals(questionType)) {
            double[] thisResult = getNumberResult(question, 1);
            resultMap.put("_total", new Long((long)thisResult[0]));
            resultMap.put("_tally", new Long((long)thisResult[1]));
            resultMap.put("_average", new Long((long)thisResult[2]));
            resultMap.put("_a_type", "long");
        } else if ("NUMBER_CURRENCY".equals(questionType)) {
            double[] thisResult = getNumberResult(question, 2);
            resultMap.put("_total", new Long((long)thisResult[0]));
            resultMap.put("_tally", new Double(thisResult[1]));
            resultMap.put("_average", new Double(thisResult[2]));
            resultMap.put("_a_type", "double");
        } else if ("NUMBER_FLOAT".equals(questionType)) {
            double[] thisResult = getNumberResult(question, 3);
            resultMap.put("_total", new Long((long)thisResult[0]));
            resultMap.put("_tally", new Double(thisResult[1]));
            resultMap.put("_average", new Double(thisResult[2]));
            resultMap.put("_a_type", "double");
        } else if ("SEPERATOR_LINE".equals(questionType) || "SEPERATOR_TEXT".equals(questionType)) {
            // not really a question; ingore completely
            return null;
        } else {
            // default is text
            resultMap.put("_total", new Long(getTextResult(question)));
            resultMap.put("_a_type", "text");
        }

        return resultMap;
    }

    private long[] getBooleanResult(GenericValue question) throws SurveyWrapperException {
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
            
            long[] result = { 0, 0, 0 };
            // index 0 = total responses
            // index 1 = total yes
            // index 2 = total no
    
            EntityListIterator eli = this.getEli(question);
    
            if (eli != null) {
                GenericValue value;
                while (((value = (GenericValue) eli.next()) != null)) {
                    if ("Y".equalsIgnoreCase(value.getString("booleanResponse"))) {
                        result[1]++;
                    } else {
                        result[2]++;
                    }
                    result[0]++; // increment the count
                }
    
                eli.close();
            }
    
            return result;
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error getting survey question responses Boolean result", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }

            throw new SurveyWrapperException(e);
        } finally {
            try {
                // only commit the transaction if we started one...
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                throw new SurveyWrapperException(e);
                //Debug.logError(e, "Could not commit transaction: " + e.toString(), module);
            }
        }
    }

    private double[] getNumberResult(GenericValue question, int type) throws SurveyWrapperException {
        double[] result = { 0, 0, 0 };
        // index 0 = total responses
        // index 1 = tally
        // index 2 = average

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
            
            EntityListIterator eli = this.getEli(question);
    
            if (eli != null) {
                GenericValue value;
                while (((value = (GenericValue) eli.next()) != null)) {
                    switch (type) {
                        case 1:
                            Long n = value.getLong("numericResponse");
                            result[1] += n.longValue();
                            break;
                        case 2:
                            Double c = value.getDouble("currencyResponse");
                            result[1] += (((double) Math.round((c.doubleValue() - c.doubleValue()) * 100)) / 100);
                            break;
                        case 3:
                            Double f = value.getDouble("floatResponse");
                            result[1] += f.doubleValue();
                            break;
                    }
                    result[0]++; // increment the count
                }
    
                eli.close();
            }
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error getting survey question responses Number result", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }

            throw new SurveyWrapperException(e);
        } finally {
            try {
                // only commit the transaction if we started one...
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                throw new SurveyWrapperException(e);
                //Debug.logError(e, "Could not commit transaction: " + e.toString(), module);
            }
        }

        // average
        switch (type) {
            case 1:
                if (result[0] > 0)
                    result[2] = ((long) result[1]) / ((long) result[0]);
                break;
            case 2:
                if (result[0] > 0)
                    result[2] = (((double) Math.round((result[1] / result[0]) * 100)) / 100);
                break;
            case 3:
                if (result[0] > 0)
                    result[2] = result[1] / result[0];
                break;
        }

        return result;
    }

    private long getTextResult(GenericValue question) throws SurveyWrapperException {
        long result = 0;

        try {
            result = delegator.findCountByCondition("SurveyResponseAndAnswer", makeEliCondition(question), null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new SurveyWrapperException("Unable to get responses", e);
        }

        return result;
    }

    private Map getOptionResult(GenericValue question) throws SurveyWrapperException {
        Map result = new HashMap();
        long total = 0;

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
            
            EntityListIterator eli = this.getEli(question);
            if (eli != null) {
                GenericValue value;
                while (((value = (GenericValue) eli.next()) != null)) {
                    String optionId = value.getString("surveyOptionSeqId");
                    Long optCount = (Long) result.remove(optionId);
                    if (optCount == null) {
                        optCount = new Long(1);
                    } else {
                        optCount = new Long(1 + optCount.longValue());
                    }
                    result.put(optionId, optCount);
                    total++; // increment the count
                }
    
                eli.close();
            }
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error getting survey question responses Option result", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }

            throw new SurveyWrapperException(e);
        } finally {
            try {
                // only commit the transaction if we started one...
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                throw new SurveyWrapperException(e);
                //Debug.logError(e, "Could not commit transaction: " + e.toString(), module);
            }
        }

        result.put("_total", new Long(total));
        return result;
    }

    private EntityCondition makeEliCondition(GenericValue question) {
        return new EntityConditionList(UtilMisc.toList(new EntityExpr("surveyQuestionId",
                EntityOperator.EQUALS, question.getString("surveyQuestionId")),
                new EntityExpr("surveyId", EntityOperator.EQUALS, surveyId)), EntityOperator.AND);
    }

    private EntityListIterator getEli(GenericValue question) throws GenericEntityException {
        EntityFindOptions efo = new EntityFindOptions();
        efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
        efo.setResultSetConcurrency(EntityFindOptions.CONCUR_READ_ONLY);
        efo.setSpecifyTypeAndConcur(true);
        efo.setDistinct(false);

        EntityListIterator eli = null;
        eli = delegator.findListIteratorByCondition("SurveyResponseAndAnswer", makeEliCondition(question), null, null, null, efo);

        return eli;
    }

    protected class SurveyWrapperException extends GeneralException {

        public SurveyWrapperException() {
            super();
        }

        public SurveyWrapperException(String str) {
            super(str);
        }

        public SurveyWrapperException(String str, Throwable nested) {
            super(str, nested);
        }

        public SurveyWrapperException(Throwable nested) {
            super(nested);
        }
    }
}
