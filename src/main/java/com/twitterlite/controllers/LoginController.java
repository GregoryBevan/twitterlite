package com.twitterlite.controllers;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
import com.twitterlite.config.CONSTANTS;
import com.twitterlite.config.TwitterLiteManagerModule.CurrentUser;
import com.twitterlite.managers.UserManager;
import com.twitterlite.models.user.User;

import static com.google.common.base.Preconditions.*;

@Singleton
@Api(
	name = "auth",
	version = "v1")
public class LoginController {

	UserManager userManager;
	Provider<Optional<Key<User>>> currentUserProvider;
	
//	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	// TODO: log everywhere
	
	@Inject
	public LoginController(UserManager userManager, @CurrentUser Provider<Optional<Key<User>>> currentUserProvider) {
		this.userManager = userManager;
		this.currentUserProvider = currentUserProvider;
	}
	
	public static class LoginDTO {
		public @CheckForNull String login;
		public @CheckForNull String email;
	}
	
	@ApiMethod(
			name = "login",
			path = "",
			httpMethod = HttpMethod.POST
	)
	public void login(LoginDTO dto, HttpServletRequest req) throws BadRequestException, NotFoundException {
		Key<User> currentUserKey;
		try {
			checkNotNull(dto.login);
			checkNotNull(dto.email);
			currentUserKey = userManager.get(dto.login, dto.email).read().getKey();
			req.getSession().setAttribute(CONSTANTS.SESSION.CURRENT_USER_KEY, currentUserKey.getString());
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
			HttpSession session = req.getSession(false);
			if (session != null) {
				session.removeAttribute(CONSTANTS.SESSION.CURRENT_USER_KEY);
				session.invalidate();
			}
		}
		else 
			throw new BadRequestException("You must be logged in to logout.");
		
	}
}