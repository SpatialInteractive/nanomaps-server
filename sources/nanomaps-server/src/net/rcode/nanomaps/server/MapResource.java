package net.rcode.nanomaps.server;

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
	public abstract mapnik.Map createMap(Object recycleTag);

	/**
	 * Recycle a map instance
	 * @param m
	 */
	public abstract void recycleMap(Object recycleTag, mapnik.Map m);

}
