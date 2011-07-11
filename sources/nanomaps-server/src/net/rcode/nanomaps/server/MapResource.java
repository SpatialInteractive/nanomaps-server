package net.rcode.nanomaps.server;

import mapnik.MapDefinition;

/**
 * Abstract class for published maps.
 * @author stella
 *
 */
public interface MapResource {

	/**
	 * @return true if the resource is valid
	 */
	public boolean isValid();
	
	/**
	 * @return null or a short string summary of the resource's state (suitable for an etag)
	 */
	public String getIdentityTag();
	
	/**
	 * Create a new map or return a recycled one
	 * @return map
	 */
	public abstract MapDefinition createMap(Object recycleTag);

	/**
	 * Recycle a map instance
	 * @param m
	 */
	public abstract void recycleMap(Object recycleTag, MapDefinition m);

}
