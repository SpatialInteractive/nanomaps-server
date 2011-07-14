package net.rcode.nanomaps.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import mapnik.MapDefinition;
import net.rcode.nanomaps.server.util.IOUtil;
import net.rcode.nanomaps.server.util.IdentityHasher;

import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapnikMapResource extends AbstractMapLocator implements MapLocator, MapResource, MapRepositoryManaged  {
	private static final Logger logger=LoggerFactory.getLogger(MapnikMapResource.class);
	private File repositoryMapFile;
	private File canonicalMapFile;
	private String mapFileContents;
	private String identityTag;
	
	private MapRepository repository;
	
	/**
	 * Pool of cached maps available for reuse, indexed by tag
	 */
	private Map<Object, Set<MapDefinition>> cachedMapBuckets=new HashMap<Object, Set<MapDefinition>>();
	
	public MapnikMapResource(File repositoryMapFile) throws IOException {
		this.repositoryMapFile=repositoryMapFile;
		this.canonicalMapFile=repositoryMapFile.getCanonicalFile();
	}
	
	public void initialize(MapRepository repository) throws Exception {
		//mapFileContents=slurpMapFile(canonicalMapFile);
		mapFileContents=IOUtil.loadXmlStandalone(canonicalMapFile).toString();
		debugContents();
		//logger.debug("Map file=" + mapFileContents);
		newMap().dispose();
		
		// Calculate the digest
		IdentityHasher hasher=new IdentityHasher();
		hasher.append("mapnik:");
		hasher.append(canonicalMapFile.toString());
		hasher.append(mapFileContents);
		identityTag=hasher.getHash();
		
		this.repository=repository;
	}
	
	private void debugContents() throws IOException {
		File debugFile=new File(this.repositoryMapFile.getParentFile(), "debug");
		debugFile.mkdirs();
		debugFile=new File(debugFile, this.repositoryMapFile.getName());
		Writer out=new OutputStreamWriter(new FileOutputStream(debugFile), CharsetUtil.UTF_8);
		try {
			out.write(mapFileContents);
		} finally {
			out.close();
		}
	}

	@Override
	public String getIdentityTag() {
		return identityTag;
	}
	
	private MapDefinition newMap() {
		MapDefinition loadMap=new MapDefinition();
		logger.info("Loading map from " + canonicalMapFile);
		loadMap.loadMapString(mapFileContents, false, canonicalMapFile.toString());
		return loadMap;
	}
	
	private String slurpMapFile(File f) throws IOException {
		Reader in=new InputStreamReader(new FileInputStream(f), CharsetUtil.UTF_8);
		try {
			long length=f.length();
			if (length<0 || length>Integer.MAX_VALUE) length=16384;
			StringBuilder contents=new StringBuilder((int)length);
			char[] buffer=new char[4096];
			for (;;) {
				int r=in.read(buffer);
				if (r<0) break;
				contents.append(buffer, 0, r);
			}
			return contents.toString();
		} finally {
			in.close();
		}
	}

	@Override
	public boolean isValid() {
		return repository!=null;
	}
	
	/**
	 * Create a new map or return a recycled one
	 * @return map
	 */
	public MapDefinition createMap(Object recycleTag) {
		if (recycleTag!=null) {
			synchronized (cachedMapBuckets) {
				Set<MapDefinition> bucket=cachedMapBuckets.get(recycleTag);
				if (bucket!=null && !bucket.isEmpty()) {
					Iterator<MapDefinition> iter=bucket.iterator();
					MapDefinition ret=iter.next();
					iter.remove();
					return ret;
				}
			}
		}
		
		// No recycled.  Create anew
		return newMap();
	}
	
	public void recycleMap(Object recycleTag, MapDefinition m) {
		synchronized (cachedMapBuckets) {
			Set<MapDefinition> bucket=cachedMapBuckets.get(recycleTag);
			if (bucket==null) {
				bucket=new HashSet<MapDefinition>();
				cachedMapBuckets.put(recycleTag, bucket);
			}
			bucket.add(m);
		}
	}

	@Override
	public MapResource resolve(RenderRequest request) {
		return this;
	}
}
