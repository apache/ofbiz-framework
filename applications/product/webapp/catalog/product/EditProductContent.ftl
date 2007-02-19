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
  eval('document.productForm.' + field + '.value="${nowTimestampString}";');
};
function insertImageName(type,nameValue) {
  eval('document.productForm.' + type + 'ImageUrl.value=nameValue;');
};
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

<#if !(product?exists)>
    <h3>${uiLabelMap.ProductCouldNotFindProduct} "${productId}".</h3>
<#else>
    <table border="1" cellpadding="2" cellspacing="0">
    <tr class="tableheadtext">
        <td>${uiLabelMap.ProductContent}</td>
        <td>${uiLabelMap.ProductType}</td>
        <td>${uiLabelMap.CommonFrom}</td>
        <td>${uiLabelMap.CommonThru}</td>
        <td>${uiLabelMap.ProductPurchaseFrom}</td>
        <td>${uiLabelMap.ProductPurchaseThru}</td>
        <td>${uiLabelMap.ProductUseCount}</td>
        <td>${uiLabelMap.ProductUseTime}</td>
        <td>${uiLabelMap.ProductUseTimeUom}</td>
        <td>${uiLabelMap.ProductUseRole}</td>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
    </tr>
    <#list productContentList as entry>
        <#assign productContent=entry.productContent/>
        <#assign productContentType=productContent.getRelatedOneCache("ProductContentType")/>
        <tr class="tabletext">
            <td><a href="<@ofbizUrl>EditProductContentContent?productId=${productContent.productId}&amp;contentId=${productContent.contentId}&amp;productContentTypeId=${productContent.productContentTypeId}&amp;fromDate=${productContent.fromDate}</@ofbizUrl>" class="buttontext">${entry.content.description?default("[${uiLabelMap.ProductNoDescription}]")} [${entry.content.contentId}]</td>
            <td>${productContentType.get("description",locale)?default(productContent.productContentTypeId)}</td>
            <td>${productContent.fromDate?default("${uiLabelMap.CommonNA}")}</td>
            <td>${productContent.thruDate?default("${uiLabelMap.CommonNA}")}</td>
            <td>${productContent.purchaseFromDate?default("${uiLabelMap.CommonNA}")}</td>
            <td>${productContent.purchaseThruDate?default("${uiLabelMap.CommonNA}")}</td>
            <td>${productContent.useCountLimit?default("${uiLabelMap.CommonNA}")}</td>
            <td>${productContent.useTime?default("${uiLabelMap.CommonNA}")}</td>
            <td>${productContent.useTimeUomId?default("${uiLabelMap.CommonNA}")}</td>
            <td>${productContent.useRoleTypeId?default("${uiLabelMap.CommonNA}")}</td>
            <td><a href="<@ofbizUrl>removeContentFromProduct?productId=${productContent.productId}&amp;contentId=${productContent.contentId}&amp;productContentTypeId=${productContent.productContentTypeId}&amp;fromDate=${productContent.fromDate}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDelete}]</a></td>
            <td><a href="/content/control/EditContent?contentId=${productContent.contentId}&externalLoginKey=${requestAttributes.externalLoginKey?if_exists}" class="buttontext">[${uiLabelMap.ProductEditContent} ${entry.content.contentId}]</td>
         </tr>
    </#list>
    </table>
    <div class="head2">${uiLabelMap.ProductCreateNewProductContent}</div>
    <#if productId?has_content && product?has_content>
        ${sections.render("PrepareAddProductContentAssoc")}
    </#if>
    <div class="head2">${uiLabelMap.ProductAddContentProduct}</div>
    <#if productId?has_content && product?has_content>
        ${sections.render("AddProductContentAssoc")}
    </#if>

    <hr class="sepbar"/>
    
    <div class="head2">${uiLabelMap.ProductOverrideSimpleFields}</div>
    <form action="<@ofbizUrl>updateProductContent</@ofbizUrl>" method="post" style="margin: 0;" name="productForm">
    <table border="0" cellpadding="2" cellspacing="0">
    <input type="hidden" name="productId" value="${productId?if_exists}">
    <tr>
        <td width="20%" align="right" valign="top"><div class="tabletext"><b>${uiLabelMap.ProductProductName}</b></div></td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
            <input type="text" class="inputBox" name="productName" value="${(product.productName?html)?if_exists}" size="30" maxlength="60">
        </td>
    </tr>        
    <tr>
        <td width="20%" align="right" valign="top"><div class="tabletext"><b>${uiLabelMap.ProductProductDescription}</b></div></td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
            <textarea class="textAreaBox" name="description" cols="60" rows="2">${(product.description)?if_exists}</textarea>
        </td>
    </tr>        
    <tr>
        <td width="20%" align="right" valign="top"><div class="tabletext"><b>${uiLabelMap.ProductLongDescription}</b></div></td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
            <textarea class="textAreaBox dojo-ResizableTextArea" name="longDescription" cols="60" rows="7">${(product.longDescription)?if_exists}</textarea>
        </td>
    </tr>        
    <tr>
        <td width="20%" align="right" valign="top"><div class="tabletext"><b>${uiLabelMap.ProductDetailScreen}</b></div></td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
            <input type="text" class="inputBox" name="detailScreen" value="${(product.detailScreen)?if_exists}" size="60" maxlength="250">
            <br/><span class="tabletext">${uiLabelMap.ProductIfNotSpecifiedDefaultsIsProductdetail} &quot;productdetail&quot;, ${uiLabelMap.ProductDetailScreenMessage}: &quot;component://ecommerce/widget/CatalogScreens.xml#productdetail&quot;</span>
        </td>
    </tr>        
    <tr>
        <td width="20%" align="right" valign="top">
            <div class="tabletext"><b>${uiLabelMap.ProductSmallImage}</b></div>
            <#if (product.smallImageUrl)?exists>
                <a href="<@ofbizContentUrl>${(product.smallImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Small Image" src="<@ofbizContentUrl>${(product.smallImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"></a>
            </#if>
        </td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
        <input type="text" class="inputBox" name="smallImageUrl" value="${(product.smallImageUrl)?default('')}" size="60" maxlength="255">
        <#if productId?has_content>
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
        <td width="20%" align="right" valign="top">
            <div class="tabletext"><b>${uiLabelMap.ProductMediumImage}</b></div>
            <#if (product.mediumImageUrl)?exists>
                <a href="<@ofbizContentUrl>${product.mediumImageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Medium Image" src="<@ofbizContentUrl>${product.mediumImageUrl}</@ofbizContentUrl>" height="40" width="40"></a>
            </#if>
        </td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
        <input type="text" class="inputBox" name="mediumImageUrl" value="${(product.mediumImageUrl)?default('')}" size="60" maxlength="255">
        <#if productId?has_content>
            <div>
            <span class="tabletext">${uiLabelMap.ProductInsertDefaultImageUrl}: </span>
            <a href="javascript:insertImageName('medium','${imageNameMedium}.jpg');" class="buttontext">[.jpg]</a>
            <a href="javascript:insertImageName('medium','${imageNameMedium}.gif');" class="buttontext">[.gif]</a>
            <a href="javascript:insertImageName('medium','');" class="buttontext">[${uiLabelMap.CommonClear}]</a>
            </div>
        </#if>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right" valign="top">
            <div class="tabletext"><b>${uiLabelMap.ProductLargeImage}</b></div>
            <#if (product.largeImageUrl)?exists>
                <a href="<@ofbizContentUrl>${product.largeImageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Large Image" src="<@ofbizContentUrl>${product.largeImageUrl}</@ofbizContentUrl>" height="40" width="40"></a>
            </#if>
        </td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
        <input type="text" class="inputBox" name="largeImageUrl" value="${(product.largeImageUrl)?default('')}" size="60" maxlength="255">
        <#if productId?has_content>
            <div>
            <span class="tabletext">${uiLabelMap.ProductInsertDefaultImageUrl}: </span>
            <a href="javascript:insertImageName('large','${imageNameLarge}.jpg');" class="buttontext">[.jpg]</a>
            <a href="javascript:insertImageName('large','${imageNameLarge}.gif');" class="buttontext">[.gif]</a>
            <a href="javascript:insertImageName('large','');" class="buttontext">[${uiLabelMap.CommonClear}]</a>
            </div>
        </#if>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right" valign="top">
            <div class="tabletext"><b>${uiLabelMap.ProductDetailImage}</b></div>
            <#if (product.detailImageUrl)?exists>
                <a href="<@ofbizContentUrl>${product.detailImageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Detail Image" src="<@ofbizContentUrl>${product.detailImageUrl}</@ofbizContentUrl>" height="40" width="40"></a>
            </#if>
        </td>
        <td>&nbsp;</td>
        <td width="80%" colspan="4" valign="top">
        <input type="text" class="inputBox" name="detailImageUrl" value="${(product.detailImageUrl)?default('')}" size="60" maxlength="255">
        <#if productId?has_content>
            <div>
            <span class="tabletext">${uiLabelMap.ProductInsertDefaultImageUrl}: </span>
            <a href="javascript:insertImageName('detail','${imageNameDetail}.jpg');" class="buttontext">[.jpg]</a>
            <a href="javascript:insertImageName('detail','${imageNameDetail}.gif');" class="buttontext">[.gif]</a>
            <a href="javascript:insertImageName('detail','');" class="buttontext">[${uiLabelMap.CommonClear}]</a>
            </div>
        </#if>
        </td>
    </tr>        
    <tr>
        <td colspan="2">&nbsp;</td>
        <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/></td>
        <td colspan="3">&nbsp;</td>
    </tr>
    </table>
    </form>

    <hr class="sepbar"/>

    <script language="JavaScript" type="text/javascript">
        function setUploadUrl(newUrl) {
        var toExec = 'document.imageUploadForm.action="' + newUrl + '";';
        eval(toExec);
        };
    </script>
    <div class="head3">${uiLabelMap.ProductUploadImage}</div>
    <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>UploadProductImage?productId=${productId}&upload_file_type=small</@ofbizUrl>" name="imageUploadForm">
        <input type="file" class="inputBox" size="50" name="fname">
        <br/>
        <span class="tabletext">
            <input class="radioButton" type="radio" name="upload_file_type_bogus" value="small" checked onclick='setUploadUrl("<@ofbizUrl>UploadProductImage?productId=${productId}&upload_file_type=small</@ofbizUrl>");'>${uiLabelMap.CommonSmall}
            <input class="radioButton" type="radio" name="upload_file_type_bogus" value="medium" onclick='setUploadUrl("<@ofbizUrl>UploadProductImage?productId=${productId}&upload_file_type=medium</@ofbizUrl>");'>${uiLabelMap.CommonMedium}
            <input class="radioButton" type="radio" name="upload_file_type_bogus" value="large"onclick='setUploadUrl("<@ofbizUrl>UploadProductImage?productId=${productId}&upload_file_type=large</@ofbizUrl>");'>${uiLabelMap.CommonLarge}
            <input class="radioButton" type="radio" name="upload_file_type_bogus" value="detail" onclick='setUploadUrl("<@ofbizUrl>UploadProductImage?productId=${productId}&upload_file_type=detail</@ofbizUrl>");'>${uiLabelMap.CommonDetail}
        </span>
        <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductUploadImage}">
    </form>
</#if>
