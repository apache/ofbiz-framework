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
package org.apache.ofbiz.ws.rs.model;


public class ModelExample {

    private String type;
    private String code;
    private String exampleText;

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param type
     * @return
     */
    public ModelExample type(String type) {
        this.type = type;
        return this;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @param code
     * @return
     */
    public ModelExample code(String code) {
        this.code = code;
        return this;
    }


    /**
     * @return the exampleText
     */
    public String getExampleText() {
        return exampleText;
    }


    /**
     * @param exampleText the exampleText to set
     */
    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }

    /**
     * @param exampleText
     * @return
     */
    public ModelExample exampleText(String exampleText) {
        this.exampleText = exampleText;
        return this;
    }

}
