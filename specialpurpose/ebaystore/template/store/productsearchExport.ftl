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
<style type="text/css">
    #tabs{
        margin-left: 4px;
        padding: 0;
        background: transparent;
        voice-family: "\"}\"";
        voice-family: inherit;
        padding-left: 5px;
    }
    #tabs ul{
        font: bold 11px Arial, Verdana, sans-serif;
        margin:0;
        padding:0;
        list-style:none;
    }
    #tabs li{
        display:inline;
        margin:0 2px 0 0;
        padding:0;
        text-transform:uppercase;
    }
    #tabs a{
        float:left;
        background:#A3BBE6;// url(images/tabs_left.gif) no-repeat left top;
        margin:0 2px 0 0;
        padding:0 0 1px 3px;
        text-decoration:none;
    }
    #tabs a span{
        float:left;
        display:block;
        background: transparent;// url(images/tabs_right.gif) no-repeat right top;
        padding:4px 9px 2px 6px;
    }
    #tabs a span{float:none;}
    #tabs a:hover{background-color: #7E94B9;color: white;}
    #tabs a:hover span{background-color: #7E94B9;}
    #tabHeaderActive_ span, #tabHeaderActive_ a { background-color: #42577B; color:#fff;}
    .tabContent {
        clear:both;
        border:2px solid #42577B;
        padding-top:2px;
        background-color:#FFF;
    }
</style>
<script language="JavaScript" type="text/javascript">

     function retrieveEbayCategoryByParent(url,cateId,productStoreId,id){
        if (cateId.match('true')){
            return true;
        }
        if (cateId.indexOf(':')!= -1) {
            cateId = cateId.substring(0,cateId.indexOf(':'));
        }
        var pars = 'ebayCategoryId='+cateId+'&productStoreId='+productStoreId;
        jQuery.ajax({
            url: url,
            type: "GET",
            data: pars,
            beforeStart: function() {
                document.getElementById('loading').innerHTML = '<b>${uiLabelMap.CommonPleaseWait}</b>';
            },
            success: function(data) {
                document.getElementById('loading').innerHTML = '';
                if (data && data.categories) {
                    removeOptions(id);
                    var resp = eval(data.categories);
                    var leng = resp.length;
                     if (leng) {
                        for (i=0;i<leng;i++) {
                            if (i == 0) {
                                document.getElementById(id).options[0] = new Option("${uiLabelMap.CommonPleaseSelect}","");
                            }
                            var optVal = resp[i].CategoryCode;
                            var optName = resp[i].CategoryName;
                            var isLeaf = resp[i].IsLeafCategory;
                            document.getElementById(id).options[i+1] = new Option(optName,optVal+":"+isLeaf);
                        }
                    }
                }
            }
        });
    }

     function retrieveTemplateByTemGroupId(templateGroupId,productStoreId,pkCategoryId){
        var pars = 'templateGroupId='+templateGroupId+'&productStoreId='+productStoreId+'&pkCategoryId='+pkCategoryId;
        var url = '<@ofbizUrl>ebayAdItemTemplate</@ofbizUrl>';

        jQuery.ajax({
            url: url,
            type: "GET",
            data: pars,
            success: function(data){
                removeOptions('theme');
                previewPic(":http://pics.ebay.com/aw/pics/vit/None2_sample_100x120.gif");
                if(data!=null && data.itemTemplates!=null){
                    var resp = eval(data.itemTemplates);
                    var leng = resp.length;
                    var j = 0;
                    for (i=0;i<leng;i++) {
                        if (i == 0) {
                            document.getElementById('theme').options[0] = new Option("${uiLabelMap.CommonPleaseSelect}","_NA_");
                        } else {
                            var optVal = resp[i].TemplateId+":"+resp[i].TemplateImageURL;
                            document.getElementById('theme').options[i] = new Option(resp[i].TemplateName,optVal);
                            j++;
                        }
                    }
                }
            }
        });
     }

     function removeOptions(id){
       var elSel = jQuery("#" + id);
       elSel.find('option').remove();
     }
     function enabledItemTemplate(val){
        var field = "enabledTheme";
        if (document.getElementById(field).checked) {
            document.getElementById('themeGroup').disabled = false;
            document.getElementById('theme').disabled = false;
        } else {
            document.getElementById('themeGroup').disabled = true;
            document.getElementById('theme').disabled = true;
        }
     }
     function previewPic(val) {
        if (val != null) val = val.substr(val.indexOf(":")+1);
        document.getElementById('themeImg').src = val;
     }
    function toggleDisp() {
        for (var i=0;i<arguments.length;i++){
            var d = document.getElementById(arguments[i]);
            if (d.style.display == 'none')
                d.style.display = 'block';
            else
                d.style.display = 'none';
        }
    }
    function toggleTab(num,numelems,opennum,animate) {
        if (document.getElementById('tabContent'+num).style.display == 'none'){
            for (var i=1;i<=numelems;i++){
                if ((opennum == null) || (opennum != i)){
                    var temph = 'tabHeader_'+i;
                    var h = document.getElementById(temph);
                    if (!h){
                        var h = document.getElementById('tabHeaderActive_');
                        h.id = temph;
                    }
                    var tempc = 'tabContent'+i;
                    var c = document.getElementById(tempc);
                    if(c.style.display != 'none'){
                        if (animate || typeof animate == 'undefined')
                            jQuery("#" + tempc).animate({opacity: 'toggle', height: 'toggle'}, "slow");
                        else
                            toggleDisp(tempc);
                    }
                }
            }
            var h = document.getElementById('tabHeader_'+num);
            if (h){
                h.id = 'tabHeaderActive_';
            }
            h.blur();
            var c = document.getElementById('tabContent'+num);
            c.style.marginTop = '2px';
            if (animate || typeof animate == 'undefined'){
                jQuery("#tabContent" + num).animate({opacity: 'toggle', height: 'toggle'}, "slow");
            }else{
                toggleDisp('tabContent'+num);
            }
        }
    }
</script>
<div class="screenlet">
  <div class="screenlet-title-bar">
  <ul>
    <li class="h3">Items to export</li>
    <li><a href="<@ofbizUrl>clearExpListing</@ofbizUrl>?productStoreId=${productStoreId!}">Clear Listing</a></li>
    <#if isExportValid?? && isExportValid == "true">
    <li><a href="<@ofbizUrl>exportListingToEbay</@ofbizUrl>?productStoreId=${productStoreId!}">Export Products Listing</a></li>
    </#if>
  </ul><br class="clear"/></div>
  <div class="screenlet-body">
<form id="ProductsExportToEbay" method="post" action="<@ofbizUrl>exportProductsFromEbayStore</@ofbizUrl>" name="ProductsExportToEbay">
    <input type="hidden" name="productStoreId" value="${productStoreId!}"/>
    <table class="basic-table"  cellspacing="0">
        <tr><td>
        <#if addItemObj?has_content>
                <div class="button-bar button-style-2">
                    <br class="clear"/>
                    <ul>
                       <#assign id = 1>
                       <#if contentList?has_content>
                           <#list contentList as content>
                                 <#if !isProductId?has_content>
                                    <li <#if id == 1>class="selected" <#assign isProductId = content.product.productId!><#else>id="tabHeader${id}"</#if>><a href="javascript:document.getElementById('ProductsExportToEbay').action = '<@ofbizUrl>exportProductListing</@ofbizUrl>?isProductId=${content.product.productId!}';document.getElementById('ProductsExportToEbay').submit();">${content.product.productName!}[${content.product.productId}]</a></li>
                                 <#else>
                                    <li <#if isProductId?? && isProductId! == content.product.productId! >class="selected" <#assign isProductId = content.product.productId!><#else>id="tabHeader${id}"</#if>><a href="javascript:document.getElementById('ProductsExportToEbay').action = '<@ofbizUrl>exportProductListing</@ofbizUrl>?isProductId=${content.product.productId!}';document.getElementById('ProductsExportToEbay').submit();">${content.product.productName!}[${content.product.productId}]</a></li>
                                 </#if>
                                 <#assign id = id+1>
                           </#list>
                       </#if>
                    </ul>
                     <br class="clear"/>
                </div>
        <#assign addItemList = addItemObj.itemListing!>
        <#if addItemList?has_content>
            <#list addItemList as addItemObj>
                 <#assign addItem = addItemObj.addItemCall!>
                 <#assign isSaved = addItemObj.isSaved!>
                 <#assign isAutoRelist = addItemObj.isAutoRelist!>
                 <#assign requireEbayInventory = addItemObj.requireEbayInventory!>
                 <#assign item = addItem.getItem()!>
                 <#assign primaryCate = item.getPrimaryCategory()!>
                 <#assign storeFront = item.getStorefront()!>
                 <#if isProductId == item.getSKU()!>
                     <input type="hidden" name="productId" value="${item.getSKU()!}"/>
                     <#assign smallImageUrl = "">
                     <#if contentList?has_content>
                          <#list contentList as content>
                                <#if content.product.productId! == item.getSKU()!><#assign smallImageUrl = content.productContentWrapper.get("SMALL_IMAGE_URL", "url")!></#if>
                          </#list>
                     </#if>
                     <#if !smallImageUrl?string?has_content><#assign smallImageUrl = "/images/defaultImage.jpg"></#if>
                          <table cellspacing="0" width="70%">
                            <tr>
                                <td class="label">ItemID</td>
                                <td><input type="text" readonly="readonly" name="item" value="${item.getItemID()!}"/></td>
                                <td class="label">Item Fee</td>
                                <td>
                                    <div>
                                        <input type="text" readonly="readonly" name="itemFee" value="${request.getAttribute("itemFee")!}"/>
                                        <!-- itemlisting buttons bar -->
                                        <a href="#" onclick="javascript:document.ProductsExportToEbay.action='<@ofbizUrl>updateProductExportDetail</@ofbizUrl>';document.ProductsExportToEbay.submit();" class="buttontext">${uiLabelMap.CommonSave}</a>
                                    <#-- request.setAttribute("isSaved")-->
                                    <#if isSaved?? && isSaved=="Y">
                                        <a href="#" class="buttontext" onclick="javascript:document.ProductsExportToEbay.action='<@ofbizUrl>verifyItemBeforeAddAndExportToEbay</@ofbizUrl>';document.ProductsExportToEbay.submit();">Verifly Item</a>
                                    </#if>
                                        <a href="#" class="buttontext" onclick="javascript:document.ProductsExportToEbay.action='<@ofbizUrl>removeProductFromListing</@ofbizUrl>';document.ProductsExportToEbay.submit();">Remove</a>
                                        <#--a href="#" class="buttontext">Save and ${uiLabelMap.EbayExportToEbay}</a-->
                                        <!-- end buttons bar --> 
                                    </div>
                                </td>
                            </tr>
                          </table>
                          <div class="screenlet">
                              <div class="screenlet-title-bar"><ul><li class="h3">Product ${item.getSKU()!}</li></ul><br class="clear"/></div>
                              <div class="screenlet-body">
                                 <!-- ebay setting section -->
                                 <table width="100%" cellspacing="0">
                                     <tr>
                                     <td width="60%"  valign="top">
                                        <table cellspacing="0">
                                        <tr>
                                            <td class="label">SiteId</td>
                                            <td>
                                                <#assign site = item.getSite().value()!>
                                                <input type="text" readonly="readonly" name="site" value="${item.getSite().name()!} [${item.getSite()!}]"/>
                                            </td>
                                        </tr>
                                        <!-- set ebay category -->
                                        <tr>
                                            <td class="label">${uiLabelMap.FormFieldTitle_ebayCategory}</td>
                                            <td>
                                              <div>
                                                  <div id="loading"></div>
                                                  <select id="ebayCategory" name="ebayCategory"  onchange="retrieveEbayCategoryByParent('<@ofbizUrl>retrieveEbayCategoryByParent</@ofbizUrl>',this.value,'${productStoreId}','ebayCategory')">
                                                        <option value="">Please select</option>
                                                        <#if categories??>
                                                            <#if primaryCate?has_content>
                                                                <#if !primaryCate.isLeafCategory()?has_content> 
                                                                    <#assign  leafCate  = "false">
                                                                <#else>
                                                                    <#assign  leafCate  = "true">
                                                                </#if>
                                                                <#assign  primaryCateId  = primaryCate.getCategoryID()!>
                                                                 <option selected="selected" value="${primaryCate.getCategoryID()!}:${leafCate!}" >${primaryCate.getCategoryName()!}</option>
                                                            <#else>
                                                                <#list categories as csCate>
                                                                    <#if !csCate.isLeafCategory()?has_content> 
                                                                        <#assign  leafCate  = "false">
                                                                    <#else>
                                                                        CH_${primaryCate.getCategoryID()!}<#assign  leafCate  = "true">
                                                                    </#if>
                                                                    <#assign  primaryCateId  = csCate.getCategoryID()!>
                                                                    <option value="${csCate.getCategoryID()!}:${leafCate!}" >${csCate.getCategoryName()!}</option>
                                                                </#list>
                                                            </#if>
                                                        </#if>
                                                  </select>
                                                  <a class="buttontext" href="javascript:retrieveEbayCategoryByParent('<@ofbizUrl>retrieveEbayCategoryByParent</@ofbizUrl>','CH_<#if primaryCate?has_content>${primaryCate.getCategoryID()!}</#if>','${productStoreId}','ebayCategory')">${uiLabelMap.EbayChangeCategory}</a> <a class="buttontext" href="javascript:document.getElementById('ProductsExportToEbay').action = '<@ofbizUrl>setSelectedCategory</@ofbizUrl>?isProductId=${isProductId!}';document.getElementById('ProductsExportToEbay').submit();">${uiLabelMap.EbaySet}</a>
                                              </div>
                                              <input type="hidden" name="primaryCateId" value="${primaryCateId!}"/>
                                              <div id="ebayCategory_Name">${priCateName!}</div>
                                            </td>
                                        </tr>
                                        <!-- end of set category -->
                                        <tr>
                                            <td class="label">Store category 1</td>
                                            <td>
                                              <div>
                                                  <div id="loading"></div>
                                                  <select id="ebayStore1Category" name="ebayStore1Category" onchange="retrieveEbayCategoryByParent('<@ofbizUrl>retrieveEbayStoreCategoryByParent</@ofbizUrl>',this.value,'${productStoreId}','ebayStore1Category')">
                                                        <option value="">Please select</option>
                                                        <option value="">------</option>
                                                        <#if storeCategories??>
                                                            <#if storeFront?has_content>
                                                                <#--if !storeFront.isLeafCategory()?has_content> 
                                                                    <#assign  leafCate  = "false">
                                                                <#else>
                                                                    <#assign  leafCate  = "true">
                                                                </#if-->
                                                                <#assign storeCate1Id  = storeFront.getStoreCategoryID()!>
                                                                 <option selected="selected" value="${storeFront.getStoreCategoryID()!}" >${storeFront.getStoreCategoryID()!}</option>
                                                            <#else>
                                                                <#list storeCategories as csCate>
                                                                    <#--if !csCate.IsLeafCategory?has_content> 
                                                                        <#assign  leafCate  = "false">
                                                                    <#else>
                                                                        CH_${storeFront.getStoreCategoryID()!}<#assign  leafCate  = "true">
                                                                    </#if-->
                                                                    <#assign categoryId = csCate.getCategoryID()!>
                                                                    <option value="${csCate.getCategoryID()!}" >${csCate.getName()!}</option>
                                                                </#list>
                                                            </#if>
                                                        </#if>
                                                  </select>
                                              </div>
                                              <input type="hidden" name="storeCate1Id" value="${storeCate1Id!}"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="label">Store category 2</td>
                                            <td>
                                              <div>
                                                  <div id="loading"></div>
                                                  <select id="ebayStore2Category" name="ebayStore2Category" onchange="retrieveEbayCategoryByParent('<@ofbizUrl>retrieveEbayStoreCategoryByParent</@ofbizUrl>',this.value,'${productStoreId}','ebayStore2Category')">
                                                        <option value="">Please select</option>
                                                        <#if storeCategories??>
                                                            <#if storeFront?has_content>
                                                                <#--if !storeFront.isLeafCategory()?has_content> 
                                                                    <#assign  leafCate  = "false">
                                                                <#else>
                                                                    <#assign  leafCate  = "true">
                                                                </#if-->
                                                                <#assign storeCate2Id  = storeFront.getStoreCategory2ID()!>
                                                                 <option selected="selected" value="${storeFront.getStoreCategory2ID()!}" >${storeFront.getStoreCategory2ID()!}</option>
                                                            <#else>
                                                                <#list storeCategories as csCate>
                                                                    <#--if !csCate.IsLeafCategory?has_content> 
                                                                        <#assign  leafCate  = "false">
                                                                    <#else>
                                                                        CH_${storeFront.getStoreCategoryID()!}<#assign  leafCate  = "true">
                                                                    </#if-->
                                                                    <#assign categoryId = csCate.getCategoryID()!>
                                                                    <option value="${csCate.getCategoryID()!}" >${csCate.getName()!}</option>
                                                                </#list>
                                                            </#if>
                                                        </#if>
                                                  </select>
                                              </div>
                                              <input type="hidden" name="storeCate2Id" value="${storeCate2Id!}"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="label">Title</td>
                                            <td><input type="text" size="60"  name="title" value="${item.getTitle()!}"/></td>
                                        </tr>
                                        <tr>
                                            <td class="label">SKU</td>
                                            <td><input type="text" readonly="readonly" name="sku" value="${item.getSKU()!}"/></td>
                                        </tr>
                                        <tr>
                                            <td class="label">PictureURL</td>
                                            <td>
                                                <#assign pic = item.getPictureDetails()!>
                                                <#assign picUrls = pic.getPictureURL()!>
                                                <#assign picUrl = picUrls[0]!>
                                                <input type="text" size="60" name="pictureUrl" value="${picUrl!}"/>
                                            </td>
                                        </tr>
                                        <#--tr>
                                            <td class="label">Description</td>
                                            <input type="text" rows="3" cols="50" rows="4" name="description" size="50" value=""/>
                                            <td><textarea  name="description" style="height:88px;width:350px;"><#if item.getDescription()??>Description of item<#else>${item.getDescription()!}</#if></textarea></td>
                                        </tr-->
                                        <tr>
                                            <td class="label">${uiLabelMap.CommonCountry}</td>
                                             <#if item.getCountry().value()??>
                                                <#assign country = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(delegator.findByAnd("Geo", {"geoCode": item.getCountry().value()}, null, false))/>
                                                <#if country?has_content>
                                                    <#assign countryname = country.geoName/>
                                                </#if>
                                            </#if>
                                            <td><input type="text" readonly="readonly" name="country" size="20" value="${countryname!?default(item.getCountry().value()!)}"/></td>
                                        </tr>
                                        <tr>
                                            <td class="label">${uiLabelMap.FormFieldTitle_location}</td>
                                            <td><input type="text" name="location" size="50" maxlength="50" value="${item.getLocation()!}" /></td>
                                        </tr>
                                        <tr>
                                            <td class="label"><b>Enable auto-relist item</b></td>
                                            <td><input type="checkbox" name="isAutoRelist" value="Y" <#if isAutoRelist == "Y">checked="checked"</#if>/></td>
                                        </tr>
                                        <#if isReserve?? && isReserve == true>
                                        <tr>
                                            <td class="label"><b>Require eBay Inventory</b></td>
                                            <td><input type="checkbox" name="requireEbayInventory" value="Y" <#if requireEbayInventory == "Y">checked="checked"</#if>/></td>
                                        </tr>
                                        </#if>
                                        <tr>
                                            <td class="label"></td>
                                            <td><br /></td>
                                        </tr>
                                     </table>
                                    </td>
                                    <td width="40%"  valign="top">
                                    <table width="100%" height="100%" id="table2" cellspacing="0">
                                        <tr>
                                                    <td>
                                                        <img src="<@ofbizContentUrl>${contentPathPrefix!}${smallImageUrl}</@ofbizContentUrl>" alt="Small Image"/><br />
                                                        ${uiLabelMap.ProductProductId}   : ${item.getSKU()!}<br />
                                                        ${uiLabelMap.ProductProductName} : ${item.getTitle()!}<br />
                                                        ${uiLabelMap.CommonDescription}  : ${item.getDescription()!}
                                                    </td>
                                                </tr>
                                            </table>
                                    </td>
                                    </tr>
                                 </table>
                              </div>
                          </div>
                          <!-- item specifices section -->
                          <#if primaryCate?has_content && primaryCate.getCategoryID()?? && listingTypes?has_content>
                             <#if checkSpecific == "true">
                             <div class="screenlet">
                                 <div class="screenlet-title-bar"><ul><li class="h3">Item specifices</li></ul><br class="clear"/></div>
                                     <div class="screenlet-body">
                                        <table width="50%" height="100%" id="table2"  cellspacing="0">
                                        <#list categorySpecifix?keys as key>
                                            <#assign values = categorySpecifix.get(key)!/>
                                            <#assign i = 0/>
                                            <#list values?keys as nameSpecific>
                                            <#assign itemSpecifics = values.get(nameSpecific)!/>
                                                <#if itemSpecifics?has_content>
                                                    <tr>
                                                        <td class="label">${nameSpecific!}</td>
                                                        <input type="hidden" name="nameValueListType_o_${i}" value="${nameSpecific!}"/>
                                                        <td>
                                                            <select id="categorySpecifics" name="categorySpecifics_o_${i}">
                                                               <option  value="" ></option>
                                                               <#list itemSpecifics as itemSpecific>
                                                                   <option  value="${itemSpecific!}" >${itemSpecific!}</option>
                                                               </#list>
                                                            </select>
                                                        </td>
                                                    </tr>
                                                    <#assign i = i + 1/>
                                                </#if>
                                            </#list>
                                        </#list>
                                        </table>
                                     </div>
                                 </div>
                             </div>
                             </#if>
                          </#if>
                          <!-- Setup ad templates section -->
                          <#if primaryCate?has_content && primaryCate.getCategoryID()?? && listingTypes?has_content>
                             <div class="screenlet">
                                 <div class="screenlet-title-bar"><ul><li class="h3">Details</li></ul><br class="clear"/></div>
                                 <div class="screenlet-body">
                                    <table width="100%" height="100%" cellspacing="0">
                                        <tr>
                                            <td width="60%" valign="top">
                                                 <table cellspacing="0">
                                                    <tr><td>
                                                        <script language="javascript" src="<@ofbizContentUrl>/images/jquery/plugins/elrte-1.3/js/elrte.min.js</@ofbizContentUrl>" type="text/javascript"></script>
                                                        <#if language?has_content && language != "en">
                                                        <script language="javascript" src="<@ofbizContentUrl>/images/jquery/plugins/elrte-1.3/js/i18n/elrte.${language!"en"}.js</@ofbizContentUrl>" type="text/javascript"></script><#rt/>
                                                        </#if>
                                                        <link href="<@ofbizContentUrl>/images/jquery/plugins/elrte-1.3/css/elrte.min.css</@ofbizContentUrl>" rel="stylesheet" type="text/css">
                                                        <script language="javascript" type="text/javascript">
                                                                var opts = {
                                                                    cssClass : 'el-rte',
                                                                    lang     : '${language}',
                                                                    toolbar  : 'maxi',
                                                                    doctype  : '<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">', //'<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN">',
                                                                    cssfiles : ['/images/jquery/plugins/elrte-1.3/css/elrte-inner.css']
                                                                }
                                                        </script>
                                                        <textarea id="description" name="description" style="width:800px; height:300px">
                                                            <#if item.getDescription()??>description<#else>${item.getDescription()!}</#if>
                                                        </textarea>
                                                        <script type="text/javascript">
                                                              jQuery('#description').elrte(opts);
                                                        </script>
                                                    </td></tr>
                                                 </table>
                                            </td>
                                            <td width="30%" valign="top">
                                                <table align="left" width="60%"  height="100%" cellspacing="0">
                                                    <tr>
                                                        <td></td>
                                                        <td><input type="checkbox" value="Y" onclick="javascript:enabledItemTemplate(this.value);" id="enabledTheme" name="enabledTheme" /><b>Add a theme</b></checkbox></td>
                                                    </tr>
                                                    <tr>
                                                        <td class="label">Select Theme</td>
                                                        <td>
                                                             <#if adItemTemplates?has_content>
                                                                <select id="themeGroup" disabled onchange="javascript:retrieveTemplateByTemGroupId(this.value,'${productStoreId!}','${primaryCate.getCategoryID()!}');" name="themeGroup">
                                                                <#list adItemTemplates as adItemTemplate>
                                                                    <option value="${adItemTemplate.TemplateGroupId!}">${adItemTemplate.TemplateGroupName!}</option>
                                                                </#list>
                                                                </select>
                                                            </#if>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td class="label">Select Design</td>
                                                        <td>
                                                             <#if adItemTemplates?has_content>
                                                                <select id="theme" disabled onchange="javascript:previewPic(this.value);" name="theme">
                                                                    <option value="">-</option>
                                                                </select>
                                                            </#if>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td></td>
                                                        <td valign="top">
                                                            <script type="text/javascript">
                                                              function popUpImg(){
                                                                //popUp(document.getElementById('themeImg').src, 'themeImgBig', '400', '550');
                                                              }
                                                            </script>
                                                            <a id="themeImgUrl" href="javascript:popUpImg();"><img hspace="5" height="120" border="0" align="top" width="100" id="themeImg" name="themeImg" src="http://pics.ebay.com/aw/pics/vit/None2_sample_100x120.gif" alt="" /></a>
                                                            <br /><div style="height:120px"></div>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                   </table>
                                 </div>
                             </div>
                          </#if>
                          <!-- product Price Type -->
                          <#if primaryCate?has_content && primaryCate.getCategoryID()?? && listingTypes?has_content>
                             <div class="screenlet">
                                 <div class="screenlet-title-bar"><ul><li class="h3">Listing Type</li></ul><br class="clear"/></div>
                                 <div class="screenlet-body">
                                    <br class="clear"/>
                                       <!--  set  listing type, duration, prices --> 
                                    <div id="tabs">
                                        <ul>
                                           <#assign id = 1>
                                           <#assign tabName = "">
                                           <#list listingTypes as listingType>
                                               <#-- default with aution and fixed price -->
                                               <#if listingType.type.equals("Chinese") || listingType.type == "FixedPriceItem">
                                                    <#if listingType.type.equals("Chinese") > <#assign tabName = "Auction"></#if>
                                                    <#if listingType.type.equals("FixedPriceItem") > <#assign tabName = "Fixed Price"></#if>
                                                    <li  <#if id==1 > style="margin-left: 1px" id="tabHeaderActive_"<#else> id="tabHeader_${id}" </#if>><a href="javascript:void(0)" onclick="toggleTab(${id},2)"><span>${tabName!}</span></a></li>
                                                    <#assign id = id + 1>
                                               </#if>
                                            </#list>
                                        </ul>
                                    </div>
                                    <div id="tabscontent">
                                       <#assign id = 1>
                                       <#list listingTypes as listingType>
                                        <#if listingType.type.equals("Chinese") || listingType.type! == "FixedPriceItem">
                                        <#if listingType.type.equals("Chinese") > <#assign tabName = "Auction"></#if>
                                        <#if listingType.type.equals("FixedPriceItem") ><#assign tabName = "Fixed Price"></#if>
                                       <div id="tabContent${id}" class="tabContent" <#if id != 1>style="display:none;"</#if>>
                                            <br />
                                            <table width="50%" height="100%" id="table2" cellspacing="0">
                                                    <tr>
                                                         <td class="label"></td>
                                                        <td>
                                                            <#if listingType.type.equals("Chinese")>
                                                                <input type="radio" name="listype" value="auction"/><b>${tabName!}</b>
                                                                <#--<input type="checkbox" value="Y" name="enabledAuction_${id}" /><b>${tabName!}</b></checkbox-->
                                                            <#elseif listingType.type == "FixedPriceItem">
                                                                <input type="radio" name="listype" value="fixedprice"/><b>${tabName!}</b>
                                                                <#--input type="checkbox" value="Y" name="enabledFixedPrice_${id}" /><b>${tabName!}</b></checkbox-->
                                                            </#if>
                                                        </td>
                                                        <td class="label">Duration</td>
                                                        <td>
                                                            <#assign durations = listingType.durations!>
                                                            <#if durations?has_content>
                                                            <select name="duration_${id}">
                                                                    <#list durations as duration>
                                                                        <#if duration.indexOf("_")!= -1>
                                                                            <#assign dura = duration.substring(duration.indexOf("_")+1)>
                                                                        <#elseif duration == "GTC">
                                                                            <#assign dura = "Good 'Til Cancelled">
                                                                        </#if>
                                                                        <option value="${duration!}">${dura!} ${uiLabelMap.CommonDays}</option>
                                                                    </#list>
                                                            </select>
                                                            </#if>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td class="label">${uiLabelMap.CommonQuantity}</td>
                                                        <td>
                                                            <#if listingType.type.equals("FixedPriceItem") >
                                                                <input type="text" size="3" value="1" name="quantity_${id}" size="12" maxlength="3"/>
                                                            <#else>
                                                                <input type="text" size="3" value="1"  disabled  name="quantity_${id}" size="12" maxlength="3"/>
                                                            </#if>
                                                        </td>
                                                        <td class="label">Lot Size</td>
                                                        <td><input type="text" size="10" name="lotsize_${id}" /></td>
                                                    </tr>
                                                    <#if productPrices?has_content>
                                                        <#list productPrices as productPrice>
                                                            <#assign currencyUomId = productPrice.currencyUomId!>
                                                            <#if productPrice.productPriceTypeId == "MINIMUM_PRICE">
                                                                <#assign min = productPrice.price!>
                                                            <#elseif productPrice.productPriceTypeId == "MAXIMUM_PRICE">
                                                                <#assign max = productPrice.price!>
                                                            </#if>
                                                        </#list>
                                                    </#if>
                                                    <tr>
                                                    <input type="hidden" name="currencyId_${id}" value="${currencyUomId!}"/>
                                                        <#if listingType.type.equals("FixedPriceItem") >
                                                            <td class="label">Start Price</td>
                                                            <td><input type="text"  size="6" name="startPrice_${id}" value="${min!}" />${currencyUomId!}</td>
                                                            <td class="label"></td>
                                                            <td></td>
                                                        <#else>
                                                            <td class="label">Start Price</td>
                                                            <td><input type="text" size="6" name="startPrice_${id}" value="${min!}" />${currencyUomId!}</td>
                                                            <td class="label">BIN Price</td>
                                                            <td><input type="text"  size="6" name="buyItNowPrice_${id}" value="${max!}" <#if listingType.type.equals("FixedPriceItem") >disabled="disabled"</#if> />${currencyUomId!}</td>
                                                        </#if>
                                                    </tr>
                                                    <#if !listingType.type.equals("FixedPriceItem") >
                                                    <tr>
                                                        <td class="label">Reserve Price</td>
                                                        <td><input type="text" size="6" name="reservePrice_${id}" <#if listingType.type.equals("FixedPriceItem") >disabled="disabled"</#if> />${currencyUomId!}</td>
                                                        <td class="label"></td>
                                                        <td></td>
                                                    </tr>
                                                    </#if>
                                                    <tr>
                                                        <td class="label">VATPercent</td>
                                                        <td><input type="text" size="6" name="vatPercent_${id}" /></td>
                                                         <td class="label">Postal code</td>
                                                        <td><input type="text" size="10" name="postalCode_${id}" /></td>
                                                    </tr>
                                                    <#if listingType.type.equals("FixedPriceItem") >
                                                    <tr>
                                                        <td class="label"></td><!-- use when fixed price and store fixed price -->
                                                        <td><input type="checkbox" value="true" name="enableBestOffer_${id}" /><b>Enable Best Offer</b></td>
                                                        <td class="label"></td>
                                                        <td><br /></td>
                                                    </tr>
                                                    </#if>
                                                    <tr><td colspan="4"><br /></td></tr>
                                                </table>
                                       </div>
                                       <#assign id = id + 1>
                                          </#if>
                                           
                                    </#list>
                                    </div>
                                    <!-- end of  set  listing type, duration, prices -->
                                 </div>
                            </div>
                          </#if>
                          <!-- payment section -->
                          <#if primaryCate?has_content && primaryCate.getCategoryID()?? && listingTypes?has_content>
                             <div class="screenlet">
                                 <div class="screenlet-title-bar"><ul><li class="h3">Payment</li></ul><br class="clear"/></div>
                                 <div class="screenlet-body">
                                     <table width="50%" height="100%" id="table2" cellspacing="0">
                                        <tr><td colspan="4"><br /></td></tr>
                                                     <tr>
                                                        <td colspan="4">
                                                            <b><u>${uiLabelMap.FormFieldTitle_paymentMethodsAccepted}</u></b>
                                                        </td>
                                                    </tr>
                                                    <tr><td colspan="4"><br /></td></tr>
                                                    <tr>
                                                        <td colspan="4">
                                                            <#assign is_payPal = false>
                                                            <#if paymentMethods?has_content>
                                                                <table>
                                                                    <#assign j = 0>
                                                                    <#list paymentMethods as paymentMethod>
                                                                        <#if paymentMethod.value()??>
                                                                            <#if j == 0><tr></#if>
                                                                        <#if paymentMethod.compareTo(buyerPayMethCode_PAY_PAL!) == 0 >
                                                                                <#assign is_payPal = true>
                                                                        </#if>
                                                                        <td valign="top"><input type="checkbox" value="true" name="Payments_${paymentMethod.value()!}" /></td>
                                                                        <td align="left"><b>${paymentMethod.value()!}</b></td>
                                                                        <#if j == 3>
                                                                             </tr>
                                                                             <#assign j = 0>
                                                                        <#else>
                                                                          <#assign j = j+1>
                                                                        </#if>
                                                                        </#if>
                                                                   </#list>
                                                                </table>
                                                                <#--assign i = 0>
                                                                <#list paymentMethods as paymentMethod>
                                                                    <input type="checkbox" value="${paymentMethod.name()!}" name="${paymentMethod.name()!}_${id}">${paymentMethod.value()!}</checkbox><span style="width:40px"/><#if i==3><br /><#assign i = -1></#if>
                                                                    <#assign i=i+1> 
                                                                </#list-->
                                                            </#if>
                                                        </td>
                                                    </tr>
                                                    <#if is_payPal == true>
                                                    <tr>
                                                        <td class="label">${uiLabelMap.FormFieldTitle_payPalEmail}</td>
                                                        <td><input type="text" name="paymentMethodPaypalEmail" id="paymentMethodPaypalEmail" size="50" maxlength="50" value="me@ebay.com" /></td>
                                                        <td class="label"></td>
                                                        <td><br /></td>
                                                    </tr>
                                                    </#if>
                                         <tr><td colspan="4"><br /></td></tr>
                                     </table>
                                 </div>
                             </div>
                          </#if>
                          <#if primaryCate?has_content && primaryCate.getCategoryID()?? && listingTypes?has_content>
                             <div class="screenlet">
                                 <div class="screenlet-title-bar"><ul><li class="h3">Shipping Service</li></ul><br class="clear"/></div>
                                 <div class="screenlet-body">
                                    <table cellSpacing="0" cellPadding="0" width="100%" border="0">
                                        <tr>
                                          <td></td>
                                          <td width="100%"><b>Demestic Shipping Service</b></td>
                                          <td><img height="1" src="http://pics.ebaystatic.com/aw/pics/tbx/s.gif" width="10" alt="" /></td>
                                        </tr>
                                        <tr>
                                          <td></td>
                                          <td width="100%">
                                          <select name="ShippingService" id="ShippingService" style="width:107px;">
                                          <#--for eBayMotors site, we add a 'None' Shipping Service-->
                                          <#if siteCode?has_content && siteCode_Ebay_Motors?has_content>
                                              <#if siteCode == siteCode_Ebay_Motors>
                                                  <option value="None">None</option>
                                              </#if>
                                          </#if>
                                          <#if shippingServiceDetails?has_content>
                                          <#list shippingServiceDetails as shippingServiceDetail>
                                              <#assign shippingService = shippingServiceDetail.getShippingService()!>
                                              <option value="${shippingService!}">${shippingService!}</option>
                                          </#list>   
                                          </#if>
                                          </select>
                                          </td>
                                          <td><img height="1" src="http://pics.ebaystatic.com/aw/pics/tbx/s.gif" width="10" alt="" /></td>
                                        </tr>
                                    </table>
                                    <table cellSpacing="0" cellPadding="0" width="100%" border="0">
                                         <tr>
                                           <td style="WIDTH: 150px"><b>Ships to locations:</b></td>
                                         </tr>
                                         <tr>
                                           <td>
                                             <#if shippingLocationDetails?has_content>
                                             <table>
                                                    <#assign j=0>
                                                    <#list shippingLocationDetails as shippingLocationDetail>
                                                        <#assign shippingLocation = shippingLocationDetail.getShippingLocation()!>
                                                        <#if j==0><tr></#if>
                                                          <td valign="top"><input type="checkbox" value="true" name="Shipping_${shippingLocation!}" /></td>
                                                          <td align="left"><b>${shippingLocationDetail.getDescription()!}</b></td>
                                                        <#if j==3></tr><#assign j=0><#else><#assign j=j+1></#if>
                                                    </#list>
                                             </table>
                                             </#if>
                                           </td>
                                         </tr>
                                   </table>
                                 </div>
                             </div>
                        </#if>
                        <!-- end shipping section -->
                 </#if>
            </#list>
        </#if>
       </#if>
       </td></tr>
    </table>
    <#--if addItemList?has_content>
        <table cellspacing="0" class="basic-table">
            <tr>
                <td align="center" colspan="2">
                    <a href="#" class="buttontext">${uiLabelMap.EbayExportToEbay}</a>
                    <a href="#" class="buttontext">VeriflyItem</a>
                
    </#if-->
</form>
</div>
</div>
