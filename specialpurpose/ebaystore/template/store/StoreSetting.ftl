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
<script language="Javascript" type="text/javascript">
    function countAreaChars(areaName, limit, charleft)
    {
        if (areaName.value.length > limit){
           areaName.value=areaName.value.substring(0,limit);
        }else{
          charleft.innerHTML = (limit - areaName.value.length) + " ${uiLabelMap.CommonCharactorsLeft}";
        }
    }
    function retrieveThemeColorSchemeByThemeId(url, themeId, productStoreId){
        var pars = 'themeId='+themeId+'&amp;productStoreId='+productStoreId;

        jQuery.ajax({
         url: url,
         type: "GET",
         data: pars,
         beforeStart: function() {document.getElementById('loading').innerHTML = ' ${uiLabelMap.CommonPleaseWait}';},
             success: function(data) {
                if (data != null && data.storeColorSchemeMap){
                    var resp = eval("("+data.storeColorSchemeMap+")");
                    if (resp.storeColorPrimary!=null) document.getElementById('storePrimaryColor').value =  resp.storeColorPrimary;
                    if (resp.storeColorAccent!=null) document.getElementById('storeSecondaryColor').value = resp.storeColorAccent;
                    if (resp.storeColorSecondary!=null) document.getElementById('storeAccentColor').value = resp.storeColorSecondary;

                    if (resp.storeFontTypeFontFaceValue!=null) selectOption( document.getElementById('storeNameFont'),resp.storeFontTypeFontFaceValue);
                    if (resp.storeFontTypeNameFaceColor!=null) document.getElementById('storeNameFontColor').value = resp.storeFontTypeNameFaceColor;
                    if (resp.storeFontTypeSizeFaceValue!=null) selectOption( document.getElementById('storeNameFontSize'), resp.storeFontTypeSizeFaceValue);

                    if (resp.storeFontTypeTitleColor!=null) document.getElementById('storeTitleFontColor').value = resp.storeFontTypeTitleColor;
                    if (resp.storeFontTypeFontTitleValue!=null) selectOption( document.getElementById('storeTitleFont'),resp.storeFontTypeFontTitleValue);
                    if (resp.storeFontSizeTitleValue!=null) selectOption( document.getElementById('storeTitleFontSize'),resp.storeFontSizeTitleValue);

                    if (resp.storeFontTypeDescColor!=null) document.getElementById('storeDescFontColor').value = resp.storeFontTypeDescColor;
                    if (resp.storeFontTypeFontDescValue!=null) selectOption( document.getElementById('storeDescFont'),resp.storeFontTypeFontDescValue);
                    if (resp.storeDescSizeValue!=null) selectOption( document.getElementById('storeDescFontSize'),resp.storeDescSizeValue);
                }
                 document.getElementById('loading').innerHTML = '';
         }
        });
    }

    function selectOption(myselect, val){
        for (var i=0; i<myselect.options.length; i++){
             if ( myselect.options[i].value == val){
                 myselect.options[i].selected=true;
                 break;
             }

        }
    }
    function switchTheme(url, themeId, productStoreId){
        var size = document.StoreSettingForm.storeThemeType.length;
        var selectTheme = '';
        for(i=0;i<size;i++){
            if (document.StoreSettingForm.storeThemeType[i].checked){
                selectTheme = document.StoreSettingForm.storeThemeType[i].value;
                break;
            }
        }
        if (selectTheme == 'Basic') {
            //retrieve basic theme by ajax then print new select list
            document.StoreSettingForm.storeAdvancedTheme.disabled = true;
            document.StoreSettingForm.storeAdvancedThemeColor.disabled = true;

            document.StoreSettingForm.storeBasicTheme.disabled = false;
            document.StoreSettingForm.storePrimaryColor.disabled = false;
            document.StoreSettingForm.storeSecondaryColor.disabled = false;
            document.StoreSettingForm.storeAccentColor.disabled = false;

            document.StoreSettingForm.storeNameFont.disabled = false;
            document.StoreSettingForm.storeNameFontSize.disabled = false;
            document.StoreSettingForm.storeNameFontColor.disabled = false;

            document.StoreSettingForm.storeTitleFont.disabled = false;
            document.StoreSettingForm.storeTitleFontSize.disabled = false;
            document.StoreSettingForm.storeTitleFontColor.disabled = false;

            document.StoreSettingForm.storeDescFont.disabled = false;
            document.StoreSettingForm.storeDescFontSize.disabled = false;
            document.StoreSettingForm.storeDescFontColor.disabled = false;
            document.StoreSettingForm.themeType.value = "Basic";

        } else if (selectTheme == 'Advanced') {
            document.StoreSettingForm.themeType.value = "Advanced";
            document.StoreSettingForm.storeBasicTheme.disabled = true;
            document.StoreSettingForm.storePrimaryColor.disabled = true;
            document.StoreSettingForm.storeSecondaryColor.disabled = true;
            document.StoreSettingForm.storeAccentColor.disabled = true;

            document.StoreSettingForm.storeNameFont.disabled = true;
            document.StoreSettingForm.storeNameFontSize.disabled = true;
            document.StoreSettingForm.storeNameFontColor.disabled = true;

            document.StoreSettingForm.storeTitleFont.disabled = true;
            document.StoreSettingForm.storeTitleFontSize.disabled = true;
            document.StoreSettingForm.storeTitleFontColor.disabled = true;

            document.StoreSettingForm.storeDescFont.disabled = true;
            document.StoreSettingForm.storeDescFontSize.disabled = true;
            document.StoreSettingForm.storeDescFontColor.disabled = true;

            document.StoreSettingForm.storeAdvancedTheme.disabled = false;
            document.StoreSettingForm.storeAdvancedThemeColor.disabled = false;
        }
    }
</script>

  <#if parameters.ebayStore?has_content>
    <#assign ebayStore = parameters.ebayStore!>
    <#--${ebayStore}-->
    <form name="StoreSettingForm" id="StoreSettingForm" method="post" action="<@ofbizUrl>editEbayStoreDetail</@ofbizUrl>" style="margin: 0;">
        <input type="hidden" name="themeType" value="${themeType!}"/>
        <input type="hidden" name="storeUrl" value="${ebayStore.storeUrl!}"/>
        <input type="hidden" name="storeLogoId" value="${ebayStore.storeLogoId!}"/>
        <input type="hidden" name="storeLogoName" value="${ebayStore.storeLogoName!}"/>
        <input type="hidden" name="productStoreId" value="${parameters.productStoreId!}"/>
      <fieldset>
        <table cellspacing="0" class="basic-table">
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreName} :</td>
              <td valign="middle">
                    <input type="text" name="storeName" value="${ebayStore.storeName!}" onKeyDown="countAreaChars(document.StoreSettingForm.storeName,35,document.getElementById('charsleft1'));"
                    onKeyUp="countAreaChars(document.StoreSettingForm.storeName,35,document.getElementById('charsleft1'));" />
                    <div id="charsleft1"></div>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreDesc} :</td>
              <td valign="middle">
                    <textarea rows="4" cols="80" name="storeDesc"
                    onKeyDown="countAreaChars(document.StoreSettingForm.storeDesc,300,document.getElementById('charsleft2'));"
                    onKeyUp="countAreaChars(document.StoreSettingForm.storeDesc,300,document.getElementById('charsleft2'));">${ebayStore.storeDesc!}</textarea>
                    <div id="charsleft2"></div>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreURL} :</td>
              <td valign="middle">
                   <a href="${ebayStore.storeUrl!}" target="_blank">${ebayStore.storeUrl!}</a>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreLogoURL} :</td>
              <td valign="middle">
                   <input type="text" name="storeLogoURL" size="50" value="${ebayStore.storeLogoURL!}"/>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle"></td>
              <td valign="middle">
                   <div onclick="javascript:switchTheme();">
                   <label><input type="radio" name="storeThemeType" <#if themeType! == "Basic">checked="checked"</#if> value="Basic" default="default" /> Basic Theme</label>
                   <label><input type="radio"  name="storeThemeType" <#if themeType! == "Advanced">checked="checked"</#if> value="Advanced" /> Advanced Theme</label>
                   </div>
              </td>
            </tr>
            <#-- advance Theme -->
             <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreAdvancedTheme} :</td>
              <td valign="middle">
                <select id="storeAdvancedTheme" name="storeAdvancedTheme" >
                   <#if storeAdvanceThemeOptList?has_content>
                       <#list storeAdvanceThemeOptList as storeAdvanceThemeOpt>
                                    <option value="${storeAdvanceThemeOpt.storeThemeId!}"
                                    <#if ebayStore.storeThemeId.equals(storeAdvanceThemeOpt.storeThemeId!)>selected="selected"</#if>>
                                    ${storeAdvanceThemeOpt.storeThemeName!}</option>
                        </#list>
                   </#if>
                </select>
              </td>
            </tr>
            <#if storeAdvancedThemeColorOptList?has_content>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreAdvancedThemeColor} :</td>
              <td valign="middle">
                <select name="storeAdvancedThemeColor">
                        <#list storeAdvancedThemeColorOptList as storeAdvancedThemeColorOpt>
                                <option value="${storeAdvancedThemeColorOpt.storeColorSchemeId!}"
                                <#if ebayStore.storeColorSchemeId.equals(storeAdvancedThemeColorOpt.storeColorSchemeId!)>selected="selected"</#if>>
                                ${storeAdvancedThemeColorOpt.storeColorName!}</option>
                        </#list>
                </select>
              </td>
            </tr>
            </#if>
            <#-- Basic Theme -->
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreBasicTheme} :</td>
              <td valign="middle">
                <#assign currentStoreThemeIdAndSchemeId = ebayStore.storeThemeId?string+"-"+ebayStore.storeColorSchemeId?string>
                <select id="storeBasicTheme" name="storeBasicTheme" onchange="javascript:retrieveThemeColorSchemeByThemeId('<@ofbizUrl>retrieveThemeColorSchemeByThemeId</@ofbizUrl>',this.value,'${parameters.productStoreId!}');">
                   <#if storeThemeOptList?has_content>
                       <#list storeThemeOptList as storeThemeOpt>
                                    <#assign storeThemeIdAndSchemeId = storeThemeOpt.storeThemeId+"-"+storeThemeOpt.storeColorSchemeId>
                                    <option value="${storeThemeIdAndSchemeId!}" 
                                        <#if currentStoreThemeIdAndSchemeId == storeThemeIdAndSchemeId!>selected="selected"</#if>>
                                        ${storeThemeOpt.storeColorSchemeName!}
                                    </option>
                        </#list>
                   </#if>
                </select>
                <div id="loading"></div>
              </td>
            </tr>
            <tr>
              <td  align="right" valign="middle"></td>
              <td valign="middle"><b>${uiLabelMap.EbayStoreStoreColorTheme}</b>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStorePrimaryColor} :</td>
              <td valign="middle">
                   ${uiLabelMap.CommonNbr}<input type="text" id="storePrimaryColor" name="storePrimaryColor" size="10" value="${ebayStore.storeColorPrimary!}"/>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreSecondColor} :</td>
              <td valign="middle">
                   ${uiLabelMap.CommonNbr}<input type="text" id="storeSecondaryColor" name="storeSecondaryColor" size="10" value="${ebayStore.storeColorSecondary!}"/>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreAccentColor} :</td>
              <td valign="middle">
                   ${uiLabelMap.CommonNbr}<input type="text" id="storeAccentColor" name="storeAccentColor" size="10" value="${ebayStore.storeColorAccent!}"/>
              </td>
            </tr>
            <tr>
              <td  align="right" valign="middle"></td>
              <td valign="middle"><b>${uiLabelMap.EbayStoreStoreChangeFont}</b>
              </td>
            </tr>
             <tr>
              <td  align="right" valign="middle"></td>
              <td valign="middle">
                    <table width="450" >
                        <tr>
                            <td><b>Font</b></td>
                            <td><b>Font size</b></td>
                            <td><b>Font color</b></td>
                        </tr>
                    </table>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreName} :</td>
              <td valign="middle">
               <#if storeFontTheme??>
                    <#if ebayStore.storeNameColor??>
                        <#assign storeFontColor = ebayStore.storeNameColor!>
                    <#else>
                        <#assign storeFontColor = storeFontTheme.storeFontTypeNameFaceColor!>
                    </#if>
                    <table width="450">
                        <tr>
                            <td>
                                <select id="storeNameFont" name="storeNameFont">
                                    <#list storeFontTheme.storeFontTypeFontFaceList as storeFontTypeFontFace>
                                        <option <#if storeFontTypeFontFace.storeFontValue.equals(ebayStore.storeNameFontFace) >selected="selected"</#if> value="${storeFontTypeFontFace.storeFontName!}">${storeFontTypeFontFace.storeFontName!}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                <select id="storeNameFontSize" name="storeNameFontSize">
                                    <#list storeFontTheme.storeFontTypeSizeFaceList as storeFontTypeSizeFace>
                                        <option <#if storeFontTypeSizeFace.storeFontSizeValue.equals(ebayStore.storeNameFontFaceSize) >selected="selected"</#if> value="${storeFontTypeSizeFace.storeFontSizeName!}">${storeFontTypeSizeFace.storeFontSizeName!}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                ${uiLabelMap.CommonNbr}<input id="storeNameFontColor" type="text" size="10" name="storeNameFontColor" value="${storeFontColor!}"/>
                            </td>
                        </tr>
                    </table>
                    </#if>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreSectionTitle} :</td>
              <td valign="middle">
               <#if storeFontTheme??>
                    <#if ebayStore.storeTitleColor??>
                        <#assign storeTitleColor = ebayStore.storeTitleColor!>
                    <#else>
                        <#assign storeTitleColor = storeFontTheme.storeFontTypeTitleColor!>
                    </#if>
                    <table width="450">
                        <tr>
                            <td>
                                <select id="storeTitleFont" name="storeTitleFont">
                                    <#list storeFontTheme.storeFontTypeFontTitleList as storeFontTypeFontTitle>
                                        <option <#if storeFontTypeFontTitle.storeFontValue.equals(ebayStore.storeTitleFontFace) >selected="selected"</#if> value="${storeFontTypeFontTitle.storeFontName!}">${storeFontTypeFontTitle.storeFontName!}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                <select id="storeTitleFontSize" name="storeTitleFontSize">
                                    <#list storeFontTheme.storeFontSizeTitleList as storeFontSizeTitle>
                                        <option <#if storeFontSizeTitle.storeFontSizeValue.equals(ebayStore.storeTitleFontFaceSize) >selected="selected"</#if> value="${storeFontSizeTitle.storeFontSizeName!}">${storeFontSizeTitle.storeFontSizeName!}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                ${uiLabelMap.CommonNbr}<input id="storeTitleFontColor" type="text" size="10" name="storeTitleFontColor" value="${storeTitleColor!}"/>
                            </td>
                        </tr>
                    </table>
                    </#if>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreDesc} :</td>
              <td valign="middle">
              <#if storeFontTheme??>
                    <#if ebayStore.storeDescColor??>
                        <#assign storeDescColor = ebayStore.storeDescColor!>
                    <#else>
                        <#assign storeDescColor = storeFontTheme.storeFontTypeDescColor!>
                    </#if>
                    <table width="450">
                        <tr>
                            <td>
                                <select id="storeDescFont" name="storeDescFont">
                                    <#list storeFontTheme.storeFontTypeFontDescList as storeFontTypeFontDesc>
                                        <option <#if storeFontTypeFontDesc.storeFontValue.equals(ebayStore.storeDescFontFace!) >selected="selected"</#if> value="${storeFontTypeFontDesc.storeFontName!}">${storeFontTypeFontDesc.storeFontName!}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                <select id="storeDescFontSize" name="storeDescFontSize">
                                    <#list storeFontTheme.storeDescSizeList as storeDescSize>
                                        <option <#if storeDescSize.storeFontSizeValue.equals(ebayStore.storeDescSizeCode) >selected="selected"</#if> value="${storeDescSize.storeFontSizeName!}">${storeDescSize.storeFontSizeName!}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                ${uiLabelMap.CommonNbr}<input id="storeDescFontColor" type="text" size="10" name="storeDescFontColor" value="${storeDescColor!}"/>
                            </td>
                        </tr>
                    </table>
                    </#if>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreHeaderDisplay} :</td>
              <td valign="middle">
                    <select id="storeCustomHeaderLayout" name="storeCustomHeaderLayout">
                        <#list ebayStore.storeCustomHeaderLayoutList as storeCustomHeaderLayout>
                               <option <#if storeCustomHeaderLayout.storeCustomHeaderLayoutValue.equals(ebayStore.storeCustomHeaderLayout) >selected="selected"</#if> value="${storeCustomHeaderLayout.storeCustomHeaderLayoutName!}">${storeCustomHeaderLayout.storeCustomHeaderLayoutValue!}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle"></td>
              <td valign="middle">
                    <textarea rows="8" cols="40" name="storeCustomHeader">
                    ${ebayStore.storeCustomHeader!}</textarea>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreHeaderStyle} :</td>
              <td valign="middle">
                    <select id="storeHeaderStyle" name="storeHeaderStyle">
                        <#list ebayStore.storeHeaderStyleList as storeHeaderStyle>
                               <option <#if storeHeaderStyle.storeHeaderStyleValue.equals(ebayStore.storeHeaderStyle) >selected="selected"</#if> value="${storeHeaderStyle.storeHeaderStyleName!}">${storeHeaderStyle.storeHeaderStyleValue!}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <#--tr>
              <td class="label" align="right" valign="middle">Home Page :</td>
              <td valign="middle">
                    <input type="text" id="homePage" name="homePage" value="${ebayStore.storeHomePage!}"/>
              </td>
            </tr-->
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreItemListDesplay} :</td>
              <td valign="middle">
                    <select id="storeItemLayout" name="storeItemLayout">
                        <#list ebayStore.storeItemLayoutList as storeItemLayout>
                               <option <#if storeItemLayout.storeItemLayoutValue.equals(ebayStore.storeItemLayoutSelected) >selected="selected"</#if> value="${storeItemLayout.storeItemLayoutName!}">${storeItemLayout.storeItemLayoutValue!}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreItemSortOrder} :</td>
              <td valign="middle">
                    <select id="storeItemSortOrder" name="storeItemSortOrder">
                        <#list ebayStore.storeItemSortOrderList as storeItemSortOrder>
                               <option <#if storeItemSortOrder.storeItemSortLayoutValue.equals(ebayStore.storeItemSortOrderSelected) >selected="selected"</#if> value="${storeItemSortOrder.storeItemSortLayoutName!}">${storeItemSortOrder.storeItemSortLayoutValue!}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <#--tr>
              <td class="label" align="right" valign="middle">Custom Listing Header Display :</td>
              <td valign="middle">
                    <select id="storeCustomListingHeaderDisplay" name="storeCustomListingHeaderDisplay">
                        <#list ebayStore.storeCustomListingHeaderDisplayList as storeCustomListingHeaderDisplay>
                               <option <#if storeCustomListingHeaderDisplay.storeCustomHeaderLayoutValue.equals(ebayStore.storeCustomListingHeaderDisplayValue) >selected="selected"</#if> value="${storeCustomListingHeaderDisplay.storeCustomHeaderLayoutValue!}">${storeCustomListingHeaderDisplay.storeCustomHeaderLayoutValue!}</option>
                        </#list>
                    </select>
              </td>
            </tr-->
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreMerchDisplay} :</td>
              <td valign="middle">
                    <select id="storeMerchDisplay" name="storeMerchDisplay">
                        <#list ebayStore.storeMerchDisplayList as storeMerchDisplay>
                               <option <#if storeMerchDisplay.merchDisplayCodeValue.equals(ebayStore.storeMerchDisplay) >selected="selected"</#if> value="${storeMerchDisplay.merchDisplayCodeName!}">${storeMerchDisplay.merchDisplayCodeValue!}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">${uiLabelMap.EbayStoreStoreSubscriptionLevel} :</td>
              <td valign="middle">
                    <select id="storeMerchDisplay" name="storeSubscriptionDisplay">
                        <#list ebayStore.storeSubscriptionLevelList as storeSubscriptionLevel>
                               <option <#if storeSubscriptionLevel.storeSubscriptionLevelCodeValue.equals(ebayStore.storeSubscriptionLevel) >selected="selected"</#if> value="${storeSubscriptionLevel.storeSubscriptionLevelCodeName!}">${storeSubscriptionLevel.storeSubscriptionLevelCodeValue!}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle"></td>
              <td valign="middle">
                    <input type="submit" value="${uiLabelMap.CommonSubmit}" name="submitButton" class="smallSubmit" />
              </td>
            </tr>
       </table>
      <script language="Javascript" type="text/javascript">
            document.getElementById('charsleft1').innerHTML =  (35 - document.StoreSettingForm.storeName.value.length)  + " charactors left.";
            document.getElementById('charsleft2').innerHTML =  (300 - document.StoreSettingForm.storeDesc.value.length)  + " charactors left.";
      </script>
     </fieldset>
   </form>
  </#if>
 <script language="Javascript" type="text/javascript">
    <#if themeType! == "Basic">
        document.StoreSettingForm.storeAdvancedTheme.disabled = true;
        document.StoreSettingForm.storeAdvancedThemeColor.disabled = true;
    <#elseif themeType! == "Advanced">
        document.StoreSettingForm.storeBasicTheme.disabled = true;
        document.StoreSettingForm.storePrimaryColor.disabled = true;
        document.StoreSettingForm.storeSecondaryColor.disabled = true;
        document.StoreSettingForm.storeAccentColor.disabled = true;
        
        document.StoreSettingForm.storeNameFont.disabled = true;
        document.StoreSettingForm.storeNameFontSize.disabled = true;
        document.StoreSettingForm.storeNameFontColor.disabled = true;
        
        document.StoreSettingForm.storeTitleFont.disabled = true;
        document.StoreSettingForm.storeTitleFontSize.disabled = true;
        document.StoreSettingForm.storeTitleFontColor.disabled = true;
        
        document.StoreSettingForm.storeDescFont.disabled = true;
        document.StoreSettingForm.storeDescFontSize.disabled = true;
        document.StoreSettingForm.storeDescFontColor.disabled = true;
    </#if>
</script>