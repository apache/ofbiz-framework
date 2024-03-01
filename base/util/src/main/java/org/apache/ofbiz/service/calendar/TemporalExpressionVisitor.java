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
package org.apache.ofbiz.service.calendar;

/** Temporal expression visitor interface. */
public interface TemporalExpressionVisitor {
    void visit(TemporalExpressions.DateRange expr);
    void visit(TemporalExpressions.DayInMonth expr);
    void visit(TemporalExpressions.DayOfMonthRange expr);
    void visit(TemporalExpressions.DayOfWeekRange expr);
    void visit(TemporalExpressions.Difference expr);
    void visit(TemporalExpressions.Frequency expr);
    void visit(TemporalExpressions.HourRange expr);
    void visit(TemporalExpressions.Intersection expr);
    void visit(TemporalExpressions.MinuteRange expr);
    void visit(TemporalExpressions.MonthRange expr);
    void visit(TemporalExpressions.Null expr);
    void visit(TemporalExpressions.Substitution expr);
    void visit(TemporalExpressions.Union expr);
}
