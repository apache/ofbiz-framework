<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PageTitleEditProductStoreSurveys}</h3>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
              <td><b>${uiLabelMap.CommonType}</b></td>
              <td><b>${uiLabelMap.CommonName}</b></td>
              <td><b>${uiLabelMap.CommonSurveys}</b></td>
              <td><b>${uiLabelMap.ProductProduct}</b></td>
              <td><b>${uiLabelMap.ProductCategory}</b></td>
              <td><b>${uiLabelMap.CommonFromDate}</b></td>
              <td><b>${uiLabelMap.CommonSequenceNum}</b></td>
              <td><b>&nbsp;</b></td>
            </tr>
            <#assign alt_row = false>
            <#list productStoreSurveys as storeSurvey>
              <#assign surveyType = storeSurvey.getRelatedOne("SurveyApplType")>
              <#assign survey = storeSurvey.getRelatedOne("Survey")>
              <tr valign="middle"<#if alt_row>class="alternate-row"</#if>>
                <td>${surveyType.get("description",locale)}</td>
                <td>${storeSurvey.groupName?if_exists}</td>
                <td><a href="/content/control/EditSurvey?surveyId=${storeSurvey.surveyId}" class="buttontext">${survey.description?default("[" + survey.surveyId + "]")}</a>
                <td>${storeSurvey.productId?default("${uiLabelMap.CommonNA}")}</td>
                <td>${storeSurvey.productCategoryId?default("${uiLabelMap.CommonNA}")}</td>
                <td>${storeSurvey.fromDate?if_exists?string}</td>
                <td>${storeSurvey.sequenceNum?if_exists}</td>
                <td>
                  <form name="deleteProductStoreSurveyAppl_${storeSurvey_index}" method="post" action="<@ofbizUrl>deleteProductStoreSurveyAppl</@ofbizUrl>">
                    <input type="hidden" name="productStoreId" value="${productStoreId}" />
                    <input type="hidden" name="productStoreSurveyId" value="${storeSurvey.productStoreSurveyId}" />
                    <a href="javascript:document.deleteProductStoreSurveyAppl_${storeSurvey_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                  </form>
                </td> 
                <#-- toggle the row color -->
                <#assign alt_row = !alt_row>
            </#list>
        </table>
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PageTitleAddProductStoreSurveys}</h3>
    </div>
    <div class="screenlet-body">
        <form name="addSurvey" action="<@ofbizUrl>createProductStoreSurveyAppl</@ofbizUrl>" method="post">
            <input type="hidden" name="productStoreId" value="${productStoreId}" />
            <table cellspacing="0" class="basic-table">
              <tr>
                <td class="label">${uiLabelMap.CommonType}</td>
                <td>
                  <select name="surveyApplTypeId">
                    <#list surveyApplTypes as type>
                      <option value="${type.surveyApplTypeId}">${type.get("description",locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.CommonGroup} ${uiLabelMap.CommonName}</td>
                <td>
                  <input type="text" size="20" name="groupName" />
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.CommonSurveys}</td>
                <td>
                  <select name="surveyId">
                    <#list surveys as survey>
                      <option value="${survey.surveyId}">${survey.description?default("[" + survey.surveyId + "]")}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.ProductProductId}</td>
                <td>
                  <input type="text" size="20" name="productId" />
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.ProductCategoryId}</td>
                <td>
                  <@htmlTemplate.lookupField formName="addSurvey" name="productCategoryId" id="productCategoryId" fieldFormName="LookupProductCategory"/>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.CommonFromDate}</td>
                <td>
                  <input type="text" size="25" name="fromDate" />
                  <a href="javascript:call_cal(document.addSurvey.fromDate, '${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.CommonThruDate}</td>
                <td>
                  <input type="text" size="25" name="thruDate" />
                  <a href="javascript:call_cal(document.addSurvey.thruDate, '${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.ProductStoreSurveyTemplatePath}</td>
                <td>
                  <input type="text" size="30" name="surveyTemplate" />
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.ProductStoreSurveyResultTemplatePath}</td>
                <td>
                  <input type="text" size="30" name="resultTemplate" />
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.CommonSequenceNum}</td>
                <td>
                  <input type="text" size="5" name="sequenceNum" />
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}" /></td>
              </tr>
            </table>
        </form>
    </div>
</div>