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
import org.apache.ofbiz.base.secret.SecretValueResolver
import java.text.SimpleDateFormat

context.usageSummary = SecretValueResolver.getUsageSummary()

SimpleDateFormat fmt = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss', Locale.US)
context.usageReportRows = SecretValueResolver.getUsageReport().collect { key, stats ->
    long lastMs = (stats.lastAccessedMs ?: 0L) as long
    String lastStr = lastMs > 0L ? fmt.format(new Date(lastMs)) : '—'
    [key, stats.hits ?: 0L, stats.misses ?: 0L, stats.total ?: 0L, lastStr]
}
