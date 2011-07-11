package net.rcode.nanomaps.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import mapnik.Mapnik;
import net.rcode.core.httpserver.DefaultHttpRequestHandler;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

/**
 * Print simple statistics.
 * 
 * @author stella
 *
 */
public class AdminStatsRequestHandler extends DefaultHttpRequestHandler {
	private static final double ONEMB=(double)(1024*1024);
	
	@Override
	protected void handle() throws Exception {
		StringWriter buffer=new StringWriter();
		PrintWriter out=new PrintWriter(buffer);
		out.println("JVM Stats");
		out.println("---------");
		out.format("Available Processors=%s\n", Runtime.getRuntime().availableProcessors());
		out.format("Total Memory=%smb\n", Runtime.getRuntime().totalMemory()/ONEMB);
		out.format("Free Memory=%smb\n", Runtime.getRuntime().freeMemory()/ONEMB);
		out.format("Max Memory=%smb\n", Runtime.getRuntime().maxMemory()/ONEMB);
		out.println();
		
		Map<String, Integer> mapnikAllocs=Mapnik.getNativeAllocations();
		out.println("Mapnik Allocations");
		out.println("------------------");
		for (Map.Entry<String,Integer> entry: mapnikAllocs.entrySet()) {
			out.format("%s=%s objects\n", entry.getKey(), entry.getValue());
		}
		out.println();
		
		HttpResponse response=new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.addHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain;charset=UTF-8");
		response.setContent(ChannelBuffers.copiedBuffer(buffer.toString(), CharsetUtil.UTF_8));
		respond(response);
	}

}
