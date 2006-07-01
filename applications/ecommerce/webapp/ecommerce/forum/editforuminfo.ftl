<#import "bloglib.ftl" as blog/>
<@blog.renderAncestryPath trail=ancestorList?default([]) endIndexOffset=1/>
${singleWrapper.renderFormString()}

