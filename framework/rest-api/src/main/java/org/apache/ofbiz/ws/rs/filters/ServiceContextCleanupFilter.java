package org.apache.ofbiz.ws.rs.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
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
