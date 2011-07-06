package net.rcode.nanomaps.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map-backed MapRepository
 * @author stella
 *
 */
public class StaticMapRepository implements MapRepository {
	private Map<String, MapLocator> contents;
	
	public StaticMapRepository() {
		contents=new HashMap<String, MapLocator>();
	}
	
	public StaticMapRepository(Map<String,MapLocator> contents) {
		this.contents=contents;
	}
	
	@Override
	public MapLocator lookupMap(String name) {
		return contents.get(name);
	}

}
