package com.twitterlite.controllers;

import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlecode.objectify.Key;
import com.twitterlite.config.CONSTANTS;
import com.twitterlite.config.TwitterLiteManagerModule.CurrentUser;
import com.twitterlite.managers.UserManager;
import com.twitterlite.models.user.User;
import com.twitterlite.models.user.User.UserGetDTO;

import static com.google.common.base.Preconditions.*;

@Singleton
@Api(
	name = "auth",
	version = "v1")
public class LoginController {

	UserManager userManager;
	@CurrentUser Provider<Optional<Key<User>>> currentUserProvider;
	
	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	// TODO: log everywhere
	
	@Inject
	public LoginController(UserManager userManager, @CurrentUser Provider<Optional<Key<User>>> currentUserProvider) {
		this.userManager = userManager;
		this.currentUserProvider = currentUserProvider;
	}
	
	public static class LoginDTO {
		public @CheckForNull String login;
		public @CheckForNull String email;
		public void setLogin(String login) {
			this.login = login;
		}
		public void setEmail(String email) {
			this.email = email;
		}
	}
	
	@ApiMethod(
			name = "login",
			path = "",
			httpMethod = HttpMethod.POST
	)
	public UserGetDTO login(LoginDTO dto, HttpServletRequest req) throws BadRequestException, NotFoundException {
		Key<User> currentUserKey;
		try {
			checkNotNull(dto.login);
			checkNotNull(dto.email);
			User usr = userManager.get(dto.login, dto.email).read();
			currentUserKey = usr.getKey();
			
			MemcacheService mem = MemcacheServiceFactory.getMemcacheService();
			mem.put(CONSTANTS.MEMCACHE.CURRENT_USER_KEY + req.getRemoteAddr(), currentUserKey.getString(), Expiration.byDeltaSeconds(CONSTANTS.MEMCACHE.SESSION_EXPIRATION_SEC));
			log.info("USER LOGGED IN : " + dto.login + " : email : " + dto.email);
			log.info("SESSION ID : " + req.getRemoteAddr());
			return UserGetDTO.get(usr);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new BadRequestException(e);
		}
	}
	
	@ApiMethod(
			name = "logout",
			path = "",
			httpMethod = HttpMethod.POST
	)
	public void logout(HttpServletRequest req) throws BadRequestException {
		if (currentUserProvider.get().isPresent()) {
			MemcacheService mem = MemcacheServiceFactory.getMemcacheService();
			mem.delete(CONSTANTS.MEMCACHE.CURRENT_USER_KEY + req.getRemoteAddr());
		}
		else 
			throw new BadRequestException("You must be logged in to logout.");
		
	}
}