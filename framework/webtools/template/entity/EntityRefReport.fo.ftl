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
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" font-family="Helvetica" font-size="8pt">
  <fo:layout-master-set>
    <fo:simple-page-master master-name="letter-portrait" page-height="11in" page-width="8.5in"
        margin-top="0.5in" margin-bottom="0.5in" margin-left="0.5in" margin-right="0.25in">
      <fo:region-body margin-top="1.5in" margin-bottom="1in"/>
      <fo:region-before precedence="true" extent="1.5in"/>
      <fo:region-after precedence="true" extent="0.5in"/>
      </fo:simple-page-master>
  </fo:layout-master-set>

  <#-- bookmark section -->
  <fo:bookmark-tree>
    <#list packagesList as package>
      <#assign packageName = package.packageName/>
      <fo:bookmark internal-destination="${packageName}">
        <fo:bookmark-title>${packageName}</fo:bookmark-title>
        <#list package.entitiesList as entity>
          <fo:bookmark internal-destination="${entity.entityName}">
            <fo:bookmark-title>${entity.entityName}</fo:bookmark-title>
          </fo:bookmark>
        </#list>
      </fo:bookmark>
    </#list>
  </fo:bookmark-tree>

  <#-- report section -->
  <#list packagesList as package>
    <#assign packageName = package.packageName/>
    <#assign newPackage = true/>
    <#list package.entitiesList as entity>

      <fo:page-sequence master-reference="letter-portrait">
        <#-- header -->
        <fo:static-content flow-name="xsl-region-before">
          <fo:block font-size="12pt" text-align="center">
            ${uiLabelMap.WebtoolsEntityReference}
          </fo:block>
          <fo:block font-size="14pt" text-align="center" margin-bottom="14pt">
            ${packageName}
          </fo:block>
          <fo:block font-size="14pt" text-align="center" margin-bottom="8pt">
            ${entity.entityName}<#if entity.plainTableName?has_content> | ${uiLabelMap.WebtoolsTable}: ${entity.plainTableName}</#if>
          </fo:block>
          <#if entity.description?has_content && !entity.description.equalsIgnoreCase("NONE")>
            <fo:block text-align="center" margin-bottom="8pt">${entity.description}</fo:block>
          </#if>
          <#if entity.location?has_content>
            <fo:block text-align="center" margin-bottom="8pt">${entity.location}</fo:block>
          </#if>
        </fo:static-content>

        <#-- footer -->
        <fo:static-content flow-name="xsl-region-after">
            <fo:block text-align="center" border-top="thin solid black">Copyright (c) 2001-${nowTimestamp?string("yyyy")} The Apache Software Foundation</fo:block>
            <fo:block text-align="center">${uiLabelMap.CommonPage} <fo:page-number/></fo:block>
        </fo:static-content>

        <#-- body -->
        <fo:flow flow-name="xsl-region-body">
          <#if newPackage>
            <fo:block id="${packageName}"/>
            <#assign newPackage = false/>
          </#if>
          <fo:block id="${entity.entityName}"/>
          <#-- entity fields -->
          <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse"
                margin-bottom="14pt">
              <fo:table-column column-width="27%"/>
              <fo:table-column column-width="31%"/>
              <fo:table-column column-width="9%"/>
              <fo:table-column column-width="16%"/>
              <fo:table-column column-width="17%"/>
            <fo:table-header border-bottom-style="solid">
              <fo:table-row font-weight="bold">
                <fo:table-cell><fo:block>${uiLabelMap.WebtoolsJavaName}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block>${uiLabelMap.WebtoolsDbName}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block>${uiLabelMap.WebtoolsFieldType}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block>${uiLabelMap.WebtoolsJavaType}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block>${uiLabelMap.WebtoolsSqlType}</fo:block></fo:table-cell>
              </fo:table-row>
            </fo:table-header>
            <fo:table-body>
              <#list entity.javaNameList as javaName>
                <fo:table-row>
                  <fo:table-cell padding="2pt">
                    <fo:block font-weight="bold">${javaName.name}<#if javaName.isPk> (pk)</#if></fo:block>
                    <#if javaName.description?has_content>
                      <fo:block>${javaName.description}</fo:block>
                    </#if>
                    <#if javaName.encrypted>
                      <fo:block>[${uiLabelMap.WebtoolsEncrypted}]</fo:block>
                    </#if>
                  </fo:table-cell>
                  <fo:table-cell padding="2pt">
                    <fo:block>${javaName.colName}</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="2pt">
                    <fo:block>${javaName.type}</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="2pt">
                    <fo:block>${javaName.javaType?default(uiLabelMap.WebtoolsNotFound)}</fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="2pt">
                    <fo:block>${javaName.sqlType?default(uiLabelMap.WebtoolsNotFound)}</fo:block>
                  </fo:table-cell>
                </fo:table-row>
              </#list>
            </fo:table-body>
          </fo:table>
          <#if entity.relationsList?has_content>
            <#-- entity relations -->
            <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse"
                margin-bottom="14pt">
              <fo:table-column column-width="proportional-column-width(1)"/>
              <fo:table-column column-width="proportional-column-width(1)"/>
              <fo:table-header border-bottom-style="solid">
                <fo:table-row font-weight="bold">
                  <fo:table-cell><fo:block>${uiLabelMap.WebtoolsRelation}</fo:block></fo:table-cell>
                  <fo:table-cell><fo:block>${uiLabelMap.WebtoolsRelationType}</fo:block></fo:table-cell>
                </fo:table-row>
              </fo:table-header>
              <fo:table-body>
                <#list entity.relationsList as relation>
                  <fo:table-row>
                    <fo:table-cell padding="2pt">
                      <fo:block font-weight="bold"><#if relation.title?has_content>${relation.title} </#if>${relation.relEntity}</fo:block>
                      <#if relation.fkName?has_content>
                        <fo:block>${uiLabelMap.WebtoolsFKName}: ${relation.fkName}</fo:block>
                      </#if>
                      <#if relation.description?has_content>
                        <fo:block>${relation.description}</fo:block>
                      </#if>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt">
                      <fo:block>
                        ${relation.type}:
                        <#if relation.keysList?has_content>
                          <#list relation.keysList as keyList>
                            ${keyList.row}
                            <#if keyList.fieldName == keyList.relFieldName>
                              ${keyList.fieldName}
                            <#else>
                              ${keyList.fieldName} : ${keyList.relFieldName}
                            </#if>
                          </#list>
                        </#if>
                      </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                </#list>
              </fo:table-body>
            </fo:table>
          </#if>
          <#if entity.indexList?has_content>
            <#-- entity indexes -->
            <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse"
                margin-bottom="14pt">
              <fo:table-column column-width="proportional-column-width(1)"/>
              <fo:table-column column-width="proportional-column-width(1)"/>
              <fo:table-header border-bottom-style="solid">
                <fo:table-row font-weight="bold">
                  <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.WebtoolsIndexName}</fo:block></fo:table-cell>
                  <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.WebtoolsIndexFieldList}</fo:block></fo:table-cell>
                </fo:table-row>
              </fo:table-header>
              <fo:table-body>
                <#list entity.indexList as index>
                  <fo:table-row>
                    <fo:table-cell padding="2pt">
                      <fo:block font-weight="bold">${index.name}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt">
                      <fo:block>
                        <#list index.fieldNameList as fieldName>
                          ${fieldName}
                        </#list>
                      </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                </#list>
              </fo:table-body>
            </fo:table>
          </#if>
        </fo:flow>
      </fo:page-sequence>
    </#list>
  </#list>
</fo:root>
</#escape>
