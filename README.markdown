nanomaps-server
===============
This is a simple tile map server written in Java.  It uses
[Netty](http://www.jboss.org/netty) to implement the HTTP server
and should therefore be moderately memory efficient when it comes
to serving a large number of clients.  Also, since it runs in
a single process, it opens the door for some better traffic control
mechanisms than are (easily) possible with other solutions that
run within apache or other http servers.

It internally uses [Mapnik](http://mapnik.org/) to render the maps
via [the Mapnik JNI bindings](https://github.com/stellaeof/mapnik-jni).

It also leverages [SQLite](http://www.sqlite.org/) via some [hacked custom
Java bindings](https://github.com/stellaeof/sqlite4java-custom) that are based
on [sqlite4java](http://code.google.com/p/sqlite4java/) to manage tile caching.
This part is still being worked on offline and is not in this repo.

Everything is self contained in this repo.  You don't need any Java expertise
to use it.

Why
---
Because I'm a Java programmer and I needed something to serve my Mapnik xml
files.  I found it easier to just mock something up quickly in Java than going
to the store and buying a bottle of Advil to combat the headache that would
ensue if I tried to remember all of the stuff that I used to know about deploying
Apache modules, python wsgi stuff, et al.  I realize that most people feel that
way when coming to Java, but c'est la vie. We all have our habits.  I had the
basic skeleton of what I needed from other projects so it wasn't that hard.

I was thinking it would just be a little development toy, but it kind of grew on
me.  I like that I can just clone the git repo on my server, run a couple of
commands and start rendering maps.

Quick Start
-----------

### Prerequisites

You need to have Mapnik2 (aka SVN trunk) checked out and built to a standard
location (I have only tested with installs to /usr/local).  There are multiple
sources out there telling you how to do this.  If you have the following paths
on your box, you are probably in good shape:

* /usr/local/lib/libmapnik2.so (or .dylib if on osx)
* /usr/local/lib/mapnik2/input/
* /usr/local/lib/mapnik2/fonts/

This is probably also the right time to let you know that you are going to need
a Java 6 JDK and Apache Ant >= 1.7.  This is the part where if you're not a Java
person, you break off the date and go home and complain to your friends.  But do
not fear, this paragraph represents the sum total of what you need to know about
Java in order to proceed.  If you are running any non ancient version of OSX, you're
probably set out of the box.  If you are on Ubuntu, the following packages should
get you going (trimming this down for servers and headless machines is left as
an exercise to the reader):

* apt-get install openjdk-6-jdk
* apt-get install ant

If you are on Windows, you have my condolences.

### Clone Repository

	git clone --recursive git://github.com/stellaeof/nanomaps-server.git
	cd nanomaps-server
	
	# Skip the following if you cloned recursively
	scripts/update-modules.sh
	
### Build dependencies

	ant depend
	
This is the part where if something can go wrong it will.  Everything
in the external/directory is built, including native bits.  If your build environment has
issues, you will get errors on the native build steps.  If you managed to build
mapnik and its input plugins, you should be fine.

### Run the server at the console

	ant instance
	ant run
	
(Note that if you're living dangerously, you can just execute
'ant run' right after checkout.  This will take care of everything for you
but if something goes wrong, it's going to be a little difficult to figure
out what happened)

### Load it up in your browser

	http://localhost:7666/
	
You should get a web page with the colorized population map as presented
[here](http://trac.mapnik.org/wiki/XMLGettingStarted).  At the top left is
a drop down of maps that were found in the instance/repository directory
when the server started.  The default install only has the one "world_sample"
but if you drop other *.mapnik.xml files into the repository directory (and
restart), they should show up here.  The ui is [Nanomaps JS](https://github.com/stellaeof/nanomaps)
and should work on modern desktop browsers and mobile.

Screenshots
-----------

### Default Installation

![First Screen](https://github.com/stellaeof/nanomaps-server/raw/master/doc/screenshots/nanomaps-server-example.png)

### Zoomed In on a MapQuest OSM Map

This is a proof of life of the server rendering a non-trivial OSM map.
The source is actually a highly optimized fork of the (MapQuest OSM Style)[https://github.com/MapQuest/MapQuest-Mapnik-Style]
rendered off of SQLite databases that have been optimized with another tool I've been working on called
[Mapnik Distiller](https://github.com/stellaeof/mapnik-distiller).  
It applies a number of optimizations to the OSM database with the
end result being that functional, relatively high performance maps can be rendered on modest hardware.
I've been running it on a 6 year old Linux desktop with 2GB of RAM with pretty decent
rendering performance at all zoom levels (~250ms/tile depending on zoom level).

![MapQuest OSM](https://github.com/stellaeof/nanomaps-server/raw/master/doc/screenshots/nanomaps-server-zoomed-in.png)

Configuration
=============
Server configuration is currently hard-coded or auto detected.  I'll be adding bits to make it all configurable shortly.
The primary moving part is the files that you put in the instance/repository directory.

The server will pick up files with the following name patterns and publish them:

* {mapname}.mapnik.xml
* {mapname}.select.js

In addition, some properties control the way that the maps are exposed.  These will be read from {mapname}.properties
if it exists.  Currently, the following properties are supported:

* announce: If "false" then the map will not be listed in the server's table of contents
* attribution: Text attribution listed with the map metadata
* attributionHtml: Html attribution listed with the map metadata

Everyone here should already know what goes into a *.mapnik.xml file, so I won't go into that except to make one note:
If you use a symlink, then the server resolves the link and passes the resolved path to mapnik for loading.  The result is that
if your symlinked map file references relative resources, those resources will be resolved relative to the actual endpoint of
the link, not the link itself.

Also, the server makes aggressive use of etags for http caching.  The etag is calculated off of the "completely loaded" contents
of the repository file (ie. the mapnik.xml or the select.js file).  Therefore, if the file contents haven't changed, all generated
maps will have the same etag.  Note that entities *are* resolved in mapnik.xml files prior to calculating the etag.

JavaScript Map Select Files
---------------------------
The server is organized to support a number of pseudo map types that are identified by file type in the repository directory.
Eventually, I expect to have proxy types and other constructions useful for stitching multiple maps together.  Right now, there is
just the .select.js file which is a little chunk of JavaScript that takes a map request and decides what underlying map should
be rendered.  I needed this straight away because my OSM maps are broken into four different detail levels and something needs
to pick the right one to render for each request.

Here is an example:

	function select(request) {
		var level=request.level, suffix;
		if (level>=15) suffix="dl1";
		else if (level>=11) suffix="dl2";
		else if (level>=7) suffix="dl3";
		else if (level>=1) suffix="dl4";
		
		if (suffix) {
			logger.info("Select detail level " + suffix + " for level " + level);
			return "mqstreet_" + suffix;
		} else {
			return null;
		}
	}

Currently, there are two global objects available:

* repository: The object that manages the repository.  Can be used to lookup additional things.
* logger: A SLF4J logger
	
The js file should define a function "select" that takes a request parameter and returns something that can be turned
into a map such as:

* A map name.  The invoker will recursively resolve any returned string against the repository to produce a map.  Cyclic recursion
is detected and stopped.
* A MapLocator object.  You can get one of these from repository.lookupMap(name).  Or conceivably, you could instantiate some
special kind of MapLocator and give it back
* A MapResource object.  This is the type of object that actually gets rendered.  There is presently not a straight-forward way
to create one of these from the JavaScript side.

The request object given to the select() method has the following properties:

* tileWidth
* tileHeight
* mapName
* level
* x
* y
* cost
* time

It is highly likely that I will refactor this class before too much longer, so I'm not going to document these properties
further.

