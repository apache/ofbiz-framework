/*
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
*/

// Only once by session
if (sessionStorage.getItem("SetTimeZoneFromBrowser") === null || sessionStorage.getItem("SetTimeZoneFromBrowser") !== "done") {
    sessionStorage.setItem("SetTimeZoneFromBrowser", "done");
    var timezone = moment.tz.guess();
    $.ajax({
        url: "SetTimeZoneFromBrowser",
        type: "POST",
        async: false,
        data: "localeName=" + timezone,
        error: function(error) {
            if (error != "") {
                console.error("Error while setting user locale: ", error);
            }
        }
    });
}
