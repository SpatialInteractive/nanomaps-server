package net.rcode.nanomaps.server.util;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.rcode.nanomaps.server.util.ScriptPool;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScriptPoolTest {

	@Test
	public void testBasicScript() throws ScriptException, NoSuchMethodException {
		ScriptPool pool=new ScriptPool("JavaScript", "function test() { return someGlobal; }", "test");
		pool.getManager().put("someGlobal", "value");
		pool.initialize();
		
		// Check one out and verify
		ScriptEngine engine=pool.getEngine();
		Invocable inv=(Invocable) engine;
		String value=(String) inv.invokeFunction("test");
		assertEquals("value", value);
	}
	
	@Test
	public void testRecycle() throws ScriptException {
		ScriptPool pool=new ScriptPool("JavaScript", "function test() { return someGlobal; }", "test");
		pool.getManager().put("someGlobal", "value");
		pool.initialize();

		ScriptEngine engine1=pool.getEngine();
		assertNotNull(engine1);
		ScriptEngine engine2=pool.getEngine();
		assertNotNull(engine2);
		assertNotSame(engine1, engine2);
		
		pool.recycle(engine2);
		ScriptEngine engine3=pool.getEngine();
		assertSame(engine2, engine3);
	}
}
