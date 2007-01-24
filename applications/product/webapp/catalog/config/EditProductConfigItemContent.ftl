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
<script language="JavaScript" type="text/javascript">
function insertNowTimestamp(field) {
  eval('document.productForm.' + field + '.value="${nowTimestamp?string}";');
}
function insertImageName(size,nameValue) {
  eval('document.productForm.' + size + 'ImageUrl.value=nameValue;');
}
</script>

<#if fileType?has_content>
    <div class='head3'>${uiLabelMap.ProductResultOfImageUpload}</div>
    <#if !(clientFileName?has_content)>
        <div class='tabletext'>${uiLabelMap.ProductNoFileSpecifiedForUpload}.</div>
    <#else>
        <div class='tabletext'>${uiLabelMap.ProductTheFileOnYourComputer}: <b>${clientFileName?if_exists}</b></div>
        <div class='tabletext'>${uiLabelMap.ProductServerFileName}: <b>${fileNameToUse?if_exists}</b></div>
        <div class='tabletext'>${uiLabelMap.ProductServerDirectory}: <b>${imageServerPath?if_exists}</b></div>
        <div class='tabletext'>${uiLabelMap.ProductTheUrlOfYourUploadedFile}: <b><a href="<@ofbizContentUrl>${imageUrl?if_exists}</@ofbizContentUrl>">${imageUrl?if_exists}</a></b></div>
    </#if>
<br/>
</#if>    

<#if !(configItem?exists)>
    <h3>${uiLabelMap.ProductCouldNotFindProductConfigItem} "${configItemId}".</h3>
<#else>
    <table border="1" cellpadding="2" cellspacing="0" width="100%">
    <tr class="tableheadtext">
        <td>${uiLabelMap.ProductContent}</td>
        <td>${uiLabelMap.ProductType}</td>
        <td>${uiLabelMap.CommonFrom}</td>
        <td>${uiLabelMap.CommonThru}</td>
        <td>&nbsp;</td>
    </tr>
    <#list productContentList as entry>
        <#assign productContent=entry.productContent/>
        <#assign productContentType=productContent.getRelatedOneCache("ProdConfItemContentType")/>
        <tr class="tabletext">
            <td><a href="<@ofbizUrl>EditProductConfigItemContentContent?configItemId=${productContent.configItemId}&amp;contentId=${productContent.contentId}&amp;confItemContentTypeId=${productContent.confItemContentTypeId}&amp;fromDate=${productContent.fromDate}</@ofbizUrl>" class="buttontext">${entry.content.description?default("[${uiLabelMap.ProductNoDescription}]")} [${entry.content.contentId}]</td>
            <td>${productContentType.description?default(productContent.confItemContentTypeId)}</td>
            <td>${productContent.fromDate?default("N/A")}</td>
            <td>${productContent.thruDate?default("N/A")}</td>
            <td><a href="<@ofbizUrl>removeContentFromProductConfigItem?configItemId=${productContent.configItemId}&amp;contentId=${productContent.contentId}&amp;confItemContentTypeId=${productContent.confItemContentTypeId}&amp;fromDate=${productContent.fromDate}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDelete}]</a></td>
            <td><a href="/content/control/EditContent?contentId=${productContent.contentId}&externalLoginKey=${requestAttributes.externalLoginKey?if_exists}" class="buttontext">[${uiLabelMap.ProductEditContent} ${entry.content.contentId}]</td>
         </tr>
    </#list>
    </table>
    <div class="head2">${uiLabelMap.ProductCreateNewProductConfigItemContent}</div>
    <#if configItemId?has_content && configItem?has_content>
        ${prepareAddProductContentWrapper.renderFormString(context)}
    </#if>
    <div class="head2">${uiLabelMap.ProductAddContentProductConfigItem}</div>
    <#if configItemId?has_content && configItem?has_content>
        ${addProductContentWrapper.renderFormString(context)}
    </#if>

    <hr class="sepbar"/>
    
    <div class="head2">${uiLabelMap.ProductOverrideSimpleFields}</div>
    <form action="<@ofbizUrl>updateProductConfigItemContent</@ofbizUrl>" method="post" style="margin: 0;" name="productForm">
    <table border="0" cellpadding="2" cellspacing="0">
    <input type="hidden" name="configItemId" value="${configItemId?if_exists}">
    <tr>
        <td width="20%" align="right" valign="top"><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
            <textarea class="textAreaBox" name="description" cols="60" rows="2">${(configItem.description)?if_exists}</textarea>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right" valign="top"><div class="tabletext"><b>${uiLabelMap.ProductLongDescription}</b></div></td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
            <textarea class="textAreaBox" name="longDescription" cols="60" rows="7">${(configItem.longDescription)?if_exists}</textarea>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right" valign="top">
            <div class="tabletext"><b>${uiLabelMap.ProductSmallImage}</b></div>
            <#if (configItem.imageUrl)?exists>
                <a href="<@ofbizContentUrl>${configItem.imageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Image" src="<@ofbizContentUrl>${configItem.imageUrl}</@ofbizContentUrl>" height="40" width="40"></a>
            </#if>
        </td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
        <input type="text" class="inputBox" name="imageUrl" value="${(configItem.imageUrl)?default(imageNameSmall + '.jpg')}" size="60" maxlength="255">
        <#if configItemId?has_content>
            <div>
            <span class="tabletext">${uiLabelMap.ProductInsertDefaultImageUrl}: </span>
            <a href="javascript:insertImageName('small','${imageNameSmall}.jpg');" class="buttontext">[.jpg]</a>
            <a href="javascript:insertImageName('small','${imageNameSmall}.gif');" class="buttontext">[.gif]</a>
            <a href="javascript:insertImageName('small','');" class="buttontext">[${uiLabelMap.CommonClear}]</a>
            </div>
        </#if>
        </td>
    </tr>
    <tr>
        <td colspan="2">&nbsp;</td>
        <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}"></td>
        <td colspan="3">&nbsp;</td>
    </tr>
    </table>
    </form>

    <hr class="sepbar"/>

    <div class="head3">${uiLabelMap.ProductUploadImage}</div>
    <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>UploadProductConfigItemImage?configItemId=${configItemId}&upload_file_type=small</@ofbizUrl>" name="imageUploadForm">
        <input type="file" class="inputBox" size="50" name="fname">
        <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductUploadImage}">
    </form>
</#if>
