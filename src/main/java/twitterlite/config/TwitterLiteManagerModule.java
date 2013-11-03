package twitterlite.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpSession;

import twitterlite.managers.MessageManager;
import twitterlite.managers.MessageManager.ManagedMessage;
import twitterlite.managers.UserManager;
import twitterlite.managers.UserManager.ManagedUser;
import twitterlite.managers.impl.MessageManagerImpl;
import twitterlite.managers.impl.MessageManagerImpl.ManagedMessageImpl;
import twitterlite.managers.impl.MessageManagerImpl.ManagedMessageImplFactory;
import twitterlite.managers.impl.UserManagerImpl;
import twitterlite.managers.impl.UserManagerImpl.ManagedUserFactory;
import twitterlite.managers.impl.UserManagerImpl.ManagedUserImpl;
import twitterlite.managers.interceptors.TransactInterceptor;
import twitterlite.managers.interceptors.TransactInterceptor.Transact;
import twitterlite.models.message.Message;
import twitterlite.models.message.MessageReceiversIndex;
import twitterlite.models.user.User;
import twitterlite.models.user.UserFollowedIndex;
import twitterlite.models.user.UserFollowersIndex;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;

public class TwitterLiteManagerModule extends AbstractModule {

	@BindingAnnotation
	@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public static @interface CurrentUser {}
	
	@Provides @CurrentUser public Optional<Key<User>> getCurrentUser(HttpSession session) {
		String key_str = (String)session.getAttribute(CONSTANTS.SESSION.CURRENT_USER_KEY);
		Key<User> key;
		if (key_str != null)
			key = Key.create(key_str);
		else
			key = null;
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
