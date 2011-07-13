package net.rcode.nanomaps.server.projection;


/**
 * Standard web mercator tile system
 * @author stella
 *
 */
public class WebMercatorRenderProjection extends Proj4RenderProjection {
	public static final String SRS="+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs +over";
	private static final double HIGHEST_RES=78271.5170;
	
	public WebMercatorRenderProjection() {
		super(SRS);
		setReferenceResolution(HIGHEST_RES);
		origin.x=-180.0;
		origin.y=85.05112878;
		forward(origin);
	}
	
}
