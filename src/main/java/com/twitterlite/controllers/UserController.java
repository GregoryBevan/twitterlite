package com.twitterlite.controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
	
//	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	// TODO: log everywhere
	
	@Inject
	public UserController(UserManager usrManager) {
		this.userManager = usrManager;
	}
	
	public static class CreateUserdDTO {
		@CheckForNull public String email;
		@CheckForNull public String login;
	}
	
	@ApiMethod(
			name = "create",
			path = "user",
			httpMethod = HttpMethod.POST
	)
	@LoginNotNeeded
	public void createUser(CreateUserdDTO udto) throws BadRequestException {
		try {
			checkNotNull(udto.login);
			checkNotNull(udto.email);
			userManager.create(udto.login, udto.email);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (NullPointerException e) {
			throw new BadRequestException("email and login are mandatory fields.");
		}
	}
	
	@ApiMethod(
			name = "read",
			path = "user/{key}",
			httpMethod = HttpMethod.GET
		)
	public UserGetDTO getUser(@Named("key") String keyStr) throws NotFoundException {
		return UserGetDTO.get(userManager.get(keyStr).read());
	}
	
	@ApiMethod(
			name = "patch", // had to call it patch to overwrite the method user.patch created by the system
							// which yields a 404 when called
			path = "user/{key}",
			httpMethod = HttpMethod.PUT
		)
	public void updateUser(@Named("key") String keyStr, UserSetDTO dto) throws NotFoundException, BadRequestException {
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
			path = "user/{key}",
			httpMethod = HttpMethod.DELETE
		)
	public void deleteUser(@Named("key") String keyStr) throws NotFoundException {
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
	
	@ApiMethod(
			name = "list",
			path = "user",
			httpMethod = HttpMethod.GET
		)
	public Map<String, Object> getUsers(@Named("limit") Integer limit,
										@Nullable @Named("cursor") String encodedCursor) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<User> users = userManager.getAllUsers(encodedCursor, limit.intValue());
		List<UserGetDTO> dtos = new LinkedList<UserGetDTO>();
		for (User user : users.chunk)
			dtos.add(UserGetDTO.get(user));
		
		Map<String, Object> map = new HashMap<>();
		map.put("list", dtos);
		map.put("cursor", users.getEncodedCursor());
		
		return map;
	}
	
	@ApiMethod(
			name = "followers",
			path = "followers",
			httpMethod = HttpMethod.GET
		)
	public Map<String, Object> getUserFollowers(@Named("limit") Integer limit,
												@Named("key") String userKey,
												@Nullable @Named("cursor") String encodedCursor) {
		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<User> users = userManager.getUserFollowers(encodedCursor, limit.intValue(), userKey);
		List<UserGetDTO> dtos = new LinkedList<UserGetDTO>();
		for (User user : users.chunk)
			dtos.add(UserGetDTO.get(user));
		
		Map<String, Object> map = new HashMap<>();
		map.put("list", dtos);
		map.put("cursor", users.getEncodedCursor());
		
		return map;
	}
	
	@ApiMethod(
			name = "followed",
			path = "followed",
			httpMethod = HttpMethod.GET
		)
	public Map<String, Object> getUserFollowed(@Named("limit") Integer limit,
												@Named("key") String userKey,
												@Nullable @Named("cursor") String encodedCursor) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<User> users = userManager.getUserFollowed(encodedCursor, limit.intValue(), userKey);
		List<UserGetDTO> dtos = new LinkedList<UserGetDTO>();
		for (User user : users.chunk)
			dtos.add(UserGetDTO.get(user));
		
		Map<String, Object> map = new HashMap<>();
		map.put("list", dtos);
		map.put("cursor", users.getEncodedCursor());
		
		return map;
	}
}