package com.twitterlite.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.twitterlite.managers.MessageManager;
import com.twitterlite.managers.MessageManager.ManagedMessage;
import com.twitterlite.managers.UserManager;
import com.twitterlite.managers.UserManager.ManagedUser;
import com.twitterlite.managers.impl.MessageManagerImpl;
import com.twitterlite.managers.impl.MessageManagerImpl.ManagedMessageImpl;
import com.twitterlite.managers.impl.MessageManagerImpl.ManagedMessageImplFactory;
import com.twitterlite.managers.impl.UserManagerImpl;
import com.twitterlite.managers.impl.UserManagerImpl.ManagedUserFactory;
import com.twitterlite.managers.impl.UserManagerImpl.ManagedUserImpl;
import com.twitterlite.managers.interceptors.TransactInterceptor;
import com.twitterlite.managers.interceptors.TransactInterceptor.Transact;
import com.twitterlite.models.message.Message;
import com.twitterlite.models.message.MessageReceiversIndex;
import com.twitterlite.models.user.User;
import com.twitterlite.models.user.UserFollowedIndex;
import com.twitterlite.models.user.UserFollowersIndex;

public class TwitterLiteManagerModule extends AbstractModule {

	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	@BindingAnnotation
	@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public static @interface CurrentUser {}
	
	@Provides @CurrentUser public Optional<Key<User>> getCurrentUser(HttpServletRequest req) {
		log.info("CURRENT USER PROVIDER : remote address" + req.getRemoteAddr());
		MemcacheService mem = MemcacheServiceFactory.getMemcacheService();
		// This is not secure.... it is a temporary work around
		String key_str = (String)mem.get(CONSTANTS.MEMCACHE.CURRENT_USER_KEY + req.getRemoteAddr());
		Key<User> key;
		if (key_str != null) {
			log.info("USER IS PRESENT");
			key = Key.create(key_str);
		}
		else {
			log.info("USER IS ABSENT");
			key = null;
		}
		return Optional.fromNullable(key);
	}
	
	@Override
	protected void configure() {
		
		ObjectifyService.register(User.class);
		ObjectifyService.register(UserFollowedIndex.class);
		ObjectifyService.register(UserFollowersIndex.class);
		ObjectifyService.register(Message.class);
		ObjectifyService.register(MessageReceiversIndex.class);
		
		// INTERCEPTORS
		// this transaction interceptor is presented in the Objectify documentation
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transact.class), new TransactInterceptor());
		
		// USER CONFIG BINDINGS
		bind(UserManager.class).to(UserManagerImpl.class);
		install(new FactoryModuleBuilder().implement(ManagedUser.class, ManagedUserImpl.class).build(ManagedUserFactory.class));
		
		// MESSAGE CONFIG BINDINGS
		bind(MessageManager.class).to(MessageManagerImpl.class);
		install(new FactoryModuleBuilder().implement(ManagedMessage.class, ManagedMessageImpl.class).build(ManagedMessageImplFactory.class));
	}
}
