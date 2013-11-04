package com.twitterlite.util;

import javax.annotation.CheckForNull;

import com.google.appengine.api.datastore.Cursor;

public class CursorUtil {

	public static @CheckForNull Cursor safeFromEncodedString(String encodedCursor) {
		Cursor cursor = null;
		if (encodedCursor != null) {
			try {
				 cursor = Cursor.fromWebSafeString(encodedCursor);
			} catch (IllegalArgumentException e) {
				cursor = null;
			}
		}
		return cursor;
	}
}
