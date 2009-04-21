/*******************************************************************************
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
 *******************************************************************************/

package org.ofbiz.crowd.user;

import java.util.List;
import java.util.Properties;
import java.util.Arrays;
import java.util.Set;

import org.ofbiz.base.util.UtilProperties;
import javolution.util.FastSet;

/**
 * UserGroupMapper
 */
public class UserGroupMapper {

    protected List<String> groups;

    public UserGroupMapper(String[] groups) {
        this.groups = Arrays.asList(groups);
    }

    public Set<String> getSecurityGroups() {
        Properties props = UtilProperties.getProperties("crowd.properties");

        Set<String> secGroups = FastSet.newInstance();
        boolean running = true;
        int index = 1;

        while (running) {
            String groupStr = (String) props.get("crowd.group.map." + index);
            if (groupStr == null) {
                running = false;
            } else {
                String[] groupSplit = groupStr.split("=");
                if (groupSplit.length == 2) {
                    if (groups.contains(groupSplit[0])) {
                        secGroups.add(groupSplit[1]);
                    }
                }
            }
            index++;
        }
        return secGroups;
    }
}
