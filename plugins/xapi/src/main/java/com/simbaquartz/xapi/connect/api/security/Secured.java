package com.simbaquartz.xapi.connect.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;

/**
 * Use @Secured annotation on end points that needs to be secured, would check for valid authorization header.
 * To secure an entire resources class just add @Secured to the class, all class methods will be secured automatically.
 * 	e.g. 
 * 		@Secured
 *    	@Path("/customers")
 *		public class CustomersApi  {}
 * To secure individual methods use @Secured at the method level 
 * 	e.g. 
 * 		@Secured
 *    	@Path("/customers")
 *		public class CustomersApi  {
 *			@Secured
 *			@POST
 *		    @Produces({ "application/json" })
 *		    public Response createCustomer(@HeaderParam("Authorization") String authorization,@Context SecurityContext securityContext)
 *		    throws NotFoundException {
 *		        return delegate.createCustomer(authorization,securityContext);
 *		    }
 *		}	
 * Implemented by com.simbaquartz.xapi.connect.api.security.AuthenticationFilter
 * @author mssidhu
 *
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured { }
