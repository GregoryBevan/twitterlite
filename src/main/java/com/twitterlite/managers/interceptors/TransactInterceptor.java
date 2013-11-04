package com.twitterlite.managers.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Singleton;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;

@Singleton
public class TransactInterceptor implements MethodInterceptor {

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD})
	public static @interface Transact {
	    TxnType value();
	}
	
	/** Work around java's annoying checked exceptions */
    private static class ExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

            public ExceptionWrapper(Throwable cause) {
                super(cause);
            }
                
            /** This makes the cost of using the ExceptionWrapper negligible */
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        }
	
	@Override
	public Object invoke(final MethodInvocation inv) throws Throwable {
		Transact attr = inv.getStaticPart().getAnnotation(Transact.class);
        TxnType type = attr.value();
        Objectify ofy = ObjectifyService.ofy();
        
        try {
	        return ofy.execute(type, new Work<Object>() {
				@Override
				public Object run() {
					try {
	                  return inv.proceed();
	              }
	              catch (RuntimeException ex) { throw ex; }
	              catch (Throwable th) { throw new ExceptionWrapper(th); }
				}
			});
        } catch (ExceptionWrapper e) { throw e.getCause(); }
    }
}
