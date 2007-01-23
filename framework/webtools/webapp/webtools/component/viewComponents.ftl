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

<#assign components = Static["org.ofbiz.base.component.ComponentConfig"].getAllComponents()?if_exists/>
<div class="head3">Loaded Components:</div>
<#if (components?has_content)>
    <table cellpadding="2" cellspacing="0" border="1" width="100%">
        <tr>
            <td><div class="tableheadtext">Name</div></td>
            <td><div class="tableheadtext">Path</div></td>
            <td><div class="tableheadtext">Enabled</div></td>
            <td colspan="3"><div class="tableheadtext">WebApps (Name, Mount, Path)</div></td>
        </tr>
        <#list components as component>
            <tr>
                <td><div class="tabletext">${component.getComponentName()?if_exists}</div></td>
                <td><div class="tabletext">${component.getRootLocation()?if_exists}</div></td>
                <td><div class="tabletext">${component.enabled()?string?if_exists}</div></td>
                <#assign webinfos = component.getWebappInfos()?if_exists/>
                <#if (webinfos?has_content)>
                    <td>
                        <table cellpadding="2" cellspacing="0" border="0" width="100%">                           
                            <#list webinfos as webinfo>
                                <tr>
                                    <td><div class="tabletext">${webinfo.getName()?if_exists}</div></td>
                                    <td><div class="tabletext">${webinfo.getContextRoot()?if_exists}</div></td>
                                    <td><div class="tabletext">${webinfo.getLocation()?if_exists}</div></td>
                                </tr>
                            </#list>
                        </table>
                    </td>
                <#else>
                    <td>&nbsp;</td>
                </#if>
        </#list>
    </table>
<#else>
    <div class="tabletext">No components loaded.</div>
</#if>