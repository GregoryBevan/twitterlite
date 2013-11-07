package com.twitterlite.models.message;

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
import com.twitterlite.models.user.User;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.google.common.base.Preconditions.*;

@Entity(name = "M_index")
@Cache
@Unindex
public class MessageReceiversIndex extends BaseModel {

	@Id
	@CheckForNull 
	private Long id = null;
	
	@Index
	private long creation;
	
	@Parent
	private Key<Message> message;
	
	@Index
	private List<Key<User>> receivers;
	
	@Override
	public Key<MessageReceiversIndex> getKey() {
		checkNotNull(this.id);
		return Key.create(message, this.getClass(), this.id.longValue());
	}

	@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Objectify")
	MessageReceiversIndex() {}
	
	public MessageReceiversIndex(Key<Message> message) {
		checkNotNull(message);
		this.message = message;
		this.creation = System.currentTimeMillis();
		this.receivers = new LinkedList<>();
	}
	
	public List<Key<User>> getReceivers() {
		if (this.receivers == null)
			this.receivers = new LinkedList<>();
		return this.receivers;
	}
	public Long getId() {
		checkNotNull(this.id);
		return id;
	}
	public long getCreation() {
		return creation;
	}
	public Key<Message> getMessage() {
		return message;
	}
	public void setMessage(Key<Message> message) {
		this.message = message;
	}
}
