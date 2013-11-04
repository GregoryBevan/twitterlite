package com.twitterlite.managers;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Lists;

public class ListChunk<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public List<T> chunk;
	private Cursor cursor;
	
	public static <T> List<T> copyQueryResultIterator(QueryResultIterator<T> it) {
		LinkedList<T> list = new LinkedList<>();
		while (it.hasNext())
			list.add(it.next());
		return list;
	}
	
	public ListChunk(QueryResultIterable<T> chunk) {
		super();
		QueryResultIterator<T> iterator = chunk.iterator();
		this.chunk = copyQueryResultIterator(iterator);
		this.cursor = iterator.getCursor();
	}
	
	public ListChunk(Iterable<T> chunk, Cursor cursor) {
		super();
		this.chunk = Lists.newArrayList(chunk);
		this.cursor = cursor;
	}
	
	public Cursor getCursor() {
		return this.cursor;
	}
	
	public String getEncodedCursor() {
		return this.cursor.toWebSafeString();
	}
}
