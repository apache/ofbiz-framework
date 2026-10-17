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
    <h3>${uiLabelMap.PageTitleScheduleJob}</h3>
  </div>
  <div class="screenlet-body" align="center">
    <form method="post" action="<@ofbizUrl>saveServiceResultsToSession</@ofbizUrl>"
        <table class="basic-table" cellspacing="0">
          <tr class="header-row">
            <td>${uiLabelMap.WebtoolsParameterName}</td>
            <td>${uiLabelMap.WebtoolsParameterValue}</td>
            <td>${uiLabelMap.WebtoolsServiceSaveValue} ?</td>
          </tr>
            <#if serviceResultList?has_content>
              <#list serviceResultList as srl>
                <tr>
                  <#if srl.hasChild=="Y">
                    <td><a href="<@ofbizUrl>/serviceResult?servicePath=</@ofbizUrl><#if parameters.servicePath??>${parameters.servicePath}||</#if>${srl.key!}">${srl.key!}</a></td>
                  <#else>
                    <td>${srl.key!}</td>
                  </#if>
                    <td>${srl.value!}</td>
                    <td><input type="checkbox" name="<#if parameters.servicePath??>${parameters.servicePath}||</#if>${srl.key!}" /></td>
                  </tr>
               </#list>
            </#if>
          <tr>
            <td>&nbsp;</td>
            <td class="label">${uiLabelMap.WebtoolsServiceClearPreviousParams} ? <input type="checkbox" name="_CLEAR_PREVIOUS_PARAMS_" /></td>
            <td><input type="submit" value="${uiLabelMap.CommonSubmit}"/></td>
          </tr>
        </table>
      </form>
  </div>
</div>
