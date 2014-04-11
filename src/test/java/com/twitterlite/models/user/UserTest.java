package com.twitterlite.models.user;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UserTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testUserWithInvalidLogin() {
		try {
			new User("", "gregory_bevan@hotmail.com");
		} catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalArgumentException);
			Assert.assertEquals("Login length should be between 3 and 100 characters.", e.getMessage());
		}		
	}
	
	@Test
	public void testUserWithInvalidEmail() {
		try {
			new User("Greg", "gregory_bevan@hotmail.");
		} catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalArgumentException);
			Assert.assertEquals("Email is not a valid email address.", e.getMessage());
		}
		
	}

}
