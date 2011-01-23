/*
 * Copyright 2007 Bruce Fancher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is a modified version of the original one.
 */

package org.ofbiz.base.container.groovy;

import groovy.lang.Binding;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

public class GroovyShellThread extends Thread {

    private static final Logger log = Logger.getLogger(GroovyShellThread.class);

    private Socket socket;
    private Binding binding;

    public GroovyShellThread(Socket socket, Binding binding) {
        super();
        this.socket = socket;
        this.binding = binding;
    }

    @Override
    public void run() {
        PrintStream out = null;
        InputStream in = null;

        try {
            in = socket.getInputStream();
            out = new PrintStream(socket.getOutputStream());
            binding.setVariable("out", out);

            final IO io = new IO(in, out, out);
            final Groovysh gsh = new Groovysh(binding, io);
            gsh.run();
        } catch (Exception e) {
            log.error("Error running the Groovy shell.", e);
        } finally {
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (socket != null) socket.close(); } catch (Exception e) {}
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
