package com.twitterlite.controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.twitterlite.controllers.interceptors.InterceptWith;
import com.twitterlite.controllers.interceptors.LoginInterceptor;
import com.twitterlite.managers.ListChunk;
import com.twitterlite.managers.MessageManager;
import com.twitterlite.managers.MessageManager.ManagedMessage;
import com.twitterlite.models.message.Message;
import com.twitterlite.models.message.Message.MessageGetDTO;
import com.twitterlite.models.message.Message.MessageSetDTO;
import com.twitterlite.util.BeanExtraUtils;

@Singleton
@InterceptWith(LoginInterceptor.class)
@Api(
	name = "message",
	version = "v1")
public class MessageController {
	
	private MessageManager msgManager;
	
//	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
	// TODO: log everywhere
	
	@Inject
	public MessageController(MessageManager msgManager) {
		this.msgManager = msgManager;
	}
	
	@ApiMethod(
			name = "post",
			path = "message",
			httpMethod = HttpMethod.POST
	)
	public void postMessage(@Named("senderKey") String senderKeyStr,
							  MessageSetDTO dto) throws BadRequestException {
		try {
			String text = dto.getText();
			if (text == null)
				throw new BadRequestException("Message text must be at least one character and less than 140 characters");
			msgManager.create(text, senderKeyStr);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		}
	}
	
	@ApiMethod(
			name = "read",
			path = "message/{key}",
			httpMethod = HttpMethod.GET
		)
	public MessageGetDTO getMessage(@Named("key") String keyStr) throws NotFoundException {
		return MessageGetDTO.get(msgManager.get(keyStr).read());
	}
	
	@ApiMethod(
			name = "patch", // had to call it patch to overwrite the method user.patch created by the system
							// which yields a 404 when called
			path = "message/{key}",
			httpMethod = HttpMethod.PUT
		)
	public void updateMessage(@Named("key") String keyStr, MessageSetDTO dto) throws NotFoundException, BadRequestException {
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
			path = "message/{key}",
			httpMethod = HttpMethod.DELETE
		)
	public void deleteMessage(@Named("key") String keyStr) throws NotFoundException {
		msgManager.get(keyStr).delete();
	}
	
	@ApiMethod(
			name = "list",
			path = "message",
			httpMethod = HttpMethod.GET
		)
	public Map<String, Object> getMessages(	@Nullable 
											@Named("cursor") String encodedCursor,
											@Named("limit") Integer limit) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<Message> msgs = msgManager.getAllMessages(encodedCursor, limit.intValue());
		List<MessageGetDTO> dtos = new LinkedList<MessageGetDTO>();
		for (Message msg : msgs.chunk)
			dtos.add(MessageGetDTO.get(msg));
		
		Map<String, Object> map = new HashMap<>();
		map.put("list", dtos);
		map.put("cursor", msgs.getEncodedCursor());
		
		return map;
	}
	
	@ApiMethod(
			name = "user.messages.list",
			path = "message/user",
			httpMethod = HttpMethod.GET
		)
	public Map<String, Object> getUserMessages(	@Nullable 
												@Named("cursor") String encodedCursor,
												@Named("limit") Integer limit,
												@Named("userKey") String userKey) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<Message> msgs = msgManager.getUserMessages(encodedCursor, limit.intValue(), userKey);
		List<MessageGetDTO> dtos = new LinkedList<MessageGetDTO>();
		for (Message msg : msgs.chunk)
			dtos.add(MessageGetDTO.get(msg));
		
		Map<String, Object> map = new HashMap<>();
		map.put("list", dtos);
		map.put("cursor", msgs.getEncodedCursor());
		
		return map;
	}
	
	@ApiMethod(
			name = "user.timeline.list",
			path = "message/user/timeline",
			httpMethod = HttpMethod.GET
		)
	public Map<String, Object> getUserTimeline(	@Nullable 
												@Named("cursor") String encodedCursor,
												@Named("limit") Integer limit,
												@Named("userKey") String userKey) {

		if (limit < 0 || limit > 25)
			limit = 25;
		ListChunk<Message> msgs = msgManager.getUserTimeLine(encodedCursor, limit.intValue(), userKey);
		List<MessageGetDTO> dtos = new LinkedList<MessageGetDTO>();
		for (Message msg : msgs.chunk)
			dtos.add(MessageGetDTO.get(msg));
		
		Map<String, Object> map = new HashMap<>();
		map.put("list", dtos);
		map.put("cursor", msgs.getEncodedCursor());
		
		return map;
	}
}