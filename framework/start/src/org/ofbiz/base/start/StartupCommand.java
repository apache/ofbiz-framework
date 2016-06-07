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
package org.ofbiz.base.start;

import java.util.Map;

/**
 * A command line argument passed to OFBiz
 * 
 * <p>
 * A <tt>StartupCommand</tt> represents a processed command line argument passed 
 * to OFBiz such that it is no longer a raw string but an instance of this class.
 * For example: <code>java -jar ofbiz.jar --status</code> where status is a command.
 * </p>
 */
final class StartupCommand {
    private String name;
    private Map<String,String> properties;

    public String getName() {
        return name;
    }
    public Map<String,String> getProperties() {
        return properties;
    }

    private StartupCommand(Builder builder) {
        this.name = builder.name;
        this.properties = builder.properties;
    }

    public static class Builder {
        //required parameters
        private final String name;

        //optional parameters       
        private Map<String,String> properties;

        public Builder(String name) {
            this.name = name;
        }
        public Builder properties(Map<String,String> properties) {
            this.properties = properties;
            return this;
        }

        public StartupCommand build() {
            return new StartupCommand(this);
        }
    }
}
