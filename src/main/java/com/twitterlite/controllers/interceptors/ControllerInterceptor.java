package com.twitterlite.controllers.interceptors;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import javax.annotation.CheckForNull;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/*
 *	This class is very convenient for intercepting all the methods of a class
 *	As an example, we use it for checking if the user is logged in before any method call
 *	to the endpoints controllers
 */
@Singleton
public class ControllerInterceptor implements MethodInterceptor {

	@Inject @CheckForNull Injector injector;

	private class InterceptorInvocation implements MethodInvocation {

		MethodInvocation invocation;
		
		Class<? extends MethodInterceptor> interceptor;
		
		InterceptorInvocation(MethodInvocation invocation, Class<? extends MethodInterceptor> interceptor) {
			super();
			this.invocation = invocation;
			this.interceptor = interceptor;
		}

		@Override public AccessibleObject getStaticPart() { return invocation.getStaticPart(); }

		@Override public Object getThis() { return invocation.getThis(); }

		@Override public Object[] getArguments() { return invocation.getArguments(); }

		@Override public Method getMethod() { return invocation.getMethod(); }

		@Override
		public Object proceed() throws Throwable {
			assert injector != null;
			return injector.getInstance(interceptor).invoke(invocation);
		}
		
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		
		Class<?> class1 = invocation.getMethod().getDeclaringClass();
		
		InterceptWith interceptWith = class1.getAnnotation(InterceptWith.class);

		if (interceptWith != null)
			for (Class<? extends MethodInterceptor> interceptor : interceptWith.value())
				invocation = new InterceptorInvocation(invocation, interceptor);
		
		return invocation.proceed();
	}
}