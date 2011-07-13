package net.rcode.nanomaps.server.projection;

import mapnik.Box2d;
import mapnik.Coord;

public abstract class RenderProjection {

	/**
	 * @return Create a new instance of the default tile projection
	 */
	public static RenderProjection createDefaultProjection() {
		return new WebMercatorRenderProjection();
	}

	public static RenderProjection createFromEpsgCode(int epsgCode) {
		// All stupid aliases for WebMercator which we support specially
		if (epsgCode==900913 || epsgCode==3785 || epsgCode==3857) {
			return new WebMercatorRenderProjection();
		} else if (epsgCode==4326) {
			return new Proj4RenderProjection("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
		} else {
			throw new IllegalArgumentException("Unrecognized EPSG code");
		}
	}

	public static RenderProjection createFromProj4Params(String srsDefn) {
		try {
			return new Proj4RenderProjection(srsDefn);
		} catch (Exception e) {
			throw new IllegalArgumentException("Illegal proj4 init '" + srsDefn + "'");
		}
	}

	/**
	 * Dispose of any native resources
	 */
	public abstract void dispose();
	
	/**
	 * @return the srs for this projection
	 */
	public abstract String getSrs();

	/**
	 * Project coordinate forward
	 * @param coord
	 */
	public abstract void forward(Coord coord);
	
	/**
	 * Project coordinate inverse
	 * @param coord
	 */
	public abstract void inverse(Coord coord);

	public abstract void forward(Box2d bounds);
	public abstract void inverse(Box2d bounds);

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
	 * @param resolution 
	 * @return The level for the resolution
	 */
	public abstract double levelFromResolution(double resolution);

	/**
	 * Override the reference resolution for the given projection
	 * @param parseDouble
	 */
	public abstract void setReferenceResolution(double parseDouble);

	/**
	 * Override the projected origin for the given projection
	 * @param originCoord
	 */
	public abstract void setProjectedOrigin(Coord originCoord);

}
