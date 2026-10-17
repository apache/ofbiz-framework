#!/usr/bin/env sh
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

# Syntax: eg ./pullPluginSource.sh bi

# Remove plugins dir in case of all plugins present (no .git)
if [ -d "plugins" ]
    then
        if [ ! -d "plugins/.git" ]
        then
            rm -rf plugins
        fi
fi

# Get the branch used in framework
branch=$(git branch --show-current)

# Clone and set if new else simply add
if [ ! -d "plugins/.git" ]
    then
        git clone --depth 1 --sparse --single-branch --branch $branch https://github.com/apache/ofbiz-plugins.git plugins
        cd plugins
        git sparse-checkout set "$1"
else
    cd plugins
    git sparse-checkout add "$1"
fi

cd ..

