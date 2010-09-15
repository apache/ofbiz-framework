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
package org.ofbiz.tools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.taskdefs.condition.And;
import org.apache.tools.ant.taskdefs.condition.Condition;

public class IfTask extends ProjectComponent {
    protected Condition condition;
    protected Sequential ifCommands;
    protected Sequential elseCommands;

    public Condition createCondition() {
        return condition = new And();
    }

    public Object createCommands() {
        return ifCommands = new Sequential();
    }

    public Object createElse() {
        return elseCommands = new Sequential();
    }

    public void execute() throws BuildException {
        if (condition == null) throw new BuildException("No condition defined");
        if (condition.eval()) {
            if (ifCommands != null) ifCommands.execute();
        } else {
            if (elseCommands != null) elseCommands.execute();
        }
    }
}
