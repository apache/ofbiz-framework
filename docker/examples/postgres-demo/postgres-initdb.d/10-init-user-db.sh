#!/bin/bash
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

set -e

psql -v ON_ERROR_STOP=1 --username "postgres" --dbname "postgres" <<-EOSQL
    CREATE USER $OFBIZ_POSTGRES_OFBIZ_USER WITH PASSWORD '$OFBIZ_POSTGRES_OFBIZ_PASSWORD';
    CREATE DATABASE $OFBIZ_POSTGRES_OFBIZ_DB;
    GRANT ALL PRIVILEGES ON DATABASE $OFBIZ_POSTGRES_OFBIZ_DB TO $OFBIZ_POSTGRES_OFBIZ_USER;

    CREATE USER $OFBIZ_POSTGRES_OLAP_USER WITH PASSWORD '$OFBIZ_POSTGRES_OLAP_PASSWORD';
    CREATE DATABASE $OFBIZ_POSTGRES_OLAP_DB;
    GRANT ALL PRIVILEGES ON DATABASE $OFBIZ_POSTGRES_OLAP_DB TO $OFBIZ_POSTGRES_OLAP_USER;

    CREATE USER $OFBIZ_POSTGRES_TENANT_USER WITH PASSWORD '$OFBIZ_POSTGRES_TENANT_PASSWORD';
    CREATE DATABASE $OFBIZ_POSTGRES_TENANT_DB;
    GRANT ALL PRIVILEGES ON DATABASE $OFBIZ_POSTGRES_TENANT_DB TO $OFBIZ_POSTGRES_TENANT_USER;
EOSQL
