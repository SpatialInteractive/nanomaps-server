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


