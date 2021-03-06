package com.osm2xp.translators;

import com.osm2xp.model.osm.polygon.OsmPolyline;

public interface IPolyHandler {
	public boolean handlePoly(OsmPolyline osmPolyline);
	
	public void translationComplete();
	
	public String getId();
	
	public boolean isTerminating();
}
