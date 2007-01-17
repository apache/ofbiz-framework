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

import java.applet.Applet;
import java.applet.AppletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.TooManyListenersException;

import javax.comm.CommPortIdentifier;
import javax.comm.CommPortOwnershipListener;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;

import netscape.javascript.JSObject;

/**
 * ShipmentScaleApplet - Applet for reading weight from a scale and input into the browser
 */
public class ShipmentScaleApplet extends Applet implements SerialPortEventListener, CommPortOwnershipListener {
    
    private AppletContext ctx = null;    
    
    private CommPortIdentifier portId = null;
    private SerialPort serialPort = null;
    private boolean portOpen = false;
    
    private InputStream in = null;
    private OutputStream out = null;
    
    public void init() {
        this.ctx = this.getAppletContext();
        /*
        String port = this.getParameter("serialPort");
        try {
            this.configurePort(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        try {
            this.sendFakeMessage();
        } catch (IOException e) {           
            e.printStackTrace();
        }
    }
    
    public void paint() {
        
    }
    
    public void configurePort(String port) throws UnsupportedCommOperationException, IOException {
        try {
            portId = CommPortIdentifier.getPortIdentifier(port);
        } catch (NoSuchPortException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            serialPort = (SerialPort) portId.open("SerialScale", 30000);
        } catch (PortInUseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
               
        serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);                       
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT);        
               
        in = serialPort.getInputStream();        
        out = serialPort.getOutputStream();
        
        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
                        
        serialPort.enableReceiveTimeout(30);                
        serialPort.notifyOnDataAvailable(true);
        serialPort.notifyOnBreakInterrupt(true); 
        portId.addPortOwnershipListener(this);  
        this.portOpen = true;
    }

    /* (non-Javadoc)
     * @see javax.comm.SerialPortEventListener#serialEvent(javax.comm.SerialPortEvent)
     */
    public void serialEvent(SerialPortEvent event) {
        // Create a StringBuffer and int to receive input data.
        StringBuffer inputBuffer = new StringBuffer();
        int newData = 0;

        // Determine type of event.
        switch (event.getEventType()) {

            // Read data until -1 is returned. If \r is received substitute
            // \n for correct newline handling.
            case SerialPortEvent.DATA_AVAILABLE:
                while (newData != -1) {
                    try {
                        newData = in.read();
                    if (newData == -1) {
                    break;
                    }
                    if (newData != 32 && newData != 3) {
                        if ('\r' == (char)newData) {
                            inputBuffer.append('|');
                        } else if ('\n' == (char)newData) {
                            inputBuffer.append("");
                        } else {                              
                            inputBuffer.append((char)newData);
                        }
                        //inputBuffer.append("(" + newData + ")");
                    }                        
                    
                    } catch (IOException ex) {
                        System.err.println(ex);
                        return;
                    }
                }

                // Append received data to messageAreaIn.
                checkResponse(inputBuffer.toString());            
                break;

            // If break event append BREAK RECEIVED message.
            case SerialPortEvent.BI:
                break;
        }                
    }

    /* (non-Javadoc)
     * @see javax.comm.CommPortOwnershipListener#ownershipChange(int)
     */
    public void ownershipChange(int arg0) {
        // TODO Auto-generated method stub
        
    }
    
    // send the code to the scale and requests the weight
    public void sendMessage() throws IOException {
        String message = "W\r";
        char[] msgChars = message.toCharArray();
        for (int i = 0; i < msgChars.length; i++) {
            out.write((int)msgChars[i]);
        }
        out.flush();
        serialPort.sendBreak(1000);        
    }
    
    public void close() throws IOException {
        out.close();
        in.close();
        serialPort.close();        
    }
    
    public static void main(String args[]) throws Exception {
        ShipmentScaleApplet applet = new ShipmentScaleApplet();
        applet.sendMessage();
        applet.close();   
    }
    
    
    // validates the response from the scale and calls the set method
    private void checkResponse(String response) {
        StringTokenizer token = new StringTokenizer(response, "|");
        if (token != null && token.hasMoreElements()) {
            String weightStr = token.nextToken();
            setWeight(weightStr);
        }
    }
    
    private void sendFakeMessage() throws IOException {
        String weight = this.getParameter("fakeWeight");
        if (weight == null) {
            weight = "5";
        }
        setWeight(weight);
    }
    
    // calls the setWeight(weight) JavaScript function on the current page
    private void setWeight(String weight) {
        JSObject win = JSObject.getWindow(this);      
        String[] args = { weight };
        win.call("setWeight", args);
    }
}
