package com.twitterlite.config;

import java.util.HashSet;
import java.util.Set;

import com.google.api.server.spi.guice.GuiceSystemServiceServletModule;
import com.google.inject.matcher.Matchers;
import com.googlecode.objectify.ObjectifyFilter;
import com.twitterlite.controllers.LoginController;
import com.twitterlite.controllers.MessageController;
import com.twitterlite.controllers.TestServerController;
import com.twitterlite.controllers.UserController;
import com.twitterlite.controllers.interceptors.ControllerInterceptor;
import com.twitterlite.controllers.interceptors.InterceptWith;

public class TwitterLiteEndPointsModule extends GuiceSystemServiceServletModule {

	@Override
	protected void configureServlets() {
		super.configureServlets();
		
		filter("/*").through(new ObjectifyFilter());
		
		Set<Class<?>> serviceClasses = new HashSet<Class<?>>();
	    
		serviceClasses.add(UserController.class);
	    serviceClasses.add(MessageController.class);
	    serviceClasses.add(LoginController.class);
	    serviceClasses.add(TestServerController.class);
	    
	    this.serveGuiceSystemServiceServlet("/_ah/spi/*", serviceClasses);
	    
	    // INTERCEPTORS
	    
	    // We can use interceptors for endpoints method arguments validation following jsr303
	    // We could also use them for caching 
	    // And we can use them for controlling access to methods for specific roles like admins, super admins or current user
	    // however this may not be the most efficient, and using filters may be better
	    
	    // LOGIN INTERCEPTOR
	    
	    // the controller interceptor is used to control any method of a class with one interceptor
 		// we will use it for the LoginInterceptor
 		ControllerInterceptor interceptor = new ControllerInterceptor();
		requestInjection(interceptor);
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(InterceptWith.class), interceptor);
		bindInterceptor(Matchers.annotatedWith(InterceptWith.class), Matchers.any(), interceptor);
	}
}
