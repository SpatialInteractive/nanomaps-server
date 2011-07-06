package net.rcode.nanomaps.server;

public interface MapRepository {

	/**
	 * Lookup a published map by name
	 * @param name
	 * @return the map or null
	 */
	public abstract MapLocator lookupMap(String name);

}