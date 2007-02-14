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


<br/>

<#if ! productCategory?has_content>
  <#if productCategoryId?has_content>
    <h3>${uiLabelMap.ProductCouldNotFindProductCategoryWithId} "${productCategoryId}".</h3>
    <form action="<@ofbizUrl>createProductCategory</@ofbizUrl>" method="post" style="margin: 0;" name="productCategoryForm">
    <table border="0" cellpadding="2" cellspacing="0">
    <tr>
      <td align="right"><div class="tabletext">${uiLabelMap.ProductProductCategoryId}</div></td>
      <td>&nbsp;</td>
      <td>
        <input type="text" name="productCategoryId" size="20" maxlength="40" value="${productCategoryId}" class="inputBox"/>
      </td>
    </tr>
  <#else>
    <form action="<@ofbizUrl>createProductCategory</@ofbizUrl>" method="post" style="margin: 0;" name="productCategoryForm">
    <table border="0" cellpadding="2" cellspacing="0">
    <tr>
      <td align="right"><div class="tabletext">${uiLabelMap.ProductProductCategoryId}</div></td>
      <td>&nbsp;</td>
      <td>
        <input type="text" name="productCategoryId" size="20" maxlength="40" value="" class="inputBox"/>
      </td>
    </tr>
  </#if>
<#else>
  <form action="<@ofbizUrl>updateProductCategory</@ofbizUrl>" method="post" style="margin: 0;" name="productCategoryForm">
  <input type="hidden" name="productCategoryId" value="${productCategoryId}"/>
  <table border="0" cellpadding="2" cellspacing="0">
  <tr>
    <td align="right"><div class="tabletext">${uiLabelMap.ProductProductCategoryId}</div></td>
    <td>&nbsp;</td>
    <td>
      <b>${productCategoryId}</b> (${uiLabelMap.ProductNotModificationRecrationCategory}.)
    </td>
  </tr>
</#if>
  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductProductCategoryType}</div></td>
    <td>&nbsp;</td>
    <td width="74%">
      <select name="productCategoryTypeId" size="1" class="selectBox">
        <#list productCategoryTypes as productCategoryTypeData>
          <option <#if ((productCategory?has_content) && (productCategory.productCategoryTypeId==productCategoryTypeData.productCategoryTypeId)) || (productCategoryTypeData.productCategoryTypeId=="CATALOG_CATEGORY")>selected="selected"</#if> value="${productCategoryTypeData.productCategoryTypeId}">${productCategoryTypeData.get("description",locale)}</option>
       </#list>
      </select>
    </td>
  </tr>
  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductName}</div></td>
    <td>&nbsp;</td>
    <td width="74%"><input type="text" value="${(productCategory.categoryName)?if_exists}" name="categoryName" size="60" maxlength="60" class="inputBox"/></td>
  </tr>
  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductDescription}</div></td>
    <td>&nbsp;</td>
    <td width="74%"><textarea class="textAreaBox" name="description" cols="60" rows="2"><#if productCategory?has_content>${(productCategory.description)?if_exists}</#if></textarea></td>
  </tr>
<#--
  <tr>
    <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.ProductLongDescription}</div></td>
    <td>&nbsp;</td>
    <td width="74%"><textarea cols="60" rows="3" name="longDescription" maxlength="2000" class="textAreaBox"><#if productCategory?has_content>${productCategory.longDescription?if_exists}</#if></textarea></td>
  </tr>
-->
        <tr>
            <td width="20%" align="right" valign="top">
                <div class="tabletext">${uiLabelMap.ProductCategoryImageUrl}</div>
                <#if (productCategory.categoryImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${(productCategory.categoryImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Category Image" src="<@ofbizContentUrl>${(productCategory.categoryImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"></a>
                </#if>
            </td>
            <td>&nbsp;</td>
            <td width="80%" colspan="4" valign="top">
            <input type="text" class="inputBox" name="categoryImageUrl" value="${(productCategory.categoryImageUrl)?default('')}" size="60" maxlength="255"/>
            <#if productCategory?has_content>
                <div>
                <span class="tabletext">${uiLabelMap.ProductInsertDefaultImageUrl}: </span>
                <a href="javascript:insertImageName('category','${imageNameCategory}.jpg');" class="buttontext">[.jpg]</a>
                <a href="javascript:insertImageName('category','${imageNameCategory}.gif');" class="buttontext">[.gif]</a>
                <a href="javascript:insertImageName('category','');" class="buttontext">[${uiLabelMap.CommonClear}]</a>
                </div>
            </#if>
            </td>
        </tr>
        <tr>
            <td width="20%" align="right" valign="top">
                <div class="tabletext">${uiLabelMap.ProductLinkOneImageUrl}</div>
                <#if (productCategory.linkOneImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${(productCategory.linkOneImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Link One Image" src="<@ofbizContentUrl>${(productCategory.linkOneImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"></a>
                </#if>
            </td>
            <td>&nbsp;</td>
            <td width="80%" colspan="4" valign="top">
            <input type="text" class="inputBox" name="linkOneImageUrl" value="${(productCategory.linkOneImageUrl)?default('')}" size="60" maxlength="255"/>
            <#if productCategory?has_content>
                <div>
                <span class="tabletext">${uiLabelMap.ProductInsertDefaultImageUrl}: </span>
                <a href="javascript:insertImageName('linkOne','${imageNameLinkOne}.jpg');" class="buttontext">[.jpg]</a>
                <a href="javascript:insertImageName('linkOne','${imageNameLinkOne}.gif');" class="buttontext">[.gif]</a>
                <a href="javascript:insertImageName('linkOne','');" class="buttontext">[${uiLabelMap.CommonClear}]</a>
                </div>
            </#if>
            </td>
        </tr>
        <tr>
            <td width="20%" align="right" valign="top">
                <div class="tabletext">${uiLabelMap.ProductLinkTwoImageUrl}</div>
                <#if (productCategory.linkTwoImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${(productCategory.linkTwoImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Link One Image" src="<@ofbizContentUrl>${(productCategory.linkTwoImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"></a>
                </#if>
            </td>
            <td>&nbsp;</td>
            <td width="80%" colspan="4" valign="top">
            <input type="text" class="inputBox" name="linkTwoImageUrl" value="${(productCategory.linkTwoImageUrl)?default('')}" size="60" maxlength="255"/>
            <#if productCategory?has_content>
                <div>
                <span class="tabletext">${uiLabelMap.ProductInsertDefaultImageUrl}: </span>
                <a href="javascript:insertImageName('linkTwo','${imageNameLinkTwo}.jpg');" class="buttontext">[.jpg]</a>
                <a href="javascript:insertImageName('linkTwo','${imageNameLinkTwo}.gif');" class="buttontext">[.gif]</a>
                <a href="javascript:insertImageName('linkTwo','');" class="buttontext">[${uiLabelMap.CommonClear}]</a>
                </div>
            </#if>
            </td>
       </tr>

  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductDetailScreen}</div></td>
    <td>&nbsp;</td>
    <td width="74%">
        <input type="text" <#if productCategory?has_content>value="${productCategory.detailScreen?if_exists}"</#if> name="detailScreen" size="60" maxlength="250" class="inputBox"/>
        <br/><span class="tabletext">${uiLabelMap.ProductDefaultsTo} &quot;categorydetail&quot;, ${uiLabelMap.ProductDetailScreenMessage}: &quot;component://ecommerce/widget/CatalogScreens.xml#categorydetail&quot;</span>
    </td>
  </tr>

  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductPrimaryParentCategory}</div></td>
    <td>&nbsp;</td>
    <td width="74%">
      <input type="text" class="inputBox" name="primaryParentCategoryId" size="20" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/>
      <a href="javascript:call_fieldlookup2(document.productCategoryForm.primaryParentCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
    </td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
    <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/></td>
  </tr>
</table>
</form>
<br/>
  <#if productCategoryId?has_content>
    <hr class="sepbar"/>

    <script language="JavaScript" type="text/javascript">
        function setUploadUrl(newUrl) {
        var toExec = 'document.imageUploadForm.action="' + newUrl + '";';
        eval(toExec);
        };
    </script>
    <div class="head3">${uiLabelMap.CategoryUploadImage}</div>
    <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId?if_exists}&amp;upload_file_type=category</@ofbizUrl>" name="imageUploadForm">
        <input type="file" class="inputBox" size="50" name="fname"/>
        <br/>
        <span class="tabletext">
            <input class="radioButton" type="radio" name="upload_file_type_bogus" value="category" checked="checked" onclick='setUploadUrl("<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId}&amp;upload_file_type=category</@ofbizUrl>");'/>${uiLabelMap.ProductCategoryImageUrl}
            <input class="radioButton" type="radio" name="upload_file_type_bogus" value="linkOne" onclick='setUploadUrl("<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId}&amp;upload_file_type=linkOne</@ofbizUrl>");'/>${uiLabelMap.ProductLinkOneImageUrl}
            <input class="radioButton" type="radio" name="upload_file_type_bogus" value="linkTwo"onclick='setUploadUrl("<@ofbizUrl>UploadCategoryImage?productCategoryId=${productCategoryId}&amp;upload_file_type=linkTwo</@ofbizUrl>");'/>${uiLabelMap.ProductLinkTwoImageUrl}
        </span>
        <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductUploadImage}"/>
    </form>
    <hr/>
    <div class="head2">${uiLabelMap.ProductDuplicateProductCategory}</div>
    <form action="/catalog/control/DuplicateProductCategory" method="post" style="margin: 0;">
        <span class="tabletext">${uiLabelMap.ProductDuplicateProductCategorySelected}:</span>
        <input type=hidden name="oldProductCategoryId" value="${productCategoryId}"/>
        <div>
            <input type="text" class="inputBox" size="20" maxlength="20" name="productCategoryId"/>&nbsp;<input type=submit class="smallSubmit" value="${uiLabelMap.CommonGo}"/>
        </div>
        <div class="tabletext">
            <b>${uiLabelMap.CommonDuplicate}:</b>
            ${uiLabelMap.ProductCategoryContent}&nbsp;<input type="checkbox" class="checkBox" name="duplicateContent" value="Y" checked />
            ${uiLabelMap.ProductCategoryRollupParentCategories}&nbsp;<input type="checkbox" class="checkBox" name="duplicateParentRollup" value="Y" checked />
            ${uiLabelMap.ProductCategoryRollupChildCategories}&nbsp;<input type="checkbox" class="checkBox" name="duplicateChildRollup" value="Y" />
            ${uiLabelMap.ProductProducts}&nbsp;<input type="checkbox" class="checkBox" name="duplicateMembers" value="Y" checked />
            ${uiLabelMap.ProductCatalogs}&nbsp;<input type="checkbox" class="checkBox" name="duplicateCatalogs" value="Y" checked />
            ${uiLabelMap.ProductFeatures}&nbsp;<input type="checkbox" class="checkBox" name="duplicateFeatures" value="Y" checked />
            ${uiLabelMap.PartyParties}&nbsp;<input type="checkbox" class="checkBox" name="duplicateRoles" value="Y" checked />
            ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" class="checkBox" name="duplicateAttributes" value="Y" checked />
        </div>
    </form>
  </#if>
