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

<script type="text/javascript">
    function cmsSave() {
        var simpleFormAction = '<@ofbizUrl>/updateContentCms</@ofbizUrl>';
        var editor = dojo.widget.byId("w_editor");
        if (editor) {
            var cmsdata = dojo.byId("cmsdata");
            cmsdata.value = editor.getEditorContent();
        }

        // get the cmsform
        var form = document.cmsform;

        // set the data resource name
        form.dataResourceName.value = form.contentName.value;

        // check to see if we need to change the form action
        var isUpload = form.elements['isUploadObject'];
        if (isUpload && isUpload.value == 'Y') {
            var uploadValue = form.elements['uploadedFile'].value;
            if (uploadValue == null || uploadValue == "") {
                form.action = simpleFormAction;
            }            
        }

        // submit the form
        if (form != null) {
            form.submit();
        } else {
            alert("Cannot find the cmsform!");
        }

        return false;
    }

    function selectDataType(contentId) {
        var selectObject = document.forms['cmsdatatype'].elements['dataResourceTypeId'];
        var typeValue = selectObject.options[selectObject.selectedIndex].value;
        callEditor(true, contentId, '', typeValue);
    }
</script>

<#-- cms menu bar -->
<div id="cmsmenu" style="margin-bottom: 8px;">
    <#if (content?has_content)>        
        <a href="javascript:void(0);" onclick="javascript:callEditor(true, '${content.contentId}', '', 'ELECTRONIC_TEXT');" class="tabButton">Quick Sub-Content</a>
        <a href="javascript:void(0);" onclick="javascript:callPathAlias('${content.contentId}');" class="tabButton">Path Alias</a>
        <a href="javascript:void(0);" onclick="javascript:callMetaInfo('${content.contentId}');" class="tabButton">Meta Tags</a>
    </#if>
</div>

<#-- content info -->
<#if (!content?has_content)>
    <div class="tabletext" style="margin-bottom: 8px;">
        New <b>${contentAssocTypeId?default("SUBSITE")}</b> attached to Content: ${contentIdFrom?default(contentRoot)}</b>
    </div>
</#if>

<#-- dataResourceTypeId -->
<#if (!dataResourceTypeId?has_content)>
    <#if (dataResource?has_content)>
        <#assign dataResourceTypeId = dataResource.dataResourceTypeId/>
    <#elseif (content?has_content)>
        <#assign dataResourceTypeId = "NONE"/>
    <#else>       
        <form name="cmsdatatype">
            <table>
                <tr>
                    <td><div class="tableheadtext">Data Type</div></td>
                    <td>            
                        <select class="inputBox" name="dataResourceTypeId">
                            <option value="NONE">None (Tree, Category, etc)</option>
                            <option value="SHORT_TEXT">Short Text (255 chars.)</option>
                            <option value="ELECTRONIC_TEXT">Long Text</option>
                            <option value="URL_RESOURCE">URL Resource</option>
                            <option value="IMAGE_OBJECT">Image</option>
                            <option value="VIDEO_OBJECT">Video</option>
                            <option value="AUDIO_OBJECT">Audio</option>
                            <option value="OTHER_OBJECT">Other</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td align="center" colspan="2">
                        <a href="javascript:void(0);" onclick="javascript:selectDataType('${contentIdFrom?default(contentRoot)}');" class="buttontext">Continue</a>
                    </td>
                </tr>
                <#list 0..15 as x>
                    <tr><td colspan="2">&nbsp;</td></tr>
                </#list>
            </table>           
        </form>        
    </#if>    
</#if>

<#-- form action -->
<#if (dataResourceTypeId?has_content)>
    <#assign actionSuffix = "ContentCms"/>
    <#if (dataResourceTypeId == "NONE" || (content?has_content && !content.dataResourceId?has_content))>
        <#assign actionMiddle = ""/>
    <#else>
        <#if (dataResourceTypeId?ends_with("_OBJECT"))>
            <#assign actionMiddle = "Object"/>
        <#else>
            <#assign actionMiddle = "Text"/>
        </#if>
    </#if>

    <#if (!contentRoot?has_content)>
        <#assign contentRoot = parameters.contentRoot/>
    </#if>
    <#if (content?has_content)>
        <#assign actionPrefix = "/update"/>
    <#else>
        <#assign actionPrefix = "/create"/>
    </#if>
    <#assign formAction = actionPrefix + actionMiddle + actionSuffix/>
<#else>
    <#assign formAction = "javascript:void(0);"/>
</#if>

<#-- main content form -->
<#if (dataResourceTypeId?has_content)>
    <form name="cmsform" enctype="multipart/form-data" method="post" action="<@ofbizUrl>${formAction}</@ofbizUrl>" style="margin: 0;">
        <#if (content?has_content)>
            <input type="hidden" name="dataResourceId" value="${(dataResource.dataResourceId)?if_exists}"/>
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
        <#if (dataResourceTypeId != 'NONE')>                  
            <input type="hidden" name="dataResourceTypeId" value="${dataResourceTypeId}"/>
        </#if>
        <input type="hidden" name="webSiteId" value="${webSiteId}"/>
        <input type="hidden" name="dataResourceName" value="${(dataResource.dataResourceName)?if_exists}"/>

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
            <td><div class="tableheadtext">Purpose</div></td>
            <td>
                <select name="contentPurposeTypeId" class="selectBox">
                    <#if (currentPurposes?has_content)>
                        <#assign purpose = currentPurposes[0].getRelatedOne("ContentPurposeType")/>
                        <option value="${purpose.contentPurposeTypeId}">${purpose.description?default(purpose.contentPurposeTypeId)}</option>
                        <option value="${purpose.contentPurposeTypeId}">----</option>
                    <#else>
                        <option value="SECTION">Section</option>
                        <option value="SECTION">----</option>
                    </#if>
                    <#list purposeTypes as type>
                        <option value="${type.contentPurposeTypeId}">${type.description}</option>
                    </#list>
                </select>
            </td>
          </tr>
          <tr>
            <td><div class="tableheadtext">Data Type</div></td>
            <td>
                <select name="dataTemplateTypeId" class="selectBox">
                    <#if (dataResource?has_content)>
                        <#if (dataResource.dataTemplateTypeId?has_content)>
                            <#assign thisType = dataResource.getRelatedOne("DataTemplateType")?if_exists/>
                            <option value="${thisType.dataTemplateTypeId}">${thisType.description}</option>
                            <option value="${thisType.dataTemplateTypeId}">----</option>
                        </#if>
                    </#if>
                    <#list templateTypes as type>
                        <option value="${type.dataTemplateTypeId}">${type.description}</option>
                    </#list>
                </select>
            </td>
          </tr>
          <tr>
            <td><div class="tableheadtext">Decorator</div></td>
            <td>
                <select name="decoratorContentId" class="selectBox">
                    <#if (content?has_content)>
                        <#if (content.decoratorContentId?has_content)>
                            <#assign thisDec = content.getRelatedOne("DecoratorContent")/>
                            <option value="${thisDec.contentId}">${thisDec.contentName}</option>
                            <option value="${thisDec.contentId}">----</option>
                        </#if>
                    </#if>
                    <option value="">None</option>
                    <#list decorators as decorator>
                        <option value="${decorator.contentId}">${decorator.contentName}</option>
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
            <td><div class="tableheadtext">Is Public</div></td>
            <td>
                <select name="isPublic" class="selectBox">
                    <#if (dataResource?has_content)>
                        <#if (dataResource.isPublic?has_content)>
                            <option>${dataResource.isPublic}</option>
                            <option value="${dataResource.isPublic}">----</option>
                        <#else>
                            <option></option>
                        </#if>
                    </#if>
                    <option>Y</option>
                    <option>N</option>
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
            </td>
          </tr>

          <#-- this all depends on the dataResourceTypeId which was selected -->
          <#if (dataResourceTypeId == 'IMAGE_OBJECT' || dataResourceTypeId == 'OTHER_OBJECT' ||
                dataResourceTypeId == 'VIDEO_OBJECT' || dataResourceTypeId == 'AUDIO_OBJECT')>
            <tr>
              <td colspan="2" align="right">
                <#if ((content.contentId)?has_content)>
                    <@renderContentAsText contentId="${content.contentId}" ignoreTemplate="true"/>
                </#if>                
              </td>
            </tr>
            <tr>
              <td><div class="tableheadtext">Upload</div></td>
              <td>
                <input type="hidden" name="isUploadObject" value="Y"/>
                <input type="file" name="uploadedFile" class="inputBox" size="30"/>
              </td>
            </tr>
          <#elseif (dataResourceTypeId == 'URL_RESOURCE')>
            <tr>
              <td><div class="tableheadtext">URL</div></td>
              <td>
                <input type="text" name="objectInfo" class="inputBox" size="40" maxsize="255" value="${(dataResource.objectInfo)?if_exists}"/>
              </td>
            </tr>
          <#elseif (dataResourceTypeId == 'SHORT_TEXT')>
            <tr>
              <td><div class="tableheadtext">Text</div></td>
              <td>
                <input type="text" name="objectInfo" class="inputBox" size="40" maxsize="255" value="${(dataResource.objectInfo)?if_exists}"/>
              </td>
            </tr>
          <#elseif (dataResourceTypeId == 'ELECTRONIC_TEXT')>
            <tr>
              <td colspan="2">
                <div id="editorcontainer" class="nocolumns">
                    <div id="cmseditor" style="margin: 0; width: 100%; border: 1px solid black;"></div>
                </div>
              </td>
            </tr>
          </#if>

          <tr>
            <td align="center" colspan="2">
                <a href="javascript:void(0);" onclick="javascript:cmsSave();" class="buttontext">Save</a>
            </td>
          </tr>
        </table>
    </form>
</#if>