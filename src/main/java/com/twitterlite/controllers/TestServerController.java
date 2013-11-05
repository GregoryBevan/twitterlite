package com.twitterlite.controllers;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.twitterlite.config.TwitterLiteManagerModule.CurrentUser;
import com.twitterlite.managers.MessageManager;
import com.twitterlite.managers.UserManager;
import com.twitterlite.models.user.User;

@Singleton
@Api(
	name = "test",
	version = "v1")
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
	
//	public static class GenerateEntityTask<T> implements DeferredTask {
//		
//		private List<T> entities;
//		private Integer count;
//		
//		public static interface EntityGenerator<T> {
//			List<T>entities(Integer count);
//		}
//		
//		public GenerateEntityTask(List<T> entities, Integer count) {
//			this.entities = entities;
//			this.count = count;
//			
//		}
//		
//		@Override
//		public void run() {
//			
//			
//		}
//	}
	
	@ApiMethod(
			name = "generate.server",
			path = "generate/server",
			httpMethod = HttpMethod.POST
	)
	public void generateServer(	@Named("usersNumber") Integer usersNumber, 
								@Named("msgsNumber") Integer msgsNumber,
								@Named("followers") Integer followersNumber
								) throws BadRequestException, NotFoundException {
		
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
}