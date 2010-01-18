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
    function countAreaChars(areaName,limit,charleft)
    {
        if (areaName.value.length>limit){
           areaName.value=areaName.value.substring(0,limit);
        }else{
          charleft.innerHTML = (limit - areaName.value.length) + " charactors left.";
        }
    }
    function retrieveThemeColorSchemeByThemeId(url,themeId,productStoreId){
    var pars = 'themeId='+themeId+'&productStoreId='+productStoreId; 
    var myAjax = new Ajax.Request( url, {
        method: 'get', 
        parameters: pars, 
        onLoading: function loading(){
                        $('loading').innerHTML = ' Please wait...';
                   },
        onComplete: function retrieveThemeColorSchemeByThemeId(originalRequest){
                            if(originalRequest.responseJSON!=null){
                                var resp = eval("("+originalRequest.responseText+")");
                                if(resp.storeColorPrimary!=null)$('storePrimaryColor').value =  resp.storeColorPrimary;
                                if(resp.storeColorAccent!=null)$('storeSecondaryColor').value = resp.storeColorAccent;
                                if(resp.storeColorSecondary!=null)$('storeAccentColor').value = resp.storeColorSecondary;
                                
                                if(resp.storeFontTypeFontFaceValue!=null) selectOption($('selectStoreNameFont'),resp.storeFontTypeFontFaceValue);
                                if(resp.storeFontTypeNameFaceColor!=null)$('storeNameFontColor').value = resp.storeFontTypeNameFaceColor;
                                if(resp.storeFontTypeSizeFaceValue!=null) selectOption($('selectStoreNameFontSize'), resp.storeFontTypeSizeFaceValue);

                                if(resp.storeFontTypeTitleColor!=null)$('storeTitleFontColor').value = resp.storeFontTypeTitleColor;
                                if(resp.storeFontTypeFontTitleValue!=null)selectOption($('selectStoreTitleFont'),resp.storeFontTypeFontTitleValue);
                                if(resp.storeFontSizeTitleValue!=null)selectOption($('selectStoreTitleFontSize'),resp.storeFontSizeTitleValue);

                                if(resp.storeFontTypeDescColor!=null)$('storeDescFontColor').value = resp.storeFontTypeDescColor;
                                if(resp.storeFontTypeFontDescValue!=null) selectOption($('selectStoreDescFont'),resp.storeFontTypeFontDescValue);
                                if(resp.storeDescSizeValue!=null) selectOption($('selectStoreDescFontSize'),resp.storeDescSizeValue);
                            } 
                            $('loading').innerHTML = '';
                    }        
        } );
    }
    function selectOption(myselect,val){
        for (var i=0; i<myselect.options.length; i++){
             if ( myselect.options[i].value == val){
                 myselect.options[i].selected=true;
                 break;
             }
             
        }
    }
</script>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>Basic Information </h3>
  </div>
  <div class="screenlet-body">
  <#if parameters.ebayStore?has_content>
    <#assign ebayStore = parameters.ebayStore?if_exists>
    <#--${ebayStore}-->
    <form name="StoreSettingForm" id="StoreSettingForm" method="post" action="#" style="margin: 0;">
      <fieldset>
        <table cellspacing="0" class="basic-table">
            <tr>
              <td class="label" align="right" valign="middle">Store Name :</td>
              <td valign="middle">
                    <input type="text" name="storeName" value="${ebayStore.storeName?if_exists}" onKeyDown="countAreaChars(document.StoreSettingForm.storeName,35,document.getElementById('charsleft1'));"
                    onKeyUp="countAreaChars(document.StoreSettingForm.storeName,35,document.getElementById('charsleft1'));">
                    <div id="charsleft1"></div>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Store Description :</td>
              <td valign="middle">
                    <textarea rows="4" cols="80" name="storeDesc"
                    onKeyDown="countAreaChars(document.StoreSettingForm.storeDesc,300,document.getElementById('charsleft2'));"
                    onKeyUp="countAreaChars(document.StoreSettingForm.storeDesc,300,document.getElementById('charsleft2'));">
                    ${ebayStore.storeDesc?if_exists}</textarea>
                    <div id="charsleft2"></div>
              </td>
            </tr>
            
            <tr>
              <td class="label" align="right" valign="middle">Store URL:</td>
              <td valign="middle">
                   <a href="${ebayStore.storeUrl?if_exists}" target="_blank">${ebayStore.storeUrl?if_exists}</a>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Logo URL (http://):</td>
              <td valign="middle">
                   <input type="text" name="storeLogoURL" size="50" value="${ebayStore.storeLogoURL?if_exists}"/>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Store Theme :</td>
              <td valign="middle">
                <#assign currentStoreThemeIdAndSchemeId = ebayStore.storeThemeId?string+"-"+ebayStore.storeColorSchemeId?string>
                <select name="selectedTheme" onChange="javascript:retrieveThemeColorSchemeByThemeId('<@ofbizUrl>retrieveThemeColorSchemeByThemeId</@ofbizUrl>',this.value,'${parameters.productStoreId?if_exists}');">
                   <#if storeThemeOptList?has_content>
                       <#list storeThemeOptList as storeThemeOpt>
                                <#if themeType?if_exists == "Basic">
                                    <#assign storeThemeIdAndSchemeId = storeThemeOpt.storeThemeId+"-"+storeThemeOpt.storeColorSchemeId>
                                    <option value="${storeThemeIdAndSchemeId?if_exists}" 
                                        <#if currentStoreThemeIdAndSchemeId == storeThemeIdAndSchemeId?if_exists>selected</#if>>
                                        ${storeThemeOpt.storeColorSchemeName?if_exists}
                                    </option>
                                <#else>
                                    <option value="${storeThemeOpt.storeThemeId?if_exists}"
                                    <#if ebayStore.storeThemeId.equals(storeThemeOpt.storeThemeId?if_exists)>selected</#if>>
                                    ${storeThemeOpt.storeThemeName?if_exists}</option>
                                </#if>
                        </#list>
                   </#if>
                </select>
                <div id="loading"></div>
              </td>
            </tr>
            <#if storeAdvancedThemeColorOptList?has_content>
            <tr>
              <td class="label" align="right" valign="middle">Store Theme Color :</td>
              <td valign="middle">
                <select name="selectedThemeColorScheme">
                        <#list storeAdvancedThemeColorOptList as storeAdvancedThemeColorOpt>
                                <option value="${storeAdvancedThemeColorOpt.storeColorSchemeId?if_exists}"
                                <#if ebayStore.storeColorSchemeId.equals(storeAdvancedThemeColorOpt.storeColorSchemeId?if_exists)>selected</#if>>
                                ${storeAdvancedThemeColorOpt.storeColorName?if_exists}</option>
                        </#list>
                </select>
              </td>
            </tr>
            </#if>
            <#if !storeAdvancedThemeColorOptList?has_content>
            <tr>
              <td  align="right" valign="middle"></td>
              <td valign="middle"><b>Change color</b>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Primary color :</td>
              <td valign="middle">
                   #<input type="text" id="storePrimaryColor" name="storePrimaryColor" size="10" value="${ebayStore.storeColorPrimary?if_exists}"/>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Secondary color :</td>
              <td valign="middle">
                   #<input type="text" id="storeSecondaryColor" name="storeSecondaryColor" size="10" value="${ebayStore.storeColorSecondary?if_exists}"/>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Accent color :</td>
              <td valign="middle">
                   #<input type="text" id="storeAccentColor" name="storeAccentColor" size="10" value="${ebayStore.storeColorAccent?if_exists}"/>
              </td>
            </tr>
            <tr>
              <td  align="right" valign="middle"></td>
              <td valign="middle"><b>Change Fonts</b>
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
              <td class="label" align="right" valign="middle">Store name</td>
              <td valign="middle">
                    <#if storeFontTheme?exists>
                    <#if ebayStore.storeNameColor?exists>
                        <#assign storeFontColor = ebayStore.storeNameColor?if_exists>
                    <#else>
                        <#assign storeFontColor = storeFontTheme.storeFontTypeNameFaceColor?if_exists>
                    </#if>
                    <table width="450">
                        <tr>
                            <td>
                                <select id="selectStoreNameFont" name="selectStoreNameFont">
                                    <#list storeFontTheme.storeFontTypeFontFaceList as storeFontTypeFontFace>
                                        <option <#if storeFontTypeFontFace.storeFontValue?if_exists.equals(ebayStore.storeNameFontFace?if_exists) >selected</#if> value="${storeFontTypeFontFace.storeFontValue?if_exists}">${storeFontTypeFontFace.storeFontName?if_exists}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                <select id="selectStoreNameFontSize" name="selectStoreNameFontSize">
                                    <#list storeFontTheme.storeFontTypeSizeFaceList as storeFontTypeSizeFace>
                                        <option <#if storeFontTypeSizeFace.storeFontSizeValue?if_exists.equals(ebayStore.storeNameFontFaceSize?if_exists) >selected</#if> value="${storeFontTypeSizeFace.storeFontSizeValue?if_exists}">${storeFontTypeSizeFace.storeFontSizeName?if_exists}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                #<input id="storeNameFontColor" type="text" size="10" name="storeNameFontColor" value="${storeFontColor?if_exists}"/>
                            </td>
                        </tr>
                    </table>
                    </#if>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Section title</td>
              <td valign="middle">
               <#if storeFontTheme?exists>
                    <#if ebayStore.storeTitleColor?exists>
                        <#assign storeTitleColor = ebayStore.storeTitleColor?if_exists>
                    <#else>
                        <#assign storeTitleColor = storeFontTheme.storeFontTypeTitleColor?if_exists>
                    </#if>
                    <table width="450">
                        <tr>
                            <td>
                                <select id="selectStoreTitleFont" name="selectStoreTitleFont">
                                    <#list storeFontTheme.storeFontTypeFontTitleList as storeFontTypeFontTitle>
                                        <option <#if storeFontTypeFontTitle.storeFontValue?if_exists.equals(ebayStore.storeTitleFontFace?if_exists) >selected</#if> value="${storeFontTypeFontTitle.storeFontValue?if_exists}">${storeFontTypeFontTitle.storeFontName?if_exists}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                <select id="selectStoreTitleFontSize" name="selectStoreTitleFontSize">
                                    <#list storeFontTheme.storeFontSizeTitleList as storeFontSizeTitle>
                                        <option <#if storeFontSizeTitle.storeFontSizeValue?if_exists.equals(ebayStore.storeTitleFontFaceSize?if_exists) >selected</#if> value="${storeFontSizeTitle.storeFontSizeValue?if_exists}">${storeFontSizeTitle.storeFontSizeName?if_exists}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                #<input id="storeTitleFontColor" type="text" size="10" name="storeTitleFontColor" value="${storeTitleColor?if_exists}"/>
                            </td>
                        </tr>
                    </table>
                    </#if>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Store description</td>
              <td valign="middle">
              <#if storeFontTheme?exists>
                    <#if ebayStore.storeDescColor?exists>
                        <#assign storeDescColor = ebayStore.storeDescColor?if_exists>
                    <#else>
                        <#assign storeDescColor = storeFontTheme.storeFontTypeDescColor?if_exists>
                    </#if>
                    <table width="450">
                        <tr>
                            <td>
                                <select id="selectStoreDescFont" name="selectStoreDescFont">
                                    <#list storeFontTheme.storeFontTypeFontDescList as storeFontTypeFontDesc>
                                        <option <#if storeFontTypeFontDesc.storeFontValue?if_exists.equals(ebayStore.storeDescFontFace?if_exists) >selected</#if> value="${storeFontTypeFontDesc.storeFontValue?if_exists}">${storeFontTypeFontDesc.storeFontName?if_exists}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                <select id="selectStoreDescFontSize" name="selectStoreDescFontSize">
                                    <#list storeFontTheme.storeDescSizeList as storeDescSize>
                                        <option <#if storeDescSize.storeFontSizeValue?if_exists.equals(ebayStore.storeDescSizeCode?if_exists) >selected</#if> value="${storeDescSize.storeFontSizeValue?if_exists}">${storeDescSize.storeFontSizeName?if_exists}</option>
                                    </#list>
                                </select>
                            </td>
                            <td>
                                #<input id="storeDescFontColor" type="text" size="10" name="storeDescFontColor" value="${storeDescColor?if_exists}"/>
                            </td>
                        </tr>
                    </table>
                    </#if>
              </td>
            </tr>
            </#if>
            
            <tr>
              <td class="label" align="right" valign="middle">Store Header Display :</td>
              <td valign="middle">
                    <select id="selectStoreCustomHeaderLayout" name="selectStoreCustomHeaderLayout">
                        <#list ebayStore.storeCustomHeaderLayoutList as storeCustomHeaderLayout>
                               <option <#if storeCustomHeaderLayout.storeCustomHeaderLayoutValue?if_exists.equals(ebayStore.storeCustomHeaderLayout?if_exists) >selected</#if> value="${storeCustomHeaderLayout.storeCustomHeaderLayoutValue?if_exists}">${storeCustomHeaderLayout.storeCustomHeaderLayoutValue?if_exists}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle"></td>
              <td valign="middle">
                    <textarea rows="8" cols="40" name="storeCustomHeader">
                    ${ebayStore.storeCustomHeader?if_exists}</textarea>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Store Header Style :</td>
              <td valign="middle">
                    <select id="selectStoreHeaderStyle" name="selectStoreHeaderStyle">
                        <#list ebayStore.storeHeaderStyleList as storeHeaderStyle>
                               <option <#if storeHeaderStyle.storeHeaderStyleValue?if_exists.equals(ebayStore.storeHeaderStyle?if_exists) >selected</#if> value="${storeHeaderStyle.storeHeaderStyleValue?if_exists}">${storeHeaderStyle.storeHeaderStyleValue?if_exists}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <#--tr>
              <td class="label" align="right" valign="middle">Home Page :</td>
              <td valign="middle">
                    <input type="text" id="homePage" name="homePage" value="${ebayStore.storeHomePage?if_exists}"/>
              </td>
            </tr-->
           
            <tr>
              <td class="label" align="right" valign="middle">Store Item List Display :</td>
              <td valign="middle">
                    <select id="selectStoreItemLayout" name="selectStoreItemLayout">
                        <#list ebayStore.storeItemLayoutList as storeItemLayout>
                               <option <#if storeItemLayout.storeItemLayoutValue?if_exists.equals(ebayStore.storeItemLayoutSelected?if_exists) >selected</#if> value="${storeItemLayout.storeItemLayoutValue?if_exists}">${storeItemLayout.storeItemLayoutValue?if_exists}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Store Item Sort Order :</td>
              <td valign="middle">
                    <select id="selectStoreItemSortOrder" name="selectStoreItemSortOrder">
                        <#list ebayStore.storeItemSortOrderList as storeItemSortOrder>
                               <option <#if storeItemSortOrder.storeItemSortLayoutValue?if_exists.equals(ebayStore.storeItemSortOrderSelected?if_exists) >selected</#if> value="${storeItemSortOrder.storeItemSortLayoutValue?if_exists}">${storeItemSortOrder.storeItemSortLayoutValue?if_exists}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            
            <#--tr>
              <td class="label" align="right" valign="middle">Custom Listing Header Display :</td>
              <td valign="middle">
                    <select id="storeCustomListingHeaderDisplay" name="storeCustomListingHeaderDisplay">
                        <#list ebayStore.storeCustomListingHeaderDisplayList as storeCustomListingHeaderDisplay>
                               <option <#if storeCustomListingHeaderDisplay.storeCustomHeaderLayoutValue?if_exists.equals(ebayStore.storeCustomListingHeaderDisplayValue?if_exists) >selected</#if> value="${storeCustomListingHeaderDisplay.storeCustomHeaderLayoutValue?if_exists}">${storeCustomListingHeaderDisplay.storeCustomHeaderLayoutValue?if_exists}</option>
                        </#list>
                    </select>
              </td>
            </tr-->
            <tr>
              <td class="label" align="right" valign="middle">Store Merch Display :</td>
              <td valign="middle">
                    <select id="storeMerchDisplay" name="storeMerchDisplay">
                        <#list ebayStore.storeMerchDisplayList as storeMerchDisplay>
                               <option <#if storeMerchDisplay.merchDisplayCodeValue?if_exists.equals(ebayStore.storeMerchDisplay?if_exists) >selected</#if> value="${storeMerchDisplay.merchDisplayCodeValue?if_exists}">${storeMerchDisplay.merchDisplayCodeValue?if_exists}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle">Store Subscription Level :</td>
              <td valign="middle">
                    <select id="storeMerchDisplay" name="storeMerchDisplay">
                        <#list ebayStore.storeSubscriptionLevelList as storeSubscriptionLevel>
                               <option <#if storeSubscriptionLevel.storeSubscriptionLevelCodeValue?if_exists.equals(ebayStore.storeSubscriptionLevel?if_exists) >selected</#if> value="${storeSubscriptionLevel.storeSubscriptionLevelCodeValue?if_exists}">${storeSubscriptionLevel.storeSubscriptionLevelCodeValue?if_exists}</option>
                        </#list>
                    </select>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="middle"></td>
              <td valign="middle">
                    <input type="submit" value="${uiLabelMap.CommonEdit}" name="submitButton" class="smallSubmit">
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
  
  </div>
 </div>
 