#!/bin/sh
#####################################################################
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
#####################################################################
#
# This shell script will will run ./ant apply patches on all components
#              present in the hot-deploy directory

    if [ -f "../ofbiz.patch" ]; then
    patch -p0 <../ofbiz.patch
    fi

    for f in hot-deploy/*
    do
        if [ "$f" != "hot-deploy/README.txt" ]; then
        if [ -f "$f/patches/applications.patch" ]; then
                echo apply patches for component $f
                cd $f
            ../../ant apply-ofbiz-patches
            echo return code $?
        cd ../../
        fi
        fi
    done

exit;

