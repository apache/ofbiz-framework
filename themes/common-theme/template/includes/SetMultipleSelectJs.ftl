<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<#if asm_multipleSelect??> <#-- we check only this var and suppose the others are also present -->
<script type="application/javascript">
    jQuery(document).ready(function () {
        multiple = jQuery("#${asm_multipleSelect!}");

      <#if asm_title??>
          // set the dropdown "title" if??
          multiple.attr('title', '${asm_title}');
      </#if>

        multiple.select2({
          tags: true,
          multiple: true,
          lang: <#if userLogin??>'${userLogin.lastLocale!"en"}'<#else>"en"</#if>,
          width: "50%"
        });

      <#if asm_relatedField??> <#-- can be used without related field -->
          // track possible relatedField changes
          // on initial focus (focus-field-name must be asm_relatedField) or if the field value changes, select related multi values.
          typeValue = jQuery('#${asm_typeField}').val();
          jQuery("#${asm_relatedField}").one('focus', function () {
              selectMultipleRelatedValues('${asm_requestName}', '${asm_paramKey}', '${asm_relatedField}', '${asm_multipleSelect}', '${asm_type}', typeValue, '${asm_responseName}');
          });
          jQuery("#${asm_relatedField}").change(function () {
              selectMultipleRelatedValues('${asm_requestName}', '${asm_paramKey}', '${asm_relatedField}', '${asm_multipleSelect}', '${asm_type}', typeValue, '${asm_responseName}');
          });
          selectMultipleRelatedValues('${asm_requestName}', '${asm_paramKey}', '${asm_relatedField}', '${asm_multipleSelect}', '${asm_type}', typeValue, '${asm_responseName}');
      </#if>
    });
</script>

<style type="text/css">
    #${asm_multipleSelectForm}
    {
        width: ${asm_formSize!700}px
    ;
        position: relative
    ;
    }

    .asmListItem {
        width: ${asm_asmListItemPercentOfForm!95}%;
    }
</style>
</#if>
