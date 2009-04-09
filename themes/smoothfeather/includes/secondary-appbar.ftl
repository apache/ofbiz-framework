<#assign nowTimestamp = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>

<#if (requestAttributes.externalLoginKey)?exists><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey?if_exists></#if>
<#if (externalLoginKey)?exists><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey?if_exists></#if>
<#assign ofbizServerName = application.getAttribute("_serverId")?default("default-server")>
<#assign contextPath = request.getContextPath()>
<#assign displayApps = Static["org.ofbiz.base.component.ComponentConfig"].getAppBarWebInfos(ofbizServerName, "secondary")>

<#if userLogin?has_content>
  <ul>
    <#list displayApps as display>
      <#assign thisApp = display.getContextRoot()>
      <#assign permission = true>
      <#assign selected = false>
      <#assign permissions = display.getBasePermission()>
      <#list permissions as perm>
        <#if perm != "NONE" && !security.hasEntityPermission(perm, "_VIEW", session)>
          <#-- User must have ALL permissions in the base-permission list -->
          <#assign permission = false>
        </#if>
      </#list>
      <#if permission == true>
        <#if thisApp == contextPath || contextPath + "/" == thisApp>
          <#assign selected = true>
        </#if>
        <#assign thisURL = thisApp>
        <#if thisApp != "/">
          <#assign thisURL = thisURL + "/control/main">
        </#if>
        <li><a<#if selected> class="current-section"</#if> href="${thisURL}${externalKeyParam}" <#if uiLabelMap?exists> title="${uiLabelMap[display.description]}">${uiLabelMap[display.title]}<#else> title="${display.description}"> ${display.title}</#if></a></li>
      </#if>
    </#list>
  </ul>
</#if>