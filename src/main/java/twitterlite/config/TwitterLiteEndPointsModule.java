package twitterlite.config;

import java.util.HashSet;
import java.util.Set;

import twitterlite.controllers.MessageController;
import twitterlite.controllers.UserController;

import com.google.api.server.spi.guice.GuiceSystemServiceServletModule;
import com.googlecode.objectify.ObjectifyFilter;

public class TwitterLiteEndPointsModule extends GuiceSystemServiceServletModule {

	@Override
	protected void configureServlets() {
		super.configureServlets();
		
		filter("/*").through(new ObjectifyFilter());
		
		Set<Class<?>> serviceClasses = new HashSet<Class<?>>();
	    serviceClasses.add(UserController.class);
	    serviceClasses.add(MessageController.class);
	    
	    this.serveGuiceSystemServiceServlet("/_ah/spi/*", serviceClasses);
	    
	    // INTERCEPTORS
	    
	    // We can use interceptors for endpoints method arguments validation following jsr303
	    // We could also use them for caching 
	    // And we can use them for controlling access to methods for specific roles like admins, super admins or current user
	}
}
