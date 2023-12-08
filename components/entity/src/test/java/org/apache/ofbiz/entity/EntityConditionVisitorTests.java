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
package org.apache.ofbiz.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityNotCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityConditionVisitor;
import org.apache.ofbiz.entity.condition.EntityDateFilterCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityFieldMap;
import org.apache.ofbiz.entity.condition.EntityWhereString;
import org.junit.Test;

/* Tests adapted from code examples described in the javadoc of the
 * EntityConditionVisitor interface.  They should be kept in sync with
 * those code examples. */
public class EntityConditionVisitorTests {

    // Checks the dummy visitor example which must print "EntityExpr\n".
    @Test
    public void basicTest() {
        EntityExpr expr = new EntityExpr("foo", EntityComparisonOperator.EQUALS, "bar");
        OutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        expr.accept(new EntityConditionVisitor() {
            @Override
            public void visit(EntityNotCondition cond) {
                pw.print("EntityNotCondition");
            }

            @Override
            public <T extends EntityCondition> void visit(EntityConditionList<T> l) {
                pw.print("EntityConditionList");
            }

            @Override
            public void visit(EntityFieldMap m) {
                pw.print("EntityFieldMap");
            }

            @Override
            public void visit(EntityDateFilterCondition df) {
                pw.print("EntityDateFilterConfition");
            }

            @Override
            public void visit(EntityExpr expr) {
                pw.print("EntityExpr");
            }

            @Override
            public void visit(EntityWhereString ws) {
                pw.print("EntityWhereString");
            }
        });
        pw.flush();
        assertEquals("EntityExpr", os.toString());
    }

    /* Checks the more complex example which asserts the presence of a raw string
     * condition even when it is embedded inside another one. */
    @Test
    public void complexTest() {
        class ContainsRawCondition implements EntityConditionVisitor {
            private boolean hasRawCondition = false;

            @Override public void visit(EntityNotCondition cond) { }
            @Override public void visit(EntityFieldMap m) { }
            @Override public void visit(EntityDateFilterCondition df) { }

            @Override
            public <T extends EntityCondition> void visit(EntityConditionList<T> l) {
                Iterator<T> it = l.getConditionIterator();
                while (it.hasNext()) {
                    it.next().accept(this);
                }
            }

            @Override
            public void visit(EntityExpr expr) {
                Object lhs = expr.getLhs();
                Object rhs = expr.getRhs();
                if (lhs instanceof EntityCondition) {
                    EntityCondition lhec = (EntityCondition) lhs;
                    lhec.accept(this);
                }
                if (rhs instanceof EntityCondition) {
                    EntityCondition rhec = (EntityCondition) lhs;
                    rhec.accept(this);
                }
            }

            @Override
            public void visit(EntityWhereString ws) {
                hasRawCondition = true;
            }
        }

        EntityCondition ec = EntityCondition.makeCondition(EntityCondition.makeConditionWhere("foo=bar"));
        ContainsRawCondition visitor = new ContainsRawCondition();
        ec.accept(visitor);
        assertTrue(visitor.hasRawCondition);
    }
}
