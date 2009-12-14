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
package org.ofbiz.birt.report.service;

import java.io.File;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.IBirtConstants;
import org.eclipse.birt.report.context.ViewerAttributeBean;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunTask;
import org.eclipse.birt.report.resource.BirtResources;
import org.eclipse.birt.report.resource.ResourceConstants;
import org.eclipse.birt.report.service.BirtViewerReportService;
import org.eclipse.birt.report.service.ReportEngineService;
import org.eclipse.birt.report.service.ReportEngineService.DummyRemoteException;
import org.eclipse.birt.report.service.api.IViewerReportDesignHandle;
import org.eclipse.birt.report.service.api.InputOptions;
import org.eclipse.birt.report.service.api.ReportServiceException;
import org.eclipse.birt.report.utility.BirtUtility;
import org.eclipse.birt.report.utility.DataUtil;
import org.eclipse.birt.report.utility.ParameterAccessor;
import org.ofbiz.base.util.Debug;
import org.ofbiz.birt.container.BirtContainer;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.jdbc.ConnectionFactory;

public class OFBizBirtViewerReportService extends BirtViewerReportService {
    
    public final static String module = OFBizBirtViewerReportService.class.getName();

    public OFBizBirtViewerReportService(ServletContext servletContext) {
        super(servletContext);
        // TODO Auto-generated constructor stub
    }
    
    public String runReport(IViewerReportDesignHandle design,
            String outputDocName, InputOptions runOptions, Map parameters,
            Map displayTexts, List<Exception> errorList) throws ReportServiceException {
        // TODO Auto-generated method stub
        
        if ( design == null || design.getDesignObject( ) == null )
            throw new ReportServiceException( BirtResources.getMessage( ResourceConstants.GENERAL_EXCEPTION_NO_REPORT_DESIGN ) );

        IReportRunnable runnable;
        HttpServletRequest request = (HttpServletRequest) runOptions.getOption( InputOptions.OPT_REQUEST );
        Locale locale = (Locale) runOptions.getOption( InputOptions.OPT_LOCALE );
        TimeZone timeZone = (TimeZone) runOptions.getOption( InputOptions.OPT_TIMEZONE );

        ViewerAttributeBean attrBean = (ViewerAttributeBean) request.getAttribute( IBirtConstants.ATTRIBUTE_BEAN );
        // Set parameters
        Map parsedParams = attrBean.getParameters( );
        if ( parameters != null )
        {
            parsedParams.putAll( parameters );
        }
        // Set display Text of select parameters
        Map displayTextMap = attrBean.getDisplayTexts( );
        if ( displayTexts != null )
        {
            displayTextMap.putAll( displayTexts );
        }

        runnable = (IReportRunnable) design.getDesignObject( );
        try
        {
            // get maxRows
            Integer maxRows = null;
            if ( ParameterAccessor.isReportParameterExist( request,
                    ParameterAccessor.PARAM_MAXROWS ) )
                maxRows = Integer.valueOf( ParameterAccessor.getMaxRows( request ) );
            
            try {
                // put all app context from Birt Container to Report Engine Service
                ReportEngineService.getInstance().getEngineConfig().getAppContext().putAll(
                        BirtContainer.getReportEngine().getConfig().getAppContext());
                /*
                --- DISABLE JDBC FEATURE
                Connection connection = getConnection();
                BirtContainer.getReportEngine().getConfig().getAppContext().put("OdaJDBCDriverPassInConnection", connection);
                */
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            List<Exception> errors = this.runReport( request,
                            runnable,
                            outputDocName,
                            locale,
                            timeZone,
                            parsedParams,
                            displayTextMap,
                            maxRows );
            if ( errors != null && !errors.isEmpty( ) )
            {
                errorList.addAll( errors );
            }
        }
        catch ( RemoteException e )
        {
            if ( e.getCause( ) instanceof ReportServiceException )
            {
                throw (ReportServiceException) e.getCause( );
            }
            else
            {
                throw new ReportServiceException( e.getLocalizedMessage( ),
                        e.getCause( ) );
            }
        }
        return outputDocName;
    }
    
    /**
     * Run report.
     * 
     * @param request
     * 
     * @param runnable
     * @param archive
     * @param documentName
     * @param locale
     * @param parameters
     * @param displayTexts
     * @param maxRows
     * @return list of exceptions which occured during the run or null
     * @throws RemoteException
     */
    public List<Exception> runReport( HttpServletRequest request,
            IReportRunnable runnable, String documentName, Locale locale,
            TimeZone timeZone, Map parameters, Map displayTexts, Integer maxRows )
            throws RemoteException
    {
        assert runnable != null;

        // Preapre the run report task.
        IRunTask runTask = null;
        try {
            runTask = BirtContainer.getReportEngine().createRunTask( runnable );
        } catch (Exception e) {
            throwDummyException(e);
        }
        runTask.setLocale( locale );
        
        com.ibm.icu.util.TimeZone tz = BirtUtility.toICUTimeZone( timeZone );
        if ( tz != null )
        {
            runTask.setTimeZone( tz );
        }

        runTask.setParameterValues( parameters );

        // set MaxRows settings
        if ( maxRows != null )
            runTask.setMaxRowsPerQuery( maxRows.intValue( ) );

        // add task into session
        BirtUtility.addTask( request, runTask );

        // Set display Text for select parameters
        if ( displayTexts != null )
        {
            Iterator keys = displayTexts.keySet( ).iterator( );
            while ( keys.hasNext( ) )
            {
                String paramName = DataUtil.getString( keys.next( ) );
                String displayText = DataUtil.getString( displayTexts
                        .get( paramName ) );
                runTask.setParameterDisplayText( paramName, displayText );
            }
        }

        // set app context
        Map context = BirtUtility.getAppContext( request );
        runTask.setAppContext( context );

        // Run report.
        try
        {
            runTask.run( documentName );
        }
        catch ( BirtException e )
        {
            // clear document file
            File doc = new File( documentName );
            if ( doc != null )
                doc.delete( );

            throwDummyException( e );
        }
        finally
        {
            // Remove task from http session
            BirtUtility.removeTask( request );

            // Append errors
            if ( ParameterAccessor.isDesigner( ) )
                BirtUtility.error( request, runTask.getErrors( ) );

            runTask.close( );

            // check for non-fatal errors
            List<Exception> errors = (List<Exception>) runTask.getErrors( );
            if ( !errors.isEmpty( ) )
            {
                return errors;
            }
        }
        return null;
    }
    
    /**
     * @see org.eclipse.birt.report.service.api.IViewerReportService#runAndRenderReport(org.eclipse.birt.report.service.api.IViewerReportDesignHandle,
     *      java.lang.String, org.eclipse.birt.report.service.api.InputOptions,
     *      java.util.Map, java.io.OutputStream, java.util.List, java.util.Map)
     */
    public void runAndRenderReport( IViewerReportDesignHandle design,
            String outputDocName, InputOptions options, Map parameters,
            OutputStream out, List activeIds, Map displayTexts )
            throws ReportServiceException
    {
        if ( design == null || design.getDesignObject( ) == null )
            throw new ReportServiceException( BirtResources.getMessage( ResourceConstants.GENERAL_EXCEPTION_NO_REPORT_DESIGN ) );

        HttpServletRequest request = (HttpServletRequest) options.getOption( InputOptions.OPT_REQUEST );

        try
        {
            ViewerAttributeBean attrBean = (ViewerAttributeBean) request.getAttribute( IBirtConstants.ATTRIBUTE_BEAN );
            String reportTitle = ParameterAccessor.htmlDecode( attrBean.getReportTitle( ) );
            IReportRunnable runnable = (IReportRunnable) design.getDesignObject( );

            // get maxRows
            Integer maxRows = null;
            if ( ParameterAccessor.isReportParameterExist( request,
                    ParameterAccessor.PARAM_MAXROWS ) )
                maxRows = Integer.valueOf( ParameterAccessor.getMaxRows( request ) );

            try {
                // put all app context from Birt Container to Report Engine Service
                ReportEngineService.getInstance().getEngineConfig().getAppContext().putAll(
                        BirtContainer.getReportEngine().getConfig().getAppContext());
                /*
                --- DISABLE JDBC FEATURE
                Connection connection = getConnection();
                ReportEngineService.getInstance( ).getEngineConfig().getAppContext().put("OdaJDBCDriverPassInConnection", connection);
                */
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            ReportEngineService.getInstance( ).runAndRenderReport( runnable,
                    out,
                    options,
                    parameters,
                    null,
                    null,
                    null,
                    displayTexts,
                    reportTitle,
                    maxRows );
        }
        catch ( RemoteException e )
        {
            throwReportServiceException( e );
        }
    }
    
    /**
     * get connection
     */
    private Connection getConnection() {
        Connection connection = null;
        try {
            String delegatorGroupHelperName = BirtContainer.getDelegatorGroupHelperName();
            Delegator delegator = BirtContainer.getDelegator();
            Debug.logInfo("Get the JDBC connection from group helper's name:" + delegatorGroupHelperName, module);
            String helperName = delegator.getGroupHelperName(delegatorGroupHelperName);    // gets the helper (localderby, localmysql, localpostgres, etc.) for your entity group org.ofbiz
            connection = ConnectionFactory.getConnection(helperName);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        } catch (SQLException e) {
            Debug.logError(e, module);
        }
        return connection;
    }
    
    /**
     * @param e
     * @throws DummyRemoteException
     */
    private void throwDummyException( Exception e )
            throws DummyRemoteException
    {
        if ( e instanceof ReportServiceException )
        {
            throw new DummyRemoteException(e);
        }
        else
        {
            throw new DummyRemoteException( new ReportServiceException( e
                .getLocalizedMessage( ), e ) );
        }
    }
    
    /**
     * Temporary method for extracting the exception from the
     * DummyRemoteException and throwing it.
     */
    private void throwReportServiceException( RemoteException e )
            throws ReportServiceException
    {
        Throwable wrappedException = e;
        if ( e instanceof ReportEngineService.DummyRemoteException )
        {
            wrappedException = e.getCause( );
        }
        if ( wrappedException instanceof ReportServiceException )
        {
            throw (ReportServiceException) wrappedException;
        }
        else if ( wrappedException != null )
        {
            throw new ReportServiceException( wrappedException.getLocalizedMessage( ),
                    wrappedException );
        }
        else
        {
            throw new ReportServiceException( e.getLocalizedMessage( ), e );
        }
    }
}
