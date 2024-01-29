package com.simbaquartz.xapi.connect.api.security;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class ApiSecurityContext implements SecurityContext{
	private Principal loggedInUser;
	private boolean isSecure;
	private String authenticationScheme;

	public ApiSecurityContext(Principal loggedInUser, boolean isSecure, String authenticationScheme){
		this.loggedInUser = loggedInUser;
		this.isSecure = isSecure;
		this.authenticationScheme = authenticationScheme;
	}
	
	@Override
	public Principal getUserPrincipal() {
		return loggedInUser;
	}

	@Override
	public boolean isUserInRole(String role) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isSecure() {
		return isSecure;
	}

	@Override
	public String getAuthenticationScheme() {
		return authenticationScheme;
	}

}
