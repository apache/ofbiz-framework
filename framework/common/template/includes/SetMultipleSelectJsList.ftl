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
<#if asm_listField??> <#-- we check only this var and suppose the others are also present -->
  <#list asm_listField as row>
    <#if row.asm_multipleSelect??>
      <script type="text/javascript">
          jQuery(document).ready(function () {
              multiple = jQuery("#${row.asm_multipleSelect!}");

              <#if row.asm_title??>
                  // set the dropdown "title" if??
                  multiple.attr('title', '${row.asm_title}');
              </#if>

              // use asmSelect in Widget Forms
              multiple.asmSelect({
                  addItemTarget: 'top',
                  sortable: ${row.asm_sortable!'false'},
                  removeLabel: '${uiLabelMap.CommonRemove!'Remove'}'
                  //, debugMode: true
              });

              <#if row.asm_relatedField??> <#-- can be used without related field -->
                  // track possible relatedField changes
                  // on initial focus (focus-field-name must be asm_relatedField) or if the field value changes, select related multi values.
                  typeValue = jQuery('#${row.asm_typeField}').val();
                  jQuery("#${row.asm_relatedField}").one('focus', function () {
                      selectMultipleRelatedValues('${row.asm_requestName}', '${row.asm_paramKey}', '${row.asm_relatedField}', '${row.asm_multipleSelect}', '${row.asm_type}', typeValue, '${row.asm_responseName}');
                  });
                  jQuery("#${row.asm_relatedField}").change(function () {
                      selectMultipleRelatedValues('${row.asm_requestName}', '${row.asm_paramKey}', '${row.asm_relatedField}', '${row.asm_multipleSelect}', '${row.asm_type}', typeValue, '${row.asm_responseName}');
                  });
                  selectMultipleRelatedValues('${row.asm_requestName}', '${row.asm_paramKey}', '${row.asm_relatedField}', '${row.asm_multipleSelect}', '${row.asm_type}', typeValue, '${row.asm_responseName}');
              </#if>
          });
      </script>
    </#if>
  </#list>
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
