package net.rcode.nanomaps.server;

/**
 * Callback interface for render work
 * @author stella
 *
 */
public interface RenderCallback {
	public void doRender(RenderRequest rr) throws Exception;
	public void handleCancelled(RenderRequest rr);
	public void handleRenderError(RenderRequest rr, Throwable t);
}
