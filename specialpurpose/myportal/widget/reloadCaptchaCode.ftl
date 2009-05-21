<#assign idkey = Static["org.ofbiz.common.Captcha"].ID_KEY>

<input  type="hidden" value="${idkey?if_exists}" name="captchaCode"/>