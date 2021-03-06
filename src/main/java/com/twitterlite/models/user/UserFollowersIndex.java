package com.twitterlite.models.user;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import com.twitterlite.models.base.BaseModel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.google.common.base.Preconditions.*;

@Entity(name = "U_Followers")
@Cache
@Unindex
public class UserFollowersIndex extends BaseModel{

	@Id
	@CheckForNull 
	private Long id = null;
	
	@Index
	private long creation;
	
	@Parent
	Key<User> user;
	
	@Index
	private List<Key<User>> followers;
	
	@Override
	public Key<? extends BaseModel> getKey() {
		checkNotNull(this.id);
		return Key.create(user, this.getClass(), this.id.longValue());
	}

	@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Objectify")
	UserFollowersIndex() {}
	
	public UserFollowersIndex(Key<User> user) {
		checkNotNull(user);
		this.user = user;
		this.creation = System.currentTimeMillis();
		this.followers = new LinkedList<>();
	}
	public List<Key<User>> getFollowers() {
		if (followers == null)
			this.followers = new LinkedList<>();
		return this.followers;
	}
	public Long getId() {
		checkNotNull(this.id);
		return id;
	}
	public long getCreation() {
		return creation;
	}
	public Key<User> getUser() {
		return user;
	}
}
