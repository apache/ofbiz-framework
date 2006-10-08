/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.service;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.Serializable;

/**
 * Service Permission Group Model Class
 */
public class ModelPermGroup implements Serializable {

    public static final String module = ModelPermGroup.class.getName();

    public static final String PERM_JOIN_AND = "AND";
    public static final String PERM_JOIN_OR = "OR";

    public List permissions = new LinkedList();
    public String joinType;

    public boolean evalPermissions(Security security, GenericValue userLogin) {
        if (permissions != null && permissions.size() > 0)  {
            boolean foundOne = false;
            Iterator i = permissions.iterator();
            while (i.hasNext()) {
                ModelPermission perm = (ModelPermission) i.next();
                if (perm.evalPermission(security, userLogin)) {
                    foundOne = true;
                } else {
                    if (joinType.equals(PERM_JOIN_AND)) {
                        return false;
                    }
                }
            }
            return foundOne;
        } else {
            return true;
        }
    }
}
