package net.rcode.nanomaps.server;

/**
 * MapLocator instances are contained by a MapRepository.  Request handlers
 * get an instance of this and then use it to lookup the physical map
 * resources.
 * 
 * @author stella
 *
 */
public interface MapLocator {
	/**
	 * @return true if the resource is valid
	 */
	public boolean isValid();
	
	/**
	 * Resolve a render request to a map resource
	 * @param request
	 * @return resource or null
	 * @throws Exception 
	 */
	public MapResource resolve(RenderRequest request) throws Exception;
}
