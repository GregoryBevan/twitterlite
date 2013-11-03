package twitterlite.controllers;

import java.util.logging.Logger;

import twitterlite.managers.MessageManager;
import twitterlite.models.message.Message.MessageDTO;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Api(
	name = "message",
	version = "v1")
public class MessageController {
	
	private MessageManager msgManager;
	
	private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
	
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
							  MessageDTO dto) throws BadRequestException {
		try {
			msgManager.create(dto.text, senderKeyStr);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

//	if (text.length() > 140)
//		throw new IllegalArgumentException("text length should be less than 140 characters.");
	
}
