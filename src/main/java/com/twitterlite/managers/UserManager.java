package com.twitterlite.managers;

import com.google.api.server.spi.response.NotFoundException;
import com.googlecode.objectify.Key;
import com.twitterlite.models.user.User;

public interface UserManager {
	
	public static interface ManagedUser {
		public User read();
		public void update();
		public void delete();
	}
	public ManagedUser create(String login, String email) throws IllegalArgumentException;
	
	public ManagedUser get(String login, String email) throws NotFoundException;
	public ManagedUser get(String keyStr) throws NotFoundException;
	public ManagedUser get(Key<User> key) throws NotFoundException;
	
	public Boolean isUserFollowing(Key<User> followedKey, Key<User> followerKey);
	public Boolean isUserFollowing(String followedKeyStr, String followerKeyStr);
	
	/*
	 * follow user management
	 */
	public void followUser(Key<User> follower, Key<User> followed);
	public void unFollowUser(Key<User> follower, Key<User> followed);
	
	public void followUser(String followerKey, String followedKey);
	public void unFollowUser(String followerKey, String followedKey);
	
	/*
	 * Get all the users in the datastore
	 */
	public ListChunk<User> getAllUsers(String cursorStr, int limit);
	
	/*
	 * Get all the followers of this user
	 */
	public ListChunk<User> getUserFollowers(String cursorStr, int limit, Key<User> key);
	public ListChunk<User> getUserFollowers(String cursorStr, int limit, String userKey);
	
	/*
	 * Get all the users this user follows
	 */
	public ListChunk<User> getUserFollowed(String cursorStr, int limit, Key<User> key);
	public ListChunk<User> getUserFollowed(String cursorStr, int limit, String userKey);
}
