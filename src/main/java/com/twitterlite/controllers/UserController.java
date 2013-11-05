package com.twitterlite.controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
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
import com.twitterlite.config.TwitterLiteManagerModule.CurrentUser;
import com.twitterlite.controllers.interceptors.InterceptWith;
import com.twitterlite.controllers.interceptors.LoginInterceptor;
import com.twitterlite.controllers.interceptors.LoginInterceptor.LoginNotNeeded;
import com.twitterlite.managers.ListChunk;
import com.twitterlite.managers.UserManager;
import com.twitterlite.managers.UserManager.ManagedUser;
import com.twitterlite.models.user.User;
import com.twitterlite.models.user.User.UserGetDTO;
import com.twitterlite.models.user.User.UserSetDTO;
import com.twitterlite.util.BeanExtraUtils;

import static com.google.common.base.Preconditions.*;

@Singleton
@InterceptWith(LoginInterceptor.class)
@Api(
	name = "user",
	version = "v1")
public class UserController {
	
	private UserManager userManager;
	@CurrentUser private Provider<Optional<Key<User>>> currentUserProvider;
	
//	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	// TODO: log everywhere
	
	@Inject
	public UserController(	@CurrentUser 
							Provider<Optional<Key<User>>> currentUserProvider,
							UserManager usrManager) {
		this.userManager = usrManager;
		this.currentUserProvider = currentUserProvider;
	}
	
	public static class CreateUserdDTO {
		@CheckForNull public String email;
		@CheckForNull public String login;
		public void setEmail(String email) {
			this.email = email;
		}
		public void setLogin(String login) {
			this.login = login;
		}
	}
	
	public UserGetDTO updateUserDTOMetadata(UserGetDTO dto) {
		Key<User> currentUserKey = currentUserProvider.get().orNull();
		if (currentUserKey != null && dto.userKey != null)  
			dto.isFollowedByCurrentUser = userManager.isUserFollowing(dto.userKey, currentUserKey.getString());
		return dto;
	}
	
	@ApiMethod(
			name = "create",
			path = "user",
			httpMethod = HttpMethod.POST
	)
	@LoginNotNeeded
	public UserGetDTO createUser(CreateUserdDTO udto) throws BadRequestException {
		try {
			checkNotNull(udto.login);
			checkNotNull(udto.email);
			return UserGetDTO.get(userManager.create(udto.login, udto.email).read());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (NullPointerException e) {
			throw new BadRequestException("email and login are mandatory fields.");
		}
	}
	
	@ApiMethod(
			name = "read",
			path = "user/{userKey}",
			httpMethod = HttpMethod.GET
		)
	public UserGetDTO getUser(@Named("userKey") String keyStr) throws NotFoundException {
		UserGetDTO dto = UserGetDTO.get(userManager.get(keyStr).read());
		return updateUserDTOMetadata(dto); 
	}
	
	@ApiMethod(
			name = "patch", // had to call it patch to overwrite the method user.patch created by the system
							// which yields a 404 when called
			path = "user/{userKey}",
			httpMethod = HttpMethod.PUT
		)
	public void updateUser(@Named("userKey") String keyStr, UserSetDTO dto) throws NotFoundException, BadRequestException {
		ManagedUser mUser = userManager.get(keyStr);
		try {
			// TODO: here we should check that we can't set a new login or email that already exists in the db
			BeanExtraUtils.copyOnlyNonNullProperties(mUser.read(), dto);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new BadRequestException(e.getMessage());
		}
		mUser.update();
	}
	
	@ApiMethod(
			name = "delete",
			path = "user/{userKey}",
			httpMethod = HttpMethod.DELETE
		)
	public void deleteUser(@Named("userKey") String keyStr) throws NotFoundException {
		userManager.get(keyStr).delete();
	}
	
	@ApiMethod(
			name = "follow",
			path = "follow",
			httpMethod = HttpMethod.POST
		)
	public void followUser(	@Named("followerKey") String followerKey, 
							@Named("followedKey") String followedKey) throws BadRequestException {
		try {
			userManager.followUser(followerKey, followedKey);
		} catch (NullPointerException | IllegalArgumentException e) {
			throw new BadRequestException(e.getCause());
		}
	}
	
	@ApiMethod(
			name = "unfollow",
			path = "unfollow",
			httpMethod = HttpMethod.GET
		)
	public void unFollowUser(	@Named("unFollowerKey") String unFollowerKey, 
								@Named("unFollowedKey") String unFollowedKey) throws BadRequestException {
		try {
			userManager.unFollowUser(unFollowerKey, unFollowedKey);
		} catch (NullPointerException | IllegalArgumentException e) {
			throw new BadRequestException(e.getCause());
		}
	}
	
	public static class UsersCollection {
		public UsersCollection(List<UserGetDTO> list, String cursor) {
			super();
			this.list = list;
			this.cursor = cursor;
		}
		public List<UserGetDTO> list;
		public String cursor;
		public List<UserGetDTO> getList() {
			return list;
		}
		public String getCursor() {
			return cursor;
		}
	}
	
	@ApiMethod(
			name = "list",
			path = "user",
			httpMethod = HttpMethod.GET
		)
	public UsersCollection getUsers(@Named("limit") Integer limit,
										@Nullable @Named("cursor") String encodedCursor) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<User> users = userManager.getAllUsers(encodedCursor, limit.intValue());
		List<UserGetDTO> dtos = new LinkedList<UserGetDTO>();
		for (User user : users.chunk)
			dtos.add(updateUserDTOMetadata(UserGetDTO.get(user)));
		
		return new UsersCollection(dtos, users.getEncodedCursor());
	}
	
	@ApiMethod(
			name = "followers",
			path = "followers",
			httpMethod = HttpMethod.GET
		)
	public UsersCollection getUserFollowers(@Named("limit") Integer limit,
												@Named("userKey") String userKey,
												@Nullable @Named("cursor") String encodedCursor) {
		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<User> users = userManager.getUserFollowers(encodedCursor, limit.intValue(), userKey);
		List<UserGetDTO> dtos = new LinkedList<UserGetDTO>();
		for (User user : users.chunk)
			dtos.add(updateUserDTOMetadata(UserGetDTO.get(user)));

		return new UsersCollection(dtos, users.getEncodedCursor());
	}
	
	@ApiMethod(
			name = "followed",
			path = "followed",
			httpMethod = HttpMethod.GET
		)
	public UsersCollection getUserFollowed(@Named("limit") Integer limit,
												@Named("userKey") String userKey,
												@Nullable @Named("cursor") String encodedCursor) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<User> users = userManager.getUserFollowed(encodedCursor, limit.intValue(), userKey);
		List<UserGetDTO> dtos = new LinkedList<UserGetDTO>();
		for (User user : users.chunk)
			// for the moment leave it like this, however this should all have the metadata set to true
			dtos.add(updateUserDTOMetadata(UserGetDTO.get(user)));
		
		return new UsersCollection(dtos, users.getEncodedCursor());
	}
}