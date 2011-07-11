package net.rcode.nanomaps.server;

import static org.junit.Assert.assertSame;

import java.io.File;

import mapnik.MapDefinition;

import org.junit.Test;

public class ScriptMapLocatorTest {
	private static class DummyResource extends AbstractMapLocator implements MapResource, MapLocator {
		@Override
		public MapDefinition createMap(Object recycleTag) {
			return null;
		}

		@Override
		public boolean isValid() {
			return false;
		}

		@Override
		public void recycleMap(Object recycleTag, MapDefinition m) {
		}

		@Override
		public MapResource resolve(RenderRequest request) throws Exception {
			return this;
		}

		@Override
		public String getIdentityTag() {
			return null;
		}
	}
	
	@Test
	public void testStringResolution() throws Exception {
		FileSystemMapRepository repos=new FileSystemMapRepository(new File("."));
		DummyResource dummy1=new DummyResource(), dummy2=new DummyResource();
		repos.add("dummy1", dummy1);
		repos.add("dummy2", dummy2);
		
		ScriptMapLocator sloc=new ScriptMapLocator("function select(rr) { if (rr.level==1) return 'dummy1'; else return 'dummy2'; }", "test.js");
		repos.add("map", sloc);
		
		
		RenderRequest rr1=new RenderRequest();
		rr1.level=1;
		MapResource map1=repos.lookupMap("map").resolve(rr1);
		assertSame(dummy1, map1);
		
		RenderRequest rr2=new RenderRequest();
		rr2.level=2;
		MapResource map2=repos.lookupMap("map").resolve(rr2);
		assertSame(dummy2, map2);
	}

	@Test
	public void testDirectResolution() throws Exception {
		FileSystemMapRepository repos=new FileSystemMapRepository(new File("."));
		DummyResource dummy1=new DummyResource(), dummy2=new DummyResource();
		repos.add("dummy1", dummy1);
		repos.add("dummy2", dummy2);
		
		ScriptMapLocator sloc=new ScriptMapLocator("function select(rr) { if (rr.level==1) return repository.lookupMap('dummy1'); else return 'dummy2'; }", "test.js");
		repos.add("map", sloc);
		
		
		RenderRequest rr1=new RenderRequest();
		rr1.level=1;
		MapResource map1=repos.lookupMap("map").resolve(rr1);
		assertSame(dummy1, map1);
		
		RenderRequest rr2=new RenderRequest();
		rr2.level=2;
		MapResource map2=repos.lookupMap("map").resolve(rr2);
		assertSame(dummy2, map2);
	}

}
