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
package org.apache.ofbiz.entity.condition;

// Keep the tests from EntityConditionVisitorTests in sync with the code examples.
/**
 * A visitor of entity conditions in the style of the visitor design pattern.
 * <p>
 * Classes implementing this interface can extend the dynamically dispatched
 * behavior associated with {@link EntityCondition} without augmenting its
 * interface.  Those classes are meant be passed to the
 * {@link EntityCondition#accept(EntityConditionVisitor) accept} method which
 * calls the corresponding method in the visitor.
 * <p>
 * <b>Usage Examples:</b>
 * Here is a dummy example that should print <i>EntityExpr\n</i> to
 * the standard output.
 * <pre>{@code
 *     EntityExpr expr;
 *     expr.accept(new EntityConditionVisitor() {
 *         public void visit(EntityNotCondition cond) {
 *              system.out.println("EntityNotCondition");
 *         }
 *
 *         public <T extends EntityCondition> void visit(EntityConditionList<T> l) {
 *              system.out.println("EntityConditionList");
 *         }
 *
 *         public void visit(EntityFieldMap m) {
 *             system.out.println("EntityFieldMap");
 *         }
 *
 *         public void visit(EntityDateFilterCondition df) {
 *              system.out.println("EntityDateFilterCondition");
 *         }
 *
 *         public void visit(EntityExpr expr) {
 *              system.out.println("EntityExpr");
 *         }
 *
 *         public void visit(EntityWhereString ws) {
 *              system.out.println("EntityWhereString");
 *         }
 *     });
 * }</pre>
 * <p>
 * Here is a more complex example asserting the presence of a raw string condition
 * even when it is embedded inside another one.
 * <pre>{@code
 *     class ContainsRawCondition implements EntityConditionVisitor {
 *         public boolean hasRawCondition = false;
 *
 *         public void visit(EntityNotCondition cond) {}
 *         public void visit(EntityFieldMap m) {}
 *         public void visit(EntityDateFilterCondition df) {}
 *
 *         public <T extends EntityCondition> void visit(EntityConditionList<T> l) {
 *             Iterator<T> it = l.getConditionIterator();
 *             while (it.hasNext()) {
 *                 it.next().accept(this);
 *             }
 *         }
 *
 *         public void visit(EntityExpr expr) {
 *             Object lhs = expr.getLhs();
 *             Object rhs = expr.getRhs();
 *             if (lhs instanceof EntityCondition) {
 *                 ((EntityCondition) lhs).accept(this);
 *             }
 *             if (rhs instanceof EntityCondition) {
 *                 ((EntityCondition) rhs).accept(this);
 *             }
 *         }
 *
 *         public void visit(EntityWhereString ws) {
 *             hasRawCondition = true;
 *         }
 *     }
 *
 *     EntityCondition ec =
 *         EntityCondition.makeCondition(EntityCondition.makeConditionWhere("foo=bar"));
 *     EntityConditionVisitor visitor = new ContainsRawCondition();
 *     ec.accept(visitor);
 *     assert visitor.hasRawCondition;
 * }</pre>
 *
 * @see EntityCondition
 */
public interface EntityConditionVisitor {
    /**
     * Visits an entity NOT expression.
     * @param cond the visited class
     * @see EntityNotCondition
     */
    void visit(EntityNotCondition cond);

    /**
     * Visits a list of entity conditions.
     * @param l the visited class
     * @see EntityConditionList
     */
    <T extends EntityCondition> void visit(EntityConditionList<T> l);

    /**
     * Visits a map of entity fields.
     * @param m the visited class
     * @see EntityFieldMap
     */
    void visit(EntityFieldMap m);

    /**
     * Visits a date filter condition.
     * @param df the visited class
     * @see EntityDateFilterCondition
     */
    void visit(EntityDateFilterCondition df);

    /**
     * Visits an entity expression.
     * @param expr the visited class
     * @see EntityExpr
     */
    void visit(EntityExpr expr);

    /**
     * Visits a raw string condition.
     * @param ws the visited class
     * @see EntityWhereString
     */
    void visit(EntityWhereString ws);
}
