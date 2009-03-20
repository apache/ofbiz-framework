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

<#macro renderSectionBegin></#macro>
<#macro renderSectionEnd>
</#macro>
<#macro renderContainerBegin id style autoUpdateLink autoUpdateInterval>
<#if autoUpdateLink?has_content>
<script type="text/javascript">ajaxUpdateAreaPeriodic('${id}', '${autoUpdateLink}', '', '${autoUpdateInterval}');</script>
</#if>
<div<#if id?hasContent> id="${id}"</#if><#if style?hasContent> class="${style}"</#if>>
</#macro>
<#macro renderContainerEnd></div></#macro>
<#macro renderContentBegin></#macro>
<#macro renderContentBody></#macro>
<#macro renderContentEnd></#macro>
<#macro renderSubContentBegin></#macro>
<#macro renderSubContentBody></#macro>
<#macro renderSubContentEnd></#macro>

<#macro renderHorizontalSeparator id style><hr<#if id?hasContent> id="${id}"</#if><#if style?hasContent> class="${style}"</#if>/></#macro>
<#macro renderLabel text id style><#if text?exists><#if id?has_content || style?has_content><span<#if id?hasContent> id="${id}"</#if><#if style?hasContent> class="${style}"</#if>></#if>${text}<#if id?has_content || style?has_content></span></#if></#if></#macro>
<#macro renderLink></#macro>
<#macro renderImage></#macro>

<#macro renderContentFrame></#macro>
<#macro renderScreenletBegin id><div class="screenlet"<#if id?hasContent> id="${id}"</#if>></#macro>
<#macro renderScreenletSubWidget></#macro>
<#macro renderScreenletEnd></div></div></#macro>

