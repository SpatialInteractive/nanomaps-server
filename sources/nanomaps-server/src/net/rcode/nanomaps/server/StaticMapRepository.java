package net.rcode.nanomaps.server;

import java.util.Collection;
import java.util.Collections;
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

	@Override
	public Collection<String> listMaps() {
		return Collections.unmodifiableCollection(contents.keySet());
	}

}
