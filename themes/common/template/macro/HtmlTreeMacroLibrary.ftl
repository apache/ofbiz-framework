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

<#macro renderNodeBegin style="">
<#if style?has_content><ul class="${style}"></#if>
<li><#rt/>
</#macro>

<#macro renderLastElement style="">
<ul<#if style?has_content> class="${style}"</#if>>
<#rt/>
</#macro>
  
<#macro renderNodeEnd processChildren="" isRootNode="">
<#if processChildren?has_content && processChildren>
</ul><#lt/>
</#if>
</li><#rt/>
<#if isRootNode?has_content && isRootNode>
</ul><#lt/>
</#if> 
</#macro>
 
<#macro renderLabel id="" style="" labelText="">
<span<#if id?has_content> id="${id}"</#if><#if style?has_content> class="${style}"</#if>><#rt/>
<#if labelText?has_content>${labelText}</#if><#rt/>
</span>    
</#macro>

<#macro formatBoundaryComment boundaryType widgetType widgetName>
<!-- ${boundaryType}  ${widgetType}  ${widgetName} -->
</#macro>

<#macro renderLink id="" style="" name="" title="" targetWindow="" linkUrl="" linkText="" imgStr="">
<a<#if id?has_content> id="${id}"</#if><#rt/>
<#if style?has_content> class="${style}"</#if><#rt/>
<#if name?has_content> name="${name}"</#if><#rt/>
<#if title?has_content> title="${title}"</#if><#rt/>
<#if targetWindow?has_content> target="${targetWindow}</#if><#if linkUrl?has_content> href="${linkUrl}"<#else> href="javascript:void(0);"</#if>><#rt/>
<#if imgStr?has_content>${imgStr}<#elseif linkText?has_content/>${linkText}<#else>&nbsp;</#if></a><#rt/>
</#macro>

<#macro renderImage src="" id="" style="" wid="" hgt="" border="" alt="" urlString="">
<#if src?has_content>
<img <#if id?has_content>id="${id}"</#if><#if style?has_content> class="${style}"</#if><#if wid?has_content> width="${wid}"</#if><#if hgt?has_content> height="${hgt}"</#if><#if border?has_content> border="${border}"</#if> alt="<#if alt?has_content>${alt}</#if>" src="${urlString}" /><#rt/>
</#if>
</#macro>
 