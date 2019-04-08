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
<script type="text/javascript">
    jQuery(document).ready(function () {
        window.setInterval(function () {
            clock()
        }, 1000);
        var serverTimestamp = 0;
        var date;

        function clock() {
            if (jQuery("#${clockField}").text() === "${uiLabelMap.CommonServerHour}:") {
                waitSpinnerShow();
                serverTimestamp = getServiceResult("getServerTimestampAsLong")['serverTimestamp'];
                serverTimeZone = getServiceResult("getServerTimeZone")['serverTimeZone'];
                ;
                initTimeZone();
                date = new timezoneJS.Date(serverTimestamp, serverTimeZone);
                waitSpinnerHide();
            } else {
                date.setSeconds(date.getSeconds() + 1);
            }
            // dateFormat does not respect the timezone :/ Fortunately toString is what we want :)
            //jQuery("#${clockField}").text("${uiLabelMap.CommonServerHour}: "  + dateFormat(date, "yyyy-mm-dd HH:MM:ss"));
            jQuery("#${clockField}").text("${uiLabelMap.CommonServerHour}: " + date.toString());
        }
    })
</script>