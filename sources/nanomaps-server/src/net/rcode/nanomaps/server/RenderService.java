package net.rcode.nanomaps.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import net.rcode.core.io.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the scheduling of rendering work.  Work is described by a RenderRequest
 * and carried out by a caller provided callback.
 * 
 * @author stella
 *
 */
public class RenderService {
	static final Logger logger=LoggerFactory.getLogger(RenderService.class);
	
	private ExecutorService executor;
	private List<RenderRunner> runners=new ArrayList<RenderRunner>();
	private BlockingQueue<Ticket> primaryWorkQueue=new PriorityBlockingQueue<Ticket>();
	
	private static class Ticket implements Comparable<Ticket> {
		RenderRequest request;
		RenderCallback callback;
		
		public Ticket(RenderRequest request, RenderCallback callback) {
			this.request=request;
			this.callback=callback;
		}
		
		@Override
		public int compareTo(Ticket rhs) {
			return request.compareTo(rhs.request);
		}
	}
	
	private class RenderRunner implements Runnable {
		@Override
		public void run() {
			try {
				for (;;) {
					serviceQueue();
				}
			} catch (InterruptedException e) {
				logger.info("Render worker interrupted.  Ending.");
			}
		}
		
		void serviceQueue() throws InterruptedException {
			Ticket ticket;
			try {
				ticket=primaryWorkQueue.take();
			} catch (InterruptedException e) {
				throw e;
			} 
			
			RenderRequest request=ticket.request;
			RenderCallback callback=ticket.callback;
			
			long renderStartTime=System.currentTimeMillis();
			try {
				if (request.cancelled) {
					logger.debug("Skipping cancelled request");
					callback.handleCancelled(request);
					return;
				}
				
				callback.doRender(request);
			} catch (Throwable t) {
				logger.error("Unhandled exception during render", t);
				try {
					callback.handleRenderError(request, t);
				} catch (Throwable nestedt) {
					logger.error("Recursive error handling render failure", nestedt);
				}
			}
			
			long totalRuntime=System.currentTimeMillis() - request.time;
			long renderRuntime=System.currentTimeMillis() - renderStartTime;
			if (logger.isDebugEnabled()) {
				//DecimalFormat fmt=new DecimalFormat("0.0");
				logger.debug("Render request of cost " + request.cost*1000 + " runtimes: total=" + totalRuntime + "ms" + ", render=" + renderRuntime + "ms");
			}
		}
	}
	
	public RenderService(int maxConcurrency) {
		executor=Executors.newFixedThreadPool(maxConcurrency, new NamedThreadFactory("Renderer"));
		for (int i=0; i<maxConcurrency; i++) {
			RenderRunner runner=new RenderRunner();
			runners.add(runner);
			executor.submit(runner);
		}
	}
	
	public Object submit(RenderRequest request, RenderCallback callback) {
		Ticket ticket=new Ticket(request, callback);
		primaryWorkQueue.offer(ticket);
		return ticket;
	}
	
	public void cancel(Object ticket) {
		Ticket ticketPair=(Ticket) ticket;
		ticketPair.request.cancelled=true;
		primaryWorkQueue.remove(ticketPair);
	}
}
