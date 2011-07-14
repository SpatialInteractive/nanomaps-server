package net.rcode.nanomaps.server;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.rcode.core.httpserver.HttpServer;
import net.rcode.core.httpserver.SimpleRequestDispatcher;
import net.rcode.core.io.NamedThreadFactory;
import net.rcode.core.web.FilesRequestHandler;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main map server
 * @author stella
 *
 */
public class ServerMain {
	private static final Logger logger=LoggerFactory.getLogger(ServerMain.class);
	private int serverPort=7666;
	
	private ExecutorService serverBossExecutor;
	private ExecutorService serverWorkerExecutor;
	private ExecutorService webWorkerThreadPool;
	
	
	public ServerMain() {
		// Netty server setup
		serverBossExecutor=Executors.newCachedThreadPool(new NamedThreadFactory("ServerBoss"));
		serverWorkerExecutor=Executors.newCachedThreadPool(new NamedThreadFactory("ServerWorker"));

		// General purpose web workers
		webWorkerThreadPool=Executors.newFixedThreadPool(5, new NamedThreadFactory("WebWorker"));
		
	}
	
	public void start() throws Exception {
		// Server socket
		Map<String,Object> serverSocketOptions=new HashMap<String,Object>();
		serverSocketOptions.put("client.tcpNoDelay", true);
		serverSocketOptions.put("backlog", 25);
		serverSocketOptions.put("reuseAddress", true);
		ChannelFactory serverSocketChannelFactory=new NioServerSocketChannelFactory(serverBossExecutor, serverWorkerExecutor);

		ServerBootstrap bootstrap=new ServerBootstrap(serverSocketChannelFactory);
		HttpServer server=new HttpServer(webWorkerThreadPool);
		bootstrap.setPipelineFactory(server);

		// Map repository/renderer
		FileSystemMapRepository repository=new FileSystemMapRepository(new File("repository").getAbsoluteFile());
		repository.scan();
		
		RenderService renderService=new RenderService(Runtime.getRuntime().availableProcessors()+1);
		//RenderService renderService=new RenderService(1);

		// Http Server setup
		SimpleRequestDispatcher mainDispatcher=new SimpleRequestDispatcher();
		server.getDispatchers().add(mainDispatcher);

		// URL Rewrites (must come first)
		mainDispatcher.rewriteStatic("/", "/static/index.html");
		
		// Main map request handler
		mainDispatcher.pathPrefix("/map", true, new MapRequestHandler(repository, renderService));
		
		// Static files
		File docRoot=new File("web");
		FilesRequestHandler files=new FilesRequestHandler(null, null, docRoot);
		mainDispatcher.pathPrefix("/static", true, files);
		
		// Admin
		mainDispatcher.path("/_admin/stats", new AdminStatsRequestHandler());
		
		// Listen
		logger.info("Starting server on port " + serverPort);
		bootstrap.bind(new InetSocketAddress(serverPort));
	}
	
	public static void main(String[] args) throws Exception {
		logger.info("Initializing environment.  If something goes wrong here, library paths are likely not setup");
		ProcessSetup.initEnvironment();
		logger.info("Environment initialized");
		
		ServerMain s=new ServerMain();
		s.start();
	}
}
