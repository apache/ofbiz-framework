
<pre>
The service : ${service.name}

The Context :
  <#list serviceContext.keySet() as ckey>
      ${ckey?if_exists} --> ${(serviceContext.get(ckey))?if_exists}
  </#list>

The Result :
  <#list serviceResult.keySet() as rkey>
      ${rkey?if_exists} --> ${(serviceResult.get(rkey))?if_exists}
  </#list>
</pre> 