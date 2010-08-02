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

public abstract class SQLStatement<S extends SQLStatement<S>> extends Atom {
    public interface Visitor {
        void visit(SQLDelete statement);
        void visit(SQLIndex statement);
        void visit(SQLInsert statement);
        void visit(SQLSelect statement);
        void visit(SQLUpdate statement);
        void visit(SQLView statement);
    }

    public static class BaseVisitor implements Visitor {
        public void visit(SQLDelete statement) {
        }

        public void visit(SQLIndex statement) {
        }

        public void visit(SQLInsert statement) {
        }

        public void visit(SQLSelect statement) {
        }

        public void visit(SQLUpdate statement) {
        }

        public void visit(SQLView statement) {
        }
    }

    public abstract void accept(Visitor visitor);
}
