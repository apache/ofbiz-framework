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
<#-- Mandatory work efforts -->
<#if mandatoryWorkEfforts?has_content>
    <p>
    ${uiLabelMap.ManufacturingMandatoryWorkEfforts}
    <#list mandatoryWorkEfforts as mandatoryWorkEffortAssoc>
        <#assign mandatoryWorkEffort = mandatoryWorkEffortAssoc.getRelatedOne("FromWorkEffort")>
        <#if "PRUN_COMPLETED" == mandatoryWorkEffort.getString("currentStatusId") || "PRUN_CLOSED" == mandatoryWorkEffort.getString("currentStatusId")>
            <form name= "productionRunDeclarationMandatory" method= "post" action= "<@ofbizUrl>ProductionRunDeclaration</@ofbizUrl>">
                <input type= "hidden" name= "productionRunId" value= "${mandatoryWorkEffort.workEffortId}"/>
                <a href="javascript:document.productionRunDeclarationMandatory.submit()" class="buttontext">${mandatoryWorkEffort.workEffortName}</a>&nbsp;
            </form
        <#else>
            <#if "PRUN_CREATED" == mandatoryWorkEffort.getString("currentStatusId") || "PRUN_SCHEDULED" == mandatoryWorkEffort.getString("currentStatusId")>
                <form name= "editProductionRunMandatory" method= "post" action= "<@ofbizUrl>EditProductionRun</@ofbizUrl>" >
                    <input type= "hidden" name= "productionRunId" value= "${mandatoryWorkEffort.workEffortId}"/>
                    <a href= "javascript:document.editProductionRunMandatory.submit()" class="buttontext">${mandatoryWorkEffort.workEffortName}</a>[*]&nbsp;
                </form>
            <#else>
                <form name= "prodRunDeclaration" method= "post" action= "<@ofbizUrl>ProductionRunDeclaration</@ofbizUrl>">
                    <input type= "hidden" name= "productionRunId" value= "${mandatoryWorkEffort.workEffortId}"/>
                    <a href= "javascript:document.prodRunDeclaration.submit()" class="buttontext">${mandatoryWorkEffort.workEffortName}</a>[*]&nbsp;
                </form>
            </#if>
        </#if>
    </#list>
    </p>
</#if>
<#-- Dependent work efforts -->
<#if dependentWorkEfforts?has_content>
    <p>
    ${uiLabelMap.ManufacturingDependentWorkEfforts}
    <#list dependentWorkEfforts as dependentWorkEffortAssoc>
        <#assign dependentWorkEffort = dependentWorkEffortAssoc.getRelatedOne("ToWorkEffort")>
        <#if "PRUN_COMPLETED" == dependentWorkEffort.currentStatusId || "PRUN_CLOSED" == dependentWorkEffort.currentStatusId>
            <form name= "productionRunDeclarationDependent" method= "post" action= "<@ofbizUrl>ProductionRunDeclaration</@ofbizUrl>">
                <input type= "hidden" name= "productionRunId" value= "${dependentWorkEffort.workEffortId}"/>
                <a href= "javascript:document.productionRunDeclarationDependent.submit()" class="buttontext">${dependentWorkEffort.workEffortName}</a>&nbsp;
            </form>
        <#else>
            <#if "PRUN_CREATED" == dependentWorkEffort.getString("currentStatusId") || "PRUN_SCHEDULED" == dependentWorkEffort.getString("currentStatusId")>
                <form name= "editProductionRunDependent" method= "post" action= "<@ofbizUrl>EditProductionRun</@ofbizUrl>">
                    <input type= "hidden" name= "productionRunId" value= "${dependentWorkEffort.workEffortId}"/>
                    <a href="javascript:document.editProductionRunDependent.submit()" class="buttontext">${dependentWorkEffort.workEffortName}</a>[*]&nbsp;
                </form>
            <#else>
                <form name= "prodRunDeclarationDependent" method= "post" action= "<@ofbizUrl>ProductionRunDeclaration</@ofbizUrl>">
                    <input type= "hidden" name= "productionRunId" value= "${dependentWorkEffort.workEffortId}"/>
                    <a href="javascript:document.prodRunDeclarationDependent.submit()" class="buttontext">${dependentWorkEffort.workEffortName}</a>[*]&nbsp;
                </form>
            </#if>
        </#if>
    </#list>
    </p>
</#if>