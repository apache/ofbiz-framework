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

<#if sessionAttributes.recentArtifactInfoList?has_content>
  <div class="right">
    <h2>Recently Viewed Artifacts:</h2>
    <#assign highRef = sessionAttributes.recentArtifactInfoList.size() - 1/>
    <#if (highRef > 19)><#assign highRef = 19/></#if>
    <#list sessionAttributes.recentArtifactInfoList[0..highRef] as recentArtifactInfo>
        <div>${recentArtifactInfo_index + 1} - ${recentArtifactInfo.getDisplayType()}: <@displayArtifactInfoLink artifactInfo=recentArtifactInfo/></div>
    </#list>
  </div>
</#if>

<#if !artifactInfo?exists>

    <#-- add form here to specify artifact info name. -->
    <div>
      <form name="ArtifactInfoByName" method="post" action="<@ofbizUrl>ArtifactInfo</@ofbizUrl>" class="basic-form">
        Search Names/Locations: <input type="text" name="name" value="${parameters.name?if_exists}" size="40"/>
        <select name="type">
          <option></option>
          <option>entity</option>
          <option>service</option>
          <option>form</option>
          <option>screen</option>
          <option>request</option>
          <option>view</option>
        </select>
        <input type="hidden" name="findType" value="search"/>
        <input type="submit" name="submitButton" value="Find"/>
      </form>
    </div>
    <div>
      <form name="ArtifactInfoByNameAndType" method="post" action="<@ofbizUrl>ArtifactInfo</@ofbizUrl>" class="basic-form">
        <div>Name: <input type="text" name="name" value="${parameters.name?if_exists}" size="40"/></div>
        <div>Location: <input type="text" name="location" value="${parameters.location?if_exists}" size="60"/></div>
        <div>Type:
          <select name="type">
            <option>entity</option>
            <option>service</option>
            <option>form</option>
            <option>screen</option>
            <option>request</option>
            <option>view</option>
          </select>
          <input type="submit" name="submitButton" value="Lookup"/>
        </div>
      </form>
    </div>

    <#-- add set of ArtifactInfo if there is not a single one identified, with link to each -->
    <#if artifactInfoSet?has_content>
    <div>
        <h4>Multiple Artifacts Found:</h4>
        <#list artifactInfoSet as curArtifactInfo>
            <div>${curArtifactInfo.getDisplayType()}: <@displayArtifactInfoLink artifactInfo=curArtifactInfo/></div>
        </#list>
    </div>
    </#if>

<#else/>

    <h1>${uiLabelMap.WebtoolsArtifactInfo} (${artifactInfo.getDisplayType()}): ${artifactInfo.getDisplayName()}</h1>
    <#if artifactInfo.getLocationURL()?exists>
        <div>Defined in: <a href="${artifactInfo.getLocationURL()}">${artifactInfo.getLocationURL()}</a></div>
    </#if>

    <#if artifactInfo.getType() == "entity">
        <div><a href="<@ofbizUrl>FindGeneric?entityName=${artifactInfo.modelEntity.getEntityName()}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>">All Entity Data</a></div>
        <h2>Entity Fields</h2>
        <table>
        <#list artifactInfo.modelEntity.getFieldsUnmodifiable() as modelField>
            <tr><td>${modelField.getName()}<#if modelField.getIsPk()>*</#if></td><td>${modelField.getType()}</td><td>${modelField.getDescription()?if_exists}</td></tr>
        </#list>
        </table>

        <div>
        <h2>Entities Related (One)</h2>
        <#list artifactInfo.getEntitiesRelatedOne()?if_exists as entityArtifactInfo>
            <@displayEntityArtifactInfo entityArtifactInfo=entityArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Entities Related (Many)</h2>
        <#list artifactInfo.getEntitiesRelatedMany()?if_exists as entityArtifactInfo>
            <@displayEntityArtifactInfo entityArtifactInfo=entityArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Services Using This Entity</h2>
        <#list artifactInfo.getServicesUsingEntity()?if_exists as serviceArtifactInfo>
            <@displayServiceArtifactInfo serviceArtifactInfo=serviceArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Forms Using This Entity</h2>
        <#list artifactInfo.getFormsUsingEntity()?if_exists as formWidgetArtifactInfo>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=formWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Screens Using This Entity</h2>
        <#list artifactInfo.getScreensUsingEntity()?if_exists as screenWidgetArtifactInfo>
            <@displayScreenWidgetArtifactInfo screenWidgetArtifactInfo=screenWidgetArtifactInfo/>
        </#list>
        </div>

    <#elseif artifactInfo.getType() == "service"/>
        <h2>Service Info</h2>
        <div>&nbsp;Description: ${artifactInfo.modelService.description}</div>
        <div>&nbsp;Run (${artifactInfo.modelService.engineName}): ${artifactInfo.modelService.location} :: ${artifactInfo.modelService.invoke}</div>
        <div>&nbsp;Impl Location: <a href="${artifactInfo.getImplementationLocationURL()?if_exists}">${artifactInfo.getImplementationLocationURL()?if_exists}</a></div>
        <h2>Service Parameters</h2>
        <table>
            <tr><td>Name</td><td>Type</td><td>Optional</td><td>Mode</td><td>Entity.field</td></tr>
        <#list artifactInfo.modelService.getAllParamNames() as paramName>
            <#assign modelParam = artifactInfo.modelService.getParam(paramName)/>
            <tr><td>${modelParam.getName()}<#if modelParam.getInternal()> (internal)</#if></td><td>${modelParam.getType()}</td><td><#if modelParam.isOptional()>optional<#else/>required</#if></td><td>${modelParam.getMode()}</td><td>${modelParam.getEntityName()?if_exists}.${modelParam.getFieldName()?if_exists}</td></tr>
        </#list>
        </table>

        <div>
        <h2>Entities Used By This Service</h2>
        <#list artifactInfo.getEntitiesUsedByService()?if_exists as entityArtifactInfo>
            <@displayEntityArtifactInfo entityArtifactInfo=entityArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Services Calling This Service</h2>
        <#list artifactInfo.getServicesCallingService()?if_exists as serviceArtifactInfo>
            <@displayServiceArtifactInfo serviceArtifactInfo=serviceArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Services Called By This Service</h2>
        <#list artifactInfo.getServicesCalledByService()?if_exists as serviceArtifactInfo>
            <@displayServiceArtifactInfo serviceArtifactInfo=serviceArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Service ECA Rules Triggered By This Service</h2>
        <#list artifactInfo.getServiceEcaRulesTriggeredByService()?if_exists as serviceEcaArtifactInfo>
            <@displayServiceEcaArtifactInfo serviceEcaArtifactInfo=serviceEcaArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Service ECA Rules Calling This Service</h2>
        <#list artifactInfo.getServiceEcaRulesCallingService()?if_exists as serviceEcaArtifactInfo>
            <@displayServiceEcaArtifactInfo serviceEcaArtifactInfo=serviceEcaArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Forms Calling This Service</h2>
        <#list artifactInfo.getFormsCallingService()?if_exists as formWidgetArtifactInfo>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=formWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Forms Based On This Service</h2>
        <#list artifactInfo.getFormsBasedOnService()?if_exists as formWidgetArtifactInfo>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=formWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Screens Calling This Service</h2>
        <#list artifactInfo.getScreensCallingService()?if_exists as screenWidgetArtifactInfo>
            <@displayScreenWidgetArtifactInfo screenWidgetArtifactInfo=screenWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Requests with Events That Call This Service</h2>
        <#list artifactInfo.getRequestsWithEventCallingService()?if_exists as controllerRequestArtifactInfo>
            <@displayControllerRequestArtifactInfo controllerRequestArtifactInfo=controllerRequestArtifactInfo/>
        </#list>
        </div>

    <#elseif artifactInfo.getType() == "form"/>
        <div>
        <h2>Form Extended by This Form</h2>
        <#if artifactInfo.getFormThisFormExtends()?exists>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=artifactInfo.getFormThisFormExtends()/>
        </#if>
        </div>

        <div>
        <h2>Entities Used in This Form</h2>
        <#list artifactInfo.getEntitiesUsedInForm()?if_exists as entityArtifactInfo>
            <@displayEntityArtifactInfo entityArtifactInfo=entityArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Services Used in This Form</h2>
        <#list artifactInfo.getServicesUsedInForm()?if_exists as serviceArtifactInfo>
            <@displayServiceArtifactInfo serviceArtifactInfo=serviceArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Forms Extending This Form</h2>
        <#list artifactInfo.getFormsExtendingThisForm()?if_exists as formWidgetArtifactInfo>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=formWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Screens Including This Form</h2>
        <#list artifactInfo.getScreensIncludingThisForm()?if_exists as screenWidgetArtifactInfo>
            <@displayScreenWidgetArtifactInfo screenWidgetArtifactInfo=screenWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Controller Requests That Are Linked to in This Form</h2>
        <#list artifactInfo.getRequestsLinkedToInForm()?if_exists as controllerRequestArtifactInfo>
            <@displayControllerRequestArtifactInfo controllerRequestArtifactInfo=controllerRequestArtifactInfo/>
        </#list>
        </div>
        <div>
        <h2>Controller Requests That Are Targeted By This Form</h2>
        <#list artifactInfo.getRequestsTargetedByForm()?if_exists as controllerRequestArtifactInfo>
            <@displayControllerRequestArtifactInfo controllerRequestArtifactInfo=controllerRequestArtifactInfo/>
        </#list>
        </div>

    <#elseif artifactInfo.getType() == "screen"/>
        <div>
        <h2>Entities Used in This Screen</h2>
        <#list artifactInfo.getEntitiesUsedInScreen()?if_exists as entityArtifactInfo>
            <@displayEntityArtifactInfo entityArtifactInfo=entityArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Services Used in This Screen</h2>
        <#list artifactInfo.getServicesUsedInScreen()?if_exists as serviceArtifactInfo>
            <@displayServiceArtifactInfo serviceArtifactInfo=serviceArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Forms Included in This Screen</h2>
        <#list artifactInfo.getFormsIncludedInScreen()?if_exists as formWidgetArtifactInfo>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=formWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Screens Include in This Screen</h2>
        <#list artifactInfo.getScreensIncludedInScreen()?if_exists as screenWidgetArtifactInfo>
            <@displayScreenWidgetArtifactInfo screenWidgetArtifactInfo=screenWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Screens Including This Screen</h2>
        <#list artifactInfo.getScreensIncludingThisScreen()?if_exists as screenWidgetArtifactInfo>
            <@displayScreenWidgetArtifactInfo screenWidgetArtifactInfo=screenWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Controller Requests That Are Linked to in This Screen</h2>
        <#list artifactInfo.getRequestsLinkedToInScreen()?if_exists as controllerRequestArtifactInfo>
            <@displayControllerRequestArtifactInfo controllerRequestArtifactInfo=controllerRequestArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Controller Views Referring to This Screen</h2>
        <#list artifactInfo.getViewsReferringToScreen()?if_exists as controllerViewArtifactInfo>
            <@displayControllerViewArtifactInfo controllerViewArtifactInfo=controllerViewArtifactInfo/>
        </#list>
        </div>

    <#elseif artifactInfo.getType() == "request"/>
        <#if artifactInfo.getServiceCalledByRequestEvent()?exists>
            <div>
            <h2>Service Called by Request Event</h2>
            <@displayServiceArtifactInfo serviceArtifactInfo=artifactInfo.getServiceCalledByRequestEvent()/>
            </div>
        </#if>

        <div>
        <h2>Forms Referring to This Request</h2>
        <#list artifactInfo.getFormInfosReferringToRequest()?if_exists as formWidgetArtifactInfo>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=formWidgetArtifactInfo/>
        </#list>
        </div>
        <div>
        <h2>Forms Targeting This Request</h2>
        <#list artifactInfo.getFormInfosTargetingRequest()?if_exists as formWidgetArtifactInfo>
            <@displayFormWidgetArtifactInfo formWidgetArtifactInfo=formWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Screens Referring to This Request</h2>
        <#list artifactInfo.getScreenInfosReferringToRequest()?if_exists as screenWidgetArtifactInfo>
            <@displayScreenWidgetArtifactInfo screenWidgetArtifactInfo=screenWidgetArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Requests That Are Responses to This Request</h2>
        <#list artifactInfo.getRequestsThatAreResponsesToThisRequest()?if_exists as controllerRequestArtifactInfo>
            <@displayControllerRequestArtifactInfo controllerRequestArtifactInfo=controllerRequestArtifactInfo/>
        </#list>
        </div>
        
        <div>
        <h2>Requests That This Request is a Responses To</h2>
        <#list artifactInfo.getRequestsThatThisRequestIsResponsTo()?if_exists as controllerRequestArtifactInfo>
            <@displayControllerRequestArtifactInfo controllerRequestArtifactInfo=controllerRequestArtifactInfo/>
        </#list>
        </div>

        <div>
        <h2>Controller Views That Are Responses to This Request</h2>
        <#list artifactInfo.getViewsThatAreResponsesToThisRequest()?if_exists as controllerViewArtifactInfo>
            <@displayControllerViewArtifactInfo controllerViewArtifactInfo=controllerViewArtifactInfo/>
        </#list>
        </div>

    <#elseif artifactInfo.getType() == "view"/>
        <div>
        <h2>Requests That This View is a Responses To</h2>
        <#list artifactInfo.getRequestsThatThisViewIsResponseTo()?if_exists as controllerRequestArtifactInfo>
            <@displayControllerRequestArtifactInfo controllerRequestArtifactInfo=controllerRequestArtifactInfo/>
        </#list>
        </div>

        <#if artifactInfo.getScreenCalledByThisView()?exists>
            <div>
            <h2>Screen Called by This View</h2>
            <@displayScreenWidgetArtifactInfo screenWidgetArtifactInfo=artifactInfo.getScreenCalledByThisView()/>
            </div>
        </#if>

    </#if>
</#if>

<#-- ==================== MACROS ===================== -->
<#macro displayEntityArtifactInfo entityArtifactInfo>
    <div>&nbsp;-&nbsp;<@displayArtifactInfoLink artifactInfo=entityArtifactInfo/></div>
</#macro>

<#macro displayServiceArtifactInfo serviceArtifactInfo>
    <div>&nbsp;-&nbsp;<@displayArtifactInfoLink artifactInfo=serviceArtifactInfo/></div>
</#macro>

<#macro displayServiceEcaArtifactInfo serviceEcaArtifactInfo>
    <h4>Service ECA Rule: ${serviceEcaArtifactInfo.getDisplayPrefixedName()}</h4>
    <#if serviceEcaArtifactInfo.serviceEcaRule.getEcaConditionList()?has_content>
        <h4>ECA Rule Conditions</h4>
        <#list serviceEcaArtifactInfo.serviceEcaRule.getEcaConditionList() as ecaCondition>
            <div>&nbsp;-&nbsp;${ecaCondition.getShortDisplayDescription(true)}</div>
        </#list>
    </#if>
    <#if serviceEcaArtifactInfo.serviceEcaRule.getEcaActionList()?has_content>
        <h4>ECA Rule Actions</h4>
        <table>
        <#list serviceEcaArtifactInfo.serviceEcaRule.getEcaActionList() as ecaAction>
            <tr>
                <td><a href="<@ofbizUrl>ArtifactInfo?type=${artifactInfo.getType()}&amp;uniqueId=${ecaAction.getServiceName()}</@ofbizUrl>">${ecaAction.getServiceName()}</a></td>
                <td>${ecaAction.getServiceMode()}<#if ecaAction.isPersist()>-persisted</#if></td>
            </tr>
        </#list>
        </table>
    </#if>

    <#-- leaving this out, will show service links for actions
    <#if serviceEcaArtifactInfo.getServicesCalledByServiceEcaActions()?has_content>
        <h4>Services Called By Service ECA Actions</h4>
        <#list serviceEcaArtifactInfo.getServicesCalledByServiceEcaActions() as serviceArtifactInfo>
            <@displayServiceArtifactInfo serviceArtifactInfo=serviceArtifactInfo/>
        </#list>
    </#if>
    -->
    <#if serviceEcaArtifactInfo.getServicesTriggeringServiceEca()?has_content>
        <h4>Services Triggering Service ECA</h4>
        <#list serviceEcaArtifactInfo.getServicesTriggeringServiceEca() as serviceArtifactInfo>
            <@displayServiceArtifactInfo serviceArtifactInfo=serviceArtifactInfo/>
        </#list>
    </#if>
</#macro>

<#macro displayFormWidgetArtifactInfo formWidgetArtifactInfo>
    <div>&nbsp;-&nbsp;<@displayArtifactInfoLink artifactInfo=formWidgetArtifactInfo/></div>
</#macro>

<#macro displayScreenWidgetArtifactInfo screenWidgetArtifactInfo>
    <div>&nbsp;-&nbsp;<@displayArtifactInfoLink artifactInfo=screenWidgetArtifactInfo/></div>
</#macro>

<#macro displayControllerRequestArtifactInfo controllerRequestArtifactInfo>
    <div>&nbsp;-&nbsp;<@displayArtifactInfoLink artifactInfo=controllerRequestArtifactInfo/></div>
</#macro>

<#macro displayControllerViewArtifactInfo controllerViewArtifactInfo>
    <div>&nbsp;-&nbsp;<@displayArtifactInfoLink artifactInfo=controllerViewArtifactInfo/></div>
</#macro>

<#macro displayArtifactInfoLink artifactInfo>
<a href="<@ofbizUrl>ArtifactInfo?type=${artifactInfo.getType()}&amp;uniqueId=${artifactInfo.getUniqueId()?url('ISO-8859-1')}</@ofbizUrl>">${artifactInfo.getDisplayName()}</a>
</#macro>
