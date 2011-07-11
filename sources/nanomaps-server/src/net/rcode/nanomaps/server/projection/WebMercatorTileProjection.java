package net.rcode.nanomaps.server.projection;

import mapnik.Box2d;
import mapnik.Projection;

/**
 * Standard web mercator tile system
 * @author stella
 *
 */
public class WebMercatorTileProjection extends TileProjection {
	public static final String SRS="+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs +over";
	private static final double HIGHEST_RES=78271.5170;
	private static final Box2d GLOBAL_EXTENT=new Box2d(-180.0, -85.05112878, 180.0, 85.05112878);
	
	private Projection projection;
	private Box2d PROJECTED_EXTENT;
	
	public WebMercatorTileProjection() {
		projection=new Projection(SRS);
		PROJECTED_EXTENT=new Box2d(GLOBAL_EXTENT);
		projection.forward(PROJECTED_EXTENT);
	}
	
	@Override
	public void dispose() {
		projection.dispose();
	}
	
	@Override
	public Projection getProjection() {
		return projection;
	}
	
	/* (non-Javadoc)
	 * @see net.rcode.mapocalypse.projection.TileProjection#getSrs()
	 */
	@Override
	public String getSrs() {
		return SRS;
	}
	
	/* (non-Javadoc)
	 * @see net.rcode.mapocalypse.projection.TileProjection#projectTile(int, int, int, int, int)
	 */
	@Override
	public Box2d projectTile(int level, int x, int y, int tileWidth, int tileHeight) {
		double resolution=resolutionFromLevel(level);
		
		double ulx=x*tileWidth*resolution + PROJECTED_EXTENT.minx;
		double uly=PROJECTED_EXTENT.maxy - y*tileHeight*resolution;
		double lrx=ulx + tileWidth*resolution;
		double lry=uly - tileHeight*resolution;
		
		Box2d ret=new Box2d(Math.min(ulx,lrx), Math.min(uly,lry), Math.max(ulx,lrx), Math.max(uly,lry));
		return ret;
	}
	
	/* (non-Javadoc)
	 * @see net.rcode.mapocalypse.projection.TileProjection#resolutionFromLevel(int)
	 */
	@Override
	public double resolutionFromLevel(int level) {
		return HIGHEST_RES/Math.pow(2, level-1);
	}
	
	@Override
	public double getCostFactor(Box2d extent) {
		return ((extent.maxx-extent.minx))/(PROJECTED_EXTENT.maxx-PROJECTED_EXTENT.minx);
			   //((double)(extent.maxy-extent.miny))/(PROJECTED_EXTENT.maxy-PROJECTED_EXTENT.miny);
	}
}
