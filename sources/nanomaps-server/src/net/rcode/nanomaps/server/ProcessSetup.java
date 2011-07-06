package net.rcode.nanomaps.server;

import com.almworks.sqlite4java.SQLite;

import mapnik.DatasourceCache;
import mapnik.FreetypeEngine;
import mapnik.Mapnik;

public class ProcessSetup {
	private static boolean inited;
	public static void initEnvironment() {
		if (inited) return;
		Mapnik.initialize();
		SQLite.loadLibrary();
		inited=true;
		
		DatasourceCache.registerDatasources("/usr/local/lib/mapnik2/input");
		FreetypeEngine.registerFonts("/usr/local/lib/mapnik2/fonts");
	}
}
