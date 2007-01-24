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
<#if productId?exists && product?exists>
    <div class="tabletext">${uiLabelMap.ProductNoteKeywordsAutomaticallyCreated}</div>
    
    <TABLE border="0" cellspacing="0" cellpadding="0" class="boxoutside">
    <TR>
        <TD width="100%">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
            <tr>
            <TD align="left">
                <DIV class="boxhead">${uiLabelMap.ProductAddProductKeyword}:</DIV>
            </TD>
            <TD align="right">
                <a href="<@ofbizUrl>EditProduct?productId=${productId?if_exists}</@ofbizUrl>" class="submenutextright">${uiLabelMap.ProductEditProduct}</a>
            </td>
            </tr>
        </table>
        </TD>
    </TR>
    <TR>
        <TD width="100%">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
            <tr>
            <td>
                <form method="post" action="<@ofbizUrl>UpdateProductKeyword</@ofbizUrl>" style="margin: 0;">
                    <input type="hidden" name="UPDATE_MODE" value="CREATE">
                    <input type="hidden" name="PRODUCT_ID" value="${productId?if_exists}">
                    <input type="hidden" name="productId" value="${productId?if_exists}">
                    <span class="tabletext">${uiLabelMap.ProductKeyword}: </span><input type="text" size="20" name="KEYWORD" value="" class="inputBox">
                    <span class="tabletext">${uiLabelMap.ProductWeight}: </span><input type="text" size="4" name="relevancyWeight" value="1" class="inputBox">
                    <input type="submit" value="${uiLabelMap.CommonAdd}" style="font-size: x-small;">
                </form>
            </td>
            </tr>
        </table>
        </TD>
    </TR>
    </TABLE>
    <BR>
    
    <TABLE border="0" cellspacing="0" cellpadding="0" class="boxoutside">
    <TR>
        <TD width="100%">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
            <TD align="left">
                <DIV class="boxhead">${uiLabelMap.ProductKeywords}</DIV>
            </TD>
            <TD align="right">
                <a href="<@ofbizUrl>UpdateProductKeywords?UPDATE_MODE=CREATE&PRODUCT_ID=${productId}&productId=${productId}</@ofbizUrl>" class="submenutext">${uiLabelMap.ProductReInduceKeywords}</a><a href="<@ofbizUrl>UpdateProductKeywords?UPDATE_MODE=DELETE&PRODUCT_ID=${productId}&productId=${productId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.ProductDeleteAllKeywords}</a>
            </td>
        </tr>
        </table>
        </TD>
    </TR>
    <TR>
        <TD width="100%">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
            <td valign="top">
            <TABLE width="100%" cellpadding="0" cellspacing="0" border="0">
            <#assign productKeywords = product.getRelated("ProductKeyword")>
            <#if (productKeywords.size() > 0)>
                <#list productKeywords as productKeyword>
                <#assign colSize = productKeywords.size()/3 + 1>
                <#assign kIdx = 0>
                <tr>                        
                    <td align="right">${(productKeyword.relevancyWeight)?if_exists}&nbsp;</td>
                    <td align="left">&nbsp;${(productKeyword.keyword)?if_exists}</td>
                    <td>&nbsp;&nbsp;</td>
                    <td align="left">
                        <form method="post" action="<@ofbizUrl>UpdateProductKeyword</@ofbizUrl>">
                            <input type="hidden" name="productId" value="${productId}">
                            <input type="hidden" name="UPDATE_MODE" value="DELETE">
                            <input type="hidden" name="PRODUCT_ID" value="${productId}">
                            <input type="hidden" name="KEYWORD" value="${(productKeyword.keyword)?if_exists}">
                            <input type="submit" value="${uiLabelMap.CommonDelete}" class="smallSubmit">
                        </form>
                    </td>
                </tr>
                <#assign kIdx = kIdx + 1>
                <#if (kIdx >= colSize)>
                    <#assign colSize = colSize + colSize>
                    </TABLE>
                    </TD>
                    <TD bgcolor="#FFFFFF" valign="top" style="border-left: solid #CCCCCC 1px;">
                    <TABLE width="100%" cellpadding="0" cellspacing="0" border="0">      
                </#if>
                </#list>
            <#else>
                <tr>
                <td colspan="3"><div class="tabletext">${uiLabelMap.ProductNoKeywordsFound}</div></td>
                </tr>
            </#if>
            </TABLE>
            </td>
        </tr>
        </table>
    </TD>
</TR>
</TABLE>        
<#else>
    <div class="head2">${uiLabelMap.ProductProductNotFoundWithProduct} "${productId?if_exists}"</div>
</#if>
