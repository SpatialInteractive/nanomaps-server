package net.rcode.nanomaps.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper base class for common MapLocator features
 * @author stella
 *
 */
public abstract class AbstractMapLocator implements MapLocator {
	private Map<String, String> properties=new HashMap<String, String>();
	
	@Override
	public Map<String, String> getProperties() {
		return properties;
	}
}
