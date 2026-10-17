#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
###############################################################################

set -e

# Read a property value. Assumes all properties are single line and do not have spaces around the '=' sign or comments following the valuel.
function getPropertyValue
{
    echo "$1" | sed "/^$2=/!d; s///"
}

echo "Getting admin port and key..."
START_PROPERTIES_CONTENT=$(cat /ofbiz/framework/start/src/main/resources/org/apache/ofbiz/base/start/start.properties)

OFBIZ_ADMIN_PORT=$(getPropertyValue "$START_PROPERTIES_CONTENT" "ofbiz.admin.port")
echo Admin port: $OFBIZ_ADMIN_PORT;

OFBIZ_ADMIN_KEY=$(getPropertyValue "$START_PROPERTIES_CONTENT" "ofbiz.admin.key")
echo Admin key: $OFBIZ_ADMIN_KEY;

echo "Sending shutdown signal..."
echo "$OFBIZ_ADMIN_KEY:SHUTDOWN" | curl telnet://localhost:$OFBIZ_ADMIN_PORT
echo "Done"
