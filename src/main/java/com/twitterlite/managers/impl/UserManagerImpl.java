package com.twitterlite.managers.impl;

import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;
import com.googlecode.objectify.util.TranslatingQueryResultIterator;
import com.twitterlite.managers.ListChunk;
import com.twitterlite.managers.MessageManager;
import com.twitterlite.managers.UserManager;
import com.twitterlite.managers.interceptors.TransactInterceptor.Transact;
import com.twitterlite.models.user.User;
import com.twitterlite.models.user.UserFollowedIndex;
import com.twitterlite.models.user.UserFollowersIndex;
import com.twitterlite.util.CursorUtil;

import static com.google.common.base.Preconditions.*;

import static com.googlecode.objectify.ObjectifyService.*;

@Singleton
public class UserManagerImpl implements UserManager {
	
	private ManagedUserFactory userFactory;
	private MessageManager msgManager;
	
	@Inject
	protected UserManagerImpl(ManagedUserFactory userFactory,
							  MessageManager msgManager) {
		this.userFactory = userFactory;
		this.msgManager = msgManager;
	}
	
	// Assisted Injection
	public static interface ManagedUserFactory {
		public abstract ManagedUserImpl create(User user);
	}
	
	public static void checkThatLoginAndEmailNotUsed(String login, String email) {
		// check that login, or email already used in DB
		// Only Ancestor queries are allowed inside transactions
		boolean loginUsed = ofy().transactionless().load().type(User.class).filter("login", login).limit(1).count() == 1;
		boolean emailUsed = ofy().transactionless().load().type(User.class).filter("email", email).limit(1).count() == 1;
		
		if (loginUsed)
			throw new IllegalArgumentException("login already exists in the database");
		if (emailUsed)
			throw new IllegalArgumentException("email already exists in the database");
	}

	@Override
	public Boolean isUserFollowing(Key<User> followedKey, Key<User> followerKey) {
		UserFollowedIndex index = ofy().load().type(UserFollowedIndex.class).ancestor(followerKey).filter("followed", followedKey).first().now();
		if (index != null)
			return Boolean.TRUE;
		else
			return Boolean.FALSE;
	}
	
	@Override
	public Boolean isUserFollowing(String followedKeyStr, String followerKeyStr) {
		Key<User> followerKey = Key.create(followerKeyStr);
		Key<User> followedKey = Key.create(followedKeyStr);
		return isUserFollowing(followedKey, followerKey);
	}

	@Override
	@Transact(TxnType.REQUIRED)
	public ManagedUser create(final String login, final String email) throws IllegalArgumentException {

		checkThatLoginAndEmailNotUsed(login, email);

		User user = new User(login, email);
		Key<User> usrKey = ofy().save().entity(user).now();
		ofy().save().entities(new UserFollowedIndex(usrKey), new UserFollowersIndex(usrKey));

		return userFactory.create(user);
	}

	public static class ManagedUserImpl implements ManagedUser {
		private User user;
		
		@Inject
		public ManagedUserImpl(@Assisted User user) {
			this.user = user;
		}
		@Override
		public User read() {
			return this.user;
		}
		@Override
		@Transact(TxnType.REQUIRED)
		public void update() {
			ofy().save().entities(this.user);
		}
		@Override
		@Transact(TxnType.REQUIRED)
		public void delete() {
			ofy().delete().entities(this.user);
			QueryKeys<UserFollowedIndex> followedIndexKeys = ofy().load().type(UserFollowedIndex.class).ancestor(user).keys();
			QueryKeys<UserFollowersIndex> followersIndexKeys = ofy().load().type(UserFollowersIndex.class).ancestor(user).keys();
			ofy().delete().keys(followedIndexKeys);
			ofy().delete().keys(followersIndexKeys);
		}
	}
	
	@Override
	public ManagedUser get(Key<User> key) throws NotFoundException {
		User user = ofy().load().key(key).now();
		if (user == null)
			throw new NotFoundException("No such user: " + key.getString());
		return userFactory.create(user);
	}
	
	@Override
	public ManagedUser get(String keyStr) throws NotFoundException {
		Key<User> key = Key.create(keyStr);
		return get(key);
	}
	
	@Override
	public ManagedUser get(String login, String email) throws NotFoundException {
		checkNotNull(login);
		checkNotNull(email);
		User user = ofy().load().type(User.class).filter("login", login).filter("email", email).first().now();
		
		if (user == null)
			throw new NotFoundException("No such user: " + login + ", " + email);
		
		return userFactory.create(user);
	}

	@Override
	public void followUser(String followerKey, String followedKey) {
		checkNotNull(followedKey);
		checkNotNull(followerKey);
		Key<User> follower = Key.create(followerKey);
		Key<User> followed = Key.create(followedKey);
		
		followUser(follower, followed);
	}
	
	@Override
	@Transact(TxnType.REQUIRED)
	public void followUser(Key<User> follower, Key<User> followed) {
		UserFollowedIndex followedIndex = ofy().load().type(UserFollowedIndex.class).ancestor(follower).first().now();
		UserFollowersIndex followersIndex = ofy().load().type(UserFollowersIndex.class).ancestor(followed).first().now();
		
		followersIndex.getFollowers().add(follower);
		followedIndex.getFollowed().add(followed);
		
		ofy().save().entities(followedIndex, followersIndex);
		msgManager.addUserMessagesReceiver(followed, follower);
	}
	
	@Override
	public void unFollowUser(String followerKey, String followedKey) {
		checkNotNull(followedKey);
		checkNotNull(followerKey);
		Key<User> follower = Key.create(followerKey);
		Key<User> followed = Key.create(followedKey);
		
		unFollowUser(follower, followed);
	}
	
	@Override
	@Transact(TxnType.REQUIRED)
	public void unFollowUser(Key<User> follower, Key<User> followed) {
		UserFollowedIndex followedIndex = ofy().load().type(UserFollowedIndex.class).ancestor(follower).first().now();
		UserFollowersIndex followersIndex = ofy().load().type(UserFollowersIndex.class).ancestor(followed).first().now();
		
		followersIndex.getFollowers().remove(follower);
		followedIndex.getFollowed().remove(followed);
		
		ofy().save().entities(followedIndex, followersIndex);
		msgManager.removeUserMessagesReceiver(followed, follower);
	}
	
	@Override
	public ListChunk<User> getAllUsers(String encodedCursor, int limit) {
		Cursor cursor = CursorUtil.safeFromEncodedString(encodedCursor);
		Query<User> query = ofy().load().type(User.class).order("-creation").limit(limit);
		QueryResultIterable<User> iterable = query.startAt(cursor).iterable();
		return new ListChunk<>(iterable);  
	}

	@Override
	public ListChunk<User> getUserFollowers(String cursorStr, int limit, String userKey) {
		checkNotNull(userKey);
		Key<User> user = Key.create(userKey);
		return getUserFollowers(cursorStr, limit, user);
	}
	
	@Override
	public ListChunk<User> getUserFollowers(String cursorStr, int limit, Key<User> key) {
		Cursor cursor = CursorUtil.safeFromEncodedString(cursorStr);
		QueryResultIterator<Key<UserFollowedIndex>> keys = ofy().load()
																.type(UserFollowedIndex.class)
																.limit(limit)
																.startAt(cursor)
																.filter("followed", key)
																.keys()
																.iterator();
		
		TranslatingQueryResultIterator<Key<UserFollowedIndex>, Key<User>> it = new TranslatingQueryResultIterator<Key<UserFollowedIndex>, Key<User>>(keys) {
			@Override
			protected Key<User> translate(Key<UserFollowedIndex> from) {
				return from.getParent();
			}
		};
		Iterable<User> followers = ofy().load().keys(ListChunk.copyQueryResultIterator(it)).values();
		return new ListChunk<>(followers, it.getCursor());
	}
	
	@Override
	public ListChunk<User> getUserFollowed(String cursorStr, int limit, String userKey) {
		checkNotNull(userKey);
		Key<User> user = Key.create(userKey);
		return getUserFollowed(cursorStr, limit, user);
	}

	@Override
	public ListChunk<User> getUserFollowed(String cursorStr, int limit, Key<User> key) {
		Cursor cursor = CursorUtil.safeFromEncodedString(cursorStr);
		QueryResultIterator<Key<UserFollowersIndex>> keys = ofy().load()
																.type(UserFollowersIndex.class)
																.limit(limit)
																.startAt(cursor)
																.filter("followers", key)
																.keys()
																.iterator();
		TranslatingQueryResultIterator<Key<UserFollowersIndex>, Key<User>> it = new TranslatingQueryResultIterator<Key<UserFollowersIndex>, Key<User>>(keys) {
			@Override
			protected Key<User> translate(Key<UserFollowersIndex> from) {
				return from.getParent();
			}
		};
		Iterable<User> followed = ofy().load().keys(ListChunk.copyQueryResultIterator(it)).values();
		return new ListChunk<>(followed, it.getCursor());
	}

//	@Override
//	public ListChunk<User> getFriends(String cursorStr, int limit, Key<User> key) {
//		Cursor cursor = CursorUtil.safeFromEncodedString(cursorStr);
//		UserFollowIndex index = ofy().load().type(UserFollowIndex.class).ancestor(key).first().now();
//		QueryResultIterable<Key<UserFollowIndex>> commonKeys = ofy().load().type(UserFollowIndex.class)
//																	.ancestor(key)
//																	.startAt(cursor)
//																	.limit(limit)
//																	// not sure this will work because it may compare 
//																	// the whole list and not each element
//																	.filter("followers", index.getFollowed()) 
//																	.filter("followed", index.getFollowers())
//																	.keys()
//																	.iterable();
//		List<Key<User>> userKeys = new LinkedList<>();
//		for (Key<UserFollowIndex> k : commonKeys) {
//			Key<User> parent = k.getParent();
//			userKeys.add(parent);
//		}
//		Iterable<User> followers = ofy().load().keys(userKeys).values();
//		return new ListChunk<>(followers, commonKeys.iterator().getCursor());	
//	}
}