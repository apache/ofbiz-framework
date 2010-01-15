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

import java.util.Properties;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.ofbiz.base.util.UtilProperties;
import com.atlassian.crowd.integration.soap.SOAPAttribute;

/**
 * UserAttributeMapper
 */
public class UserAttributeMapper {

    private List<SOAPAttribute> attributes;

    public UserAttributeMapper(SOAPAttribute[] attributes) {
        this();
        setAttributes(attributes);
    }

    public UserAttributeMapper() {
        this.attributes = new ArrayList<SOAPAttribute>();
    }

    public SOAPAttribute[] getAttributes() {
        SOAPAttribute[] attrs = new SOAPAttribute[attributes.size()];
        int index = 0;
        for (SOAPAttribute a : attributes) {
            attrs[index] = a;
            index++;
        }
        return attrs;
    }

    public void setAttributes(SOAPAttribute[] attributes) {
        this.attributes.addAll(Arrays.asList(attributes));
    }

    public String getFirstName() {
        return getOFBizValue("firstName");
    }

    public void setFirstName(String firstName) {
        makeAttribute("firstName", firstName);
    }

    public String getLastName() {
        return getOFBizValue("lastName");
    }

    public void setLastName(String lastName) {
        makeAttribute("lastName", lastName);
    }

    public String getEmail() {
        return getOFBizValue("email");
    }

    public void setEmail(String email) {
        makeAttribute("email", email);
    }

    private String getOFBizValue(String name) {
        String key = getCrowdKey(name);
        if (key != null) {
            for (SOAPAttribute a : attributes) {
                if (a.getName().equals(key)) {
                    if (a.getValues() != null && a.getValues().length > 0) {
                        return a.getValues()[0];
                    }
                }
            }
        }
        return null;
    }

    private String getCrowdKey(String name) {
        Properties props = UtilProperties.getProperties("crowd.properties");
        return (String) props.get("crowd.attribute.map." + name);
    }

    private SOAPAttribute makeAttribute(String name, String value) {
        SOAPAttribute attr = new SOAPAttribute();
        attr.setName(getCrowdKey(name));
        attr.setValues(new String[] {value});

        removeAttributeByName(attr.getName());
        attributes.add(attr);
        return attr;
    }

    private void removeAttributeByName(String name) {
        List<SOAPAttribute> toRemove = new ArrayList<SOAPAttribute>();
        for (SOAPAttribute a : attributes) {
            if (a.getName().equals(name)) {
                toRemove.add(a);
            }
        }
        for (SOAPAttribute a : toRemove) {
            attributes.remove(a);
        }
    }
}
