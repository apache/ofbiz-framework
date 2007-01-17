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
package org.ofbiz.content.openoffice;

import java.io.ByteArrayOutputStream;

import com.sun.star.io.XSeekable;
import com.sun.star.io.XOutputStream;
import com.sun.star.io.BufferSizeExceededException;
import com.sun.star.io.NotConnectedException;


/**
 * OpenOfficeByteArrayOutputStream Class
 */

public class OpenOfficeByteArrayOutputStream extends ByteArrayOutputStream implements XOutputStream {

    public static final String module = OpenOfficeByteArrayOutputStream.class.getName();
    
	public OpenOfficeByteArrayOutputStream() {
		super();
		// TODO Auto-generated constructor stub
	}

	public OpenOfficeByteArrayOutputStream(int arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}


	  public void writeBytes(byte[] buf) throws BufferSizeExceededException, NotConnectedException, com.sun.star.io.IOException
	  {
		  try {
			  write(buf);
		  } catch ( java.io.IOException e ) {
			  throw(new com.sun.star.io.IOException(e.getMessage()));
		  }
	  }

	  public void closeOutput() throws BufferSizeExceededException, NotConnectedException, com.sun.star.io.IOException
	  {
		  try {
			  super.flush();
			  close();
		  } catch ( java.io.IOException e ) {
			  throw(new com.sun.star.io.IOException(e.getMessage()));
		  }
	  }
	  
	  public void flush()
	  {
		  try {
			  super.flush();
		  } catch ( java.io.IOException e ) {
		  }
	  }

}
