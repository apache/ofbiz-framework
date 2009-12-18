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
package org.ofbiz.sql;

import java.util.Iterator;
import java.util.Set;

import org.ofbiz.base.util.StringUtil;

public final class FieldAll extends Atom implements Iterable<String> {
    private final String alias;
    private final Set<String> exclude;

    public FieldAll(String alias, Set<String> exclude) {
        this.alias = alias;
        this.exclude = exclude;
    }

    public String getAlias() {
        return alias;
    }

    public Iterator<String> iterator() {
        return exclude.iterator();
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append(alias).append(".*");
        if (!exclude.isEmpty()) {
            sb.append(" EXCLUDE (");
            StringUtil.append(sb, exclude, null, null, ", ");
            sb.append(')');
        }
        return sb;
    }
}
