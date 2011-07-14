package net.rcode.nanomaps.server;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.IdentityHashMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rcode.nanomaps.server.util.ScriptPool;

/**
 * Map locator that uses scripts to programatically determine which
 * map to return.  The script must provide a global function "select"
 * which takes a RenderRequest as an argument.  It must return one of:
 * <ul>
 * <li>String: The name of another locator to load
 * <li>MapResource: A resolved MapResource
 * <li>MapLocator: A locator which must be resolved
 * </ul>
 * 
 * The script will have a "repository" variable defined globally that it
 * can use to look up other maps.  It can also use the "logger" global.
 * 
 * @author stella
 *
 */
public class ScriptMapLocator extends AbstractMapLocator implements MapLocator, MapRepositoryManaged {
	private Logger logger=LoggerFactory.getLogger(ScriptMapLocator.class);
	
	private ScriptPool pool;
	private MapRepository repository;
	
	public ScriptMapLocator(File locatorFile) throws IOException {
		pool=new ScriptPool("JavaScript", locatorFile);
	}

	public ScriptMapLocator(Reader in, String name) throws IOException {
		pool=new ScriptPool("JavaScript", in, name);
	}
	
	public ScriptMapLocator(String scriptText, String name) {
		pool=new ScriptPool("JavaScript", scriptText, name);
	}

	@Override
	public boolean isValid() {
		return repository!=null;
	}
	
	@Override
	public void initialize(MapRepository repository) throws ScriptException {
		pool.getManager().put("repository", repository);
		pool.getManager().put("logger", logger);
		pool.initialize();
		this.repository=repository;
	}
	
	@Override
	public MapResource resolve(RenderRequest request) throws Exception {
		IdentityHashMap<Object, Boolean> cycleDetect=new IdentityHashMap<Object, Boolean>();
		ScriptEngine engine=pool.getEngine();
		Invocable invocable=(Invocable) engine;
		try {
			Object resolved=invocable.invokeFunction("select", request);
			
			while (resolved!=null && !(resolved instanceof MapResource)) {
				if (cycleDetect.put(resolved, Boolean.TRUE)!=null) {
					throw new IllegalStateException("Cycle detected using script to detect map resource: " + pool.getScriptName());
				}
				
				if (resolved instanceof String) {
					resolved=repository.lookupMap((String)resolved);
				} else if (resolved instanceof MapLocator) {
					MapLocator loc=(MapLocator)resolved;
					if (!loc.isValid()) {
						throw new IllegalStateException("Attempt to resolve locator " + loc + " but it is not valid");
					}
					resolved=((MapLocator)resolved).resolve(request);
				} else {
					throw new IllegalStateException("Script locator " + pool.getScriptName() + " returned illegal value: " + resolved);
				}
			}
			return (MapResource) resolved;
		} catch (Exception e) {
			throw new RuntimeException("Exception while processing script " + pool.getScriptName(), e);
		} finally {
			pool.recycle(engine);
		}
	}
	
}
