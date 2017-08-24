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
<#if layoutSettings.styleSheets?has_content>
  <#--layoutSettings.styleSheets is a list of style sheets -->
  <#list layoutSettings.styleSheets as styleSheet>
    <?xml-stylesheet type="text/xsl" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>"?>
  </#list>
</#if>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    font-family="${(layoutSettings.defaultFontFamily)?default("Helvetica, sans-serif")}"
    font-size="${(layoutSettings.defaultFontSize)?default("12pt")}">
  <fo:layout-master-set>
<#if layoutSettings.pageMasters?has_content>
  <#--layoutSettings.pageMasters is a list of fo page master element ftl templates -->
  <#list layoutSettings.pageMasters as pageMaster>
    <#include pageMaster/>
  </#list>
<#else>
  <#include "component://common-theme/template/includes/fo/Pm-11x17.fo.ftl"/>
  <#include "component://common-theme/template/includes/fo/Pm-iso216.fo.ftl"/>
  <#include "component://common-theme/template/includes/fo/Pm-legal.fo.ftl"/>
  <#include "component://common-theme/template/includes/fo/Pm-letter.fo.ftl"/>
</#if>
  </fo:layout-master-set>
  <#assign masterReference = (layoutSettings.masterReference)?default("letter-portrait")/>
  <fo:page-sequence master-reference="${masterReference}">
</#escape>
