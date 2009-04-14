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
function insertImageName(type,nameValue) {
  eval('document.productCategoryForm.' + type + 'ImageUrl.value=nameValue;');
};
</script>
<#if fileType?has_content>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductResultOfImageUpload}</h3>
            <#if !(clientFileName?has_content)>
                <div>${uiLabelMap.ProductNoFileSpecifiedForUpload}.</div>
            <#else>
                <div>${uiLabelMap.ProductTheFileOnYourComputer}: <b>${clientFileName?if_exists}</b></div>
                <div>${uiLabelMap.ProductServerFileName}: <b>${fileNameToUse?if_exists}</b></div>
                <div>${uiLabelMap.ProductServerDirectory}: <b>${imageServerPath?if_exists}</b></div>
                <div>${uiLabelMap.ProductTheUrlOfYourUploadedFile}: <b><a href="<@ofbizContentUrl>${imageUrl?if_exists}</@ofbizContentUrl>">${imageUrl?if_exists}</a></b></div>
            </#if>
        </div>
    </div>
</#if>
<div class="screenlet">
<#if ! productCategory?has_content>
    <#if productCategoryId?has_content>
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductCouldNotFindProductCategoryWithId} "${productCategoryId}".</h3>
        </div>
        <div class="screenlet-body">
            <form action="<@ofbizUrl>createProductCategory</@ofbizUrl>" method="post" style="margin: 0;" name="productCategoryForm">
                <table cellspacing="0" class="basic-table">
                    <tr>
                        <td align="right" class="label">${uiLabelMap.ProductProductCategoryId}</td>
                        <td>&nbsp;</td>
                        <td>
                            <input type="text" name="productCategoryId" size="20" maxlength="40" value="${productCategoryId}"/>
                        </td>
                    </tr>
    <#else>
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.PageTitleCreateProductCategory}</h3>
        </div>
        <div class="screenlet-body">
            <form action="<@ofbizUrl>createProductCategory</@ofbizUrl>" method="post" style="margin: 0;" name="productCategoryForm">
                <table cellspacing="0" class="basic-table">
                    <tr>
                        <td align="right" class="label">${uiLabelMap.ProductProductCategoryId}</td>
                        <td>&nbsp;</td>
                        <td>
                            <input type="text" name="productCategoryId" size="20" maxlength="40" value=""/>
                        </td>
                    </tr>
    </#if>
<#else>
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PageTitleEditProductCategories}</h3>
    </div>
    <div class="screenlet-body">
        <form action="<@ofbizUrl>updateProductCategory</@ofbizUrl>" method="post" style="margin: 0;" name="productCategoryForm">
            <input type="hidden" name="productCategoryId" value="${productCategoryId}"/>
            <table cellspacing="0" class="basic-table">
                <tr>
                    <td align="right" class="label">${uiLabelMap.ProductProductCategoryId}</td>
                    <td>&nbsp;</td>
                    <td>
                      <b>${productCategoryId}</b> (${uiLabelMap.ProductNotModificationRecrationCategory}.)
                    </td>
                </tr>
</#if>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductProductCategoryType}</td>
                    <td>&nbsp;</td>
                    <td width="74%">
                        <select name="productCategoryTypeId" size="1">
                        <#list productCategoryTypes as productCategoryTypeData>
                        <option <#if ((productCategory?has_content) && (productCategory.productCategoryTypeId==productCategoryTypeData.productCategoryTypeId)) || (productCategoryTypeData.productCategoryTypeId=="CATALOG_CATEGORY")>selected="selected"</#if> value="${productCategoryTypeData.productCategoryTypeId}">${productCategoryTypeData.get("description",locale)}</option>
                        </#list>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductName}</td>
                    <td>&nbsp;</td>
                    <td width="74%"><input type="text" value="${(productCategory.categoryName)?if_exists}" name="categoryName" size="60" maxlength="60"/></td>
                </tr>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductProductDescription}</td>
                    <td>&nbsp;</td>
                    <td width="74%"><textarea name="description" cols="60" rows="2"><#if productCategory?has_content>${(productCategory.description)?if_exists}</#if></textarea></td>
                </tr>
                <tr>
                    <td width="20%" align="right" valign="top" class="label">
                        ${uiLabelMap.ProductCategoryImageUrl}
                        <#if (productCategory.categoryImageUrl)?exists>
                            <a href="<@ofbizContentUrl>${(productCategory.categoryImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Category Image" src="<@ofbizContentUrl>${(productCategory.categoryImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"></a>
                        </#if>
                    </td>
                    <td>&nbsp;</td>
                    <td width="80%" colspan="4" valign="top">
                        <input type="text" name="categoryImageUrl" value="${(productCategory.categoryImageUrl)?default('')}" size="60" maxlength="255"/>
                        <#if productCategory?has_content>
                            <div>
                            ${uiLabelMap.ProductInsertDefaultImageUrl}:
                            <a href="javascript:insertImageName('category','${imageNameCategory}.jpg');" class="buttontext">.jpg</a>
                            <a href="javascript:insertImageName('category','${imageNameCategory}.gif');" class="buttontext">.gif</a>
                            <a href="javascript:insertImageName('category','');" class="buttontext">${uiLabelMap.CommonClear}</a>
                            </div>
                        </#if>
                    </td>
                </tr>
                <tr>
                    <td width="20%" align="right" valign="top" class="label">
                        ${uiLabelMap.ProductLinkOneImageUrl}
                        <#if (productCategory.linkOneImageUrl)?exists>
                            <a href="<@ofbizContentUrl>${(productCategory.linkOneImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Link One Image" src="<@ofbizContentUrl>${(productCategory.linkOneImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"></a>
                        </#if>
                    </td>
                    <td>&nbsp;</td>
                    <td width="80%" colspan="4" valign="top">
                        <input type="text" name="linkOneImageUrl" value="${(productCategory.linkOneImageUrl)?default('')}" size="60" maxlength="255"/>
                        <#if productCategory?has_content>
                            <div>
                                ${uiLabelMap.ProductInsertDefaultImageUrl}:
                                <a href="javascript:insertImageName('linkOne','${imageNameLinkOne}.jpg');" class="buttontext">.jpg</a>
                                <a href="javascript:insertImageName('linkOne','${imageNameLinkOne}.gif');" class="buttontext">.gif</a>
                                <a href="javascript:insertImageName('linkOne','');" class="buttontext">${uiLabelMap.CommonClear}</a>
                            </div>
                        </#if>
                    </td>
                </tr>
                <tr>
                    <td width="20%" align="right" valign="top" class="label">
                        ${uiLabelMap.ProductLinkTwoImageUrl}
                        <#if (productCategory.linkTwoImageUrl)?exists>
                            <a href="<@ofbizContentUrl>${(productCategory.linkTwoImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Link One Image" src="<@ofbizContentUrl>${(productCategory.linkTwoImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"></a>
                        </#if>
                    </td>
                    <td>&nbsp;</td>
                    <td width="80%" colspan="4" valign="top">
                        <input type="text" name="linkTwoImageUrl" value="${(productCategory.linkTwoImageUrl)?default('')}" size="60" maxlength="255"/>
                        <#if productCategory?has_content>
                            <div>
                                ${uiLabelMap.ProductInsertDefaultImageUrl}:
                                <a href="javascript:insertImageName('linkTwo','${imageNameLinkTwo}.jpg');" class="buttontext">.jpg</a>
                                <a href="javascript:insertImageName('linkTwo','${imageNameLinkTwo}.gif');" class="buttontext">.gif</a>
                                <a href="javascript:insertImageName('linkTwo','');" class="buttontext">${uiLabelMap.CommonClear}</a>
                            </div>
                        </#if>
                    </td>
                </tr>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductDetailScreen}</td>
                    <td>&nbsp;</td>
                    <td width="74%">
                        <input type="text" <#if productCategory?has_content>value="${productCategory.detailScreen?if_exists}"</#if> name="detailScreen" size="60" maxlength="250"/>
                        <br/><span class="tooltip">${uiLabelMap.ProductDefaultsTo} &quot;categorydetail&quot;, ${uiLabelMap.ProductDetailScreenMessage}: &quot;component://ecommerce/widget/CatalogScreens.xml#categorydetail&quot;</span>
                    </td>
                </tr>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductPrimaryParentCategory}</td>
                    <td>&nbsp;</td>
                    <td width="74%">
                        <input type="text" name="primaryParentCategoryId" size="20" maxlength="20" value="${(productCategory.primaryParentCategoryId)?default('')}"/>
                        <a href="javascript:call_fieldlookup2(document.productCategoryForm.primaryParentCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}"/></td>
                </tr>
            </table>
        </form>
    </div>
</div>
<#if productCategoryId?has_content>
    <script language="JavaScript" type="text/javascript">
        function setUploadUrl(newUrl) {
        var toExec = 'document.imageUploadForm.action="' + newUrl + '";';
        eval(toExec);
        };
    </script>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductCategoryUploadImage}</h3>
        </div>
        <div class="screenlet-body">
            <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId?if_exists}&amp;upload_file_type=category</@ofbizUrl>" name="imageUploadForm">
                <table cellspacing="0" class="basic-table">
                    <tr><td>
                        <input type="file" size="50" name="fname"/>
                        <br/>
                        <span>
                            <input type="radio" name="upload_file_type_bogus" value="category" checked="checked" onclick='setUploadUrl("<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId}&amp;upload_file_type=category</@ofbizUrl>");'/>${uiLabelMap.ProductCategoryImageUrl}
                            <input type="radio" name="upload_file_type_bogus" value="linkOne" onclick='setUploadUrl("<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId}&amp;upload_file_type=linkOne</@ofbizUrl>");'/>${uiLabelMap.ProductLinkOneImageUrl}
                            <input type="radio" name="upload_file_type_bogus" value="linkTwo"onclick='setUploadUrl("<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId}&amp;upload_file_type=linkTwo</@ofbizUrl>");'/>${uiLabelMap.ProductLinkTwoImageUrl}
                        </span>
                        <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductUploadImage}"/>
                    </td></tr>
                </table>
            </form>
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductDuplicateProductCategory}</h3>
        </div>
        <div class="screenlet-body">
            <form action="<@ofbizUrl>DuplicateProductCategory</@ofbizUrl>" method="post" style="margin: 0;">
                <table cellspacing="0" class="basic-table">
                    <tr><td>
                        ${uiLabelMap.ProductDuplicateProductCategorySelected}:
                        <input type=hidden name="oldProductCategoryId" value="${productCategoryId}"/>
                        <div>
                            <input type="text" size="20" maxlength="20" name="productCategoryId"/>&nbsp;<input type=submit class="smallSubmit" value="${uiLabelMap.CommonGo}"/>
                        </div>
                        <div>
                            <b>${uiLabelMap.CommonDuplicate}:</b>
                            ${uiLabelMap.ProductCategoryContent}&nbsp;<input type="checkbox" name="duplicateContent" value="Y" checked />
                            ${uiLabelMap.ProductCategoryRollupParentCategories}&nbsp;<input type="checkbox" name="duplicateParentRollup" value="Y" checked />
                            ${uiLabelMap.ProductCategoryRollupChildCategories}&nbsp;<input type="checkbox" name="duplicateChildRollup" value="Y" />
                            ${uiLabelMap.ProductProducts}&nbsp;<input type="checkbox" name="duplicateMembers" value="Y" checked />
                            ${uiLabelMap.ProductCatalogs}&nbsp;<input type="checkbox" name="duplicateCatalogs" value="Y" checked />
                            ${uiLabelMap.ProductFeatures}&nbsp;<input type="checkbox" name="duplicateFeatures" value="Y" checked />
                            ${uiLabelMap.PartyParties}&nbsp;<input type="checkbox" name="duplicateRoles" value="Y" checked />
                            ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" name="duplicateAttributes" value="Y" checked />
                        </div>
                    </td></tr>
                </table>
            </form>
        </div>
    </div>
</#if>
