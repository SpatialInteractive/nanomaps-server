package net.rcode.nanomaps.server;

import mapnik.Mapnik;

import com.almworks.sqlite4java.SQLite;

public class ProcessSetup {
	private static boolean inited;
	public static void initEnvironment() {
		if (inited) return;
		Mapnik.initialize();
		SQLite.loadLibrary();
		inited=true;
	}
}
