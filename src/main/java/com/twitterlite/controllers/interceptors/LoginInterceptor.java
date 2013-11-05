package com.twitterlite.controllers.interceptors;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlecode.objectify.Key;
import com.twitterlite.config.TwitterLiteManagerModule.CurrentUser;
import com.twitterlite.models.user.User;

@Singleton
public class LoginInterceptor implements MethodInterceptor {

	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	@CurrentUser private Provider<Optional<Key<User>>> provider;
	
	@Inject
	public LoginInterceptor(@CurrentUser Provider<Optional<Key<User>>> provider) {
		this.provider = provider;
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Documented
	public static @interface LoginNotNeeded {}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (invocation.getMethod().getAnnotation(LoginNotNeeded.class) != null)
			return invocation.proceed();

		if (!provider.get().isPresent()) {
			log.info("CURRENT USER IS NOT PRESENT");
			String msg = "Call of a WS not logged in : " + invocation.getMethod() + " !";
			log.warning(msg);
			throw new RuntimeException(msg);
		}
		return invocation.proceed();
	}
}
