package com.twitterlite.controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlecode.objectify.Key;
import com.twitterlite.config.TwitterLiteManagerModule.CurrentUser;
import com.twitterlite.controllers.interceptors.InterceptWith;
import com.twitterlite.controllers.interceptors.LoginInterceptor;
import com.twitterlite.managers.ListChunk;
import com.twitterlite.managers.MessageManager;
import com.twitterlite.managers.MessageManager.ManagedMessage;
import com.twitterlite.models.message.Message;
import com.twitterlite.models.message.Message.MessageGetDTO;
import com.twitterlite.models.message.Message.MessageSetDTO;
import com.twitterlite.models.user.User;
import com.twitterlite.util.BeanExtraUtils;

@Singleton
@InterceptWith(LoginInterceptor.class)
@Api(
	name = "message",
	version = "v1")
public class MessageController {
	
	private MessageManager msgManager;
	@CurrentUser private Provider<Optional<Key<User>>> currentUserProvider;
//	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	// TODO: log everywhere
	
	@Inject
	public MessageController(	@CurrentUser	
								Provider<Optional<Key<User>>> currentUserProvider,
								MessageManager msgManager) {
		this.msgManager = msgManager;
		this.currentUserProvider = currentUserProvider;
	}
	
	public MessageGetDTO updateMessageDTOMetadata(MessageGetDTO dto) {
		Key<User> currentUserKey = currentUserProvider.get().orNull();
		if (currentUserKey != null && dto.messageKey != null)  
			dto.isMessageFromCurrentUser = msgManager.isMessageSender(dto.messageKey, currentUserKey.getString());
		return dto;
	}
	
	@ApiMethod(
			name = "post",
			path = "message",
			httpMethod = HttpMethod.POST
	)
	public MessageGetDTO postMessage(@Named("senderKey") String senderKeyStr,
							  		MessageSetDTO dto) throws BadRequestException {
		try {
			String text = dto.getText();
			if (text == null)
				throw new BadRequestException("Message text must be at least one character and less than 140 characters");
			return MessageGetDTO.get(msgManager.create(text, senderKeyStr).read());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		}
	}
	
	@ApiMethod(
			name = "read",
			path = "message/{msgKey}",
			httpMethod = HttpMethod.GET
		)
	public MessageGetDTO getMessage(@Named("msgKey") String keyStr) throws NotFoundException {
		return updateMessageDTOMetadata(MessageGetDTO.get(msgManager.get(keyStr).read()));
	}
	
	@ApiMethod(
			name = "patch", // had to call it patch to overwrite the method user.patch created by the system
							// which yields a 404 when called
			path = "message/{msgKey}",
			httpMethod = HttpMethod.PUT
		)
	public void updateMessage(@Named("msgKey") String keyStr, MessageSetDTO dto) throws NotFoundException, BadRequestException {
		ManagedMessage mngMsg = msgManager.get(keyStr);
		try {
			String text = dto.getText();
			if (text != null && text.length() > 140)
				throw new IllegalArgumentException("Message text must be at least one character and less than 140 characters");
			BeanExtraUtils.copyOnlyNonNullProperties(mngMsg.read(), dto);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new BadRequestException(e.getMessage());
		}
		mngMsg.update();
	}
	
	@ApiMethod(
			name = "delete",
			path = "message/{msgKey}",
			httpMethod = HttpMethod.DELETE
		)
	public void deleteMessage(@Named("msgKey") String keyStr) throws NotFoundException {
		// only the sender or an admin should be able to delete a message
		msgManager.get(keyStr).delete();
	}
	
	public static class MessagesCollection {
		public MessagesCollection(List<MessageGetDTO> list, String cursor) {
			super();
			this.list = list;
			this.cursor = cursor;
		}
		public List<MessageGetDTO> list;
		public String cursor;
		public List<MessageGetDTO> getList() {
			return list;
		}
		public String getCursor() {
			return cursor;
		}
	}
	
	@ApiMethod(
			name = "list",
			path = "message",
			httpMethod = HttpMethod.GET
		)
	public MessagesCollection getMessages(	@Nullable 
											@Named("cursor") String encodedCursor,
											@Named("limit") Integer limit) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<Message> msgs = msgManager.getAllMessages(encodedCursor, limit.intValue());
		List<MessageGetDTO> dtos = new LinkedList<MessageGetDTO>();
		for (Message msg : msgs.chunk)
			dtos.add(updateMessageDTOMetadata(MessageGetDTO.get(msg)));
		
		return new MessagesCollection(dtos, msgs.getEncodedCursor());
	}
	
	@ApiMethod(
			name = "user.messages.list",
			path = "message/user",
			httpMethod = HttpMethod.GET
		)
	public MessagesCollection getUserMessages(	@Nullable 
												@Named("cursor") String encodedCursor,
												@Named("limit") Integer limit,
												@Named("userKey") String userKey) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<Message> msgs = msgManager.getUserMessages(encodedCursor, limit.intValue(), userKey);
		List<MessageGetDTO> dtos = new LinkedList<MessageGetDTO>();
		for (Message msg : msgs.chunk)
			dtos.add(updateMessageDTOMetadata(MessageGetDTO.get(msg)));
		
		return new MessagesCollection(dtos, msgs.getEncodedCursor());
	}
	
	@ApiMethod(
			name = "user.timeline.list",
			path = "message/user/timeline",
			httpMethod = HttpMethod.GET
		)
	public MessagesCollection getUserTimeline(	@Nullable 
												@Named("cursor") String encodedCursor,
												@Named("limit") Integer limit,
												@Named("userKey") String userKey) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<Message> msgs = msgManager.getUserTimeLine(encodedCursor, limit.intValue(), userKey);
		List<MessageGetDTO> dtos = new LinkedList<MessageGetDTO>();
		for (Message msg : msgs.chunk)
			dtos.add(updateMessageDTOMetadata(MessageGetDTO.get(msg)));
		
		return new MessagesCollection(dtos, msgs.getEncodedCursor());
	}
}