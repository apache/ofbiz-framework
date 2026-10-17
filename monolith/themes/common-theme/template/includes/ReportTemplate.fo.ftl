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
      <fo:simple-page-master master-name="main-page"
          page-width="8.5in" page-height="11in"
          margin-top="0.4in" margin-bottom="0.4in"
          margin-left="0.6in" margin-right="0.4in">
        <#-- main body -->
        <fo:region-body margin-top="1.5in" margin-bottom="0.4in"/>
        <#-- the header -->
        <fo:region-before extent="1.2in"/>
        <#-- the footer -->
        <fo:region-after extent="0.4in"/>
      </fo:simple-page-master>
      <fo:simple-page-master master-name="main-page-landscape"
          page-width="11in" page-height="8.5in"
          margin-top="0.4in" margin-bottom="0.4in"
          margin-left="0.6in" margin-right="0.4in">
        <#-- main body -->
        <fo:region-body margin-top="1.2in" margin-bottom="0.4in"/>
        <#-- the header -->
        <fo:region-before extent="1.2in"/>
        <#-- the footer -->
        <fo:region-after extent="0.4in"/>
      </fo:simple-page-master>
    </fo:layout-master-set>

    <fo:page-sequence master-reference="${pageLayoutName?default("main-page")}">

      <#-- Header -->
      <#-- The elements it it are positioned using a table composed by one row
           composed by two cells (each 50% of the total table that is 100% of the page):
           in the left side cell the "topLeft" template is included
           in the right side cell the "topRight" template is included
      -->
      <#if sections.get("topLeft")?has_content || sections.get("topRight")?has_content>
        <fo:static-content flow-name="xsl-region-before">
          <fo:table table-layout="fixed" width="100%">
            <fo:table-column column-number="1" column-width="proportional-column-width(50)"/>
            <fo:table-column column-number="2" column-width="proportional-column-width(50)"/>
            <fo:table-body>
              <fo:table-row>
                <#if sections.get("topLeft")?has_content>
                  <fo:table-cell>
                    ${sections.render("topLeft")}
                  </fo:table-cell>
                </#if>
                <#if sections.get("topRight")?has_content>
                  <fo:table-cell>
                    ${sections.render("topRight")}
                  </fo:table-cell>
                </#if>
              </fo:table-row>
            </fo:table-body>
          </fo:table>
        </fo:static-content>
      </#if>

      <#-- the footer -->
      <fo:static-content flow-name="xsl-region-after">
        <fo:block font-size="10pt" text-align="center" space-before="10pt">
          ${uiLabelMap.CommonPage}
          <fo:page-number/> ${uiLabelMap.CommonOf}
          <fo:page-number-citation ref-id="theEnd"/>
        </fo:block>
      </fo:static-content>

      <#-- the body -->
      <fo:flow flow-name="xsl-region-body">
      ${sections.render("body")}
          <fo:block id="theEnd"/>  <#-- marks the end of the pages and used to identify page-number at the end -->
      </fo:flow>
    </fo:page-sequence>
  </fo:root>
</#escape>
