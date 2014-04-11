package com.twitterlite.models.user;

import static com.googlecode.objectify.ObjectifyService.ofy;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;

public class DataStoreUserTest {

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() throws Exception {
		helper.setUp();
		ObjectifyService.register(User.class);
	}

	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	@Test
	public void testUserStore() {
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Assert.assertEquals(0, ds.prepare(new Query("U")).countEntities(FetchOptions.Builder.withLimit(10)));
		User user = new User("Greg", "gregory_bevan@hotmail.com");
		Key<User> usrKey = ofy().save().entity(user).now();
		Assert.assertEquals(1, ds.prepare(new Query("U")).countEntities(FetchOptions.Builder.withLimit(10)));
		try {
			Entity entity2 = ds.get(Key.key(usrKey));
			Assert.assertEquals("Greg", entity2.getProperty("login"));
		} catch (EntityNotFoundException e) {
			Assert.fail(e.getMessage());
		}
	}
}
