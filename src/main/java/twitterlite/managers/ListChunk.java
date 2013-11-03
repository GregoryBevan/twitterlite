package twitterlite.managers;

import java.io.Serializable;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;

public class ListChunk<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Iterable<T> chunk;
	public Cursor cursor;
	
	public ListChunk(QueryResultIterable<T> chunk) {
		super();
		this.chunk = chunk;
		this.cursor = chunk.iterator().getCursor();
	}
	
	public ListChunk(Iterable<T> chunk, Cursor cursor) {
		super();
		this.chunk = chunk;
		this.cursor = cursor;
	}
}
