package twitterlite.managers.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;

import twitterlite.managers.ListChunk;
import twitterlite.managers.MessageManager;
import twitterlite.managers.interceptors.TransactInterceptor.Transact;
import twitterlite.models.message.Message;
import twitterlite.models.message.MessageReceiversIndex;
import twitterlite.models.user.User;
import twitterlite.models.user.UserFollowedIndex;
import twitterlite.util.CursorUtil;

import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.repackaged.com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;

public class MessageManagerImpl implements MessageManager {
	
	private ManagedMessageImplFactory msgFactory;
	
	@Inject
	protected MessageManagerImpl(ManagedMessageImplFactory userFactory) {
		this.msgFactory = userFactory;
	}
	
	// Assisted Injection
	public static interface ManagedMessageImplFactory {
		public abstract ManagedMessageImpl create(Message msg);
	}

	@Override
	public ManagedMessage create(String text, String senderKeyStr) throws IllegalArgumentException {
		Key<User> senderKey = Key.create(senderKeyStr);
		Ref<User> sender = Ref.create(senderKey);
		return create(text, sender);
	}
	
	@Override
	@Transact(TxnType.REQUIRED)
	public ManagedMessage create(String text, Ref<User> sender) throws IllegalArgumentException {
		
		Message msg = new Message(text, sender);
		Key<Message> msgKey = ofy( ).save().entity(msg).now();
		ofy().save().entity(new MessageReceiversIndex(msgKey));
		
		addNewMessageReceivers(msgKey, sender.getKey());
		
		return msgFactory.create(msg);
	}

	public static class ManagedMessageImpl implements ManagedMessage {
		private Message msg;
		@Inject
		public ManagedMessageImpl(@Assisted Message msg) {
			this.msg = msg;
		}
		@Override
		public Message read() {
			return this.msg;
		}
		@Override
		@Transact(TxnType.REQUIRED)
		public void update() {
			ofy().save().entities(this.msg);
		}
		@Override
		@Transact(TxnType.REQUIRED)
		public void delete() {
			ofy().delete().entities(this.msg);
			QueryKeys<MessageReceiversIndex> keys = ofy().load().type(MessageReceiversIndex.class).ancestor(msg).keys();
			ofy().delete().keys(keys);
		}
	}

	@Override
	public ManagedMessage get(String keyStr) throws NotFoundException {
		Key<Message> key = Key.create(keyStr);
		return get(key);
	}

	@Override
	public ManagedMessage get(Key<Message> key) throws NotFoundException {
		Message msg= ofy().load().key(key).now();
		if (msg == null)
			throw new NotFoundException("No such message: " + key.getString());
		return msgFactory.create(msg);
	}

	@Override
	public ListChunk<Message> getAllMessages(String cursorStr, int limit) {
		Cursor cursor = CursorUtil.safeFromEncodedString(cursorStr);
		Query<Message> query = ofy().load().type(Message.class).order("-creation").limit(limit);
		QueryResultIterable<Message> iterable = query.startAt(cursor).iterable();
		return new ListChunk<>(iterable);
	}
	
	@Override
	public ListChunk<Message> getUserMessages(String cursorStr, int limit, String keyStr) {
		Key<User> userKey = Key.create(keyStr);
		return getUserMessages(cursorStr, limit, userKey);
	}

	@Override
	public ListChunk<Message> getUserMessages(String cursorStr, int limit, Key<User> userKey) {
		Cursor cursor = CursorUtil.safeFromEncodedString(cursorStr);
		Query<Message> query = ofy().load()
									.type(Message.class)
									.filter("sender", userKey)
									.order("-creation")
									.limit(limit);
		QueryResultIterable<Message> iterable = query.startAt(cursor).iterable();
		return new ListChunk<>(iterable);
	}
	
	@Override
	public ListChunk<Message> getUserTimeLine(String cursorStr, int limit, String keyStr) {
		Key<User> userKey = Key.create(keyStr);
		return getUserTimeLine(cursorStr, limit, userKey);
	}

	@Override
	public ListChunk<Message> getUserTimeLine(String cursorStr, int limit, Key<User> userKey) {
		Cursor cursor = CursorUtil.safeFromEncodedString(cursorStr);
		QueryResultIterable<Key<MessageReceiversIndex>> keys = ofy().load()
																.type(MessageReceiversIndex.class)
																.limit(limit)
																.startAt(cursor)
																.filter("receivers", userKey)
																.keys()
																.iterable();
		List<Key<Message>> msgKeys = new LinkedList<>();
		for (Key<MessageReceiversIndex> k : keys) {
			Key<Message> parent = k.getParent();
			msgKeys.add(parent);
		}
		Iterable<Message> messages = ofy().load().keys(msgKeys).values();
		return new ListChunk<>(messages, keys.iterator().getCursor());
	}

	// There are ways to create a more general recursive task
	// However this is out of the scope of this example
	public static class ProcessReceiversTask implements DeferredTask {
		private static final long serialVersionUID = 2138600237595508391L;
		
		private Key<User> sender;
		private Key<User> receiver;
		private boolean addAction;
		
		@CheckForNull
		private Cursor currentCursor;
		private int limit;
		
		public ProcessReceiversTask(boolean addAction, Key<User> sender, Key<User> receiver, @CheckForNull Cursor cursor, int limit) {
			checkNotNull(sender);
			checkNotNull(receiver);
			
			this.sender = sender;
			this.receiver = receiver;
			this.addAction = addAction;
			
			this.currentCursor = cursor;
			if (limit < 0 || limit > 100)
				this.limit = 25; // default
			else
				this.limit = limit;
		}
		
		@Override
		public void run() {
			// the most recent messages first
			QueryResultIterable<Message> msgs = ofy().load().type(Message.class)
															.filter("sender", sender)
															.order("-creation")
															.startAt(currentCursor)
															.limit(limit)
															.iterable();
			
			List<MessageReceiversIndex> indexes =  new LinkedList<>();
			for (Message msg : msgs) {
				// TODO: 
				// we should do this in yet another task in the case a user
				// has hundreds of thousands of followers 
				// However for the moment it is ok because
				// each MessageReceiverIndex can have up to 5000 receivers (datastore limit)
				// In the case we had multiple MessageReceiveIndexes for a message
				// we should find the most recent index object and add the receiver
				
				MessageReceiversIndex index = ofy().load()
													.type(MessageReceiversIndex.class)
													.ancestor(msg)
													.first()
													.now();
				
				// TODO: what should we do if the receiver is already in the list ?
				if (addAction)
					index.getReceivers().add(receiver);
				else
					index.getReceivers().remove(receiver);
				indexes.add(index);
			}
			ofy().save().entities(indexes);
			QueueFactory.getDefaultQueue().add(TaskOptions
												.Builder
												.withPayload(new ProcessReceiversTask(addAction, sender, receiver, msgs.iterator().getCursor(), 25))
												.header("Host", BackendServiceFactory.getBackendService().getBackendAddress("message-backend")));
		}
	}
	
	@Override
	public void addUserMessagesReceiver(Key<User> senderKey, Key<User> receiverKey) {
		QueueFactory.getDefaultQueue().add(TaskOptions
											.Builder
											.withPayload(new ProcessReceiversTask(true, senderKey, receiverKey, null, 25))
											.header("Host", BackendServiceFactory.getBackendService().getBackendAddress("message-backend")));
	}

	@Override
	public void removeUserMessagesReceiver(Key<User> senderKey, Key<User> receiverKey) {
		QueueFactory.getDefaultQueue().add(TaskOptions
											.Builder
											.withPayload(new ProcessReceiversTask(false, senderKey, receiverKey, null, 25))
											.header("Host", BackendServiceFactory.getBackendService().getBackendAddress("message-backend")));
	}

	public static class AddNewMessageReceiversTask implements DeferredTask {
		private static final long serialVersionUID = -1111965433577784046L;

		private Key<Message> msgKey;
		private Key<User> followedKey;
		
		@CheckForNull
		private Cursor currentCursor;
		private int limit;
		
		public AddNewMessageReceiversTask(Key<Message> msgKey, Key<User> followedKey, @CheckForNull Cursor cursor, int limit) { 
			this.msgKey = msgKey;
			this.followedKey = followedKey;
			
			this.currentCursor = cursor;
			if (limit < 0 || limit > 100)
				this.limit = 25; // default
			else
				this.limit = limit;
		}
		
		@Override
		public void run() {

			QueryResultIterable<Key<UserFollowedIndex>> keys = ofy().load()
																	.type(UserFollowedIndex.class)
																	.limit(limit)
																	.startAt(currentCursor)
																	.filter("followed", followedKey)
																	.keys()
																	.iterable();
			final List<Key<User>> userKeys = new LinkedList<>();
			for (Key<UserFollowedIndex> k : keys) {
				Key<User> parent = k.getParent();
				userKeys.add(parent);
			}
			
			ofy().transact(new VoidWork() {
				
				@Override
				public void vrun() {
					MessageReceiversIndex index = ofy().load().type(MessageReceiversIndex.class).ancestor(msgKey).first().now();
					index.getReceivers().addAll(Lists.newArrayList(userKeys));
					ofy().save().entity(index);
				}
			});
			
			QueueFactory.getDefaultQueue().add(TaskOptions
					.Builder
					.withPayload(new AddNewMessageReceiversTask(msgKey, followedKey, null, 25))
					.header("Host", BackendServiceFactory.getBackendService().getBackendAddress("message-backend")));
		}
		
	}
	
	@Override
	public void addNewMessageReceivers(Key<Message> msgKey, Key<User> followedKey) {
		QueueFactory.getDefaultQueue().add(TaskOptions
				.Builder
				.withPayload(new AddNewMessageReceiversTask(msgKey, followedKey, null, 25))
				.header("Host", BackendServiceFactory.getBackendService().getBackendAddress("message-backend")));
	}
}