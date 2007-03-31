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

<#--Dispatcher Name: ${dispatcherName?default("NA")} -->

<#assign url='availableServices'>
<#assign popupUrl='serviceEcaDetail'>

<#-- Selected Service is available -->
<#if selectedServiceMap?exists>
  <#if showWsdl?exists && showWsdl = true>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <h3>${uiLabelMap.WebtoolsServiceWSDL} - ${uiLabelMap.WebtoolsService}: ${selectedServiceMap.serviceName}</h3>
      </div>
      <div class="screenlet-body" align="center">
        <form><textarea rows="20" cols="85" name="wsdloutput">${selectedServiceMap.wsdl}</textarea></form>
        <br />
        <a href='<@ofbizUrl>${url}?sel_service_name=${selectedServiceMap.serviceName}</@ofbizUrl>' class='smallSubmit'>${uiLabelMap.CommonBack}</a>
      </div>
    </div>
  <#else>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <h3>${uiLabelMap.WebtoolsService}: ${selectedServiceMap.serviceName}</h3>
          <li><a href='<@ofbizUrl>${url}</@ofbizUrl>'>${uiLabelMap.CommonListAll}</a></li>
          <li><a href='<@ofbizUrl>/scheduleJob?SERVICE_NAME=${selectedServiceMap.serviceName}</@ofbizUrl>'>${uiLabelMap.WebtoolsSchedule}</a></li>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <table class="basic-table" cellspacing='0'>
          <tr>
            <td class="label">${uiLabelMap.WebtoolsServiceName}:</td>
            <td>${selectedServiceMap.serviceName}</td>
            <td class="label">${uiLabelMap.WebtoolsEngineName}:</td>
            <td><a href='<@ofbizUrl>${url}?constraint=engine_name@${selectedServiceMap.engineName}</@ofbizUrl>'>${selectedServiceMap.engineName}</a></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.CommonDescription}:</td>
            <td>${selectedServiceMap.description}</td>
            <td class="label">${uiLabelMap.WebtoolsInvoke}:</td>
            <td>${selectedServiceMap.invoke}</td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.WebtoolsExportable}:</td>
            <td>${selectedServiceMap.export}<#if selectedServiceMap.export = "True">&nbsp;(<a href='<@ofbizUrl>${url}?sel_service_name=${selectedServiceMap.serviceName}&show_wsdl=true</@ofbizUrl>'>${uiLabelMap.WebtoolsShowShowWSDL}</a>)</#if></td>
            <td class="label">${uiLabelMap.WebtoolsLocation}:</td>
            <td><a href='<@ofbizUrl>${url}?constraint=location@${selectedServiceMap.location}</@ofbizUrl>'>${selectedServiceMap.location}</a></td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td class="label">${uiLabelMap.WebtoolsDefaultEntityName}:</td>
            <td><a href='<@ofbizUrl>${url}?constraint=default_entity_name@${selectedServiceMap.defaultEntityName}</@ofbizUrl>'>${selectedServiceMap.defaultEntityName}</a></td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td class="label">${uiLabelMap.WebtoolsRequireNewTransaction}:</td>
            <td>${selectedServiceMap.requireNewTransaction}</td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td class="label">${uiLabelMap.WebtoolsUseTransaction}:</td>
            <td>${selectedServiceMap.useTrans}</td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td class="label">${uiLabelMap.WebtoolsMaxRetries}:</td>
            <td>${selectedServiceMap.maxRetry}</td>
          </tr>
        </table>
      </div>
    </div>

    <div class="screenlet">
      <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PartySecurityGroups}</h3>
      </div>
      <#if selectedServiceMap.permissionGroups != 'NA'>
        <table class="basic-table" cellspacing='0'>
          <tr class="header-row">
            <td>${uiLabelMap.WebtoolsNameOrRole}</td>
            <td>${uiLabelMap.WebtoolsPermissionType}</td>
            <td>${uiLabelMap.WebtoolsAction}</td>
          </tr>
          <#list selectedServiceMap.permissionGroups as permGrp>
            <tr>
              <td>${permGrp.nameOrRole?default("NA")}</td>
              <td>${permGrp.permType?default("NA")}</td>
              <td>${permGrp.action?default("NA")}</td>
            </tr>
          </#list>
        </table>
      <#else>
        <div class="screenlet-body">
          <b>${selectedServiceMap.permissionGroups}</b>
        </div>
      </#if>
    </div>

    <div class="screenlet">
      <div class="screenlet-title-bar">
        <h3>${uiLabelMap.WebtoolsImplementedServices}</h3>
      </div>
      <div class="screenlet-body">
        <#if selectedServiceMap.implServices == 'NA'>
          <b>${selectedServiceMap.implServices}</b>
        <#elseif selectedServiceMap.implServices?has_content>
          <#list selectedServiceMap.implServices as implSrv>
            <a href='<@ofbizUrl>${url}?sel_service_name=${implSrv}</@ofbizUrl>'>${implSrv}</a><br>
          </#list>
        </#if>
      </div>
    </div>

    <#-- If service has ECA's -->
    <#if ecaMapList?exists && ecaMapList?has_content>
      <#-- add the javascript for modalpopup's -->
      <script language='javascript' type='text/javascript'>
          function detailsPopup(viewName){
              var lookupWinSettings = 'top=50,left=50,width=600,height=300,scrollbars=auto,status=no,resizable=no,dependent=yes,alwaysRaised=yes';
              var params = '';
              var lookupWin = window.open(viewName, params, lookupWinSettings);
              if(lookupWin.opener == null) lookupWin.opener = self;
              lookupWin.focus();
          }
      </script>
      <div class="screenlet">
        <div class="screenlet-title-bar">
          <h3>${uiLabelMap.WebtoolsServiceECA}</h3>
        </div>
        <table class="basic-table" cellspacing='0'>
          <tr class="header-row">
            <td>${uiLabelMap.WebtoolsEventName}</td>
            <td>${uiLabelMap.WebtoolsRunOnError}</td>
            <td>${uiLabelMap.WebtoolsRunOnFailure}</td>
            <td>${uiLabelMap.WebtoolsActions}</td>
            <td>${uiLabelMap.WebtoolsConditions}</td>
          </tr>
          <#list ecaMapList as ecaMap>
            <tr>
              <td>${ecaMap.eventName?if_exists}</td>
              <td>${ecaMap.runOnError?if_exists}</div></td>
              <td>${ecaMap.runOnFailure?if_exists}</div></td>
              <td>
                <#if ecaMap.actions?exists && ecaMap.actions?has_content>
                  <#list ecaMap.actions as action>
                    <a href='<@ofbizUrl>${url}?sel_service_name=${action.serviceName}</@ofbizUrl>'>${action.serviceName?default("NA")}</a>
                    <a href='javascript:detailsPopup("<@ofbizUrl>${popupUrl}?detail_type=action&prt_srv=${selectedServiceMap.serviceName}<#if ecaMap.eventName?exists>&prt_evt_name=${ecaMap.eventName}</#if><#if ecaMap.runOnError?exists>&prt_run_on_err=${ecaMap.runOnError}</#if><#if ecaMap.runOnFailure?exists>&prt_run_on_fail=${ecaMap.runOnFailure}</#if>&acx_srv=${action.serviceName}<#if action.eventName?exists>&acx_evt_name=${action.eventName}</#if><#if action.ignoreError?exists>&acx_ig_err=${action.ignoreError}</#if><#if action.ignoreFailure?exists>&acx_ig_fail=${action.ignoreFailure}</#if><#if action.persist?exists>&acx_pers=${action.persist}</#if><#if action.resultToContext?exists>&acx_res_to_ctx=${action.resultToContext}</#if><#if action.serviceMode?exists>&acx_srv_mode=${action.serviceMode}</#if><#if action.resultMapName?exists>&acx_res_map_name=${action.resultMapName}</#if></@ofbizUrl>")'>
                    [${uiLabelMap.CommonDetail}]</a>
                  </#list>
                </#if>
              </td>
              <td>
                <#if ecaMap.conditions?exists && ecaMap.conditions?has_content>
                  <#list ecaMap.conditions as condition>
                    <table class='basic-table' cellspacing='0'>
                      <tr>
                        <td><b>${uiLabelMap.WebtoolsCompareType}:</b> ${condition.compareType?default("NA")}</td>
                        <td>
                          <b>${uiLabelMap.WebtoolsConditionService}:</b> 
                          <#if condition.conditionService?exists && condition.conditionService?has_content>
                            <a href='<@ofbizUrl>${url}?sel_service_name=${condition.conditionService}</@ofbizUrl>'>${condition.conditionService?default("NA")}</a>
                          <#else>
                            ${condition.conditionService?default("NA")}
                          </#if>
                        </td>
                        <td><b>${uiLabelMap.WebtoolsFormat}:</b> ${condition.format?default("NA")}</td>
                      </tr>
                      <tr>                                                    
                        <td><b>${uiLabelMap.WebtoolsIsService}:</b> ${condition.isService?default("NA")}</td>
                        <td><b>${uiLabelMap.WebtoolsIsConstant}:</b> ${condition.isConstant?default("NA")}</td>
                        <td><b>${uiLabelMap.WebtoolsOperator}:</b> ${condition.operator?default("NA")}</td>
                      </tr>
                      <tr>
                        <td><b>${uiLabelMap.WebtoolsLHSMapName}:</b> ${condition.lhsMapName?default("NA")}</td>
                        <td><b>${uiLabelMap.WebtoolsLHSValueName}:</b> ${condition.lhsValueName?default("NA")}</td>
                        <td>&nbsp;</td>
                      </tr>
                      <tr>
                        <td><b>${uiLabelMap.WebtoolsRHSMapName}:</b> ${condition.rhsMapName?default("NA")}</td>
                        <td><b>${uiLabelMap.WebtoolsRHSValueName}:</b> ${condition.rhsValueName?default("NA")}</td>
                        <td>&nbsp;</td>
                      </tr>
                    </table><br/>
                  </#list>
                </#if>
              </td>
            </tr>
            <tr><td colspan='5'><hr></td></tr>
          </#list>
        </table>
      </div>
    </#if>
    <#-- End if service has ECA's -->

    <#list selectedServiceMap.allParamsList?if_exists as paramList>
      <div class="screenlet">
        <div class="screenlet-title-bar">
          <h3>${paramList.title}</h3>
        </div>
        <#if paramList.paramList?exists && paramList.paramList?has_content>
          <table class="basic-table" cellspacing='0'>
              <tr class="header-row">
                <td>${uiLabelMap.WebtoolsParameterName}</td>
                <td>${uiLabelMap.WebtoolsOptional}</td>
                <td>${uiLabelMap.CommonType}</td>
                <td>${uiLabelMap.WebtoolsMode}</td>
                <td>${uiLabelMap.WebtoolsIsSetInternally}</td>
                <td>${uiLabelMap.WebtoolsEntityName}</td>
                <td>${uiLabelMap.WebtoolsFieldName}</td>
              </tr>
              <#list paramList.paramList as params>
                <tr>
                  <td>${params.name?if_exists}</td>
                  <td>${params.optional?if_exists}</td>
                  <td>${params.type?if_exists}</td>
                  <td>${params.mode?if_exists}</td>
                  <td>${params.internal?if_exists}</td>
                  <td>
                    <#if params.entityName?exists>
                      <a href='<@ofbizUrl>${url}?constraint=default_entity_name@${params.entityName}</@ofbizUrl>'>${params.entityName?if_exists}</a>
                    </#if>
                  </td>
                  <td>${params.fieldName?if_exists}</td>
                </tr>
              </#list>
          </table>
        <#else>
          <div class="screenlet-body">
            ${uiLabelMap.WebtoolsNoParametersDefined}
          </div>
        </#if>
      </div>
    </#list>
  </#if>
<#-- No Service selected , we list all-->
<#elseif servicesList?exists && servicesList?has_content>

  <#-- Show alphabetical index -->
  <#if serviceNamesAlphaList?exists && serviceNamesAlphaList?has_content>
    <form id='dispForm' method='post' action='<@ofbizUrl>${url}</@ofbizUrl>'>
      <div class="button-bar">
        <#assign isfirst=true>
        <#list serviceNamesAlphaList as alpha>
          <#if !isfirst>
            |
          </#if>
          <a href='<@ofbizUrl>${url}?constraint=alpha@${alpha}</@ofbizUrl>'>${alpha}</a>
          <#assign isfirst=false>
        </#list>
        <#if dispArrList?exists && dispArrList?has_content>                        
          &nbsp;&nbsp;&nbsp;&nbsp;
          <script language='javascript' type='text/javascript'>
            function submitDispForm(){
                selObj = document.getElementById('sd');
                var dispVar = selObj.options[selObj.selectedIndex].value;
                if(dispVar != ''){
                    document.getElementById('dispForm').submit();
                }
            }
          </script>
          <select id='sd' name='selDisp' onChange='submitDispForm();'>
            <option value='' selected="selected">${uiLabelMap.WebtoolsSelectDispatcher}</option>
            <option value='' ></option>
            <#list dispArrList as disp>
              <option value='${disp}'>${disp}</option>
            </#list>
          </select>
        </#if>
      </div>
    </form>
    <br />
  </#if>

  <div class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.WebtoolsServicesListFor} ${dispatcherName?default("NA")} (${servicesFoundCount} ${uiLabelMap.CommonFound})</h3>
    </div>
    <table class="basic-table hover-bar" cellspacing='0'>
      <tr class="header-row">
        <td>${uiLabelMap.WebtoolsServiceName}</td>
        <td>${uiLabelMap.WebtoolsEngineName}</td>
        <td>${uiLabelMap.WebtoolsDefaultEntityName}</td>
        <td>${uiLabelMap.WebtoolsInvoke}</td>
        <td>${uiLabelMap.WebtoolsLocation}</td>
      </tr>
      <#assign alt_row = false>
      <#list servicesList as service>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td><a href='<@ofbizUrl>${url}?sel_service_name=${service.serviceName}</@ofbizUrl>'>${service.serviceName}</a></td>
          <td><a href='<@ofbizUrl>${url}?constraint=engine_name@${service.engineName?default("NA")}</@ofbizUrl>'>${service.engineName}</a></td>
          <td><a href='<@ofbizUrl>${url}?constraint=default_entity_name@${service.defaultEntityName?default("NA")}</@ofbizUrl>'>${service.defaultEntityName}</a></td>
          <td>${service.invoke}</td>
          <td><a href='<@ofbizUrl>${url}?constraint=location@${service.location?default("NA")}</@ofbizUrl>'>${service.location}</a></td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </div>
<#else>
  ${uiLabelMap.WebtoolsNoServicesFound}.
  <a href='<@ofbizUrl>${url}</@ofbizUrl>' class="smallSubmit">${uiLabelMap.CommonListAll}</a>
</#if>
