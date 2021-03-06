package net.rcode.nanomaps.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a repository of maps.  This default implementation scans a
 * directory for special files that define a map.  Any file ending
 * in "{basename}.mapnik.xml" is published by its basename.  In the future,
 * additional "virtual" map types will also be recognized.
 * <p>
 * The repository maintains its collection of published maps in the background
 * such that a call to lookup a map is always a non blocking operation.
 * However, manipulating the map is an expensive operation which should be
 * done in a separate thread.
 * 
 * @author stella
 *
 */
public class FileSystemMapRepository implements MapRepository {
	private static final Logger logger=LoggerFactory.getLogger(FileSystemMapRepository.class);
	private static final Pattern MAPNIK_FILE_PATTERN=Pattern.compile("^([a-z0-9\\_\\-]+)\\.mapnik\\.xml$", Pattern.CASE_INSENSITIVE);
	private static final Pattern SELECT_SCRIPT_FILE_PATTERN=Pattern.compile("^([a-z0-9\\_\\-]+)\\.select\\.js$", Pattern.CASE_INSENSITIVE);
	
	/**
	 * The location of the repository
	 */
	private File basedir;
	
	/**
	 * Map of PublishedMap instances that are available.  This map is never
	 * updated, just replaced.  Synchronize on MapRepository to read/write.
	 */
	private Map<String, MapLocator> managedMaps;
	
	/**
	 * Statically registered map instances
	 */
	private Map<String, MapLocator> staticMaps=new HashMap<String, MapLocator>();
	
	public FileSystemMapRepository(File basedir) {
		this.basedir=basedir;
	}
	
	public File getBasedir() {
		return basedir;
	}
	
	/* (non-Javadoc)
	 * @see net.rcode.mapocalypse.MapRepository#lookupMap(java.lang.String)
	 */
	public synchronized MapLocator lookupMap(String name) {
		MapLocator locator;
		if (managedMaps!=null) {
			locator=managedMaps.get(name);
			if (locator!=null) return locator;
		}
		locator=staticMaps.get(name);
		return locator;
	}
	
	@Override
	public Collection<String> listMaps() {
		Map<String,MapLocator> maps;
		synchronized (this) {
			maps=managedMaps;
		}
		
		Set<String> ret=new HashSet<String>();
		ret.addAll(staticMaps.keySet());
		ret.addAll(maps.keySet());
		return ret;
	}
	
	/**
	 * Add a statically registered map
	 * @param name
	 * @param locator
	 * @throws Exception 
	 */
	public synchronized void add(String name, MapLocator locator) throws Exception {
		staticMaps.put(name, locator);
		if (locator instanceof MapRepositoryManaged) {
			((MapRepositoryManaged)locator).initialize(this);
		}
	}
	
	/**
	 * Scans the repository
	 * @throws Exception 
	 */
	public void scan() throws Exception {
		Map<String, MapLocator> newContents=new HashMap<String, MapLocator>();
		File[] children=basedir.listFiles();
		if (children!=null) {
			scanChildren(newContents, children);
		}
		
		// Prepare a special frozen copy of the repository to give to out
		// children for initialization
		Map<String, MapLocator> innerCopy=new HashMap<String, MapLocator>();
		synchronized (this) {
			innerCopy.putAll(staticMaps);
		}
		innerCopy.putAll(newContents);
		StaticMapRepository staticRepository=new StaticMapRepository(innerCopy);
		
		// Initialize all children
		for (Map.Entry<String,MapLocator> entry: newContents.entrySet()) {
			MapLocator locator=entry.getValue();
			if (locator instanceof MapRepositoryManaged) {
				try {
					((MapRepositoryManaged)locator).initialize(staticRepository);
				} catch (Throwable t) {
					logger.error("Error initializing map resource " + entry.getKey(), t);
				}
			}
			if (locator instanceof MapResource) {
				logger.info("Resource " + entry.getKey() + " has identityTag=" + ((MapResource)locator).getIdentityTag());
			}
		}
		
		synchronized (this) {
			managedMaps=newContents;
		}
	}

	private void scanChildren(Map<String, MapLocator> newContents,
			File[] children) {
		for (File file: children) {
			if (!file.isFile()) continue;
			
			// Detect mapnik file
			Matcher m=MAPNIK_FILE_PATTERN.matcher(file.getName());
			if (m.matches()) {
				// It is a mapnik file
				String mapName=m.group(1);
				try {
					MapnikMapResource mapnikMap=new MapnikMapResource(file);
					loadCompanionProperties(mapnikMap, file);
					newContents.put(mapName, mapnikMap);
					logger.info("Registered new repository map " + mapName);
				} catch (Throwable t) {
					logger.error("Error loading mapnik map from " + file, t);
				}
				
				continue;
			}
			
			// Detect select script
			m=SELECT_SCRIPT_FILE_PATTERN.matcher(file.getName());
			if (m.matches()) {
				// It is a select script
				String mapName=m.group(1);
				try {
					ScriptMapLocator scriptLoc=new ScriptMapLocator(file);
					loadCompanionProperties(scriptLoc, file);
					newContents.put(mapName, scriptLoc);
					logger.info("Registered new script select map " + mapName);
				} catch (Throwable t) {
					logger.error("Error loading script select map " + file, t);
				}
				
				continue;
			}
		}
	}

	private void loadCompanionProperties(MapLocator loc, File file) throws IOException {
		String fileName=file.getName();
		int dotPos=fileName.indexOf('.');
		if (dotPos<0) return;
		
		String rootName=fileName.substring(0, dotPos);
		File propFile=new File(file.getParentFile(), rootName + ".properties");
		if (!propFile.exists()) return;
		
		Properties props=new Properties();
		FileInputStream in=new FileInputStream(propFile);
		try {
			props.load(in);
		} finally {
			in.close();
		}
		
		Map<String,String> mapProps=loc.getProperties();
		Enumeration<?> nameEnum=props.propertyNames();
		while (nameEnum.hasMoreElements()) {
			String name=(String) nameEnum.nextElement();
			if (!mapProps.containsKey(name)) {
				mapProps.put(name, props.getProperty(name));
			}
		}
	}
}
