package twitterlite.models.user;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;

import twitterlite.models.base.BaseModel;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
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
		return followers;
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
