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
package org.enhydra.shark;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

//import org.enhydra.jawe.xml.elements.Implementation;
//import org.enhydra.jawe.xml.elements.Tools;
import org.enhydra.shark.SharkEngineManager;
import org.enhydra.shark.api.ApplicationMappingTransaction;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.client.wfbase.BaseException;
import org.enhydra.shark.api.internal.appmappersistence.ApplicationMap;
import org.enhydra.shark.api.internal.appmappersistence.ApplicationMappingManager;
import org.enhydra.shark.api.internal.toolagent.AppParameter;
import org.enhydra.shark.api.internal.toolagent.ConnectFailed;
import org.enhydra.shark.api.internal.toolagent.SessionHandle;
import org.enhydra.shark.api.internal.toolagent.ToolAgent;

import org.enhydra.shark.api.internal.toolagent.ToolAgentGeneralException;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.internal.working.ToolAgentManager;
import org.enhydra.shark.api.internal.working.WfActivityInternal;
import org.enhydra.shark.api.internal.working.WfProcessInternal;

import org.enhydra.shark.xpdl.XMLComplexElement;
import org.enhydra.shark.xpdl.XPDLConstants;
import org.enhydra.shark.xpdl.XMLUtil;
import org.enhydra.shark.xpdl.elements.Activity;
import org.enhydra.shark.xpdl.elements.ActivityTypes;
import org.enhydra.shark.xpdl.elements.ActualParameter;
import org.enhydra.shark.xpdl.elements.ActualParameters;
import org.enhydra.shark.xpdl.elements.Application;
import org.enhydra.shark.xpdl.elements.FormalParameter;
import org.enhydra.shark.xpdl.elements.FormalParameters;
import org.enhydra.shark.xpdl.elements.ImplementationTypes;
import org.enhydra.shark.xpdl.elements.Tool;
import org.enhydra.shark.xpdl.elements.WorkflowProcess;

/**
 * Executes tool agents for Tool activities.
 */
public class ThreadedToolAgentManager implements ToolAgentManager {

   private final static long APP_STATUS_INVALID=-1;

   private final static String DEFAULT_TOOL_AGENT="DefaultToolAgent";

   private String defaultToolAgentClassName;

   private CallbackUtilities cus;

   protected ThreadedToolAgentManager () {
      this.cus=SharkEngineManager.getInstance().getCallbackUtilities();
      Properties props= cus.getProperties();
      // setting default tool agent
      try {
         defaultToolAgentClassName=(String)props.get(DEFAULT_TOOL_AGENT);
      } catch (Throwable ex) {
         cus.error("ToolAgentManagerImpl -> Can't read default tool agent name - can't work without mappings !!!");
      }
   }

   public void executeActivity (SharkTransaction t,WfActivityInternal act) throws BaseException, ToolAgentGeneralException {
      ToolRunner tr=new ToolRunner(t,act);
      tr.run();
   }

   protected class ToolRunner{// implements Runnable {
      protected WfActivityInternal activity;
      protected Activity actDef;
      protected SharkTransaction transaction;

      protected ToolRunner (SharkTransaction t,WfActivityInternal wai) throws BaseException {
         this.transaction=t;
         this.activity=wai;
         WfProcessInternal pr=wai.container(t);
         WorkflowProcess wp=SharkUtilities.
            getWorkflowProcess(pr.package_id(t),
                               pr.manager_version(t),
                               pr.process_definition_id(t));
         this.actDef=SharkUtilities.getActivityDefinition(t,wai,wp,wai.block_activity(t));
      }

      public void run () throws BaseException, ToolAgentGeneralException {
          Iterator tools = null;
          try{
         ActivityTypes acTypes = actDef.getActivityTypes();
         org.enhydra.shark.xpdl.elements.Implementation impl = acTypes.getImplementation();
         ImplementationTypes implt = impl.getImplementationTypes();
         org.enhydra.shark.xpdl.elements.Tools tolls = implt.getTools();
         ArrayList al = tolls.toElements();
         tools = al.iterator();
          }catch (Exception e) {
              e.printStackTrace();
              throw new BaseException(e);
        }
         
         while(tools.hasNext()) {
            Tool tool = (Tool)tools.next();
            cus.info("Activity"+activity.toString()+" - Executing tool [id="+tool.getId()+"]");
            // implement me
            /*if (tool.get("Type").toValue().toString().equals("APPLICATION")){
             } else {*/
            try {
               invokeApplication(tool);
               // if some application is not executed, throw an exception
            } catch (Throwable ex) {
               cus.error("Activity"+activity.toString()+" - failed to execute tool [id="+tool.getId()+"]");
               if (ex instanceof ToolAgentGeneralException) {
                  throw (ToolAgentGeneralException)ex;
               } else {
                  throw new BaseException(ex);
               }
            }
            //}
         }
      }

      protected Map createContextMap (ActualParameters aps,FormalParameters fps) throws Exception {
         return SharkUtilities.createContextMap(transaction,activity,aps,fps);
      }

      protected String getAssignmentId (String procId,String actId) throws Exception {
         String actRes=activity.getResourceRequesterUsername(transaction);
         String assId=SharkUtilities.createAssignmentKey(actId,actRes);
         return assId;
      }

      protected void invokeApplication(Tool tool) throws Throwable {
         String applicationId = tool.getId();
         Application app=SharkUtilities.getApplication(tool, applicationId);

          ArrayList parameters = new ArrayList();

         // the extended attributes are always the first parameter passed to tool agent
         String appPStr=app.getExtendedAttributes().getExtendedAttributesString();
          AppParameter param=new AppParameter("ExtendedAttributes","ExtendedAttributes",XPDLConstants.FORMAL_PARAMETER_MODE_IN,appPStr,String.class);
         parameters.add(param);

         ActualParameters aps=tool.getActualParameters();
         FormalParameters fps=app.getApplicationTypes().getFormalParameters();
         Map m=createContextMap(aps,fps);
         Iterator itFps=fps.toElements().iterator();
         Iterator itAps=aps.toElements().iterator();
         while (itFps.hasNext() && itAps.hasNext()) {
            FormalParameter fp=(FormalParameter)itFps.next();
            ActualParameter ap=(ActualParameter)itAps.next();
            String fpMode=fp.getMode();
            String fpId=fp.getId();
            Object paramVal=m.get(fpId);

            param=new AppParameter(ap.toValue(),fpId,fpMode,paramVal,SharkUtilities.getJavaClass(fp));
            parameters.add(param);
         }
         ApplicationMappingManager mm=SharkEngineManager.getInstance().getApplicationMapPersistenceManager();
         ApplicationMap tad=null;
         if (mm!=null) {
            XMLComplexElement cOwn=(XMLComplexElement)app.getParent().getParent();
            boolean isProcessApp=(cOwn instanceof WorkflowProcess);
            ApplicationMappingTransaction t=null;
            try {
               t = SharkUtilities.createApplicationMappingTransaction();
               tad= mm.getApplicationMap(
                                    t,
                                    XMLUtil.getPackage(app).getId(),
                                       ((isProcessApp)? cOwn.get("Id").toValue() : null),
                                    applicationId
                                   );
               SharkUtilities.commitMappingTransaction(t);
            } catch (RootException e) {
               SharkUtilities.rollbackMappingTransaction(t,e);
               throw e;
            } finally {
               SharkUtilities.releaseMappingTransaction(t);
            }
         }
         SessionHandle shandle=null;
         String tacn=(tad!=null) ? tad.getToolAgentClassName() : defaultToolAgentClassName;
         String uname=(tad!=null) ? tad.getUsername() : "";
         String pwd=(tad!=null) ? tad.getPassword() : "";
         String appN=(tad!=null) ? tad.getApplicationName() : "";
         Integer appM=(tad!=null) ? tad.getApplicationMode() : null;
         ToolAgent ta=SharkEngineManager.getInstance().
            getToolAgentFactory().
            createToolAgent(transaction,tacn);
         // try to connect to the tool agent
         try {
            shandle=ta.connect(transaction,uname,pwd,cus.getProperty("enginename","imaobihostrezube"),"");
         } catch (ConnectFailed cf) {
            cus.error("Activity"+activity.toString()+" - connection to Tool agent "+tacn+" failed !");
            throw cf;
         }

         String procId=activity.container(transaction).key(transaction);
         String actKey=activity.key(transaction);
         String assId=getAssignmentId(procId,actKey);

         // invoke the procedure with the specified parameters
         AppParameter[] aprs=(AppParameter[])parameters.toArray(
                                                                new AppParameter[parameters.size()]);
         ta.invokeApplication(transaction,
                              shandle.getHandle(),
                              appN,
                              procId,
                              assId,
                              aprs,
                              appM);
         long appStatus;


         appStatus=ta.requestAppStatus(transaction,
                                       shandle.getHandle(),
                                       procId,
                                       assId,
                                       aprs);
         if (appStatus==APP_STATUS_INVALID) {
            ta.disconnect(transaction,shandle);
            throw new Exception("Tool agent status is invalid!");
         }
         ta.disconnect(transaction,shandle);

         AppParameter[] returnValues=aprs;

         // copy the return values into the workflow data
         Map newData=new HashMap();
         for(int i = 0; i < returnValues.length; i++){
            if (returnValues[i].the_mode.equals(XPDLConstants.FORMAL_PARAMETER_MODE_OUT) ||
                   returnValues[i].the_mode.equals(XPDLConstants.FORMAL_PARAMETER_MODE_INOUT)) {
               String name = returnValues[i].the_actual_name;
               Object value = returnValues[i].the_value;
               newData.put(name,value);
            }
         }
         activity.set_result(transaction,newData);
      }
   }
}



