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
package org.apache.ofbiz.ws.rs.core;

import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

public final class ResponseStatus {

    public enum Custom implements StatusType {
        /**
         * 422 Unprocessable Entity, see <a href=
         * "https://tools.ietf.org/html/rfc4918#section-11.2">https://tools.ietf.org/html/rfc4918#section-11.2</a>
         */
        UNPROCESSABLE_ENTITY(422, "Unprocessable Entity");

        private final int code;
        private final String reason;
        private final Family family;

        Custom(final int statusCode, final String reasonPhrase) {
            this.code = statusCode;
            this.reason = reasonPhrase;
            this.family = Family.familyOf(statusCode);
        }

        /**
         * Get the class of status code.
         *
         * @return the class of status code.
         */
        @Override
        public Family getFamily() {
            return family;
        }

        /**
         * Get the associated status code.
         *
         * @return the status code.
         */
        @Override
        public int getStatusCode() {
            return code;
        }

        /**
         * Get the reason phrase.
         *
         * @return the reason phrase.
         */
        @Override
        public String getReasonPhrase() {
            return toString();
        }

        /**
         * Get the reason phrase.
         *
         * @return the reason phrase.
         */
        @Override
        public String toString() {
            return reason;
        }

        /**
         * Convert a numerical status code into the corresponding Status.
         * @param code
         * @return the matching Status or null is no matching Status is defined.
         */
        public static ResponseStatus.Custom fromStatusCode(int code) {
            for (Custom s : Custom.values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return null;
        }
    }
}
