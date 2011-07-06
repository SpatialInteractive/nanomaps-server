package net.rcode.nanomaps.server;

import java.util.Collection;

public interface MapRepository {

	/**
	 * Lookup a published map by name
	 * @param name
	 * @return the map or null
	 */
	public abstract MapLocator lookupMap(String name);

	/**
	 * List all map names.  Note that by the time you call lookupMap,
	 * it may not be there anymore so watch for null.
	 * @return map names
	 */
	public Collection<String> listMaps();
	
}