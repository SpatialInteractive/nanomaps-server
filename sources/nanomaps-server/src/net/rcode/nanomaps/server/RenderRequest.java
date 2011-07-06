package net.rcode.nanomaps.server;


/**
 * Value object representing a request for a tile.  This class presently munges
 * flow control and request parameters together.  A future refactor will break this up.
 * @author stella
 *
 */
public class RenderRequest implements Comparable<RenderRequest> {
	/**
	 * Tile requests are ordered in inverse order by cost
	 */
	public double cost=1.0;
	public final long time=System.currentTimeMillis();
	
	// Flow control
	public boolean error=false;
	public volatile boolean cancelled=false;
	
	// Defaults that can be changed
	public int tileWidth=256;
	public int tileHeight=256;
	
	// Originating Request parameters
	public String mapName;
	public Integer level;
	public Integer x;
	public Integer y;
	
	public boolean isValid() {
		return !error && mapName!=null && level!=null && x!=null && y!=null;
	}
	
	@Override
	public int compareTo(RenderRequest o) {
		double otherCost=o.cost;
		int ret=Double.compare(cost, otherCost);
		return ret;
	}
	
	
}
