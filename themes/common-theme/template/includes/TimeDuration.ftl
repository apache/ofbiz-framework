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
<script type="application/javascript">
    function setTimeDuration() {
        var years = window.document.getElementsByName("years")[0].value;
        var weeks = window.document.getElementsByName("weeks")[0].value;
        var days = window.document.getElementsByName("days")[0].value;
        var hours = window.document.getElementsByName("hours")[0].value;
        var minutes = window.document.getElementsByName("minutes")[0].value;
        var seconds = window.document.getElementsByName("seconds")[0].value;
        var millis = window.document.getElementsByName("millis")[0].value;
        var duration = 0;
        duration += years == null ? 0 : years * 31536000000;
        duration += weeks == null ? 0 : weeks * 604800000;
        duration += days == null ? 0 : days * 86400000;
        duration += hours == null ? 0 : hours * 3600000;
        duration += minutes == null ? 0 : minutes * 60000;
        duration += seconds == null ? 0 : seconds * 1000;
        duration += millis == null ? 0 : millis;
        set_duration_value(duration);
    }
</script>
<form name="TimeDuration" action="javascript:setTimeDuration()">
  <table cellspacing="0" class="basic-table">
    <tr>
      <td class="label">${uiLabelMap.CommonYear}</td>
      <td><input type="text" name="years" size="4" maxlength="4"/></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonWeek}</td>
      <td><input type="text" name="weeks" size="4" maxlength="2"/></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonDay}</td>
      <td>
        <select name="days">
          <#list 0..7 as days>
            <option value="${days}">${days}</option>
          </#list>
        </select>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonHour}</td>
      <td>
        <select name="hours">
          <#list 0..23 as hours>
            <option value="${hours}">${hours}</option>
          </#list>
        </select>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonMinute}</td>
      <td>
        <select name="minutes">
          <#list 0..59 as minutes>
            <option value="${minutes}">${minutes}</option>
          </#list>
        </select>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonSecond}</td>
      <td>
        <select name="seconds">
          <#list 0..59 as seconds>
            <option value="${seconds}">${seconds}</option>
          </#list>
        </select>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonMilliSecond}</td>
      <td><input type="text" name="millis" size="4" maxlength="4"/></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <input type="submit" value="${uiLabelMap.CommonSet}"/>
      </td>
    </tr>
  </table>
</form>
