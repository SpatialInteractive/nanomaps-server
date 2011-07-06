package net.rcode.nanomaps.server;

import java.text.DecimalFormat;

public class DumpLevels {

	public static void main(String[] args) {
		double HIGHEST_RES=78271.5170;
		for (int level=1; level<=18; level++) {
			double res=HIGHEST_RES/Math.pow(2, level-1);
			double den=res/0.00028;
			
			DecimalFormat fmt=new DecimalFormat("0.0");
			System.out.println("Level " + level + ":\t" + res + "\t\t" + fmt.format(den));
		}
	}
}
