package net.rcode.nanomaps.server.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.rcode.nanomaps.server.util.IOUtil;

/**
 * Since most scripts are not implemented with any kind of thread
 * safety in mind, we pool evaluated scripts here and allow clients
 * to check out an instance, use it and then recycle it.
 * 
 * @author stella
 *
 */
public class ScriptPool {
	private ScriptEngineManager manager;
	private String engineName;
	private String scriptText;
	private String scriptName;
	
	private LinkedList<Instance> pool=new LinkedList<Instance>();
	
	private static class Instance {
		public ScriptEngine engine;
	}
	
	public ScriptPool(String engineName, String scriptText, String scriptName) {
		this.manager=new ScriptEngineManager();
		this.engineName=engineName;
		this.scriptText=scriptText;
		this.scriptName=scriptName;
	}
	
	public ScriptPool(String engineName, Reader in, String scriptName) throws IOException {
		this(engineName, IOUtil.slurpReader(in).toString(), scriptName);
	}
	
	public ScriptPool(String engineName, File file) throws IOException {
		this(engineName, IOUtil.slurpFile(file).toString(), file.toString());
	}
	
	public ScriptEngineManager getManager() {
		return manager;
	}
	
	public String getScriptName() {
		return scriptName;
	}
	
	public void initialize() throws ScriptException {
		// Prime the pump
		recycle(createInstance());
	}
	
	public ScriptEngine getEngine() throws ScriptException {
		synchronized (pool) {
			if (!pool.isEmpty()) {
				Instance instance=pool.removeFirst();
				return instance.engine;
			}
		}
		
		return createInstance();
	}

	public void recycle(ScriptEngine engine) {
		Instance instance=new Instance();
		instance.engine=engine;
		synchronized (pool) {
			pool.add(instance);
		}
	}
	
	private ScriptEngine createInstance() throws ScriptException {
		ScriptEngine engine=manager.getEngineByName(engineName);
		engine.eval(scriptText);
		return engine;
	}
}
