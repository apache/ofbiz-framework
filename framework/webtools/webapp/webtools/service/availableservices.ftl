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
    <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
            <td align='left'><div class='boxhead'>
                ${uiLabelMap.WebtoolsService}: ${selectedServiceMap.serviceName}</div>
            </td>
            <td align='right'>
                <a href='<@ofbizUrl>/scheduleJob?SERVICE_NAME=${selectedServiceMap.serviceName}</@ofbizUrl>' class='submenutext'>${uiLabelMap.WebtoolsSchedule}</a>
                <a href='<@ofbizUrl>${url}</@ofbizUrl>' class='submenutextright'>${uiLabelMap.CommonListAll}</a>
            </td>
        </tr>
    </table>

	<#if showWsdl?exists && showWsdl = true>
	    <br>
		<table border='0' width='100%' cellspacing='0' cellpadding='0' class='boxtop'>
            <tr>
                <td><div class='boxhead'>${uiLabelMap.WebtoolsServiceWSDL}</div></td>
            </tr>
        </table>
        <table border=0 width='100%' cellspacing='0' cellpadding='5' class='boxoutside'>
            <tr>
               <td align="center"><form><textarea class="textAreaBox" rows="20" cols="85" name="wsdloutput">${selectedServiceMap.wsdl}</textarea></form></td>
            </tr>
            <tr>
               <td align="center"><a href='<@ofbizUrl>${url}?sel_service_name=${selectedServiceMap.serviceName}</@ofbizUrl>' class='linktext'>${uiLabelMap.CommonBack}</a></td>
            </tr>
        </table>
    <#else>

	
    <table border=0 width='100%' cellspacing='5' cellpadding='5' class='tabletext'>
        <tr>
            <td width='10%'>&nbsp;</td>
            <td align='left' valign='top'><br>
                <b>${uiLabelMap.WebtoolsServiceName}:</b>&nbsp;${selectedServiceMap.serviceName}<br>
                <b>${uiLabelMap.CommonDescription}:</b>&nbsp;${selectedServiceMap.description}<br>
                <b>${uiLabelMap.WebtoolsExportable}:</b>&nbsp;${selectedServiceMap.export}<#if selectedServiceMap.export = "True">&nbsp;(<a href='<@ofbizUrl>${url}?sel_service_name=${selectedServiceMap.serviceName}&show_wsdl=true</@ofbizUrl>' class='linktext'>${uiLabelMap.WebtoolsShowShowWSDL}</a>)</#if><br>
            <td width='10' align='left'>&nbsp;</td>
            <td align='left' valign='top'><br>
                <b>${uiLabelMap.WebtoolsEngineName}:</b>&nbsp;<a href='<@ofbizUrl>${url}?constraint=engine_name@${selectedServiceMap.engineName}</@ofbizUrl>' class='linktext'>${selectedServiceMap.engineName}</a><br>
                <b>${uiLabelMap.WebtoolsInvoke}:</b>&nbsp;${selectedServiceMap.invoke}<br>
                <b>${uiLabelMap.WebtoolsLocation}:</b>&nbsp;</b><a href='<@ofbizUrl>${url}?constraint=location@${selectedServiceMap.location}</@ofbizUrl>' class='linktext'>${selectedServiceMap.location}</a><br>
                <b>${uiLabelMap.WebtoolsDefaultEntityName}:</b>&nbsp;<a href='<@ofbizUrl>${url}?constraint=default_entity_name@${selectedServiceMap.defaultEntityName}</@ofbizUrl>' class='linktext'>${selectedServiceMap.defaultEntityName}</a><br>
                <b>${uiLabelMap.WebtoolsRequireNewTransaction}:</b>&nbsp;${selectedServiceMap.requireNewTransaction}<br>
                <b>${uiLabelMap.WebtoolsUseTransaction}:</b>&nbsp;${selectedServiceMap.useTrans}<br>
                <b>${uiLabelMap.WebtoolsMaxRetries}:</b>&nbsp;${selectedServiceMap.maxRetry}
            </td>
        </tr>
    </table>

    <table border=0 width='100%' cellspacing='5' cellpadding='1'>
        <tr>
            <#-- Permission Groups -->
            <td valign='top' width='50%'>
                <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxtop'>
                    <tr>
                        <td class='tabletext'><div class='boxhead'>${uiLabelMap.PartySecurityGroups}</div></td>
                    </tr>
                </table>
                <table border=0 width='100%' cellspacing='5' cellpadding='1' class='boxoutside'>
                    <tr>
                        <td class='tabletext'>
                        <#if selectedServiceMap.permissionGroups!='NA'>
                        <table border=0 width='100%' cellspacing='5' cellpadding='1'>
                            <tr>
                                <td class='tableheadtext'>${uiLabelMap.WebtoolsNameOrRole}</td>
                                <td class='tableheadtext'>${uiLabelMap.WebtoolsPermissionType}</td>
                                <td class='tableheadtext'>${uiLabelMap.WebtoolsAction}</td>
                            </tr>
                            <tr>
                                <td class='sepbar' colspan='3'><hr class='sepbar'></td>
                            </tr>
                            <#list selectedServiceMap.permissionGroups as permGrp>
                            <tr>
                                <td class='tabletext'>${permGrp.nameOrRole?default("NA")}</b></td>
                                <td class='tabletext'>${permGrp.permType?default("NA")}</td>
                                <td class='tabletext'>${permGrp.action?default("NA")}</td>
                            </tr>
                            </#list>
                    </table>
                        <#else>
                            <b>${selectedServiceMap.permissionGroups}</b>
                        </#if>
                        </td>
                    </tr>
                </table>
            </td>
            <#-- Impl Services -->
            <td valign='top' width='50%'>
                <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxtop'>
                    <tr>
                        <td class='tabletext'><div class='boxhead'>${uiLabelMap.WebtoolsImplementedServices}</div></td>
                    </tr>
                </table>
                <table border=0 width='100%' cellspacing='5' cellpadding='1' class='boxoutside'>
                    <tr>
                        <td class='tabletext'>
                        <#if selectedServiceMap.implServices=='NA'>
                            <b>${selectedServiceMap.implServices}</b>
                        <#elseif selectedServiceMap.implServices?has_content>
                            <#list selectedServiceMap.implServices as implSrv>
                                <a href='<@ofbizUrl>${url}?sel_service_name=${implSrv}</@ofbizUrl>' class='linktext'>${implSrv}</a><br>
                            </#list>
                        </#if>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
    <!-- If service has ECA's -->
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
        <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxtop'>
            <tr>
                <td><div class='boxhead'>${uiLabelMap.WebtoolsServiceECA}</div></td>
            </tr>
        </table>
        <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
            <tr>
                <td width='50%' align='left'>
                    <table border=0 width='100%' cellspacing='0' cellpadding='2'>
                        <tr>
                            <td align='left' valign='top'><div class='tableheadtext'>${uiLabelMap.WebtoolsEventName}</div></td>
                            <td align='left' valign='top'><div class='tableheadtext'>${uiLabelMap.WebtoolsRunOnError}</div></td>
                            <td align='left' valign='top'><div class='tableheadtext'>${uiLabelMap.WebtoolsRunOnFailure}</div></td>
                            <td align='left' valign='top'><div class='tableheadtext'>${uiLabelMap.WebtoolsActions}</div></td>
                            <td align='left' valign='top'><div class='tableheadtext'>${uiLabelMap.WebtoolsConditions}</div></td>
                        </tr>
                        <tr><td colspan='5'><hr class='sepbar'></td></tr>
                        <#list ecaMapList as ecaMap>
                            <tr>
                                <td align='left' valign='top'><div class='tableheadtext'>${ecaMap.eventName?if_exists}</div></td>
                                <td align='left' valign='top'><div class='tableheadtext'>${ecaMap.runOnError?if_exists}</div></td>
                                <td align='left' valign='top'><div class='tableheadtext'>${ecaMap.runOnFailure?if_exists}</div></td>
                                <td align='left' valign='top'>
                                    <#if ecaMap.actions?exists && ecaMap.actions?has_content>
                                        <#list ecaMap.actions as action>
                                            <a href='<@ofbizUrl>${url}?sel_service_name=${action.serviceName}</@ofbizUrl>' class='linktext'>${action.serviceName?default("NA")}</a>
                                            <a href='javascript:detailsPopup("<@ofbizUrl>${popupUrl}?detail_type=action&prt_srv=${selectedServiceMap.serviceName}<#if ecaMap.eventName?exists>&prt_evt_name=${ecaMap.eventName}</#if><#if ecaMap.runOnError?exists>&prt_run_on_err=${ecaMap.runOnError}</#if><#if ecaMap.runOnFailure?exists>&prt_run_on_fail=${ecaMap.runOnFailure}</#if>&acx_srv=${action.serviceName}<#if action.eventName?exists>&acx_evt_name=${action.eventName}</#if><#if action.ignoreError?exists>&acx_ig_err=${action.ignoreError}</#if><#if action.ignoreFailure?exists>&acx_ig_fail=${action.ignoreFailure}</#if><#if action.persist?exists>&acx_pers=${action.persist}</#if><#if action.resultToContext?exists>&acx_res_to_ctx=${action.resultToContext}</#if><#if action.serviceMode?exists>&acx_srv_mode=${action.serviceMode}</#if><#if action.resultMapName?exists>&acx_res_map_name=${action.resultMapName}</#if></@ofbizUrl>")' class='linktext'>
                                                [${uiLabelMap.CommonDetail}]
                                            </a>
                                        </#list>
                                    </#if>
                                </td>
                                <td valign='top'>
                                    <#if ecaMap.conditions?exists && ecaMap.conditions?has_content>
                                        <#list ecaMap.conditions as condition>
                                            <table class='boxoutside' width='100%'>
                                                <tr>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsCompareType}:</b> ${condition.compareType?default("NA")}
                                                    </td>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsConditionService}:</b> 
                                                        <#if condition.conditionService?exists && condition.conditionService?has_content>
                                                            <a href='<@ofbizUrl>${url}?sel_service_name=${condition.conditionService}</@ofbizUrl>' class='linktext'>${condition.conditionService?default("NA")}</a>
                                                        <#else>
                                                            ${condition.conditionService?default("NA")}
                                                        </#if>
                                                    </td>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsFormat}:</b> ${condition.format?default("NA")}
                                                    </td>
                                                </tr>
                                                <tr>                                                    
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsIsService}:</b> ${condition.isService?default("NA")}
                                                    </td>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsIsConstant}:</b> ${condition.isConstant?default("NA")}
                                                    </td>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsOperator}:</b> ${condition.operator?default("NA")}
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsLHSMapName}:</b> ${condition.lhsMapName?default("NA")}
                                                    </td>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsLHSValueName}:</b> ${condition.lhsValueName?default("NA")}
                                                    </td>
                                                    <td></td>
                                                </tr>
                                                <tr>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsRHSMapName}:</b> ${condition.rhsMapName?default("NA")}
                                                    </td>
                                                    <td class='tabletext'>
                                                        <b>${uiLabelMap.WebtoolsRHSValueName}:</b> ${condition.rhsValueName?default("NA")}
                                                    </td>
                                                    <td></td>
                                                </tr>
                                            </table><br>
                                        </#list>
                                    </#if>
                                </td>
                            </tr>
                            <tr><td colspan='5'><hr class='sepbar'></td></tr>
                        </#list>
                    </table>
                </td>
            </tr>
        </table>
        <br>
    </#if>
    <!-- End if service has ECA's -->

    <#list selectedServiceMap.allParamsList?if_exists as paramList>
        <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxtop'>
            <tr>
                <td><div class='boxhead'>${paramList.title}</div></td>
            </tr>
        </table>
        <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
            <tr>
                <td width='50%' align='left'>
                    <table border=0 width='100%' cellspacing='0' cellpadding='2'>
                        <#if paramList.paramList?exists && paramList.paramList?has_content>
                            <tr>
                                <td align='left'><div class='tableheadtext'>${uiLabelMap.WebtoolsParameterName}</div></td>
                                <td align='left'><div class='tableheadtext'>${uiLabelMap.WebtoolsOptional}</div></td>
                                <td align='left'><div class='tableheadtext'>${uiLabelMap.CommonType}</div></td>
                                <td align='left'><div class='tableheadtext'>${uiLabelMap.WebtoolsMode}</div></td>
                                <td align='left'><div class='tableheadtext'>${uiLabelMap.WebtoolsIsSetInternally}</div></td>
                                <td align='left'><div class='tableheadtext'>${uiLabelMap.WebtoolsEntityName}</div></td>
                                <td align='left'><div class='tableheadtext'>${uiLabelMap.WebtoolsFieldName}</div></td>
                            <tr>
                            <tr>
                                <td align='left' colspan='7'><hr class='sepbar'></td>
                            </tr>
                            <#list paramList.paramList as params>
                                <tr>
                                    <td align='left'><div class='tabletext'>${params.name?if_exists}</div></td>
                                    <td align='left'><div class='tabletext'>${params.optional?if_exists}</div></td>
                                    <td align='left'><div class='tabletext'>${params.type?if_exists}</div></td>
                                    <td align='left'><div class='tabletext'>${params.mode?if_exists}</div></td>
                                    <td align='left'><div class='tabletext'>${params.internal?if_exists}</div></td>
                                    <td align='left'>
                                        <#if params.entityName?exists>
                                            <a href='<@ofbizUrl>${url}?constraint=default_entity_name@${params.entityName}</@ofbizUrl>' class='linktext'>${params.entityName?if_exists}</a>
                                        </#if>
                                    </td>
                                    <td align='left'><div class='tabletext'>${params.fieldName?if_exists}</div></td>
                                </tr>
                            </#list>
                        <#else>
                            ${uiLabelMap.WebtoolsNoParametersDefined}
                        </#if>
                    </table>
                </td>
            </tr>
        </table>
        <br>
    </#list>
	</#if>
	
<#-- No Service selected , we list all-->
<#elseif servicesList?exists && servicesList?has_content>

    <#-- Show alphabetical index -->
    <#if serviceNamesAlphaList?exists && serviceNamesAlphaList?has_content>
        <table border='0' width='100%' cellspacing='0' cellpadding='0'>
            <tr>
                <td align='center'>
                    <#if dispArrList?exists && dispArrList?has_content>
                        <script language='javascript' type='text/javascript'>
                            function submitDispForm(){
                                selObj = document.getElementById('sd');
                                var dispVar = selObj.options[selObj.selectedIndex].value;
                                if(dispVar != ''){
                                    document.getElementById('dispForm').submit();
                                }
                            }
                        </script>
                        <form id='dispForm' method='post' action='<@ofbizUrl>${url}</@ofbizUrl>'>
                    </#if>
                    <#assign isfirst=true>
                    <#list serviceNamesAlphaList as alpha>
                        <#if !isfirst>
                            &nbsp;|&nbsp;
                        </#if>
                        <a href='<@ofbizUrl>${url}?constraint=alpha@${alpha}</@ofbizUrl>' class='linktext'>${alpha}</a>
                        <#assign isfirst=false>
                    </#list>
                    &nbsp;&nbsp;&nbsp;&nbsp;
                    <#if dispArrList?exists && dispArrList?has_content>                        
                            <select id='sd' name='selDisp' onChange='submitDispForm();' class='selectBox'>
                                <option value='' selected>${uiLabelMap.WebtoolsSelectDispatcher}</option>
                                <option value='' ></option>
                                <#list dispArrList as disp>
                                    <option value='${disp}'>${disp}</option>
                                </#list>
                            </select>
                        </form>
                    </#if>
                </td>
            </tr>
        </table>
    </#if>

<br>

    <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
            <td align='left'><div class='boxhead'>
                ${uiLabelMap.WebtoolsServicesListFor} ${dispatcherName?default("NA")}(${servicesFoundCount} ${uiLabelMap.CommonFound})</div>
            </td>
            <td align='right'>
                <a href='<@ofbizUrl>${url}</@ofbizUrl>' class='submenutextright'>${uiLabelMap.CommonListAll}</a>&nbsp;&nbsp;
            </td>
        </tr>
    </table>

    <#assign rowClass='viewManyTR1'>
    <table border=0 width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
        <tr>
            <td align='left'><div class='tableheadtext'>
                ${uiLabelMap.WebtoolsServiceName}</div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td align='left'><div class='tableheadtext'>
                ${uiLabelMap.WebtoolsEngineName}</div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td align='left'><div class='tableheadtext'>
                ${uiLabelMap.WebtoolsDefaultEntityName}</div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td align='left'><div class='tableheadtext'>
                ${uiLabelMap.WebtoolsInvoke}</div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td align='left'><div class='tableheadtext'>
                ${uiLabelMap.WebtoolsLocation}</div>
            </td>
        </tr>

        <tr>
            <td align='left' colspan='9'>
                <hr class='sepbar'>
            </td>
        </tr>

        <#list servicesList as service>
            <tr class='${rowClass}'>
                <td align='left'>
                    <a href='<@ofbizUrl>${url}?sel_service_name=${service.serviceName}</@ofbizUrl>' class='linktext'>${service.serviceName}</a>
                </td>
                <td>&nbsp;&nbsp;</td>
                <td align='left'>
                    <a href='<@ofbizUrl>${url}?constraint=engine_name@${service.engineName?default("NA")}</@ofbizUrl>' class='linktext'>${service.engineName}</a>
                </td>
                <td>&nbsp;&nbsp;</td>
                <td align='left'>
                    <a href='<@ofbizUrl>${url}?constraint=default_entity_name@${service.defaultEntityName?default("NA")}</@ofbizUrl>' class='linktext'>${service.defaultEntityName}</a>
                </td>
                <td>&nbsp;&nbsp;</td>
                <td align='left'>
                    <div class='tabletext'>${service.invoke}</div>
                </td>
                <td>&nbsp;&nbsp;</td>
                <td align='left'>
                    <a href='<@ofbizUrl>${url}?constraint=location@${service.location?default("NA")}</@ofbizUrl>' class='linktext'>${service.location}
                </td>
            </tr>
            <#if rowClass=='viewManyTR1'>
                <#assign rowClass='viewManyTR2'>
            <#else>
                <#assign rowClass='viewManyTR1'>
            </#if>
        </#list>
    </table>
<#else>
    <table border=0 width='100%' cellspacing='2' cellpadding='5' class='boxoutside'>
        <tr>
            <td align='left' class='tableheadtext'>
                ${uiLabelMap.WebtoolsNoServicesFound}...
            <td>
            <td align='right'>
                <a href='<@ofbizUrl>${url}</@ofbizUrl>' class='linktext'>${uiLabelMap.CommonListAll}</a>&nbsp;&nbsp;
            </td>
        </tr>    
    </table>
</#if>
