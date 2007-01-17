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
import org.ofbiz.pos.component.Input;

public class CharacterEvents {

    public static boolean capsLockSet = false;

    public static void triggerShift(PosScreen pos) {
        pos.getInput().setFunction("SHIFT");
        // TODO refresh the button display
    }

    public static void triggerCaps(PosScreen pos) {
        capsLockSet = !capsLockSet;
        // TODO refresh the button display
    }

    public static void triggerDel(PosScreen pos) {
        pos.getInput().stripLastChar();
    }

    public static void triggerA(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('A');
        } else {
            input.appendChar('a');
        }
    }

    public static void triggerB(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('B');
        } else {
            input.appendChar('b');
        }
    }

    public static void triggerC(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('C');
        } else {
            input.appendChar('c');
        }
    }

    public static void triggerD(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('D');
        } else {
            input.appendChar('d');
        }
    }

    public static void triggerE(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('E');
        } else {
            input.appendChar('e');
        }
    }

    public static void triggerF(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('F');
        } else {
            input.appendChar('f');
        }
    }

    public static void triggerG(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('G');
        } else {
            input.appendChar('g');
        }
    }

    public static void triggerH(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('H');
        } else {
            input.appendChar('h');
        }
    }

    public static void triggerI(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('I');
        } else {
            input.appendChar('i');
        }
    }

    public static void triggerJ(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('J');
        } else {
            input.appendChar('j');
        }
    }

    public static void triggerK(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('K');
        } else {
            input.appendChar('k');
        }
    }

    public static void triggerL(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('L');
        } else {
            input.appendChar('l');
        }
    }

    public static void triggerM(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('M');
        } else {
            input.appendChar('m');
        }
    }

    public static void triggerN(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('N');
        } else {
            input.appendChar('n');
        }
    }

    public static void triggerO(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('O');
        } else {
            input.appendChar('o');
        }
    }

    public static void triggerP(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('P');
        } else {
            input.appendChar('p');
        }
    }

    public static void triggerQ(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('Q');
        } else {
            input.appendChar('q');
        }
    }

    public static void triggerR(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('R');
        } else {
            input.appendChar('r');
        }
    }

    public static void triggerS(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('S');
        } else {
            input.appendChar('s');
        }
    }

    public static void triggerT(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('T');
        } else {
            input.appendChar('t');
        }
    }

    public static void triggerU(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('U');
        } else {
            input.appendChar('u');
        }
    }

    public static void triggerV(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('V');
        } else {
            input.appendChar('v');
        }
    }

    public static void triggerW(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('W');
        } else {
            input.appendChar('w');
        }
    }

    public static void triggerX(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('X');
        } else {
            input.appendChar('x');
        }
    }

    public static void triggerY(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('Y');
        } else {
            input.appendChar('y');
        }
    }

    public static void triggerZ(PosScreen pos) {
        Input input = pos.getInput();
        if (capsLockSet || input.isFunctionSet("SHIFT")) {
            input.appendChar('Z');
        } else {
            input.appendChar('z');
        }
    }
}
