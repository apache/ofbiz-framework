<#if requestAttributes._ERROR_MESSAGE_?exists>
<br/><div class='errorMessage'>${requestAttributes._ERROR_MESSAGE_}</div><br/>
<#else>
    <#if trailList?exists>
        <#assign indent = "">
        <#assign csv = "">
        <#assign counter = 1>
        <#assign len = trailList?size>
        <#list trailList as pair>
        <#if 0 < csv?length >
            <#assign csv = csv + ","/>
        </#if>
        <#assign csv = csv + pair[0]?if_exists/>
            <#if counter < len>
        ${indent}
        ${pair[0]?if_exists} - ${pair[1]?if_exists}
        <a class="tabButton" href="<@ofbizUrl>ViewBlog?contentId=${pair[0]?if_exists}&nodeTrailCsv=${csv?if_exists}"></@ofbizUrl>${uiLabelMap.CommonView}</a> <br/> 
            <#assign indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
            <#else>
        <hr>
        <u>${uiLabelMap.EcommerceAddResponseFor}${pair[0]?if_exists} - ${pair[1]?if_exists}:</u><br/>
            </#if>
            <#assign counter = counter + 1>
        </#list>
    
        <#if dataResourceId?exists>
            <br/>
            <img src="<@ofbizUrl>img?imgId=${dataResourceId}</@ofbizUrl>" />
        </#if>
        <br/>
    </#if>
    ${singleWrapper.renderFormString()}
<br/>
</#if>

