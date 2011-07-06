package net.rcode.nanomaps.server;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import mapnik.Box2d;
import mapnik.Image;
import mapnik.Renderer;
import net.rcode.core.httpserver.HttpServer;
import net.rcode.core.web.ThreadedRequestHandler;
import net.rcode.nanomaps.server.projection.TileProjection;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

/**
 * Handles all requests to a map.
 * @author stella
 *
 */
public class MapRequestHandler extends ThreadedRequestHandler implements RenderCallback {
	static final Pattern SLASH_SPLIT=Pattern.compile("\\/");
	MapRepository repository;
	RenderService renderService;
	int cacheMaxAge=5;
	
	// -- Request state
	MapLocator locator;
	MapResource resource;
	TileRequest tileRequest;
	
	private static class TileRequest extends RenderRequest {
		public MapLocator locator;
		
		// Map State
		public TileProjection tileProjection;
		public Box2d bounds;
	}
	
	public MapRequestHandler(MapRepository repository, RenderService renderService) {
		this.repository=repository;
		this.renderService=renderService;
	}
	
	@Override
	protected void handleInThread() throws Exception {
		// Decode the parameters
		QueryStringDecoder qs=new QueryStringDecoder(request.getUri());
		String path=qs.getPath();
		
		// Strip leading slash
		if (path.startsWith("/")) path=path.substring(1);
		
		tileRequest=new TileRequest();
		if (path.length()>0) {
			String[] comps=SLASH_SPLIT.split(path);
			uriDecode(comps);
			
			// Take the first component as the map name
			if (comps.length>0) {
				tileRequest.mapName=comps[0];
			}
			try {
				if (comps.length>1) {
					tileRequest.level=Integer.parseInt(comps[1]);
				}
				if (comps.length>2) {
					tileRequest.x=Integer.parseInt(comps[2]);
				}
				if (comps.length>3) {
					tileRequest.y=Integer.parseInt(comps[3]);
				}
			} catch (NumberFormatException e) {
				tileRequest.error=true;
			}
		}
		
		// Validate the request
		if (!tileRequest.isValid()) {
			respondError(HttpResponseStatus.BAD_REQUEST, "Not a valid tile request");
			return;
		}
		
		// Lookup the map
		locator=repository.lookupMap(tileRequest.mapName);
		if (locator==null || !locator.isValid()) {
			respondError(HttpResponseStatus.NOT_FOUND, "Map '" + tileRequest.mapName + "' is not valid");
			return;
		}
		tileRequest.locator=locator;
		
		// Resolve the projection
		// TODO: Allow different projections in parameters
		tileRequest.tileProjection=TileProjection.createDefaultProjection();
		tileRequest.bounds=tileRequest.tileProjection.projectTile(
				tileRequest.level,
				tileRequest.x,
				tileRequest.y,
				tileRequest.tileWidth,
				tileRequest.tileHeight);
		tileRequest.cost=tileRequest.tileProjection.getCostFactor(tileRequest.bounds);
		
		// Get the underlying resource so we can get at the cache info
		resource=tileRequest.locator.resolve(tileRequest);
		if (resource==null) {
			respondError(HttpResponseStatus.NOT_FOUND, "Map '" + tileRequest.mapName + "' is not valid");
			return;
		}
		
		// Check etag and conditional get
		String resourceEtag=resource.getIdentityTag();
		if (resourceEtag!=null) {
			String ifNoneMatch=request.getHeader(HttpHeaders.Names.IF_NONE_MATCH);
			if (ifNoneMatch!=null && resourceEtag.equals(ifNoneMatch)) {
				HttpResponse notModifiedResponse=new DefaultHttpResponse(HttpVersion.HTTP_1_1, 
						HttpResponseStatus.NOT_MODIFIED);
				notModifiedResponse.addHeader(HttpHeaders.Names.CACHE_CONTROL, "max-age=" + cacheMaxAge);
				respond(notModifiedResponse);
				return;
			}
		}
		
		renderService.submit(tileRequest, this);
	}
	
	private void uriDecode(String[] comps) {
		for (int i=0; i<comps.length; i++) {
			try {
				comps[i]=URLDecoder.decode(comps[i], "UTF-8");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void doRender(RenderRequest rr) throws Exception {
		mapnik.Map m=resource.createMap(MapRequestHandler.class);
		m.resize(tileRequest.tileWidth, tileRequest.tileHeight);
		m.zoomToBox(tileRequest.bounds);
		m.setBufferSize(128);
		
		logger.info("Rendering tile with bounds " + tileRequest.bounds);
		Image image=new Image(tileRequest.tileWidth, tileRequest.tileHeight);
		Renderer.renderAgg(m, image);
		
		byte[] contents=image.saveToMemory("png");
		image.dispose();

		HttpResponse response=new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.addHeader(HttpHeaders.Names.CONTENT_TYPE, "image/png");
		String etag=resource.getIdentityTag();
		if (etag!=null) {
			response.addHeader(HttpHeaders.Names.ETAG, etag);
			response.addHeader(HttpHeaders.Names.CACHE_CONTROL, "max-age=" + cacheMaxAge);
		}
		
		response.setContent(ChannelBuffers.wrappedBuffer(contents));
		resource.recycleMap(MapRequestHandler.class, m);
		
		respond(response);
	}

	@Override
	public void handleRenderError(RenderRequest rr, Throwable t) {
		respondError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error rendering tile", t);
	}

	@Override
	public void handleCancelled(RenderRequest rr) {
		respondError(HttpResponseStatus.REQUEST_TIMEOUT, "Request cancelled");
	}

}
