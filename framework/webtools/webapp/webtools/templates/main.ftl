<!doctype HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
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

${common.get("/includes/header.ftl")}
${common.get("/includes/appbar.ftl")}

<div class="centerarea">
  ${pages.get("/includes/appheader.ftl")}
  <div class="contentarea">
    <div style='border: 0; margin: 0; padding: 0; width: 100%;'>
      <table style='border: 0; margin: 0; padding: 0; width: 100%;' cellpadding='0' cellspacing='0'>
        <tr>
          <#if page.leftbar?exists>${pages.get(page.leftbar)}</#if>
          <td width='100%' valign='top' align='left'>
            ${common.get("/includes/messages.ftl")}
            ${pages.get(page.path)}
          </td>
          <#if page.rightbar?exists>${pages.get(page.rightbar)}</#if>
        </tr>
      </table>       
    </div>
    <div class='spacer'></div>
  </div>
</div>

${common.get("/includes/footer.ftl")}
