package net.rcode.nanomaps.server;


/**
 * Interface that the MapRepository can use to manage its children
 * @author stella
 *
 */
public interface MapRepositoryManaged {
	/**
	 * Called after all managed objects have been registered (but not necessarily
	 * initialized) in order to initialize the object
	 * @param repository
	 * @throws Exception
	 */
	public void initialize(MapRepository repository) throws Exception;
}
