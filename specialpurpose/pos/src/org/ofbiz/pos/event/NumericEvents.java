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
package org.ofbiz.pos.event;

import org.ofbiz.pos.screen.PosScreen;

public class NumericEvents {

    public static final String module = NumericEvents.class.getName();

    // standard number events
    public static void triggerOne(PosScreen pos) {
        pos.getInput().appendString("1");
    }

    public static void triggerTwo(PosScreen pos) {
        pos.getInput().appendString("2");
    }

    public static void triggerThree(PosScreen pos) {
        pos.getInput().appendString("3");
    }

    public static void triggerFour(PosScreen pos) {
        pos.getInput().appendString("4");
    }

    public static void triggerFive(PosScreen pos) {
        pos.getInput().appendString("5");
    }

    public static void triggerSix(PosScreen pos) {
        pos.getInput().appendString("6");
    }

    public static void triggerSeven(PosScreen pos) {
        pos.getInput().appendString("7");
    }

    public static void triggerEight(PosScreen pos) {
        pos.getInput().appendString("8");
    }

    public static void triggerNine(PosScreen pos) {
        pos.getInput().appendString("9");
    }

    public static void triggerZero(PosScreen pos) {
        pos.getInput().appendString("0");
    }

    public static void triggerDZero(PosScreen pos) {
        pos.getInput().appendString("00");
    }

    public static void triggerPercent(PosScreen pos) {
        pos.getInput().appendString("%");
    }

    public static void triggerMinus(PosScreen pos) {
        pos.getInput().appendString("-");
    }
}


