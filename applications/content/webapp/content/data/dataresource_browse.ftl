<#assign INDENT_PIXS=18 />
<script type="text/javascript">
function submitBrowse(id) {
    document.brws.dataCategoryId.value=id;
    document.brws.submit();
}
</script>
<div class="apptitle">&nbsp;DataCategories&nbsp;</div>
        <@showResponseTree masterNode=masterNode depth=0 />
<br/>
<div class="inputBox">
<a href="<@ofbizUrl>EditDataCategory</@ofbizUrl>">New DataCategory</a>
</div>

<form name="brws" method="post" action="<@ofbizUrl>BrowseDataResource</@ofbizUrl>">
<input type="hidden" name="viewSize" value="10" />
<input type="hidden" name="viewIndex" value="0" />
<input type="hidden" name="dataCategoryId" value="" />
</form>

<#macro showResponseTree masterNode  depth=0 >
   <#if masterNode.id?exists && (depth >= 0 )>
        <#if currentDataCategoryId?exists && ( masterNode.id == currentDataCategoryId)>
            <#local bgColor="lightblue">
            <#local cls="headerButtonLeftSelected">
            <#local styleText="font-family: Verdana, Arial, Helvetica, sans-serif;
font-size: 10px;
font-weight: bold;
text-decoration: none;
text-align: left;
color: #000099;
background-color: #D4D0C8;
padding-right: 10px;
padding-left: 10px;
">

        <#else>
            <#local bgColor="white">
            <#local cls="headerButtonLeft">
            <#local styleText="font-family: Verdana, Arial, Helvetica, sans-serif;
font-size: 10px;
font-weight: bold;
text-decoration: none;
text-align: left;
color: #000099;
background-color: #B4B0AA;
padding-right: 10px;
padding-left: 10px;
">

        </#if>
       <table border="0" cellpadding="0" cellspacing="0" style="${styleText}" width="100%"><tr>
<!--
       <table border="0" cellpadding="0" cellspacing="0" class="${cls}"><tr>
-->
        <#local indent = (depth - 1) * INDENT_PIXS />
        <td width="${indent}" > </td>
        <td width="${INDENT_PIXS}"><#if !masterNode.kids?exists && (masterNode.count > 0) >+<#else>&nbsp;</#if></td>
        <th>
        <a href="javascript:submitBrowse('${masterNode.id}')">
        ${masterNode.name?if_exists}</a>
        </th>
        </tr>
        </table>
    </#if>

    <#if masterNode.kids?exists>
        <#local kids = masterNode.kids/>
        <#local newDepth = depth + 1 />
        <#list kids as node>
             <@showResponseTree masterNode=node depth=newDepth />
        </#list>
    </#if>

</#macro>
