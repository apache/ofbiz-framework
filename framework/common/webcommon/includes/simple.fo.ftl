<?xml version="1.0" encoding="UTF-8"?>
<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <fo:layout-master-set>
        <fo:simple-page-master master-name="main-page"
            margin-top="0.3in" margin-bottom="0.3in"
            margin-left="0.4in" margin-right="0.3in">
          <fo:region-body margin-top="1in" margin-bottom="0.5in"/>
            <fo:region-after extent="0.5in" />
            <fo:region-before extent="1in"/>
        </fo:simple-page-master>
    </fo:layout-master-set>
  
    <fo:page-sequence master-reference="main-page" font-size="9pt">
        <#-- Header -->
        <fo:static-content flow-name="xsl-region-before" font-size="8pt">
            <fo:table>
                <fo:table-column column-width="4.5in"/>
                <fo:table-column column-width="2in"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        <#if logoImageUrl?exists>
                            <fo:external-graphic src="${logoImageUrl}" overflow="hidden" height="40px"/>
                        </#if>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block font-weight="bold" space-after="0.03in"><#if titleProperty?exists>${titleProperty}<#else>${title?if_exists}</#if></fo:block>
                            <fo:block>${uiLabelMap.CommonUsername}: <#if userLogin?exists>${userLogin.userLoginId?if_exists}</#if></fo:block>
                            <fo:block>${uiLabelMap.CommonDate}: ${nowTimestamp?if_exists}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
            <fo:block white-space-collapse="false"> </fo:block> 
        </fo:static-content>
         
        <#-- Footer -->
        <fo:static-content flow-name="xsl-region-after" font-size="8pt">
            <fo:block text-align="center" border-top="thin solid black" padding="3pt">Page <fo:page-number/> of <fo:page-number-citation ref-id="theEnd"/></fo:block>
        </fo:static-content>
       
        <#-- Body -->
        <fo:flow flow-name="xsl-region-body">
${sections.render("body")}
            <fo:block id="theEnd"/>
        </fo:flow>
    </fo:page-sequence>
</fo:root>
