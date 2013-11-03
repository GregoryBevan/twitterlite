package twitterlite.managers;

import java.io.Serializable;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;

public class ListChunk<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Iterable<T> chunk;
	private Cursor cursor;
	
	public ListChunk(QueryResultIterable<T> chunk) {
		super();
		this.chunk = chunk;
	}
	
	public ListChunk(Iterable<T> chunk, Cursor cursor) {
		super();
		this.chunk = chunk;
		this.cursor = cursor;
	}
	
	@SuppressWarnings("rawtypes")
	public Cursor getCursor() {
		if (chunk instanceof QueryResultIterable) {
			Cursor cursor2 = ((QueryResultIterable)chunk).iterator().getCursor();
			System.out.println(cursor2.toWebSafeString());
			return ((QueryResultIterable)chunk).iterator().getCursor();
		}
		else
			return cursor;
	}
	
	public String getEncodedCursor() {
		return getCursor().toWebSafeString();
	}
}
