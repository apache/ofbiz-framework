/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// standard partymgr permissions
context.hasViewPermission = security.hasEntityPermission("PARTYMGR", "_VIEW", session);
context.hasCreatePermission = security.hasEntityPermission("PARTYMGR", "_CREATE", session);
context.hasUpdatePermission = security.hasEntityPermission("PARTYMGR", "_UPDATE", session);
context.hasDeletePermission = security.hasEntityPermission("PARTYMGR", "_DELETE", session);
// extended pay_info permissions
context.hasPayInfoPermission = security.hasEntityPermission("PAY_INFO", "_VIEW", session) || security.hasEntityPermission("ACCOUNTING", "_VIEW", session);
// extended pcm (party contact mechanism) permissions
context.hasPcmCreatePermission = security.hasEntityPermission("PARTYMGR_PCM", "_CREATE", session);
context.hasPcmUpdatePermission = security.hasEntityPermission("PARTYMGR_PCM", "_UPDATE", session);
