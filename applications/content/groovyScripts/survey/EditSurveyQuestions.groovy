/*
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
 */

import org.ofbiz.entity.*
import org.ofbiz.entity.condition.*
import org.ofbiz.base.util.*

surveyQuestionId = parameters.surveyQuestionId;
context.surveyQuestionId = surveyQuestionId;

surveyQuestion = from("SurveyQuestion").where("surveyQuestionId", surveyQuestionId).queryOne();

surveyQuestionAndApplList = from("SurveyQuestionAndAppl").where("surveyId", surveyId).orderBy("sequenceNum").queryList();
surveyPageList = from("SurveyPage").where("surveyId", surveyId).orderBy("sequenceNum").queryList();
surveyMultiRespList = from("SurveyMultiResp").where("surveyId", surveyId).orderBy("multiRespTitle").queryList();

if (surveyQuestion && surveyQuestion.surveyQuestionTypeId && "OPTION".equals(surveyQuestion.surveyQuestionTypeId)) {
    // get the options
    questionOptions = from("SurveyQuestionOption").where("surveyQuestionId", surveyQuestionId).orderBy("sequenceNum").queryList();
    context.questionOptions = questionOptions;

    // survey question option
    optionSeqId = parameters.surveyOptionSeqId;
    surveyQuestionOption = null;
    if (optionSeqId) {
        surveyQuestionOption = from("SurveyQuestionOption").where("surveyQuestionId", surveyQuestionId, "surveyOptionSeqId", optionSeqId).queryOne();
    }
    context.surveyQuestionOption = surveyQuestionOption;
}

surveyQuestionCategoryId = parameters.surveyQuestionCategoryId;
surveyQuestionCategory = null;
categoryQuestions = null;
if (surveyQuestionCategoryId && "Y".equals(parameters.applyQuestionFromCategory)) {
    surveyQuestionCategory = from("SurveyQuestionCategory").where("surveyQuestionCategoryId", surveyQuestionCategoryId).queryOne();
    if (surveyQuestionCategory) {
        categoryQuestions = surveyQuestionCategory.getRelated("SurveyQuestion", null, null, false);
    }
}
questionCategories = from("SurveyQuestionCategory").orderBy("description").queryList();
context.surveyQuestion = surveyQuestion;

context.surveyQuestionAndApplList = surveyQuestionAndApplList;
context.surveyPageList = surveyPageList;
context.surveyMultiRespList = surveyMultiRespList;
context.surveyQuestionCategory = surveyQuestionCategory;
context.categoryQuestions = categoryQuestions;
context.questionCategories = questionCategories;
