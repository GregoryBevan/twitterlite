package com.twitterlite.models.user;

import java.io.Serializable;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.twitterlite.models.base.BaseModel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.google.common.base.Preconditions.*;

@Entity(name = "U")
@Cache
@Unindex
public class User extends BaseModel {

	@Id
	@CheckForNull 
	private Long id = null;
	
	@Index
	private long creation;
	
	@Index
	private String login;
	
	@NotNull
	@Index
	private String email;
	
	@CheckForNull
	@Index
	private String firstName;
	
	@CheckForNull 
	private String lastName;
	
	@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Objectify")
	User() {}
	
	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	
	// We don't use any means of authentication for the sake of simplicity
	public User(String login, String email) {
		checkNotNull(login);
		checkNotNull(email);
		
		// Some basic validation
		// this test could be directly annotated on the property
		// but let's stay simple
		if (login.length() > 100 || login.length() < 3)
			throw new IllegalArgumentException("Login length should be between 3 and 100 characters.");
		
		// This only validates the structure of the string but not the validity of the email address
		if (!Pattern.matches(EMAIL_PATTERN, email))
			throw new IllegalArgumentException("Email is not a valid email address.");
		
		this.creation = System.currentTimeMillis();
		this.login = login;
		this.email = email;
	}
	
	@Override
	public Key<User> getKey() {
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
	public static class UserGetDTO implements Serializable { 
		private static final long serialVersionUID = 1L;
		@CheckForNull public String email;
		@CheckForNull public String login;
		@CheckForNull public String firstName;
		@CheckForNull public String lastName;
		@CheckForNull public String userKey;
		@CheckForNull public Long creation;
		
		// Metadata
		@CheckForNull public Boolean isFollowedByCurrentUser = Boolean.FALSE;
		
		public UserGetDTO(){}
		public static UserGetDTO get(User user) {
			UserGetDTO dto = new UserGetDTO();
			dto.creation = user.creation;
			dto.email = user.email;
			dto.login = user.login;
			dto.firstName = user.firstName;
			dto.lastName = user.lastName;
			dto.userKey = user.getKey().getString();
			return dto;
		}
		public String getEmail() {
			return email;
		}
		public String getLogin() {
			return login;
		}
		public String getFirstName() {
			return firstName;
		}
		public String getLastName() {
			return lastName;
		}
		public String getUserKey() {
			return userKey;
		}
		public Long getCreation() {
			return creation;
		}
		public Boolean getIsFollowedByCurrentUser() {
			return isFollowedByCurrentUser;
		}
	}
	public static class UserSetDTO implements Serializable {
		private static final long serialVersionUID = 1L;
		@CheckForNull private String email;
		@CheckForNull private String login;
		@CheckForNull private String firstName;
		@CheckForNull private String lastName;
		public UserSetDTO(){}
		public void setEmail(String email) {
			this.email = email;
		}
		public void setLogin(String login) {
			this.login = login;
		}
		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
		public @CheckForNull String getEmail() {
			return email;
		}
		public @CheckForNull String getLogin() {
			return login;
		}
		public @CheckForNull String getFirstName() {
			return firstName;
		}
		public @CheckForNull String getLastName() {
			return lastName;
		}
	}
	
	/*	
	 * 	GETTERS AND SETTERS BOILERPLATE
	 */
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public @CheckForNull String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public @CheckForNull String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
