/**
 * 
 */
package net.rcode.nanomaps.server;

import mapnik.AspectFixMode;
import mapnik.Box2d;
import net.rcode.nanomaps.server.projection.RenderProjection;

class RenderInfo {
	// Projection
	public RenderProjection projection;
	public boolean coordinatesAreGlobal;
	
	// Map State
	public Box2d bounds;
	public AspectFixMode aspectFixMode;
	
	// Output information
	public int bufferPixels=128;
	public double pixelRatio=1.0;
	public int width;
	public int height;
	public String format;
	
	public double getResolutionX() {
		return (bounds.maxx-bounds.minx) / width;
	}
	
	public double getResolutionY() {
		return (bounds.maxy-bounds.miny) / height;
	}
	
	public double getResolution() {
		return Math.max(getResolutionX(), getResolutionY());
	}
	
	public double getLevel() {
		return projection.levelFromResolution(getResolution());
	}
}