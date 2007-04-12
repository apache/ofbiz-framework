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

<#macro cmsNewMetaRec>
    <input type="hidden" name="contentTypeId" value="DOCUMENT"/>
    <input type="hidden" name="dataResourceTypeId" value="SHORT_TEXT"/>
    <input type="hidden" name="contentAssocTypeId" value="SUB_CONTENT"/>
    <input type="hidden" name="statusId" value="CTNT_PUBLISHED"/>
    <input type="hidden" name="ownerContentId" value="${(content.contentId)?if_exists}"/>
    <input type="hidden" name="contentIdFrom" value="${(content.contentId)?if_exists}"/>
</#macro>

<#-- cms menu bar -->
<div id="cmsmenu" style="margin-bottom: 8px;">
    <#if (content?has_content)>
        <a href="javascript:void(0);" onclick="javascript:callEditor(true, '${content.contentId}', '', 'ELECTRONIC_TEXT');" class="tabButton">Quick Sub-Content</a>
        <a href="javascript:void(0);" onclick="javascript:callPathAlias('${content.contentId}');" class="tabButton">Path Alias</a>
        <a href="javascript:void(0);" onclick="javascript:callMetaInfo('${content.contentId}');" class="tabButtonSelected">Meta Tags</a>
    </#if>
</div>

<#if (content?has_content)>
    <div class="tabletext" style="margin-bottom: 8px;">
        Set <b>Meta-Data</b> for Content: <b>${content.contentId}</b></b>
    </div>
</#if>

<#if (title?has_content)>
    <#assign titleAction = "/updateWebSiteMetaInfoJson"/>
<#else>
    <#assign titleAction = "/createWebSiteMetaInfoJson"/>
</#if>
<#if (titleProperty?has_content)>
    <#assign titlePropertyAction = "/updateWebSiteMetaInfoJson"/>
<#else>
    <#assign titlePropertyAction = "/createWebSiteMetaInfoJson"/>
</#if>
<#if (metaDescription?has_content)>
    <#assign metaDescriptionAction = "/updateWebSiteMetaInfoJson"/>
<#else>
    <#assign metaDescriptionAction = "/createWebSiteMetaInfoJson"/>
</#if>
<#if (metaKeywords?has_content)>
    <#assign metaKeywordsAction = "/updateWebSiteMetaInfoJson"/>
<#else>
    <#assign metaKeywordsAction = "/createWebSiteMetaInfoJson"/>
</#if>

<form name="cmsmeta_title" action="<@ofbizUrl>/${titleAction}</@ofbizUrl>" style="margin: 0;">
    <#if (title?has_content)>
        <input type="hidden" name="dataResourceId" value="${title.dataResourceId}"/>
    <#else>
        <input type="hidden" name="contentName" value="Meta-Title: ${contentId}"/>
        <input type="hidden" name="mapKey" value="title"/>
        <@cmsNewMetaRec/>
    </#if>
    <input type="hidden" name="objectInfo" value=""/>
</form>
<form name="cmsmeta_titleProperty" action="<@ofbizUrl>/${titlePropertyAction}</@ofbizUrl>" style="margin: 0;">
    <#if (titleProperty?has_content)>
        <input type="hidden" name="dataResourceId" value="${titleProperty.dataResourceId}"/>
    <#else>
        <input type="hidden" name="contentName" value="Meta-TitleProperty: ${contentId}"/>
        <input type="hidden" name="mapKey" value="titleProperty"/>
        <@cmsNewMetaRec/>
    </#if>
    <input type="hidden" name="objectInfo" value=""/>
</form>
<form name="cmsmeta_metaDescription" action="<@ofbizUrl>/${metaDescriptionAction}</@ofbizUrl>" style="margin: 0;">
    <#if (metaDescription?has_content)>
        <input type="hidden" name="dataResourceId" value="${metaDescription.dataResourceId}"/>
    <#else>
        <input type="hidden" name="contentName" value="Meta-Description: ${contentId}"/>
        <input type="hidden" name="mapKey" value="metaDescription"/>
        <@cmsNewMetaRec/>
    </#if>
    <input type="hidden" name="objectInfo" value=""/>
</form>
<form name="cmsmeta_metaKeywords" action="<@ofbizUrl>/${metaKeywordsAction}</@ofbizUrl>" style="margin: 0;">
    <#if (metaKeywords?has_content)>
        <input type="hidden" name="dataResourceId" value="${metaKeywords.dataResourceId}"/>
    <#else>
        <input type="hidden" name="contentName" value="Meta-Keywords: ${contentId}"/>
        <input type="hidden" name="mapKey" value="metaKeywords"/>
        <@cmsNewMetaRec/>
    </#if>
    <input type="hidden" name="objectInfo" value=""/>
</form>

<form name="cmsmetaform" action="javascript:void(0);" style="margin: 0;">
    <table>        
        <tr>
            <td><div class="tableheadtext">Page Title</div></td>
            <td><input type="text" class="inputBox" name="title" value="${(title.objectInfo)?if_exists}" size="40"></td>
        </tr>
        <tr>
            <td><div class="tableheadtext">Title Property</div></td>
            <td><input type="text" class="inputBox" name="titleProperty" value="${(titleProperty.objectInfo)?if_exists}" size="40"></td>
        </tr>
        <tr>
            <td><div class="tableheadtext">Meta-Description</div></td>
            <td><input type="text" class="inputBox" name="metaDescription" value="${(metaDescription.objectInfo)?if_exists}" size="40"></td>
        </tr>
        <tr>
            <td><div class="tableheadtext">Meta-Keywords</div></td>
            <td><input type="text" class="inputBox" name="metaKeywords" value="${(metaKeywords.objectInfo)?if_exists}" size="40"></td>
        </tr>
        <tr><td colspan="2">&nbsp;</td></tr>        
        <tr>
            <td colspan="2" align="center"><input id="submit" type="button" onclick="javascript:saveMetaInfo(cmsmetaform);" class="smallSubmit" value="Save"/></td>
        </tr>
    </table>
</form>
