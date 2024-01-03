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


# Remove plugins dir in case of all plugins present
if [ -d "plugins" ]
    then
        if [ ! -d "plugins\.git" ]
        then
            rm -rf plugins
        fi
fi

# Clone if new else simply init sparse-checkout
if [ ! -d "plugins\.git" ]
    then
        git clone --filter=blob:none --sparse https://github.com/apache/ofbiz-plugins.git plugins
        cd plugins
else
    cd plugins
    # the documentation says init is deprecated but set does work here: https://git-scm.com/docs/git-sparse-checkout
    git sparse-checkout init --cone --sparse-index
fi

# Add the plugin
git sparse-checkout add "$1"

# Get the branch used in framework
cd ..
git branch --show-current > temp.txt
branch=$(cat temp.txt)
rm temp.txt

# By default the clone branch is trunk
if [ ! "$branch" = trunk ]
    then
        git switch -C "$branch"
fi
cd ..
