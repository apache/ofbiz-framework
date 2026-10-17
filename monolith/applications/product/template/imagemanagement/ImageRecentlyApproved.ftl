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
        
<table cellspacing="0" class="basic-table">
    <td style="vertical-align:top;">
        <table>
            <#if approved_0?has_content>
                <div class="label">${date0}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_0 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_0}&date2=${timeStampDate2_0}&showDate=${date0}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_0[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_1?has_content>
                <div class="label">${date1}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_1 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_1}&date2=${timeStampDate2_1}&showDate=${date1}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_1[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_2?has_content>
                <div class="label">${date2}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_2 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_2}&date2=${timeStampDate2_2}&showDate=${date2}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_2[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_3?has_content>
                <div class="label">${date3}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_3 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_3}&date2=${timeStampDate2_3}&showDate=${date3}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_3[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_4?has_content>
                <div class="label">${date4}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_4 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_4}&date2=${timeStampDate2_4}&showDate=${date4}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_4[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_5?has_content>
                <div class="label">${date5}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_5 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_5}&date2=${timeStampDate2_5}&showDate=${date5}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_5[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_6?has_content>
                <div class="label">${date6}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_6 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_6}&date2=${timeStampDate2_6}&showDate=${date6}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_6[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_7?has_content>
                <div class="label">${date7}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_7 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_7}&date2=${timeStampDate2_7}&showDate=${date7}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_7[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_8?has_content>
                <div class="label">${date8}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_8 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_8}&date2=${timeStampDate2_8}&showDate=${date8}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_8[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_9?has_content>
                <div class="label">${date9}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_9 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_9}&date2=${timeStampDate2_9}&showDate=${date9}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_9[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_10?has_content>
                <div class="label">${date10}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_10 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_10}&date2=${timeStampDate2_10}&showDate=${date10}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_10[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_11?has_content>
                <div class="label">${date11}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_11 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_11}&date2=${timeStampDate2_11}&showDate=${date11}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_11[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_12?has_content>
                <div class="label">${date12}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_12 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_12}&date2=${timeStampDate2_12}&showDate=${date12}</@ofbizUrl>" class="test">${show.productId}</a> - ${time_12[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
    <td style="vertical-align:top;">
        <table>
            <#if approved_13?has_content>
                <div class="label">${date13}</div>
                <br/>
                <#assign alt_row = false>
                <#list approved_13 as show>
                    <tr>
                        <td>
                            <a href="<@ofbizUrl>ListImageRecentlyApproved?productId=${show.productId}&date1=${timeStampDate1_13}&date2=${timeStampDate2_13}&showDate=${date13}</@ofbizUrl>" class="test">${show.productId}</a> - ${time13[show_index]}
                        </td>
                    </tr>
                </#list>
            </#if>
        </table>
    </td>
</table>
