
<br/>
<h2>${text1}</h2>
<br/>
<#if text2?exists>
<h2>${text2}</h2>
</#if><br/>
<#if link1?exists>
<h2>1. <a href="${link1}" target="new1">${link1Text}</a></h2>
</#if>
<#if link2?exists>
<h2>2. <a href="${link2}" target="new2">${link2Text}</a></h2>
</#if>
<#if link3?exists>
<h2>3. <a href="${link3}" target="new3">${link3Text}</a></h2>
</#if>
