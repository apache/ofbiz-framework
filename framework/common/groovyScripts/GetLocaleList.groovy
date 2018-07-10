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

import static java.util.stream.Collectors.toList
import static org.apache.ofbiz.base.util.UtilMisc.availableLocales

// Check that `a` contains `b` when ignoring case.
boolean contains(String a, String b) {
    b && a.toUpperCase().contains(b.toUpperCase())
}

hasFilter = parameters.with { localeString || localeName }

context.locales = availableLocales()
    .stream()
    .map { [localeName: it.getDisplayName(it), localeString: it.toString()] }
    .filter {
        !hasFilter ||
        contains(it.localeString, parameters.localeString) ||
        contains(it.localeName, parameters.localeName)
    }
    .collect toList()
