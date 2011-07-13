package net.rcode.nanomaps.server.projection;

import mapnik.Box2d;
import mapnik.Coord;
import mapnik.Projection;

/**
 * RenderProjection that uses Proj4 (via mapnik.Projection class for its backing
 * implementation.
 * 
 * @author stella
 *
 */
public class Proj4RenderProjection extends RenderProjection {
	protected Projection projection;
	protected String srs;
	protected double referenceResolution=100000;
	protected Coord origin;
	
	public Proj4RenderProjection(String srs) {
		this.projection=new Projection(srs);
		this.srs=srs;
		this.origin=new Coord();
	}
	
	@Override
	public void dispose() {
		projection.dispose();
	}

	@Override
	public String getSrs() {
		return srs;
	}

	@Override
	public Box2d projectTile(int level, int x, int y, int tileWidth,
			int tileHeight) {
		double resolution=resolutionFromLevel(level);
		
		double ulx=x*tileWidth*resolution + origin.x;
		double uly=origin.y - y*tileHeight*resolution;
		double lrx=ulx + tileWidth*resolution;
		double lry=uly - tileHeight*resolution;
		
		Box2d ret=new Box2d(Math.min(ulx,lrx), Math.min(uly,lry), Math.max(ulx,lrx), Math.max(uly,lry));
		return ret;
	}

	@Override
	public double resolutionFromLevel(int level) {
		return referenceResolution/Math.pow(2, level-1);
	}

	@Override
	public double levelFromResolution(double resolution) {
		return Math.log(referenceResolution/resolution) / Math.log(2) + 1;
	}

	@Override
	public void forward(Coord coord) {
		projection.forward(coord);
	}

	@Override
	public void inverse(Coord coord) {
		projection.inverse(coord);
	}

	@Override
	public void setProjectedOrigin(Coord originCoord) {
		origin.x=originCoord.x;
		origin.y=originCoord.y;
	}

	@Override
	public void setReferenceResolution(double referenceResolution) {
		this.referenceResolution=referenceResolution;
	}

	@Override
	public void forward(Box2d bounds) {
		projection.forward(bounds);
	}

	@Override
	public void inverse(Box2d bounds) {
		projection.inverse(bounds);
	}

}
