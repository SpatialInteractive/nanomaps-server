package net.rcode.nanomaps.server.projection;

import mapnik.Box2d;
import mapnik.Projection;

public abstract class TileProjection {

	/**
	 * @return Create a new instance of the default tile projection
	 */
	public static TileProjection createDefaultProjection() {
		return new WebMercatorTileProjection();
	}
	
	/**
	 * @return the srs for this projection
	 */
	public abstract String getSrs();

	/**
	 * @return the projection
	 */
	public abstract Projection getProjection();
	
	/**
	 * Project tile coordinates to an envelope in the projection coordinate system
	 * @param level
	 * @param x
	 * @param y
	 * @param tileWidth
	 * @param tileHeight
	 * @return envelope
	 */
	public abstract Box2d projectTile(int level, int x, int y, int tileWidth,
			int tileHeight);

	/**
	 * @param level
	 * @return The resolution in projected units/px for the given level
	 */
	public abstract double resolutionFromLevel(int level);

	/**
	 * Calculate a default render cost based on the given extent. 1.0 is the highest 
	 * standard cost (ie. request to render the entire world.  Fractions are lower cost.
	 * @return cost factor normalized to 0..1.0 nominally
	 */
	public abstract double getCostFactor(Box2d extent);
}
