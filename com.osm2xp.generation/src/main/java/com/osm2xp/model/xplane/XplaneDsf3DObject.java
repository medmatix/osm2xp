package com.osm2xp.model.xplane;

import com.osm2xp.core.exceptions.Osm2xpBusinessException;
import com.osm2xp.generation.options.rules.XplaneObjectTagRule;
import com.osm2xp.model.osm.polygon.OsmPolygon;
import com.osm2xp.utils.MiscUtils;
import com.osm2xp.utils.geometry.GeomUtils;

import math.geom2d.Point2D;
import math.geom2d.polygon.LinearRing2D;

/**
 * XplaneDsfObject.
 * 
 * @author Benjamin Blanchet
 * 
 */
public class XplaneDsf3DObject extends XplaneDsfObject {

	private XplaneObjectTagRule rule;

	public XplaneDsf3DObject(OsmPolygon poly, XplaneObjectTagRule rule) {
		super(poly);
		this.rule = rule;
	}

	public XplaneDsf3DObject() {
	}

	/**
	 * @param polygon
	 * @return
	 */
	private XplaneObjGeoRef getOriginAndAngle() {
		XplaneObjGeoRef result = null;

		LinearRing2D polygon = this.osmPolygon.getPolygon();

		if (polygon.vertices().size() == 5) {

			for (int i = 0; i < polygon.vertices().size() - 2; i++) {
				Point2D ptX = polygon.vertex(i);
				Point2D ptOrigin = polygon.vertex(i + 1);
				Point2D ptY = polygon.vertex(i + 2);
				Point2D ptLast;

				if (i == polygon.vertices().size() - 3) {
					ptLast = polygon.vertex(0);
				} else {
					ptLast = polygon.vertex(i + 3);
				}

				double segmentX = GeomUtils.latLonDistance(ptX.y(), ptX.x(),
						ptOrigin.y(), ptOrigin.x());
				double segmentY = GeomUtils.latLonDistance(ptOrigin.y(),
						ptOrigin.x(), ptY.y(), ptY.x());
				// check if the rule x/y segments "fits" the current osm
				// polygon
				Boolean xVectorCheck = segmentX > rule.getxVectorMinLength()
						&& segmentX < rule.getxVectorMaxLength();
				Boolean yVectorCheck = segmentY > rule.getyVectorMinLength()
						&& segmentY < rule.getyVectorMaxLength();
				Boolean dimensionsCheck = xVectorCheck && yVectorCheck;

				// if that's the case, compute the rotation point (origin)
				// of
				// the object
				if (dimensionsCheck) {
					result = new XplaneObjGeoRef();
					result.origin = ptOrigin;
					result.angle = GeomUtils.getTrueBearing(ptOrigin, ptY);
				}

				if (result != null)
					break;
			}

		}

		return result;
	}

	private XplaneObjGeoRef computeObjGeoRef() {
		XplaneObjGeoRef result = new XplaneObjGeoRef();
		if (osmPolygon != null && osmPolygon.getPolygon() != null) {
			// if polygon is under 5 points, use first point
			if (osmPolygon.getPolygon().vertices().size() < 5) {
				result = computeBasicOriginAndAngle();
			}
			// if more complex polygon
			else {
				// if simple rectangle and if rule is made for simple polygon
				if (osmPolygon.getPolygon().vertices().size() == 5
						&& rule.isSimplePolygonOnly()) {
					result = getOriginAndAngle();
				}
				// if complex polygon
				else {
					result = computeComplexPolygonOriginAndAngle();
				}

			}
		}
		return result;
	}

	private XplaneObjGeoRef computeComplexPolygonOriginAndAngle() {
		XplaneObjGeoRef result = new XplaneObjGeoRef();
		result.origin = GeomUtils.getPolylineCenter(osmPolygon.getPolygon());
		if (rule.isRandomAngle()) {
			result.angle = Double.valueOf(MiscUtils.getRandomSize(0, 360));
		} else {
			result.angle = Double.valueOf(rule.getAngle());
		}
		return result;
	}

	private XplaneObjGeoRef computeBasicOriginAndAngle() {
		XplaneObjGeoRef result = new XplaneObjGeoRef();
		result.origin = osmPolygon.getPolygon().firstPoint();
		if (rule.isRandomAngle()) {
			result.angle = Double.valueOf(MiscUtils.getRandomSize(0, 360));
		} else {
			result.angle = Double.valueOf(rule.getAngle());
		}
		return result;
	}

	public String asObjDsfText() throws Osm2xpBusinessException {

		StringBuffer sb = new StringBuffer();
		// compute the center of the object.

		XplaneObjGeoRef obj = computeObjGeoRef();
		if (obj == null || obj.angle == null | obj.origin == null) {
			throw new Osm2xpBusinessException("Error computing 3D object");
		}
		sb.append("OBJECT " + this.dsfIndex + " " + obj.origin.x() + " "
				+ obj.origin.y() + " " + obj.angle);
		sb.append(System.getProperty("line.separator"));
		return sb.toString();
	}

	public XplaneObjectTagRule getRule() {
		return rule;
	}

	public void setRule(XplaneObjectTagRule rule) {
		this.rule = rule;
	}

	/**
	 * inner class for angle and origin storage.
	 * 
	 * @author Benjamin Blanchet
	 * 
	 */
	private class XplaneObjGeoRef {
		protected Point2D origin;
		protected Double angle;

	}
}