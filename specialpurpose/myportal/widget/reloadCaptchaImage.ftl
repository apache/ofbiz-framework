<#-- For add Captcha Capture -->
<#assign fileName = Static["org.ofbiz.common.Captcha"].getCodeCaptcha(request,response)>
<#assign fileName = Static["org.ofbiz.common.Captcha"].CAPTCHA_FILE_NAME>

<img  src="<@ofbizContentUrl>/tempfiles/captcha/${fileName?if_exists}</@ofbizContentUrl>"/>
