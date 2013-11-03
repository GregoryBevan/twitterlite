package twitterlite.managers;

import twitterlite.models.message.Message;
import twitterlite.models.user.User;

import com.google.api.server.spi.response.NotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

public interface MessageManager {
	
	public static interface ManagedMessage {
		public Message read();
		public void update();
		public void delete();
	}
	public ManagedMessage create(String text, Ref<User> sender) throws IllegalArgumentException;
	public ManagedMessage create(String text, String senderKeyStr) throws IllegalArgumentException;
	
	public ManagedMessage get(String keyStr) throws NotFoundException;
	public ManagedMessage get(Key<Message> key) throws NotFoundException;
	
	/*
	 * Get all the messages in the datastore
	 */
	public ListChunk<Message> getAllMessages(String cursorStr, int limit);

	/*
	 * Get all the messages written by a user
	 */
	public ListChunk<Message> getUserMessages(String cursorStr, int limit, Key<User> userKey);
	public ListChunk<Message> getUserMessages(String cursorStr, int limit, String keyStr);
	
	/*
	 * Get all the messages written by users this user follows
	 */
	public ListChunk<Message> getUserTimeLine(String cursorStr, int limit, Key<User> userKey);
	public ListChunk<Message> getUserTimeLine(String cursorStr, int limit, String keyStr);
	
	/*
	 * In a task add/remove the receiver key to/from each message of the sender.
	 * We will start from the most recent messages of the sender
	 */
	public void addUserMessagesReceiver(Key<User> senderKey, Key<User> receiver);
	public void removeUserMessagesReceiver(Key<User> senderKey, Key<User> receiver);
	
	/*
	 * In a task add the receivers to the message index based on the user followers
	 */
	public void addNewMessageReceivers(Key<Message> msgKey, Key<User> followed);
}
