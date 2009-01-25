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
<div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>UpdateLabelKey</@ofbizUrl>" name="UpdateLabelForm">
        <table class="basic-table" cellspacing="3">
            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td align="right"><b>${uiLabelMap.WebtoolsLabelManagerKey}</b></td>
                <td align="left">
                    <#if parameters.sourceKey?exists>
                        ${parameters.sourceKey}
                        <input type="hidden" name="key" value="${parameters.sourceKey}">
                        <input type="hidden" name="update_label" value="Y">
                    <#else>
                        <input type="text" name="key" size="70" value="${parameters.key?if_exists}" >
                        <input type="hidden" name="update_label" value="N">
                    </#if>
                </td>
            </tr>
            <tr>
                <td align="right"><b>${uiLabelMap.WebtoolsLabelManagerFileName}</b></td>
                <td align="left">
                    <#if parameters.sourceFileName?exists>
                        ${parameters.sourceFileName}
                        <input type="hidden" name="fileName" value="${parameters.sourceFileName}">
                    <#else>
                        <select name="fileName">
                            <#assign fileNames = fileNamesFound.keySet()>
                            <#list fileNames as fileName>
                              <option <#if parameters.fileName?exists && parameters.fileName == fileName>selected="selected"</#if> value="${fileName}">${fileName}</option>
                            </#list>
                        </select>
                    </#if>
                </td>
            </tr>
            <tr>
                <td colspan="2" align="center">
                    <input type="submit" value="${uiLabelMap.CommonBack}"/>
                    <#if parameters.sourceKey?exists>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" name="confirm"/>
                        <input type="submit" value="${uiLabelMap.WebtoolsLabelManagerRemove}" name="removeLabel"/>
                    <#else>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" name="confirm"/>
                    </#if>
                </td>
            </tr>
            <#list localesFound as localeFound>
                <#if parameters.sourceKey?exists>
                    <#assign labelVal = (label.getLabelValue(localeFound))?if_exists>
                <#else>
                    <#assign labelVal = "">
                </#if>
                <#assign showLocale = true>
                <#if parameters.labelLocaleName?exists && parameters.labelLocaleName != "" && parameters.labelLocaleName != localeFound>
                    <#assign showLocale = false>
                </#if>
                <#if showLocale == true>
                    <tr>
                        <#assign locale = Static["org.ofbiz.base.util.UtilMisc"].parseLocale(localeFound)?if_exists/>
                        <#if locale?exists && locale?has_content>
                            <#assign langAttr = localeFound.toString()?replace("_", "-")>
                            <#assign langDir = "ltr">
                            <#if "ar.iw"?contains(langAttr?substring(0, 2))>
                                <#assign langDir = "rtl">
                            </#if>
                            <td align="right" lang="${langAttr}" dir="${langDir}">
                                <b>${locale.getDisplayName(locale)}</b>
                            </td>
                        <#else>
                            <td align="right"><b>${localeFound}</b></td>
                        </#if>
                        <td align="left">
                            <input type="hidden" name="localeNames" value="${localeFound}">
                            <input type="text" name="localeValues" size="70" value="${labelVal?if_exists}">
                        </td>
                    </tr>
                </#if>
            </#list>
            <tr>
                <td colspan="2" align="center">
                    <input type="submit" value="${uiLabelMap.CommonBack}"/>
                    <#if parameters.sourceKey?exists>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" name="confirm"/>
                        <input type="submit" value="${uiLabelMap.WebtoolsLabelManagerRemove}" name="removeLabel"/>
                    <#else>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" name="confirm"/>
                    </#if>
                </td>
            </tr>
        </table>
    </form>
</div>
