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

<#if (!contentRoot?has_content)>
    <#assign contentRoot = parameters.contentRoot/>
</#if>
<#assign formAction = "/createTextContentCms"/>
<#if (content?has_content)>
    <#assign formAction = "/updateTextContentCms"/>
</#if>

<!-- cms menu bar -->
<div id="cmsmenu" style="margin-bottom: 8px;">
    <#if (content?has_content)>        
        <a href="javascript:void(0);" onclick="javascript:callEditor(true, '${content.contentId}');" class="tabButton">New Content</a>
        <a href="javascript:void(0);" onclick="javascript:callPathAlias('${content.contentId}');" class="tabButton">New PathAlias</a>
    </#if>
</div>

<#if (!content?has_content)>
    <div class="tabletext" style="margin-bottom: 8px;">
        New <b>${contentAssocTypeId?default("SUBSITE")}</b> attached to Content: ${contentIdFrom?default(contentRoot)}</b>
    </div>
</#if>

<form name="cmsform" method="post" action="<@ofbizUrl>${formAction}</@ofbizUrl>" style="margin: 0;">
    <#if (content?has_content)>
        <input type="hidden" name="dataResourceId" value="${(dataText.dataResourceId)?if_exists}"/>        
        <input type="hidden" name="mimeTypeId" value="${content.mimeTypeId?default(mimeTypeId)}"/>
        <input type="hidden" name="contentId" value="${content.contentId}"/>

        <#list requestParameters.keySet() as paramName>
            <#if (paramName == 'contentIdFrom' || paramName == 'contentAssocTypeId' || paramName == 'fromDate')>
                <input type="hidden" name="${paramName}" value="${requestParameters.get(paramName)}"/>
            </#if>
        </#list>
    <#else>
        <input type="hidden" name="contentAssocTypeId" value="${contentAssocTypeId?default('SUBSITE')}"/>
        <input type="hidden" name="ownerContentId" value="${contentIdFrom?default(contentRoot)}"/>
        <input type="hidden" name="contentIdFrom" value="${contentIdFrom?default(contentRoot)}"/>
        <input type="hidden" name="mimeTypeId" value="${mimeTypeId}"/>
    </#if>
    <input type="hidden" name="webSiteId" value="${webSiteId}"/>
              
    <table>
      <#if (content?has_content)>
        <tr>
            <td><div class="tableheadtext">Content ID</div></td>
            <td><div class="tabletext">${content.contentId}</div></td>
        </tr>
      </#if>
      <tr>
        <td><div class="tableheadtext">Name</div></td>
        <td>
            <input type="text" name="contentName" class="inputBox" value="${(content.contentName)?if_exists}" size="40"/>
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">Description</div></td>
        <td>
            <textarea name="description" class="inputBox" cols="40" rows="6">${(content.description)?if_exists}</textarea>
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">Key</div></td>
        <td>
            <input type="text" name="mapKey" class="inputBox" value="${(assoc.mapKey)?if_exists}" size="40"/>
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">Data Type</div></td>
        <td>
            <select name="dataTemplateTypeId" class="selectBox">
                <#if (dataResource?has_content)>
                    <#if (dataResource.dataTemplateTypeId?has_content)>
                        <#assign thisType = dataResource.getRelatedOne("DataTemplateType")?if_exists/>
                        <option type="${thisType.dataTemplateTypeId}">${thisType.description}</option>
                        <option type="${thisType.dataTemplateTypeId}">----</option>
                    </#if>
                </#if>
                <#list templateTypes as type>
                    <option value="${type.dataTemplateTypeId}">${type.description}</option>
                </#list>
            </select>
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">Template</div></td>
        <td>
            <select name="templateDataResourceId" class="selectBox">
                <#if (content?has_content)>
                    <#if (content.templateDataResourceId?has_content && content.templateDataResourceId != "NONE")>
                        <#assign template = content.getRelatedOne("TemplateDataResource")/>
                        <option value="${template.dataResourceId}">${template.dataResourceName}</option>
                        <option value="${template.dataResourceId}">----</option>
                    </#if>
                </#if>
                <option value="">None</option>
                <#list templates as template>
                    <option value="${template.dataResourceId}">${template.dataResourceName}</option>                  
                </#list>
            </select>
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">Status</div></td>
        <td>
            <select name="statusId" class="selectBox">
                <#if (content?has_content)>
                    <#if (content.statusId?has_content)>
                        <#assign statusItem = content.getRelatedOne("StatusItem")/>
                        <option value="${statusItem.statusId}">${statusItem.description}</option>
                        <option value="${statusItem.statusId}">----</option>
                    </#if>
                </#if>
                <#list statuses as status>
                    <option value="${status.statusId}">${status.description}</option>
                </#list>
            </select>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <textarea id="cmsdata" name="textData" class="inputBox" cols="40" rows="6" style="display: none;">
            <#if (dataText?has_content)>
                ${dataText.textData}
            </#if>
          </textarea>
    </table>
</form>