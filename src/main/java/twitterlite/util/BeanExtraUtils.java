package twitterlite.util;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class BeanExtraUtils {
	
	public static void copyOnlyNonNullProperties(Object dest, Object orig) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		checkNotNull(dest);
		checkNotNull(orig);
		
		@SuppressWarnings("unchecked")
		Map<String, Object> properties = BeanUtils.describe(orig);
		for (String prop : properties.keySet()) {
			if (!prop.equalsIgnoreCase("class") // the describe method adds the class as property of the bean 
				&& properties.get(prop) != null)
				BeanUtils.copyProperty(dest, prop, properties.get(prop));
		}
	}
}
