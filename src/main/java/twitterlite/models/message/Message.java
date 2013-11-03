package twitterlite.models.message;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import javax.annotation.CheckForNull;

import twitterlite.models.LoadGroups.WithSender;
import twitterlite.models.base.BaseModel;
import twitterlite.models.user.User;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Unindex;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Cache
@Unindex
public class Message extends BaseModel {

	@Id
	@CheckForNull 
	private Long id = null;
	
	@Index
	private long creation;
	
	@Index
	@Load(WithSender.class)
	private Ref<User> sender;
	
	private String text;
	
	@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Objectify")
	Message() {}
	
	public Message(String text, Ref<User> sender) {
		checkNotNull(text);
		checkNotNull(sender);
		
		// Some basic validation
		// this test could be directly annotated on the property
		// but let's stay simple
		if (text.length() > 140)
			throw new IllegalArgumentException("text length should be less than 140 characters.");
		
		this.creation = System.currentTimeMillis();
		this.sender = sender;
		this.text = text;
	}
	
	@Override
	public Key<Message> getKey() {
		checkNotNull(this.id);
		return Key.create(this.getClass(), this.id.longValue());
	}
	
	/*
	 * This class is needed as the Endpoints library 
	 * does not support parameterized types T Object<T> for the moment.
	 * 
	 * Unfortunately we use Objectify's Key<T> and Ref<T> types extensively in our models
	 * for referencing other entities. We could just stores keys as strings in the entities
	 * but we would loose some very convenient Objectify's features (ex: loadgroups)
	 * 
	 * Here we use a no-arguments constructor and 
	 * public properties so that Endpoints which uses
	 * jackson and bean utils can create this bean even from an incomplete
	 * json representation of this object. Ex: {firstName:"toto"}.
	 * Any property missing from the json will just be null.
	 * 
	 * This is very convenient for updating the model however
	 * it is important to acknowledge that we need the getters for the dto properties 
	 * in order to copy the properties between beans.
	 * 
	 * Notice how the userKey getter is missing 
	 * because the User Bean does not have a key property but must overwrite 
	 * the abstract method getKey from the BaseModel.
	 * 
	 */
	public static class MessageDTO implements Serializable { 
		private static final long serialVersionUID = 1L;
		@CheckForNull public String text;
		@CheckForNull public String messageKey;
		@CheckForNull public String senderKey;
		@CheckForNull public Long creation;
		
		public MessageDTO(){}
		
		public static MessageDTO get(Message message) {
			MessageDTO dto = new MessageDTO();
			dto.text = message.text;
			dto.creation = message.creation;
			dto.senderKey = message.sender.getKey().getString();
			dto.messageKey = message.getKey().getString();
			return dto;
		}
		public @CheckForNull String getText() {
			return text;
		}
	}

	/*	
	 * 	GETTERS AND SETTERS BOILERPLATE
	 */
	public Long getId() {
		checkNotNull(this.id);
		return id;
	}
	public Ref<User> getSender() {
		return sender;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public long getCreation() {
		return creation;
	}
}
