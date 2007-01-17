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
package org.ofbiz.workflow.definition;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * XpdlReader - Reads Process Definition objects from XPDL
 */
public class XpdlReader {

    protected GenericDelegator delegator = null;
    protected List values = null;

    public static final String module = XpdlReader.class.getName();

    public XpdlReader(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    /** Imports an XPDL file at the given location and imports it into the
     * datasource through the given delegator */
    public static void importXpdl(URL location, GenericDelegator delegator) throws DefinitionParserException {
        List values = readXpdl(location, delegator);
        
        // attempt to start a transaction
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException gte) {
            Debug.logError(gte, "Unable to begin transaction", module);
        }
        
        try {
            delegator.storeAll(values);
            TransactionUtil.commit(beganTransaction);
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error importing XPDL", e);
            } catch (GenericEntityException e2) {                
                Debug.logError(e2, "Problems rolling back transaction", module);
            }                                  
            throw new DefinitionParserException("Could not store values", e);
        }
    }

    /** Gets an XML file from the specified location and reads it into
     * GenericValue objects from the given delegator and returns them in a
     * List; does not write to the database, just gets the entities. */
    public static List readXpdl(URL location, GenericDelegator delegator) throws DefinitionParserException {
        if (Debug.infoOn()) Debug.logInfo("Beginning XPDL File Parse: " + location.toString(), module);

        XpdlReader reader = new XpdlReader(delegator);

        try {
            Document document = UtilXml.readXmlDocument(location);

            return reader.readAll(document);
        } catch (ParserConfigurationException e) {
            Debug.logError(e, module);
            throw new DefinitionParserException("Could not configure XML reader", e);
        } catch (SAXException e) {
            Debug.logError(e, module);
            throw new DefinitionParserException("Could not parse XML (invalid?)", e);
        } catch (IOException e) {
            Debug.logError(e, module);
            throw new DefinitionParserException("Could not load file", e);
        }
    }

    public List readAll(Document document) throws DefinitionParserException {
        values = new LinkedList();
        Element docElement;

        docElement = document.getDocumentElement();
        // read the package element, and everything under it
        // puts everything in the values list for returning, etc later
        readPackage(docElement);

        return (values);
    }

    // ----------------------------------------------------------------
    // Package
    // ----------------------------------------------------------------

    protected void readPackage(Element packageElement) throws DefinitionParserException {
        if (packageElement == null)
            return;
        if (!"Package".equals(packageElement.getTagName()))
            throw new DefinitionParserException("Tried to make Package from element not named Package");

        GenericValue packageValue = delegator.makeValue("WorkflowPackage", null);

        values.add(packageValue);

        String packageId = packageElement.getAttribute("Id");

        packageValue.set("packageId", packageId);
        packageValue.set("packageName", packageElement.getAttribute("Name"));

        // PackageHeader
        Element packageHeaderElement = UtilXml.firstChildElement(packageElement, "PackageHeader");

        if (packageHeaderElement != null) {
            packageValue.set("specificationId", "XPDL");
            packageValue.set("specificationVersion", UtilXml.childElementValue(packageHeaderElement, "XPDLVersion"));
            packageValue.set("sourceVendorInfo", UtilXml.childElementValue(packageHeaderElement, "Vendor"));
            String createdStr = UtilXml.childElementValue(packageHeaderElement, "Created");

            if (createdStr != null) {
                try {
                    packageValue.set("creationDateTime", java.sql.Timestamp.valueOf(createdStr));
                } catch (IllegalArgumentException e) {
                    throw new DefinitionParserException("Invalid Date-Time format in Package->Created: " + createdStr, e);
                }
            }
            packageValue.set("description", UtilXml.childElementValue(packageHeaderElement, "Description"));
            packageValue.set("documentationUrl", UtilXml.childElementValue(packageHeaderElement, "Documentation"));
            packageValue.set("priorityUomId", UtilXml.childElementValue(packageHeaderElement, "PriorityUnit"));
            packageValue.set("costUomId", UtilXml.childElementValue(packageHeaderElement, "CostUnit"));
        }

        // RedefinableHeader?
        Element redefinableHeaderElement = UtilXml.firstChildElement(packageElement, "RedefinableHeader");
        boolean packageOk = readRedefinableHeader(redefinableHeaderElement, packageValue, "package");
        String packageVersion = packageValue.getString("packageVersion");       

        // Only do these if the package hasn't been imported.
        if (packageOk) {
            // ConformanceClass?
            Element conformanceClassElement = UtilXml.firstChildElement(packageElement, "ConformanceClass");

            if (conformanceClassElement != null) {
                packageValue.set("graphConformanceEnumId", "WGC_" + conformanceClassElement.getAttribute("GraphConformance"));
            }

            // Participants?
            Element participantsElement = UtilXml.firstChildElement(packageElement, "Participants");
            List participants = UtilXml.childElementList(participantsElement, "Participant");

            readParticipants(participants, packageId, packageVersion, "_NA_", "_NA_", packageValue);

            // ExternalPackages?
            Element externalPackagesElement = UtilXml.firstChildElement(packageElement, "ExternalPackages");
            List externalPackages = UtilXml.childElementList(externalPackagesElement, "ExternalPackage");

            readExternalPackages(externalPackages, packageId, packageVersion);

            // TypeDeclarations?
            Element typeDeclarationsElement = UtilXml.firstChildElement(packageElement, "TypeDeclarations");
            List typeDeclarations = UtilXml.childElementList(typeDeclarationsElement, "TypeDeclaration");

            readTypeDeclarations(typeDeclarations, packageId, packageVersion);

            // Applications?
            Element applicationsElement = UtilXml.firstChildElement(packageElement, "Applications");
            List applications = UtilXml.childElementList(applicationsElement, "Application");

            readApplications(applications, packageId, packageVersion, "_NA_", "_NA_");

            // DataFields?
            Element dataFieldsElement = UtilXml.firstChildElement(packageElement, "DataFields");
            List dataFields = UtilXml.childElementList(dataFieldsElement, "DataField");

            readDataFields(dataFields, packageId, packageVersion, "_NA_", "_NA_");
        } else {
            values = new LinkedList();
        }

        // WorkflowProcesses?
        Element workflowProcessesElement = UtilXml.firstChildElement(packageElement, "WorkflowProcesses");
        List workflowProcesses = UtilXml.childElementList(workflowProcessesElement, "WorkflowProcess");

        readWorkflowProcesses(workflowProcesses, packageId, packageVersion);
    }

    protected boolean readRedefinableHeader(Element redefinableHeaderElement, GenericValue valueObject, String prefix) throws DefinitionParserException {
        if (redefinableHeaderElement == null) {
            valueObject.set(prefix + "Version", UtilDateTime.nowDateString());
            return checkVersion(valueObject, prefix);
        }

        valueObject.set("author", UtilXml.childElementValue(redefinableHeaderElement, "Author"));
        valueObject.set(prefix + "Version", UtilXml.childElementValue(redefinableHeaderElement, "Version", UtilDateTime.nowDateString()));
        valueObject.set("codepage", UtilXml.childElementValue(redefinableHeaderElement, "Codepage"));
        valueObject.set("countryGeoId", UtilXml.childElementValue(redefinableHeaderElement, "Countrykey"));
        valueObject.set("publicationStatusId", "WPS_" + redefinableHeaderElement.getAttribute("PublicationStatus"));

        if (!checkVersion(valueObject, prefix)) return false;

        // Responsibles?
        Element responsiblesElement = UtilXml.firstChildElement(redefinableHeaderElement, "Responsibles");
        List responsibles = UtilXml.childElementList(responsiblesElement, "Responsible");

        readResponsibles(responsibles, valueObject, prefix);
        return true;
    }

    private boolean checkVersion(GenericValue valueObject, String prefix) {
        // Test if the object already exists. If so throw an exception.
        try {
            String message = new String();

            if (prefix.equals("package")) {
                GenericValue gvCheck = valueObject.getDelegator().findByPrimaryKey("WorkflowPackage",
                        UtilMisc.toMap("packageId", valueObject.getString("packageId"),
                            "packageVersion", valueObject.getString("packageVersion")));

                if (gvCheck != null) {
                    message = "[xpdl] Package: " + valueObject.getString("packageId") +
                            " (ver " + valueObject.getString("packageVersion") +
                            ") has already been imported. Will not update/import.";
                }
            } else if (prefix.equals("process")) {
                GenericValue gvCheck = valueObject.getDelegator().findByPrimaryKey("WorkflowProcess",
                        UtilMisc.toMap("packageId", valueObject.getString("packageId"),
                            "packageVersion", valueObject.getString("packageVersion"),
                            "processId", valueObject.getString("processId"),
                            "processVersion", valueObject.getString("processVersion")));

                if (gvCheck != null) {
                    message = "[xpdl] Process: " + valueObject.getString("processId") +
                            " (ver " + valueObject.getString("processVersion") +
                            ") has already been imported. Not importing.";
                }
            }
            if (message.length() > 0) {
                StringBuffer lines = new StringBuffer();

                for (int i = 0; i < message.length(); i++) {
                    lines.append("-");
                }
                Debug.logWarning(lines.toString(), module);
                Debug.logWarning(message, module);
                Debug.logWarning(lines.toString(), module);
                return false;
            }
        } catch (GenericEntityException e) {
            return false;
        }
        return true;
    }

    protected void readResponsibles(List responsibles, GenericValue valueObject, String prefix) throws DefinitionParserException {
        if (responsibles == null || responsibles.size() == 0) {
            return;
        }

        String responsibleListId = delegator.getNextSeqId("WorkflowParticipantList");
        valueObject.set("responsibleListId", responsibleListId);

        Iterator responsibleIter = responsibles.iterator();
        int responsibleIndex = 1;

        while (responsibleIter.hasNext()) {
            Element responsibleElement = (Element) responsibleIter.next();
            String responsibleId = UtilXml.elementValue(responsibleElement);
            GenericValue participantListValue = delegator.makeValue("WorkflowParticipantList", null);

            participantListValue.set("packageId", valueObject.getString("packageId"));
            participantListValue.set("packageVersion", valueObject.getString("packageVersion"));
            participantListValue.set("participantListId", responsibleListId);
            participantListValue.set("participantId", responsibleId);
            participantListValue.set("participantIndex", new Long(responsibleIndex));
            if (prefix.equals("process")) {
                participantListValue.set("processId", valueObject.getString("processId"));
                participantListValue.set("processVersion", valueObject.getString("processVersion"));
            } else {
                participantListValue.set("processId", "_NA_");
                participantListValue.set("processVersion", "_NA_");
            }
            values.add(participantListValue);                      
            responsibleIndex++;
        }
    }

    protected void readExternalPackages(List externalPackages, String packageId, String packageVersion) {
        if (externalPackages == null || externalPackages.size() == 0)
            return;
        Iterator externalPackageIter = externalPackages.iterator();

        while (externalPackageIter.hasNext()) {
            Element externalPackageElement = (Element) externalPackageIter.next();
            GenericValue externalPackageValue = delegator.makeValue("WorkflowPackageExternal", null);

            values.add(externalPackageValue);
            externalPackageValue.set("packageId", packageId);
            externalPackageValue.set("packageVersion", packageVersion);
            externalPackageValue.set("externalPackageId", externalPackageElement.getAttribute("href"));
        }
    }

    protected void readTypeDeclarations(List typeDeclarations, String packageId, String packageVersion) throws DefinitionParserException {
        if (typeDeclarations == null || typeDeclarations.size() == 0)
            return;
        Iterator typeDeclarationsIter = typeDeclarations.iterator();

        while (typeDeclarationsIter.hasNext()) {
            Element typeDeclarationElement = (Element) typeDeclarationsIter.next();
            GenericValue typeDeclarationValue = delegator.makeValue("WorkflowTypeDeclaration", null);

            values.add(typeDeclarationValue);

            typeDeclarationValue.set("packageId", packageId);
            typeDeclarationValue.set("packageVersion", packageVersion);
            typeDeclarationValue.set("typeId", typeDeclarationElement.getAttribute("Id"));
            typeDeclarationValue.set("typeName", typeDeclarationElement.getAttribute("Name"));

            // (%Type;)
            readType(typeDeclarationElement, typeDeclarationValue);

            // Description?
            typeDeclarationValue.set("description", UtilXml.childElementValue(typeDeclarationElement, "Description"));
        }
    }

    // ----------------------------------------------------------------
    // Process
    // ----------------------------------------------------------------

    protected void readWorkflowProcesses(List workflowProcesses, String packageId, String packageVersion) throws DefinitionParserException {
        if (workflowProcesses == null || workflowProcesses.size() == 0)
            return;
        Iterator workflowProcessIter = workflowProcesses.iterator();

        while (workflowProcessIter.hasNext()) {
            Element workflowProcessElement = (Element) workflowProcessIter.next();

            readWorkflowProcess(workflowProcessElement, packageId, packageVersion);
        }
    }

    protected void readWorkflowProcess(Element workflowProcessElement, String packageId, String packageVersion) throws DefinitionParserException {
        GenericValue workflowProcessValue = delegator.makeValue("WorkflowProcess", null);

        values.add(workflowProcessValue);

        String processId = workflowProcessElement.getAttribute("Id");

        workflowProcessValue.set("packageId", packageId);
        workflowProcessValue.set("packageVersion", packageVersion);
        workflowProcessValue.set("processId", processId);
        workflowProcessValue.set("objectName", workflowProcessElement.getAttribute("Name"));

        // ProcessHeader
        Element processHeaderElement = UtilXml.firstChildElement(workflowProcessElement, "ProcessHeader");

        if (processHeaderElement != null) {
            // TODO: add prefix to duration Unit or map it to make it a real uomId
            workflowProcessValue.set("durationUomId", processHeaderElement.getAttribute("DurationUnit"));
            String createdStr = UtilXml.childElementValue(processHeaderElement, "Created");

            if (createdStr != null) {
                try {
                    workflowProcessValue.set("creationDateTime", java.sql.Timestamp.valueOf(createdStr));
                } catch (IllegalArgumentException e) {
                    throw new DefinitionParserException("Invalid Date-Time format in WorkflowProcess->ProcessHeader->Created: " + createdStr, e);
                }
            }
            workflowProcessValue.set("description", UtilXml.childElementValue(processHeaderElement, "Description"));

            String priorityStr = UtilXml.childElementValue(processHeaderElement, "Priority");

            if (priorityStr != null) {
                try {
                    workflowProcessValue.set("objectPriority", Long.valueOf(priorityStr));
                } catch (NumberFormatException e) {
                    throw new DefinitionParserException("Invalid whole number format in WorkflowProcess->ProcessHeader->Priority: " + priorityStr, e);
                }
            }
            String limitStr = UtilXml.childElementValue(processHeaderElement, "Limit");

            if (limitStr != null) {
                try {
                    workflowProcessValue.set("timeLimit", Double.valueOf(limitStr));
                } catch (NumberFormatException e) {
                    throw new DefinitionParserException("Invalid decimal number format in WorkflowProcess->ProcessHeader->Limit: " + limitStr, e);
                }
            }

            String validFromStr = UtilXml.childElementValue(processHeaderElement, "ValidFrom");

            if (validFromStr != null) {
                try {
                    workflowProcessValue.set("validFromDate", java.sql.Timestamp.valueOf(validFromStr));
                } catch (IllegalArgumentException e) {
                    throw new DefinitionParserException("Invalid Date-Time format in WorkflowProcess->ProcessHeader->ValidFrom: " + validFromStr, e);
                }
            }
            String validToStr = UtilXml.childElementValue(processHeaderElement, "ValidTo");

            if (validToStr != null) {
                try {
                    workflowProcessValue.set("validToDate", java.sql.Timestamp.valueOf(validToStr));
                } catch (IllegalArgumentException e) {
                    throw new DefinitionParserException("Invalid Date-Time format in WorkflowProcess->ProcessHeader->ValidTo: " + validToStr, e);
                }
            }

            // TimeEstimation?
            Element timeEstimationElement = UtilXml.firstChildElement(processHeaderElement, "TimeEstimation");

            if (timeEstimationElement != null) {
                String waitingTimeStr = UtilXml.childElementValue(timeEstimationElement, "WaitingTime");

                if (waitingTimeStr != null) {
                    try {
                        workflowProcessValue.set("waitingTime", Double.valueOf(waitingTimeStr));
                    } catch (NumberFormatException e) {
                        throw new DefinitionParserException("Invalid decimal number format in WorkflowProcess->ProcessHeader->TimeEstimation->WaitingTime: " + waitingTimeStr, e);
                    }
                }
                String workingTimeStr = UtilXml.childElementValue(timeEstimationElement, "WorkingTime");

                if (workingTimeStr != null) {
                    try {
                        workflowProcessValue.set("waitingTime", Double.valueOf(workingTimeStr));
                    } catch (NumberFormatException e) {
                        throw new DefinitionParserException("Invalid decimal number format in WorkflowProcess->ProcessHeader->TimeEstimation->WorkingTime: " + workingTimeStr, e);
                    }
                }
                String durationStr = UtilXml.childElementValue(timeEstimationElement, "Duration");

                if (durationStr != null) {
                    try {
                        workflowProcessValue.set("duration", Double.valueOf(durationStr));
                    } catch (NumberFormatException e) {
                        throw new DefinitionParserException("Invalid decimal number format in WorkflowProcess->ProcessHeader->TimeEstimation->Duration: " + durationStr, e);
                    }
                }
            }
        }

        // RedefinableHeader?
        Element redefinableHeaderElement = UtilXml.firstChildElement(workflowProcessElement, "RedefinableHeader");
        boolean processOk = readRedefinableHeader(redefinableHeaderElement, workflowProcessValue, "process");
        String processVersion = workflowProcessValue.getString("processVersion");        

        if (!processOk) {
            values.remove(workflowProcessValue);
            return;
        }

        // FormalParameters?
        Element formalParametersElement = UtilXml.firstChildElement(workflowProcessElement, "FormalParameters");
        List formalParameters = UtilXml.childElementList(formalParametersElement, "FormalParameter");

        readFormalParameters(formalParameters, packageId, packageVersion, processId, processVersion, "_NA_");

        // (%Type;)* TODO

        // DataFields?
        Element dataFieldsElement = UtilXml.firstChildElement(workflowProcessElement, "DataFields");
        List dataFields = UtilXml.childElementList(dataFieldsElement, "DataField");

        readDataFields(dataFields, packageId, packageVersion, processId, processVersion);

        // Participants?
        Element participantsElement = UtilXml.firstChildElement(workflowProcessElement, "Participants");
        List participants = UtilXml.childElementList(participantsElement, "Participant");

        readParticipants(participants, packageId, packageVersion, processId, processVersion, workflowProcessValue);

        // Applications?
        Element applicationsElement = UtilXml.firstChildElement(workflowProcessElement, "Applications");
        List applications = UtilXml.childElementList(applicationsElement, "Application");

        readApplications(applications, packageId, packageVersion, processId, processVersion);

        // Activities
        Element activitiesElement = UtilXml.firstChildElement(workflowProcessElement, "Activities");
        List activities = UtilXml.childElementList(activitiesElement, "Activity");

        readActivities(activities, packageId, packageVersion, processId, processVersion, workflowProcessValue);

        // Transitions
        Element transitionsElement = UtilXml.firstChildElement(workflowProcessElement, "Transitions");
        List transitions = UtilXml.childElementList(transitionsElement, "Transition");

        readTransitions(transitions, packageId, packageVersion, processId, processVersion);
        
        // ExtendedAttributes?
        workflowProcessValue.set("defaultStartActivityId", getExtendedAttributeValue(workflowProcessElement, "defaultStartActivityId", workflowProcessValue.getString("defaultStartActivityId")));        
        workflowProcessValue.set("sourceReferenceField", getExtendedAttributeValue(workflowProcessElement, "sourceReferenceField", "sourceReferenceId"));                               
    }

    // ----------------------------------------------------------------
    // Activity
    // ----------------------------------------------------------------

    protected void readActivities(List activities, String packageId, String packageVersion, String processId,
        String processVersion, GenericValue processValue) throws DefinitionParserException {
        if (activities == null || activities.size() == 0)
            return;
        Iterator activitiesIter = activities.iterator();

        // do the first one differently because it will be the defaultStart activity
        if (activitiesIter.hasNext()) {
            Element activityElement = (Element) activitiesIter.next();
            String activityId = activityElement.getAttribute("Id");

            processValue.set("defaultStartActivityId", activityId);
            readActivity(activityElement, packageId, packageVersion, processId, processVersion);
        }

        while (activitiesIter.hasNext()) {
            Element activityElement = (Element) activitiesIter.next();

            readActivity(activityElement, packageId, packageVersion, processId, processVersion);
        }
    }

    protected void readActivity(Element activityElement, String packageId, String packageVersion, String processId,
        String processVersion) throws DefinitionParserException {
        if (activityElement == null)
            return;

        GenericValue activityValue = delegator.makeValue("WorkflowActivity", null);

        values.add(activityValue);

        String activityId = activityElement.getAttribute("Id");

        activityValue.set("packageId", packageId);
        activityValue.set("packageVersion", packageVersion);
        activityValue.set("processId", processId);
        activityValue.set("processVersion", processVersion);
        activityValue.set("activityId", activityId);
        activityValue.set("objectName", activityElement.getAttribute("Name"));

        activityValue.set("description", UtilXml.childElementValue(activityElement, "Description"));
        String limitStr = UtilXml.childElementValue(activityElement, "Limit");

        if (limitStr != null) {
            try {
                activityValue.set("timeLimit", Double.valueOf(limitStr));
            } catch (NumberFormatException e) {
                throw new DefinitionParserException("Invalid decimal number format in Activity->Limit: " + limitStr, e);
            }
        }

        // (Route | Implementation)
        Element routeElement = UtilXml.firstChildElement(activityElement, "Route");
        Element implementationElement = UtilXml.firstChildElement(activityElement, "Implementation");

        if (routeElement != null) {
            activityValue.set("activityTypeEnumId", "WAT_ROUTE");
        } else if (implementationElement != null) {
            Element noElement = UtilXml.firstChildElement(implementationElement, "No");
            Element subFlowElement = UtilXml.firstChildElement(implementationElement, "SubFlow");
            Element loopElement = UtilXml.firstChildElement(implementationElement, "Loop");
            List tools = UtilXml.childElementList(implementationElement, "Tool");

            if (noElement != null) {
                activityValue.set("activityTypeEnumId", "WAT_NO");
            } else if (subFlowElement != null) {
                activityValue.set("activityTypeEnumId", "WAT_SUBFLOW");
                readSubFlow(subFlowElement, packageId, packageVersion, processId, processVersion, activityId);
            } else if (loopElement != null) {
                activityValue.set("activityTypeEnumId", "WAT_LOOP");
                readLoop(loopElement, packageId, packageVersion, processId, processVersion, activityId);
            } else if (tools != null && tools.size() > 0) {
                activityValue.set("activityTypeEnumId", "WAT_TOOL");
                readTools(tools, packageId, packageVersion, processId, processVersion, activityId);
            } else {
                throw new DefinitionParserException(
                        "No, SubFlow, Loop or one or more Tool elements must exist under the Implementation element of Activity with ID " + activityId +
                        " in Process with ID " + processId);
            }
        } else {
            throw new DefinitionParserException("Route or Implementation must exist for Activity with ID " + activityId + " in Process with ID " + processId);
        }

        // Performer?
        activityValue.set("performerParticipantId", UtilXml.childElementValue(activityElement, "Performer"));

        // StartMode?
        Element startModeElement = UtilXml.firstChildElement(activityElement, "StartMode");

        if (startModeElement != null) {
            if (UtilXml.firstChildElement(startModeElement, "Automatic") != null)
                activityValue.set("startModeEnumId", "WAM_AUTOMATIC");
            else if (UtilXml.firstChildElement(startModeElement, "Manual") != null)
                activityValue.set("startModeEnumId", "WAM_MANUAL");
            else
                throw new DefinitionParserException("Could not find Mode under StartMode");
        }

        // FinishMode?
        Element finishModeElement = UtilXml.firstChildElement(activityElement, "FinishMode");

        if (finishModeElement != null) {
            if (UtilXml.firstChildElement(finishModeElement, "Automatic") != null)
                activityValue.set("finishModeEnumId", "WAM_AUTOMATIC");
            else if (UtilXml.firstChildElement(finishModeElement, "Manual") != null)
                activityValue.set("finishModeEnumId", "WAM_MANUAL");
            else
                throw new DefinitionParserException("Could not find Mode under FinishMode");
        }

        // Priority?
        String priorityStr = UtilXml.childElementValue(activityElement, "Priority");

        if (priorityStr != null) {
            try {
                activityValue.set("objectPriority", Long.valueOf(priorityStr));
            } catch (NumberFormatException e) {
                throw new DefinitionParserException("Invalid whole number format in Activity->Priority: " + priorityStr, e);
            }
        }

        // SimulationInformation?
        Element simulationInformationElement = UtilXml.firstChildElement(activityElement, "SimulationInformation");

        if (simulationInformationElement != null) {
            if (simulationInformationElement.getAttribute("Instantiation") != null)
                activityValue.set("instantiationLimitEnumId", "WFI_" + simulationInformationElement.getAttribute("Instantiation"));
            String costStr = UtilXml.childElementValue(simulationInformationElement, "Cost");

            if (costStr != null) {
                try {
                    activityValue.set("cost", Double.valueOf(costStr));
                } catch (NumberFormatException e) {
                    throw new DefinitionParserException("Invalid decimal number format in Activity->SimulationInformation->Cost: " + costStr, e);
                }
            }

            // TimeEstimation
            Element timeEstimationElement = UtilXml.firstChildElement(simulationInformationElement, "TimeEstimation");

            if (timeEstimationElement != null) {
                String waitingTimeStr = UtilXml.childElementValue(timeEstimationElement, "WaitingTime");

                if (waitingTimeStr != null) {
                    try {
                        activityValue.set("waitingTime", Double.valueOf(waitingTimeStr));
                    } catch (NumberFormatException e) {
                        throw new DefinitionParserException("Invalid decimal number format in Activity->SimulationInformation->TimeEstimation->WaitingTime: " + waitingTimeStr, e);
                    }
                }
                String workingTimeStr = UtilXml.childElementValue(timeEstimationElement, "WorkingTime");

                if (workingTimeStr != null) {
                    try {
                        activityValue.set("waitingTime", Double.valueOf(workingTimeStr));
                    } catch (NumberFormatException e) {
                        throw new DefinitionParserException("Invalid decimal number format in Activity->SimulationInformation->TimeEstimation->WorkingTime: " + workingTimeStr, e);
                    }
                }
                String durationStr = UtilXml.childElementValue(timeEstimationElement, "Duration");

                if (durationStr != null) {
                    try {
                        activityValue.set("duration", Double.valueOf(durationStr));
                    } catch (NumberFormatException e) {
                        throw new DefinitionParserException("Invalid decimal number format in Activity->SimulationInformation->TimeEstimation->Duration: " + durationStr, e);
                    }
                }
            }
        }

        activityValue.set("iconUrl", UtilXml.childElementValue(activityElement, "Icon"));
        activityValue.set("documentationUrl", UtilXml.childElementValue(activityElement, "Documentation"));

        // TransitionRestrictions?
        Element transitionRestrictionsElement = UtilXml.firstChildElement(activityElement, "TransitionRestrictions");
        List transitionRestrictions = UtilXml.childElementList(transitionRestrictionsElement, "TransitionRestriction");

        readTransitionRestrictions(transitionRestrictions, activityValue);

        // ExtendedAttributes?
        activityValue.set("acceptAllAssignments", getExtendedAttributeValue(activityElement, "acceptAllAssignments", "N"));
        activityValue.set("completeAllAssignments", getExtendedAttributeValue(activityElement, "completeAllAssignments", "N"));
        activityValue.set("limitService", getExtendedAttributeValue(activityElement, "limitService", null), false);
        activityValue.set("limitAfterStart", getExtendedAttributeValue(activityElement, "limitAfterStart", "Y"));
        activityValue.set("restartOnDelegate", getExtendedAttributeValue(activityElement, "restartOnDelegate", "N"));
        activityValue.set("delegateAfterStart", getExtendedAttributeValue(activityElement, "delegateAfterStart", "Y"));
        activityValue.set("inheritPriority", getExtendedAttributeValue(activityElement, "inheritPriority", "N"));
        activityValue.set("canStart", getExtendedAttributeValue(activityElement, "canStart", "Y"));
    }

    protected void readSubFlow(Element subFlowElement, String packageId, String packageVersion, String processId,
        String processVersion, String activityId) throws DefinitionParserException {
        if (subFlowElement == null)
            return;

        GenericValue subFlowValue = delegator.makeValue("WorkflowActivitySubFlow", null);

        values.add(subFlowValue);

        subFlowValue.set("packageId", packageId);
        subFlowValue.set("packageVersion", packageVersion);
        subFlowValue.set("processId", processId);
        subFlowValue.set("processVersion", processVersion);
        subFlowValue.set("activityId", activityId);
        subFlowValue.set("subFlowProcessId", subFlowElement.getAttribute("Id"));

        if (subFlowElement.getAttribute("Execution") != null)
            subFlowValue.set("executionEnumId", "WSE_" + subFlowElement.getAttribute("Execution"));
        else
            subFlowValue.set("executionEnumId", "WSE_ASYNCHR");

        // ActualParameters?
        Element actualParametersElement = UtilXml.firstChildElement(subFlowElement, "ActualParameters");
        List actualParameters = UtilXml.childElementList(actualParametersElement, "ActualParameter");

        subFlowValue.set("actualParameters", readActualParameters(actualParameters), false);
    }

    protected void readLoop(Element loopElement, String packageId, String packageVersion, String processId,
        String processVersion, String activityId) throws DefinitionParserException {
        if (loopElement == null)
            return;

        GenericValue loopValue = delegator.makeValue("WorkflowActivityLoop", null);

        values.add(loopValue);

        loopValue.set("packageId", packageId);
        loopValue.set("packageVersion", packageVersion);
        loopValue.set("processId", processId);
        loopValue.set("processVersion", processVersion);
        loopValue.set("activityId", activityId);

        if (loopElement.getAttribute("Kind") != null)
            loopValue.set("loopKindEnumId", "WLK_" + loopElement.getAttribute("Kind"));
        else
            loopValue.set("loopKindEnumId", "WLK_WHILE");

        // Condition?
        loopValue.set("conditionExpr", UtilXml.childElementValue(loopElement, "Condition"));
    }

    protected void readTools(List tools, String packageId, String packageVersion, String processId,
        String processVersion, String activityId) throws DefinitionParserException {
        if (tools == null || tools.size() == 0)
            return;
        Iterator toolsIter = tools.iterator();

        while (toolsIter.hasNext()) {
            Element toolElement = (Element) toolsIter.next();

            readTool(toolElement, packageId, packageVersion, processId, processVersion, activityId);
        }
    }

    protected void readTool(Element toolElement, String packageId, String packageVersion, String processId,
        String processVersion, String activityId) throws DefinitionParserException {
        if (toolElement == null)
            return;

        GenericValue toolValue = delegator.makeValue("WorkflowActivityTool", null);

        values.add(toolValue);

        toolValue.set("packageId", packageId);
        toolValue.set("packageVersion", packageVersion);
        toolValue.set("processId", processId);
        toolValue.set("processVersion", processVersion);
        toolValue.set("activityId", activityId);
        toolValue.set("toolId", toolElement.getAttribute("Id"));

        if (toolElement.getAttribute("Type") != null)
            toolValue.set("toolTypeEnumId", "WTT_" + toolElement.getAttribute("Type"));
        else
            toolValue.set("toolTypeEnumId", "WTT_PROCEDURE");

        // Description?
        toolValue.set("description", UtilXml.childElementValue(toolElement, "Description"));

        // ActualParameters/ExtendedAttributes?
        Element actualParametersElement = UtilXml.firstChildElement(toolElement, "ActualParameters");
        Element extendedAttributesElement = UtilXml.firstChildElement(toolElement, "ExtendedAttributes");
        List actualParameters = UtilXml.childElementList(actualParametersElement, "ActualParameter");
        List extendedAttributes = UtilXml.childElementList(extendedAttributesElement, "ExtendedAttribute");

        toolValue.set("actualParameters", readActualParameters(actualParameters), false);
        toolValue.set("extendedAttributes", readExtendedAttributes(extendedAttributes), false);
    }

    protected String readActualParameters(List actualParameters) {
        if (actualParameters == null || actualParameters.size() == 0) return null;
        StringBuffer actualParametersBuf = new StringBuffer();
        Iterator actualParametersIter = actualParameters.iterator();

        while (actualParametersIter.hasNext()) {
            Element actualParameterElement = (Element) actualParametersIter.next();

            actualParametersBuf.append(UtilXml.elementValue(actualParameterElement));
            if (actualParametersIter.hasNext())
                actualParametersBuf.append(',');
        }
        return actualParametersBuf.toString();
    }

    protected String readExtendedAttributes(List extendedAttributes) {
        if (extendedAttributes == null || extendedAttributes.size() == 0) return null;
        Map ea = new HashMap();
        Iterator i = extendedAttributes.iterator();

        while (i.hasNext()) {
            Element e = (Element) i.next();

            ea.put(e.getAttribute("Name"), e.getAttribute("Value"));
        }
        return StringUtil.mapToStr(ea);
    }

    // ----------------------------------------------------------------
    // Transition
    // ----------------------------------------------------------------

    protected void readTransitions(List transitions, String packageId, String packageVersion, String processId,
        String processVersion) throws DefinitionParserException {
        if (transitions == null || transitions.size() == 0)
            return;
        Iterator transitionsIter = transitions.iterator();

        while (transitionsIter.hasNext()) {
            Element transitionElement = (Element) transitionsIter.next();

            readTransition(transitionElement, packageId, packageVersion, processId, processVersion);
        }
    }

    protected void readTransition(Element transitionElement, String packageId, String packageVersion,
        String processId, String processVersion) throws DefinitionParserException {
        if (transitionElement == null)
            return;

        GenericValue transitionValue = delegator.makeValue("WorkflowTransition", null);

        values.add(transitionValue);

        String transitionId = transitionElement.getAttribute("Id");

        transitionValue.set("packageId", packageId);
        transitionValue.set("packageVersion", packageVersion);
        transitionValue.set("processId", processId);
        transitionValue.set("processVersion", processVersion);
        transitionValue.set("transitionId", transitionId);
        transitionValue.set("fromActivityId", transitionElement.getAttribute("From"));
        transitionValue.set("toActivityId", transitionElement.getAttribute("To"));

        if (transitionElement.getAttribute("Loop") != null && transitionElement.getAttribute("Loop").length() > 0)
            transitionValue.set("loopTypeEnumId", "WTL_" + transitionElement.getAttribute("Loop"));
        else
            transitionValue.set("loopTypeEnumId", "WTL_NOLOOP");

        transitionValue.set("transitionName", transitionElement.getAttribute("Name"));

        // Condition?
        Element conditionElement = UtilXml.firstChildElement(transitionElement, "Condition");

        if (conditionElement != null) {
            if (conditionElement.getAttribute("Type") != null)
                transitionValue.set("conditionTypeEnumId", "WTC_" + conditionElement.getAttribute("Type"));
            else
                transitionValue.set("conditionTypeEnumId", "WTC_CONDITION");

            // a Condition will have either a list of XPression elements, or plain PCDATA
            List xPressions = UtilXml.childElementList(conditionElement, "XPression");

            if (xPressions != null && xPressions.size() > 0) {
                throw new DefinitionParserException("XPression elements under Condition not yet supported, just use text inside Condition with the expression");
            } else {
                transitionValue.set("conditionExpr", UtilXml.elementValue(conditionElement));
            }
        }

        // Description?
        transitionValue.set("description", UtilXml.childElementValue(transitionElement, "Description"));
        
        // ExtendedAttributes?        
        Element extendedAttributesElement = UtilXml.firstChildElement(transitionElement, "ExtendedAttributes");      
        List extendedAttributes = UtilXml.childElementList(extendedAttributesElement, "ExtendedAttribute");      
        transitionValue.set("extendedAttributes", readExtendedAttributes(extendedAttributes), false);        
    }

    protected void readTransitionRestrictions(List transitionRestrictions, GenericValue activityValue) throws DefinitionParserException {
        if (transitionRestrictions == null || transitionRestrictions.size() == 0)
            return;
        Iterator transitionRestrictionsIter = transitionRestrictions.iterator();

        if (transitionRestrictionsIter.hasNext()) {
            Element transitionRestrictionElement = (Element) transitionRestrictionsIter.next();

            readTransitionRestriction(transitionRestrictionElement, activityValue);
        }
        if (transitionRestrictionsIter.hasNext()) {
            throw new DefinitionParserException("Multiple TransitionRestriction elements found, this is not currently supported. Please remove extras.");
        }
    }

    protected void readTransitionRestriction(Element transitionRestrictionElement, GenericValue activityValue) throws DefinitionParserException {
        String packageId = activityValue.getString("packageId");
        String packageVersion = activityValue.getString("packageVersion");
        String processId = activityValue.getString("processId");
        String processVersion = activityValue.getString("processVersion");
        String activityId = activityValue.getString("activityId");

        // InlineBlock?
        Element inlineBlockElement = UtilXml.firstChildElement(transitionRestrictionElement, "InlineBlock");

        if (inlineBlockElement != null) {
            activityValue.set("isInlineBlock", "Y");
            activityValue.set("blockName", UtilXml.childElementValue(inlineBlockElement, "BlockName"));
            activityValue.set("blockDescription", UtilXml.childElementValue(inlineBlockElement, "Description"));
            activityValue.set("blockIconUrl", UtilXml.childElementValue(inlineBlockElement, "Icon"));
            activityValue.set("blockDocumentationUrl", UtilXml.childElementValue(inlineBlockElement, "Documentation"));

            activityValue.set("blockBeginActivityId", inlineBlockElement.getAttribute("Begin"));
            activityValue.set("blockEndActivityId", inlineBlockElement.getAttribute("End"));
        }

        // Join?
        Element joinElement = UtilXml.firstChildElement(transitionRestrictionElement, "Join");

        if (joinElement != null) {
            String joinType = joinElement.getAttribute("Type");

            if (joinType != null && joinType.length() > 0) {
                activityValue.set("joinTypeEnumId", "WJT_" + joinType);
            }
        }

        // Split?
        Element splitElement = UtilXml.firstChildElement(transitionRestrictionElement, "Split");

        if (splitElement != null) {
            String splitType = splitElement.getAttribute("Type");

            if (splitType != null && splitType.length() > 0) {
                activityValue.set("splitTypeEnumId", "WST_" + splitType);
            }

            // TransitionRefs
            Element transitionRefsElement = UtilXml.firstChildElement(splitElement, "TransitionRefs");
            List transitionRefs = UtilXml.childElementList(transitionRefsElement, "TransitionRef");

            readTransitionRefs(transitionRefs, packageId, packageVersion, processId, processVersion, activityId);
        }
    }

    protected void readTransitionRefs(List transitionRefs, String packageId, String packageVersion, String processId, String processVersion, String activityId) throws DefinitionParserException {        
        if (transitionRefs == null || transitionRefs.size() == 0)
            return;
        Iterator transitionRefsIter = transitionRefs.iterator();

        while (transitionRefsIter.hasNext()) {
            Element transitionRefElement = (Element) transitionRefsIter.next();
            GenericValue transitionRefValue = delegator.makeValue("WorkflowTransitionRef", null);

            values.add(transitionRefValue);

            transitionRefValue.set("packageId", packageId);
            transitionRefValue.set("packageVersion", packageVersion);
            transitionRefValue.set("processId", processId);
            transitionRefValue.set("processVersion", processVersion);
            transitionRefValue.set("activityId", activityId);
            transitionRefValue.set("transitionId", transitionRefElement.getAttribute("Id"));
        }
    }

    // ----------------------------------------------------------------
    // Others
    // ----------------------------------------------------------------

    protected void readParticipants(List participants, String packageId, String packageVersion, String processId, String processVersion, GenericValue valueObject) throws DefinitionParserException {
        if (participants == null || participants.size() == 0)
            return;
        Iterator participantsIter = participants.iterator();
        
        while (participantsIter.hasNext()) {
            Element participantElement = (Element) participantsIter.next();
            String participantId = participantElement.getAttribute("Id");
            GenericValue participantValue = delegator.makeValue("WorkflowParticipant", null);
            
            values.add(participantValue);
            
            participantValue.set("packageId", packageId);
            participantValue.set("packageVersion", packageVersion);
            participantValue.set("processId", processId);
            participantValue.set("processVersion", processVersion);
            participantValue.set("participantId", participantId);
            participantValue.set("participantName", participantElement.getAttribute("Name"));
            
            // ParticipantType
            Element participantTypeElement = UtilXml.firstChildElement(participantElement, "ParticipantType");

            if (participantTypeElement != null) {
                participantValue.set("participantTypeId", participantTypeElement.getAttribute("Type"));
            }

            // Description?
            participantValue.set("description", UtilXml.childElementValue(participantElement, "Description"));

            // ExtendedAttributes
            participantValue.set("partyId", getExtendedAttributeValue(participantElement, "partyId", null), false);
            participantValue.set("roleTypeId", getExtendedAttributeValue(participantElement, "roleTypeId", null), false);            
        }
    }
    
    /*
    protected void readParticipants(List participants, String packageId, String packageVersion, String processId, String processVersion, GenericValue valueObject) throws DefinitionParserException {
        if (participants == null || participants.size() == 0)
            return;

        Long nextSeqId = delegator.getNextSeqId("WorkflowParticipantList");

        if (nextSeqId == null)
            throw new DefinitionParserException("Could not get next sequence id from data source");
        String participantListId = nextSeqId.toString();

        valueObject.set("participantListId", participantListId);

        Iterator participantsIter = participants.iterator();
        long index = 1;

        while (participantsIter.hasNext()) {
            Element participantElement = (Element) participantsIter.next();
            String participantId = participantElement.getAttribute("Id");

            // if participant doesn't exist, create it; don't do an update because if settings are manually changed it would be annoying as all get out
            GenericValue testValue = null;

            try {
                testValue = delegator.findByPrimaryKey("WorkflowParticipant", UtilMisc.toMap("participantId", participantId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
            if (testValue == null) {
                GenericValue participantValue = delegator.makeValue("WorkflowParticipant", null);

                values.add(participantValue);
                participantValue.set("packageId", packageId);
                participantValue.set("packageVersion", packageVersion);
                participantValue.set("processId", processId);
                participantValue.set("processVersion", processVersion);
                participantValue.set("participantId", participantId);
                participantValue.set("participantName", participantElement.getAttribute("Name"));

                // ParticipantType
                Element participantTypeElement = UtilXml.firstChildElement(participantElement, "ParticipantType");

                if (participantTypeElement != null) {
                    participantValue.set("participantTypeId", participantTypeElement.getAttribute("Type"));
                }

                // Description?
                participantValue.set("description", UtilXml.childElementValue(participantElement, "Description"));

                // ExtendedAttributes
                participantValue.set("partyId", getExtendedAttributeValue(participantElement, "partyId", null), false);
                participantValue.set("roleTypeId", getExtendedAttributeValue(participantElement, "roleTypeId", null), false);
            }

            // regardless of whether the participant was created, create a participant list entry
            GenericValue participantListValue = delegator.makeValue("WorkflowParticipantList", null);

            values.add(participantListValue);
            participantListValue.set("participantListId", participantListId);
            participantListValue.set("participantId", participantId);
            participantListValue.set("participantIndex", new Long(index));
            index++;
        }
    }
    */

    protected void readApplications(List applications, String packageId, String packageVersion, String processId,
            String processVersion) throws DefinitionParserException {
        if (applications == null || applications.size() == 0)
            return;
        Iterator applicationsIter = applications.iterator();

        while (applicationsIter.hasNext()) {
            Element applicationElement = (Element) applicationsIter.next();
            GenericValue applicationValue = delegator.makeValue("WorkflowApplication", null);

            values.add(applicationValue);

            String applicationId = applicationElement.getAttribute("Id");

            applicationValue.set("packageId", packageId);
            applicationValue.set("packageVersion", packageVersion);
            applicationValue.set("processId", processId);
            applicationValue.set("processVersion", processVersion);
            applicationValue.set("applicationId", applicationId);
            applicationValue.set("applicationName", applicationElement.getAttribute("Name"));

            // Description?
            applicationValue.set("description", UtilXml.childElementValue(applicationElement, "Description"));

            // FormalParameters?
            Element formalParametersElement = UtilXml.firstChildElement(applicationElement, "FormalParameters");
            List formalParameters = UtilXml.childElementList(formalParametersElement, "FormalParameter");

            readFormalParameters(formalParameters, packageId, packageVersion, processId, processVersion, applicationId);
        }
    }

    protected void readDataFields(List dataFields, String packageId, String packageVersion, String processId,
            String processVersion) throws DefinitionParserException {
        if (dataFields == null || dataFields.size() == 0)
            return;
        Iterator dataFieldsIter = dataFields.iterator();

        while (dataFieldsIter.hasNext()) {
            Element dataFieldElement = (Element) dataFieldsIter.next();
            GenericValue dataFieldValue = delegator.makeValue("WorkflowDataField", null);

            values.add(dataFieldValue);

            String dataFieldId = dataFieldElement.getAttribute("Id");
            String dataFieldName = dataFieldElement.getAttribute("Name");
            if (dataFieldName == null || dataFieldName.length() == 0)
                dataFieldName = dataFieldId;

            dataFieldValue.set("packageId", packageId);
            dataFieldValue.set("packageVersion", packageVersion);
            dataFieldValue.set("processId", processId);
            dataFieldValue.set("processVersion", processVersion);
            dataFieldValue.set("dataFieldId", dataFieldId);
            dataFieldValue.set("dataFieldName", dataFieldName);

            // IsArray attr
            dataFieldValue.set("isArray", ("TRUE".equals(dataFieldElement.getAttribute("IsArray")) ? "Y" : "N"));

            // DataType
            Element dataTypeElement = UtilXml.firstChildElement(dataFieldElement, "DataType");

            if (dataTypeElement != null) {
                // (%Type;)
                readType(dataTypeElement, dataFieldValue);
            }

            // InitialValue?
            dataFieldValue.set("initialValue", UtilXml.childElementValue(dataFieldElement, "InitialValue"));

            // Length?
            String lengthStr = UtilXml.childElementValue(dataFieldElement, "Length");

            if (lengthStr != null && lengthStr.length() > 0) {
                try {
                    dataFieldValue.set("lengthBytes", Long.valueOf(lengthStr));
                } catch (NumberFormatException e) {
                    throw new DefinitionParserException("Invalid whole number format in DataField->Length: " + lengthStr, e);
                }
            }

            // Description?
            dataFieldValue.set("description", UtilXml.childElementValue(dataFieldElement, "Description"));
        }
    }

    protected void readFormalParameters(List formalParameters, String packageId, String packageVersion,
        String processId, String processVersion, String applicationId) throws DefinitionParserException {
        if (formalParameters == null || formalParameters.size() == 0)
            return;
        Iterator formalParametersIter = formalParameters.iterator();
        long index = 1;

        while (formalParametersIter.hasNext()) {
            Element formalParameterElement = (Element) formalParametersIter.next();
            GenericValue formalParameterValue = delegator.makeValue("WorkflowFormalParam", null);

            values.add(formalParameterValue);

            String formalParamId = formalParameterElement.getAttribute("Id");

            formalParameterValue.set("packageId", packageId);
            formalParameterValue.set("packageVersion", packageVersion);
            formalParameterValue.set("processId", processId);
            formalParameterValue.set("processVersion", processVersion);
            formalParameterValue.set("applicationId", applicationId);
            formalParameterValue.set("formalParamId", formalParamId);
            formalParameterValue.set("modeEnumId", "WPM_" + formalParameterElement.getAttribute("Mode"));

            String indexStr = formalParameterElement.getAttribute("Index");

            if (indexStr != null && indexStr.length() > 0) {
                try {
                    formalParameterValue.set("indexNumber", Long.valueOf(indexStr));
                } catch (NumberFormatException e) {
                    throw new DefinitionParserException("Invalid decimal number format in FormalParameter->Index: " + indexStr, e);
                }
            } else
                formalParameterValue.set("indexNumber", new Long(index));
            index++;

            // DataType
            Element dataTypeElement = UtilXml.firstChildElement(formalParameterElement, "DataType");

            if (dataTypeElement != null) {
                // (%Type;)
                readType(dataTypeElement, formalParameterValue);
            }

            // Description?
            formalParameterValue.set("description", UtilXml.childElementValue(formalParameterElement, "Description"));
        }
    }

    /** Reads information about "Type" entity member sub-elements; the value
     * object passed must have two fields to contain Type information:
     * <code>dataTypeEnumId</code> and <code>complexTypeInfoId</code>.
     */
    protected void readType(Element element, GenericValue value) {
        // (%Type;) - (RecordType | UnionType | EnumerationType | ArrayType | ListType | BasicType | PlainType | DeclaredType)
        Element typeElement = null;

        if ((typeElement = UtilXml.firstChildElement(element, "RecordType")) != null) {// TODO: write code for complex type
        } else if ((typeElement = UtilXml.firstChildElement(element, "UnionType")) != null) {// TODO: write code for complex type
        } else if ((typeElement = UtilXml.firstChildElement(element, "EnumerationType")) != null) {// TODO: write code for complex type
        } else if ((typeElement = UtilXml.firstChildElement(element, "ArrayType")) != null) {// TODO: write code for complex type
        } else if ((typeElement = UtilXml.firstChildElement(element, "ListType")) != null) {// TODO: write code for complex type
        } else if ((typeElement = UtilXml.firstChildElement(element, "BasicType")) != null) {
            value.set("dataTypeEnumId", "WDT_" + typeElement.getAttribute("Type"));
        } else if ((typeElement = UtilXml.firstChildElement(element, "PlainType")) != null) {
            value.set("dataTypeEnumId", "WDT_" + typeElement.getAttribute("Type"));
        } else if ((typeElement = UtilXml.firstChildElement(element, "DeclaredType")) != null) {
            // For DeclaredTypes complexTypeInfoId will actually be the type id
            value.set("dataTypeEnumId", "WDT_DECLARED");
            value.set("complexTypeInfoId", typeElement.getAttribute("Id"));
        }

        /*
         <entity entity-name="WorkflowComplexTypeInfo"
         <field name="complexTypeInfoId" type="id-ne"></field>
         <field name="memberParentInfoId" type="id"></field>
         <field name="dataTypeEnumId" type="id"></field>
         <field name="subTypeEnumId" type="id"></field>
         <field name="arrayLowerIndex" type="numeric"></field>
         <field name="arrayUpperIndex" type="numeric"></field>
         */
    }

    protected String getExtendedAttributeValue(Element element, String name, String defaultValue) {
        if (element == null || name == null)
            return defaultValue;

        Element extendedAttributesElement = UtilXml.firstChildElement(element, "ExtendedAttributes");

        if (extendedAttributesElement == null)
            return defaultValue;
        List extendedAttributes = UtilXml.childElementList(extendedAttributesElement, "ExtendedAttribute");

        if (extendedAttributes == null || extendedAttributes.size() == 0)
            return defaultValue;

        Iterator iter = extendedAttributes.iterator();

        while (iter.hasNext()) {
            Element extendedAttribute = (Element) iter.next();
            String elementName = extendedAttribute.getAttribute("Name");

            if (name.equals(elementName)) {
                return extendedAttribute.getAttribute("Value");
            }
        }
        return defaultValue;
    }
    
    // ---------------------------------------------------------
    // RUNTIME, TEST, AND SAMPLE METHODS
    // ---------------------------------------------------------

    public static void main(String[] args) throws Exception {
        String sampleFileName = "../../docs/examples/sample.xpdl";

        if (args.length > 0)
            sampleFileName = args[0];
        List values = readXpdl(UtilURL.fromFilename(sampleFileName), GenericDelegator.getGenericDelegator("default"));
        Iterator viter = values.iterator();

        while (viter.hasNext())
            System.out.println(viter.next().toString());
    }
}

