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
<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    <#-- inheritance -->
    <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>>
  <fo:layout-master-set>
    <fo:simple-page-master master-name="simple-portrait"
        page-width="8.5in" page-height="11in"
        margin-top="0.3in" margin-bottom="0.3in"
        margin-left="0.4in" margin-right="0.3in">
      <fo:region-body margin-top="1in" margin-bottom="0.5in"/>
      <fo:region-before extent="1in"/>
      <fo:region-after extent="0.5in"/>
    </fo:simple-page-master>
    <fo:simple-page-master master-name="simple-landscape"
        page-width="11in" page-height="8.5in"
        margin-top="0.3in" margin-bottom="0.3in"
        margin-left="0.4in" margin-right="0.3in">
      <fo:region-body margin-top="1in" margin-bottom="0.5in"/>
      <fo:region-before extent="1in"/>
      <fo:region-after extent="0.5in"/>
    </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="${pageLayoutName?default("simple-portrait")}" font-size="8pt">
    <#-- Header -->
    <#-- The elements it it are positioned using a table composed by one row
         composed by two cells (each 50% of the total table that is 100% of the page):
         in the left side cell we put the logo
         in the right side cell we put the title, username and date
    -->
    <fo:static-content flow-name="xsl-region-before" font-size="${headerFontSize?default("8pt")}">
      <fo:table table-layout="fixed">
        <fo:table-column column-number="1" column-width="proportional-column-width(50)"/>
        <fo:table-column column-number="2" column-width="proportional-column-width(50)"/>
        <fo:table-body>
          <fo:table-row>
            <fo:table-cell>
              <fo:block>
                <#if logoImageUrl??>
                  <fo:external-graphic src="${logoImageUrl}" overflow="hidden" height="40px"
                      content-height="scale-to-fit"/>
                </#if>
              </fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <#-- The title of the report -->
              <fo:block font-weight="bold" text-decoration="underline" space-after="0.03in">
                <#if titleProperty??>${uiLabelMap.get(titleProperty)}<#else>${title!}</#if>
              </fo:block>
              <#-- Username and date -->
              <fo:list-block provisional-distance-between-starts="1in">
                <fo:list-item>
                  <fo:list-item-label>
                    <fo:block font-weight="bold">${uiLabelMap.CommonUsername}</fo:block>
                  </fo:list-item-label>
                  <fo:list-item-body start-indent="body-start()">
                    <fo:block><#if userLogin??>${userLogin.userLoginId!}</#if></fo:block>
                  </fo:list-item-body>
                </fo:list-item>
                <fo:list-item>
                  <fo:list-item-label>
                    <fo:block font-weight="bold">${uiLabelMap.CommonDate}</fo:block>
                  </fo:list-item-label>
                  <fo:list-item-body start-indent="body-start()">
                    <fo:block>${nowTimestamp!}</fo:block>
                  </fo:list-item-body>
                </fo:list-item>
              </fo:list-block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </fo:static-content>

    <#-- Footer -->
    <fo:static-content flow-name="xsl-region-after" font-size="${footerFontSize?default("8pt")}">
      <fo:block text-align="center" border-top="thin solid black" padding="3pt">${uiLabelMap.CommonPage}
        <fo:page-number/> ${uiLabelMap.CommonOf}
        <fo:page-number-citation ref-id="theEnd"/>
      </fo:block>
    </fo:static-content>

    <#-- Body -->
    <fo:flow flow-name="xsl-region-body" font-size="${bodyFontSize?default("8pt")}">
      ${sections.render("body")}
      <fo:block id="theEnd"/>
    </fo:flow>
  </fo:page-sequence>
</fo:root>
</#escape>
