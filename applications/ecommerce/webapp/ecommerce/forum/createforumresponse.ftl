<#import "bloglib.ftl" as blog/>
<div class="screenlet" >
<div style="margin:10px;" >
<#--
<@blog.renderSiteAncestryPath trail=siteAncestorList?default([])/>
<@blog.renderAncestryPath trail=trailList startIndex=1/>
-->

    ${singleWrapper.renderFormString()}
</div>
</div>

