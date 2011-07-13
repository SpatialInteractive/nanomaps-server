package net.rcode.nanomaps.server;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mapnik.AspectFixMode;
import mapnik.Box2d;
import mapnik.Coord;
import mapnik.Image;
import mapnik.MapDefinition;
import mapnik.Renderer;
import net.rcode.core.util.JsonBuilder;
import net.rcode.core.web.ThreadedRequestHandler;
import net.rcode.nanomaps.server.projection.RenderProjection;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

/**
 * Handles all requests to a map.  This casts a MapRepository as a hierarchical
 * structure with the following format:
 * <ul>
 * <li>/map/{mapname}/wms?{WMS parameters}
 * <li>/map/{mapname}/tile/level/x/y?{TILE parameters}
 * </ul>
 * 
 * Hitting either /map/ or /map/{mapname} result in a TOC result in JSON.
 * <h2>WMS Parameters</h2>
 * <ul>
 * <li>REQUEST=GetMap (required)
 * <li>VERSION=1.x (optional)
 * <li>BBOX=minx,miny,maxx,maxy
 * <li>ASPECTFIXMODE={GROW_BBOX,GROW_CANVAS,SHRINK_BBOX,SHRINK_CANVAS,ADJUST_BBOX_WIDTH,ADJUST_BBOX_HEIGHT,ADJUST_CANVAS_WIDTH,ADJUST_CANVAS_HEIGHT}
 * </ul>
 * 
 * <h2>Common parameters</h2>
 * <ul>
 * <li>SRS=EPSG:{code} or PROJ4:{init}
 * <li>SRSORIGIN=x,y
 * <li>SRSDEFRES=number
 * <li>CM=global|map (default=map)
 * <li>PIXELRATIO={ratio of map pixels to device pixels}
 * <li>FORMAT=image/png or other
 * <li>BUFFER=pixel buffer override
 * <li>WIDTH=pixel width
 * <li>HEIGHT=pixel height
 * </ul>
 * 
 * @author stella
 *
 */
public class MapRequestHandler extends ThreadedRequestHandler implements RenderCallback {
	static final Pattern SLASH_SPLIT=Pattern.compile("\\/");
	static final Pattern COMMA_SPLIT=Pattern.compile("\\,");
	static final Pattern SRS_PATTERN=Pattern.compile("^(epsg|proj4)\\:(.+)$", Pattern.CASE_INSENSITIVE);
	
	MapRepository repository;
	RenderService renderService;
	int cacheMaxAge=5;
	
	// -- Request state
	/**
	 * Path, not including query string.  Leading and trailing slash removed.
	 */
	String path;

	/**
	 * Path components decoded
	 */
	LinkedList<String> pathComponents;

	/**
	 * Query parameters.  Each parameter name is lower-cased.
	 */
	Map<String, String> queryParams;
	
	/**
	 * Before executing we store the request here so we can cancel it and/or
	 * cleanup when all done
	 */
	RenderRequest pendingRenderRequest;
	
	public MapRequestHandler(MapRepository repository, RenderService renderService) {
		this.repository=repository;
		this.renderService=renderService;
	}
	
	protected void decodePath() {
		QueryStringDecoder qs=new QueryStringDecoder(request.getUri());
		path=qs.getPath();
		if (path.startsWith("/")) path=path.substring(1);
		if (path.endsWith("/")) path=path.substring(0, path.length()-1);
		
		// Split into pathComponents
		pathComponents=new LinkedList<String>();
		if (!path.isEmpty()) {
			String[] comps=SLASH_SPLIT.split(path);
			for (String comp: comps) {
				pathComponents.add(uriDecode(comp));
			}
		}
		
		queryParams=new HashMap<String, String>(qs.getParameters().size()*2);
		for (Map.Entry<String,List<String>> entry: qs.getParameters().entrySet()) {
			if (entry.getValue().isEmpty()) continue;
			String value=entry.getValue().get(0);
			String name=entry.getKey().toLowerCase();
			queryParams.put(name, value);
		}
	}
	
	@Override
	protected void handleInThread() throws Exception {
		decodePath();
		
		// If it is a request for the root, then we handle it as a TOC
		// request
		if (pathComponents.isEmpty()) {
			handleTOCRequest(null, null);
			return;
		}
		
		// Lookup the map
		String mapName=pathComponents.removeFirst();
		MapLocator locator=repository.lookupMap(mapName);
		if (locator==null || !locator.isValid()) {
			respondError(HttpResponseStatus.BAD_REQUEST, "Map name " + mapName + " is not valid");
			return;
		}
		
		// Dispatch based on class
		String requestClass=null;
		if (!pathComponents.isEmpty()) {
			requestClass=pathComponents.removeFirst();
		}
		
		if (requestClass==null) {
			handleTOCRequest(mapName, locator);
			return;
		} else if ("tile".equals(requestClass)) {
			handleTileRequest(mapName, locator);
			return;
		} else if ("wms".equals(requestClass)) {
			handleWmsRequest(mapName, locator);
			return;
		} else {
			throw new IllegalArgumentException("Request class '" + requestClass + " is invalid");
		}
	}
	
	protected void handleTOCRequest(String mapName, MapLocator locator) {
		JsonBuilder tocBuilder=new JsonBuilder();
		
		if (mapName==null || locator==null) {
			// List
			tocBuilder.startObject();
			tocBuilder.key("maps", false);
			tocBuilder.startArray();
			
			for (String listName: repository.listMaps()) {
				MapLocator map=repository.lookupMap(listName);
				if (map==null || !isAnnounced(map)) continue;
				buildMapTOCEntry(tocBuilder, listName, map);
			}
		} else {
			buildMapTOCEntry(tocBuilder, mapName, locator);
		}
		
		CharSequence json=tocBuilder.getJson();
		HttpResponse response=new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json;charset=UTF-8");
		response.setContent(ChannelBuffers.copiedBuffer(json, CharsetUtil.UTF_8));
		
		respond(response);
	}

	protected void buildMapTOCEntry(JsonBuilder tocBuilder, String mapName, MapLocator map) {
		String uriRoot="http://" + request.getHeader(HttpHeaders.Names.HOST) + "/map/";
		
		// Name
		tocBuilder.startObject();
		tocBuilder.key("name", false);
		tocBuilder.value(mapName);
		
		// Uri
		String uri=uriRoot + uriEncode(mapName);
		tocBuilder.key("uri", false);
		tocBuilder.value(uri);
		
		// Tilespec
		tocBuilder.key("tileSpec", false);
		tocBuilder.value(uri + "/tile/${level}/${tileX}/${tileY}?pixelRatio=${pixelRatio}");
		
		// Properties
		tocBuilder.key("properties", false);
		tocBuilder.startObject();
		java.util.Map<String,String> properties=map.getProperties();
		for (java.util.Map.Entry<String, String> entry: properties.entrySet()) {
			tocBuilder.key(entry.getKey());
			tocBuilder.value(entry.getValue());
		}
		tocBuilder.endObject();
		
		tocBuilder.endObject();
	}
	
	protected RenderInfo setupRenderInfo() {
		RenderInfo renderInfo=new RenderInfo();
		
		// Determine coordinate mode
		String cmParam=queryParams.get("cm");
		if (cmParam==null || "map".equalsIgnoreCase(cmParam)) {
			renderInfo.coordinatesAreGlobal=false;
		} else if ("global".equalsIgnoreCase(cmParam)) {
			renderInfo.coordinatesAreGlobal=true;
		} else {
			throw new IllegalArgumentException("Illegal value for cm parameter");
		}
		
		// Determine projection
		String srsParam=queryParams.get("srs");
		if (srsParam==null) {
			renderInfo.projection=RenderProjection.createDefaultProjection();
		} else {
			Matcher srsMatcher=SRS_PATTERN.matcher(srsParam);
			if (!srsMatcher.matches()) {
				throw new IllegalArgumentException("Illegal syntax for srs parameter");
			}
			String srsQualifier=srsMatcher.group(1);
			String srsDefn=srsMatcher.group(2);
			
			if ("epsg".equalsIgnoreCase(srsQualifier)) {
				int epsgCode;
				try {
					epsgCode=Integer.parseInt(srsDefn);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal syntax for epsg code");
				}
				renderInfo.projection=RenderProjection.createFromEpsgCode(epsgCode);
			} else if ("proj4".equalsIgnoreCase(srsQualifier)) {
				renderInfo.projection=RenderProjection.createFromProj4Params(srsDefn);
			} else {
				throw new IllegalArgumentException("Illegal srs qualifier");
			}
		}
		
		// Pixel Ratio
		String pixelRatioParam=queryParams.get("pixelratio");
		if (pixelRatioParam==null) {
			renderInfo.pixelRatio=1.0;
		} else {
			try {
				renderInfo.pixelRatio=Double.parseDouble(pixelRatioParam);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegal value for pixelratio parameter");
			}
		}
		
		// Buffer
		String bufferParam=queryParams.get("buffer");
		if (bufferParam!=null) {
			try {
				renderInfo.bufferPixels=Integer.parseInt(bufferParam);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegal value for buffer parameter");
			}
		}
		
		// Format
		String formatParam=queryParams.get("format");
		renderInfo.format=translateFormat(formatParam);
		
		// srsorigin
		String originParam=queryParams.get("srsorigin");
		if (originParam!=null) {
			String[] originComps=COMMA_SPLIT.split(originParam);
			if (originComps.length!=2) {
				throw new IllegalArgumentException("Illegal value for srsorigin parameter");
			}
			
			Coord originCoord=new Coord();
			try {
				originCoord.x=Double.parseDouble(originComps[0]);
				originCoord.y=Double.parseDouble(originComps[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegal value for srsorigin parameter");
			}
			
			if (renderInfo.coordinatesAreGlobal) {
				renderInfo.projection.forward(originCoord);
			}
			renderInfo.projection.setProjectedOrigin(originCoord);
		}

		// srsdefres
		String defResParam=queryParams.get("srsdefres");
		if (defResParam!=null) {
			try {
				renderInfo.projection.setReferenceResolution(Double.parseDouble(defResParam));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegal value for srsdefres parameter");
			}
		}
		
		// width/height
		String widthParam=queryParams.get("width"), heightParam=queryParams.get("height");
		if (widthParam!=null) {
			try {
				renderInfo.width=Integer.parseInt(widthParam);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegal value for width parameter");
			}
		}
		if (heightParam!=null) {
			try {
				renderInfo.height=Integer.parseInt(heightParam);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegal value for height parameter");
			}
		}
		
		return renderInfo;
	}
	
	protected void handleWmsRequest(String mapName, MapLocator locator) throws Exception {
		RenderInfo renderInfo=setupRenderInfo();
		if (renderInfo.width==0 || renderInfo.height==0) {
			throw new IllegalArgumentException("WMS request must have width and height parameters");
		}
		
		// Bounds
		String bboxParam=queryParams.get("bbox");
		if (bboxParam==null) {
			throw new IllegalArgumentException("Parameter bbox is required");
		}
		
		String[] bboxComps=COMMA_SPLIT.split(bboxParam);
		if (bboxComps.length!=4) {
			throw new IllegalArgumentException("Illegal value for bbox parameter");
		}
		
		double[] bboxValues=new double[4];
		for (int i=0; i<4; i++) {
			try {
				bboxValues[i]=Double.parseDouble(bboxComps[i]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegal value for bbox parameter");
			}
		}
		
		renderInfo.bounds=new Box2d(bboxValues[0], bboxValues[1], bboxValues[2], bboxValues[3]);
		if (bboxValues[0]>bboxValues[2] || bboxValues[1]>bboxValues[3]) {
			throw new IllegalArgumentException("Bbox parameter is not organized as minx,miny,maxx,maxy");
		}
		
		if (renderInfo.coordinatesAreGlobal) {
			renderInfo.projection.forward(renderInfo.bounds);
		}
		
		// AspectFixMode
		String aspectFixModeParam=queryParams.get("aspectfixmode");
		if (aspectFixModeParam!=null) {
			aspectFixModeParam=aspectFixModeParam.toUpperCase();
			try {
				renderInfo.aspectFixMode=AspectFixMode.valueOf(aspectFixModeParam);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Illegal value for aspectfixmode parameter");
			}
		}
		
		handleRenderRequest(renderInfo, mapName, locator);
	}
	
	protected void handleTileRequest(String mapName, MapLocator locator) throws Exception {
		RenderInfo renderInfo=setupRenderInfo();
		
		// Default width/height for tile request
		if (renderInfo.width==0) renderInfo.width=256;
		if (renderInfo.height==0) renderInfo.height=256;
		
		// Take the first component as the map name
		int level;
		int x, y;
		try {
			level=Integer.parseInt(pathComponents.removeFirst());
			x=Integer.parseInt(pathComponents.removeFirst());
			y=Integer.parseInt(pathComponents.removeFirst());
		} catch (Exception e) {
			throw new IllegalArgumentException("Illegal values for tile coordinates");
		}
		
		// Resolve the bounds
		renderInfo.bounds=renderInfo.projection.projectTile(
				level,
				x,
				y,
				renderInfo.width,
				renderInfo.height);
		
		handleRenderRequest(renderInfo, mapName, locator);
	}
	
	protected void handleRenderRequest(RenderInfo renderInfo, String mapName, MapLocator locator) throws Exception {
		RenderRequest renderRequest=new RenderRequest();
		renderRequest.renderInfo=renderInfo;
		
		// Get the underlying resource so we can get at the cache info
		MapResource resource=locator.resolve(renderRequest);
		if (resource==null || !resource.isValid()) {
			respondError(HttpResponseStatus.NOT_FOUND, "Map '" + mapName + "' is not valid");
			return;
		}
		renderRequest.resource=resource;
		
		
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
		
		pendingRenderRequest=renderRequest;
		renderService.submit(renderRequest, this);
	}

	private boolean isAnnounced(MapLocator map) {
		String announced=map.getProperties().get("announced");
		if (announced!=null && "false".equals(announced)) return false;
		return true;
	}

	private String uriEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String uriDecode(String comp) {
		try {
			return URLDecoder.decode(comp, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doRender(RenderRequest rr) throws Exception {
		MapDefinition m=rr.resource.createMap(MapRequestHandler.class);
		int bufferSize=rr.renderInfo.bufferPixels;

		// Important! Because maps are cached, we need to reset this first or else
		// it will not effect this draw as we expect
		AspectFixMode afm=rr.renderInfo.aspectFixMode;
		if (afm==null) {
			m.setAspectFixMode(AspectFixMode.GROW_BBOX);
		} else {
			m.setAspectFixMode(afm);
		}

		m.setSrs(rr.renderInfo.projection.getSrs());
		m.resize(rr.renderInfo.width, rr.renderInfo.height);
		m.zoomToBox(rr.renderInfo.bounds);
		m.setBufferSize((int)(bufferSize * rr.renderInfo.pixelRatio));
		
		logger.info("Rendering tile with bounds " + rr.renderInfo.bounds + " and aspect fix mode=" + m.getAspectFixMode());
		Image image;
		
		image=new Image(m.getWidth(), 
				m.getHeight());
		
		Renderer.renderAgg(m, image, rr.renderInfo.pixelRatio, 0, 0);
		
		byte[] contents=image.saveToMemory(rr.renderInfo.format);
		image.dispose();

		HttpResponse response=new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.addHeader(HttpHeaders.Names.CONTENT_TYPE, "image/png");
		String etag=rr.resource.getIdentityTag();
		if (etag!=null) {
			response.addHeader(HttpHeaders.Names.ETAG, etag);
			response.addHeader(HttpHeaders.Names.CACHE_CONTROL, "max-age=" + cacheMaxAge);
		}
		
		response.setContent(ChannelBuffers.wrappedBuffer(contents));
		rr.resource.recycleMap(MapRequestHandler.class, m);
		
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

	/**
	 * Force cleans up resources at the end of the response cycle
	 */
	@Override
	public void respond(HttpResponse response) {
		super.respond(response);
		
		if (pendingRenderRequest!=null) {
			if (pendingRenderRequest.renderInfo.projection!=null) {
				pendingRenderRequest.renderInfo.projection.dispose();
			}
		}
	}

	private String translateFormat(String formatMimeType) {
		if (formatMimeType==null) return "png";
		
		if (formatMimeType.indexOf('/')<0) return formatMimeType;
		else if ("image/png".equalsIgnoreCase(formatMimeType)) return "png";
		else if ("image/jpeg".equalsIgnoreCase(formatMimeType)) return "jpg";
		else if ("image/tiff".equalsIgnoreCase(formatMimeType)) return "tiff";
		else if ("image/gif".equalsIgnoreCase(formatMimeType)) return "gif";
		
		throw new IllegalArgumentException("Unknown output format " + formatMimeType);
	}
}
