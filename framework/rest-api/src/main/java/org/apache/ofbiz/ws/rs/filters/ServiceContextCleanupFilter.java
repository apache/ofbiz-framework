package org.apache.ofbiz.ws.rs.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import org.apache.ofbiz.ws.rs.ServiceNameContextHolder;

@Provider
public class ServiceContextCleanupFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        ServiceNameContextHolder.clear(); // ✅ runs after ExceptionMapper
    }
}
