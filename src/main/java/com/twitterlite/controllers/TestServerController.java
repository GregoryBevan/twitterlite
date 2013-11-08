package com.twitterlite.controllers;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.twitterlite.config.TwitterLiteManagerModule.CurrentUser;
import com.twitterlite.managers.MessageManager;
import com.twitterlite.managers.UserManager;
import com.twitterlite.models.message.Message;
import com.twitterlite.models.message.MessageReceiversIndex;
import com.twitterlite.models.user.User;
import com.twitterlite.models.user.UserFollowedIndex;
import com.twitterlite.models.user.UserFollowersIndex;

import static com.googlecode.objectify.ObjectifyService.*;

@Singleton
@Api(
	name = "test",
	version = "v1"
)
public class TestServerController {

	UserManager userManager;
	MessageManager msgManager;
	Provider<Optional<Key<User>>> currentUserProvider;
	
//	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	// TODO: log everywhere
	
	@Inject
	public TestServerController(UserManager userManager,
								MessageManager msgManager,
								@CurrentUser Provider<Optional<Key<User>>> currentUserProvider) {
		this.userManager = userManager;
		this.msgManager = msgManager;
		this.currentUserProvider = currentUserProvider;
	}
	
	public static class DeleteTestServerTask implements DeferredTask {
		private static final long serialVersionUID = 1L;

		@Override
		public void run() {
			
			// this will certainly throw a DataStore TimeOut Exception if there are too many user entities
			Iterable<Key<User>> it = ofy().load().type(User.class).keys().iterable();
			for (Key<User> key : it) {
				Iterable<Key<UserFollowedIndex>> it2 = ofy().load().type(UserFollowedIndex.class).ancestor(key).keys().iterable();
				Iterable<Key<UserFollowersIndex>> it3 = ofy().load().type(UserFollowersIndex.class).ancestor(key).keys().iterable();
				ofy().delete().entities(it2);
				ofy().delete().entities(it3);
			}
			ofy().delete().entities(it);
			Iterable<Key<Message>> it4 = ofy().load().type(Message.class).keys().iterable();
			for (Key<Message> key : it4) {
				Iterable<Key<MessageReceiversIndex>> it5 = ofy().load().type(MessageReceiversIndex.class).ancestor(key).keys().iterable();
				ofy().delete().entities(it5);
			}
			ofy().delete().entities(it4);
		}
	}
	
	@ApiMethod(
			name = "generate.server",
			path = "generate/server",
			httpMethod = HttpMethod.POST
	)
	public void generateServer(	@Named("usersNumber") Integer usersNumber, 
								@Named("msgsNumber") Integer msgsNumber,
								@Named("followers") Integer followersNumber) {
		
		List<User> users = new LinkedList<>();
		
		for (int i = 1; i < usersNumber + 1; i++)
			users.add(userManager.create("login-" + i, "test@test-" + i +  ".com").read());
		
		int i = 1;
		for (User u : users) {
			int c = 10;
			while (c > 0) {
				--c;
				msgManager.create("login: " + u.getLogin() +  " :: message number nº " + i++, Ref.create(u.getKey()));
			}
			if (i > msgsNumber)
				break;
		}
		
		i = 0;
		User[] usersArray = users.toArray(new User[usersNumber]);
		while (i < followersNumber) {
			int u1 = (int)Math.floor(Math.random() * usersNumber);
			int u2 = (int)Math.floor(Math.random() * usersNumber);
			userManager.followUser(usersArray[u1].getKey(), usersArray[u2].getKey());
			i = i + 2;
		}
	}
	
	@ApiMethod(
			name = "clear.server",
			path = "clear/server",
			httpMethod = HttpMethod.POST
	)
	public void clearServer() {
		QueueFactory.getDefaultQueue().add(TaskOptions
				.Builder
				.withPayload(new DeleteTestServerTask())
				.header("Host", BackendServiceFactory.getBackendService().getBackendAddress("message-backend")));
	}
}