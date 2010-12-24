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
<#if product?exists>
        <table cellspacing="0" class="basic-table">
            <tr>
                <td width="20%" align="right" valign="top">
                    <div><b>${uiLabelMap.ProductSmallImage}</b></div>
    <#if (product.smallImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${(product.smallImageUrl)?if_exists}</@ofbizContentUrl>" target="_blank"><img alt="Small Image" src="<@ofbizContentUrl>${(product.smallImageUrl)?if_exists}</@ofbizContentUrl>" height="40" width="40"/></a>
    </#if>
                </td>
                <td>&nbsp;</td>
                <td width="80%" colspan="4" valign="top">
                    <input type="text" name="smallImageUrl" value="${(product.smallImageUrl)?default('')}" readOnly="readonly" onclick="javascript:select();" size="60" maxlength="255"/>
                </td>
            </tr>
            <tr>
                <td width="20%" align="right" valign="top">
                    <div><b>${uiLabelMap.ProductMediumImage}</b></div>
    <#if (product.mediumImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${product.mediumImageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Medium Image" src="<@ofbizContentUrl>${product.mediumImageUrl}</@ofbizContentUrl>" height="40" width="40"/></a>
    </#if>
                </td>
                <td>&nbsp;</td>
                <td width="80%" colspan="4" valign="top">
                    <input type="text" name="mediumImageUrl" value="${(product.mediumImageUrl)?default('')}" readOnly="readonly" onclick="javascript:select();" size="60" maxlength="255"/>
                </td>
            </tr>
            <tr>
                <td width="20%" align="right" valign="top">
                    <div><b>${uiLabelMap.ProductLargeImage}</b></div>
    <#if (product.largeImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${product.largeImageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Large Image" src="<@ofbizContentUrl>${product.largeImageUrl}</@ofbizContentUrl>" height="40" width="40"/></a>
    </#if>
                </td>
                <td>&nbsp;</td>
                <td width="80%" colspan="4" valign="top">
                    <input type="text" name="largeImageUrl" value="${(product.largeImageUrl)?default('')}" readOnly="readonly" onclick="javascript:select();" size="60" maxlength="255"/>
                </td>
            </tr>
            <tr>
                <td width="20%" align="right" valign="top">
                    <div><b>${uiLabelMap.ProductDetailImage}</b></div>
    <#if (product.detailImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${product.detailImageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Detail Image" src="<@ofbizContentUrl>${product.detailImageUrl}</@ofbizContentUrl>" height="40" width="40"/></a>
    </#if>
                </td>
                <td>&nbsp;</td>
                <td width="80%" colspan="4" valign="top">
                    <input type="text" name="detailImageUrl" value="${(product.detailImageUrl)?default('')}" readOnly="readonly" onclick="javascript:select();" size="60" maxlength="255"/>
                </td>
            </tr>
            <tr>
                <td width="20%" align="right" valign="top">
                    <div><b>${uiLabelMap.ProductOriginalImage}</b></div>
    <#if (product.originalImageUrl)?exists>
                    <a href="<@ofbizContentUrl>${product.originalImageUrl}</@ofbizContentUrl>" target="_blank"><img alt="Original Image" src="<@ofbizContentUrl>${product.originalImageUrl}</@ofbizContentUrl>" height="40" width="40"/></a>
    </#if>
                </td>
                <td>&nbsp;</td>
                <td width="80%" colspan="4" valign="top">
                    <input type="text" name="originalImageUrl" value="${(product.originalImageUrl)?default('')}" readOnly="readonly" onclick="javascript:select();" size="60" maxlength="255"/>
                </td>
            </tr>
        </table>
    <script language="JavaScript" type="text/javascript">
        function setUploadUrl(newUrl) {
            var toExec = 'document.imageUploadForm.action="' + newUrl + '";';
            eval(toExec);
        };
    </script>
    <form method="post" action="<@ofbizUrl>UploadProductImageForManageMent?productId=${productId}&amp;upload_file_type=original</@ofbizUrl>" style="margin: 0;" name="imageUploadForm">
        <table cellspacing="0" class="basic-table">
            <tr>
                <td width="6%" >
                    <div><b>Choose Image</b></div>
                </td>
                <td width="15%" valign="top">
                    <span class="field-lookup">
                        <@htmlTemplate.lookupField name="contentId" id="contentId" formName="imageUploadForm" fieldFormName="LookupImage?productId=${productId}"/>
                    </span>
                </td>
                <td>&nbsp;</td>
                <td width="80%" colspan="4" valign="top">
                    <input type="radio" name="upload_file_type_bogus" value="small" onclick='setUploadUrl("<@ofbizUrl>UploadProductImageForManageMent?productId=${productId}&amp;upload_file_type=small</@ofbizUrl>");'/>${uiLabelMap.CommonSmall}
                    <input type="radio" name="upload_file_type_bogus" value="medium" onclick='setUploadUrl("<@ofbizUrl>UploadProductImageForManageMent?productId=${productId}&amp;upload_file_type=medium</@ofbizUrl>");'/>${uiLabelMap.CommonMedium}
                    <input type="radio" name="upload_file_type_bogus" value="large"onclick='setUploadUrl("<@ofbizUrl>UploadProductImageForManageMent?productId=${productId}&amp;upload_file_type=large</@ofbizUrl>");'/>${uiLabelMap.CommonLarge}
                    <input type="radio" name="upload_file_type_bogus" value="detail" onclick='setUploadUrl("<@ofbizUrl>UploadProductImageForManageMent?productId=${productId}&amp;upload_file_type=detail</@ofbizUrl>");'/>${uiLabelMap.CommonDetail}
                    <input type="radio" name="upload_file_type_bogus" value="original" checked="checked" onclick='setUploadUrl("<@ofbizUrl>UploadProductImageForManageMent?productId=${productId}&amp;upload_file_type=original</@ofbizUrl>");'/>${uiLabelMap.ProductOriginal}
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductUploadImage}"/>
                </td>
            </tr>
        </table>
    </form>
</#if>
