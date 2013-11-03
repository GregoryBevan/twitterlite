package twitterlite.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class TwitterLiteServletContextListener extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		Injector injector = Guice.createInjector(
				new TwitterLiteEndPointsModule(),
				new TwitterLiteManagerModule()
		);
		return injector;
	}
}

