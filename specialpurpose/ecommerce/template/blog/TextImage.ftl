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

<#--
# This page displays both the textarea for text and the file upload control.
# It was awkward to do it with screen widgets because need to show checkboxes
# by each control.
-->

<#-- This code sets the checkboxes based on existing data -->
<#assign text_check=""/>
<#assign img_check=""/>
<#if (imageContent?has_content && (drDataTemplateTypeId?default("") == "SCREEN_COMBINED") || drMimeTypeId?default("")?starts_with("image"))>
    <#assign img_check="checked='checked'"/>
</#if>
<#if (textContent?has_content && drMimeTypeId?default("")?starts_with("text"))
        || (textContent?has_content && (drDataTemplateTypeId?default("") == "SCREEN_COMBINED"))
        || !img_check?has_content>
    <#assign text_check="checked='checked'"/>
</#if>

<#-- Sets one of the two templates -->
<#assign topleft_check=""/>
<#assign topcenter_check=""/>
<#if view.drDataResourceId?has_content && view.drDataResourceId == "BLOG_TPL_TOPLEFT">
    <#assign topleft_check="checked='checked'"/>
<#else>
    <#assign topcenter_check="checked='checked'"/>
</#if>

<#-- Fills in existing text -->
<#assign textData=""/>
<#if electronicText?has_content >
    <#if electronicText.textData?has_content >
        <#assign textData=electronicText.textData/>
    </#if>
</#if>

<#-- Stores the ids for existing data -->
<#assign textContentId=""/>
<#assign textDataResourceId=""/>

<#if textContent?has_content >
    <#assign textContentId=textContent.contentId!/>
    <#assign textDataResourceId=textContent.drDataResourceId!/>
</#if>

<#if textElectronicText?has_content && textElectronicText.textData?has_content >
    <#assign textData=textElectronicText.textData/>
</#if>

<#assign imageContentId=""/>
<#assign imageDataResourceId=""/>

<#if imageContent?has_content >
    <#assign imageContentId=imageContent.contentId!/>
    <#assign imageDataResourceId=imageContent.drDataResourceId!/>
</#if>

<input type="hidden" name="textContentId" value="${textContentId}"/>
<input type="hidden" name="imageContentId" value="${imageContentId}"/>
<input type="hidden" name="textDataResourceId" value="${textDataResourceId}"/>
<input type="hidden" name="imageDataResourceId" value="${imageDataResourceId}"/>

<table>
    <tr>
        <td width="10%" align="right">
        <span class="treeHeader"> </span>
        </td>
        <td>&nbsp;</td>
        <td width="5%" valign="top">
        <div class="inputBox"><input type="checkBox" ${text_check} name="drMimeTypeId_TEXT" value="Y"/>Text</div>
        </td>
        <td>&nbsp;</td>
        <td width="60%">
        <textarea class="textAreaBox" class="inputBox" name="textData" cols="60" rows="24">${textData!}</textarea>
        </td>
        <td width="10%" align="right">
        <span class="treeHeader"> </span>
        </td>
    </tr>

    <tr>
        <td width="10%" align="right">
            <span class="treeHeader"> </span>
        </td>
        <td>&nbsp;</td>
        <td width="5%" valign="top">
            <div class="inputBox"><input type="checkbox" ${img_check} name="drMimeTypeId_IMAGE" value="Y"/>Image</div>
        </td>
        <td>&nbsp;</td>
        <td width="60%">
            <div class="inputBox">Existing file name:  <#if imageContent?has_content && imageContent.drObjectInfo?has_content>${imageContent.drObjectInfo}</#if></div>
            <br />
            <input type="file" class="inputBox" name="uploadedFile" size="25"/>
            <#--
            Force: <input type="checkbox" value="true" name="forceElectronicText"/>
            -->
            <br />
            Top-left:<input type="radio" ${topleft_check} class="inputBox" name="templateId" value="BLOG_TPL_TOPLEFT"/>
            &nbsp;Top-center:<input type="radio" ${topcenter_check} class="inputBox" name="templateId" value="BLOG_TPL_TOPCENTER"/>
        </td>
        <td width="10%" align="right">
            <span class="treeHeader">&nbsp;</span>
        </td>
    </tr>
</table>
