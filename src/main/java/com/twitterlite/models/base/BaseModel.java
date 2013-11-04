package com.twitterlite.models.base;

import com.googlecode.objectify.Key;


public abstract class BaseModel {

	/*
	 * An easy way to get the Entities Key
	 */
	public abstract Key<? extends BaseModel> getKey();
	
//	@OnSave
	/*
	 * Should be uncommented and overriden for validation purposes using a ValidationService
	 */
//	public abstract void validate();
}

