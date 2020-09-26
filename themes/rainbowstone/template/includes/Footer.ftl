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
<div id="footer-offset"></div>
<div id="footer">
    <span>${nowTimestamp?datetime?string.short} - <a href="<@ofbizUrl>ListTimezones</@ofbizUrl>">${timeZone.getDisplayName(timeZone.useDaylightTime(), Static["java.util.TimeZone"].LONG, locale)}</a></span>
    <span>${uiLabelMap.CommonCopyright} (c) 2001-${nowTimestamp?string("yyyy")} 
        <a href="http://www.apache.org" target="_blank">The Apache Software Foundation</a>. ${uiLabelMap.CommonPoweredBy}
        <a href="http://ofbiz.apache.org" target="_blank">Apache OFBiz.</a> ${uiLabelMap.CommonRelease}
        <#include "ofbizhome://VERSION" ignore_missing=true/>
        <#include "ofbizhome://runtime/SvnInfo.ftl" ignore_missing=true/>
        <#include "ofbizhome://runtime/GitInfo.ftl" ignore_missing=true/>
    </span>
</div>
</div>
<#if layoutSettings.VT_FTR_JAVASCRIPT?has_content>
  <#list layoutSettings.VT_FTR_JAVASCRIPT as javaScript>
    <script type="application/javascript" src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>"></script>
  </#list>
</#if>
<@scriptTagsFooter/>
</body>
</html>
