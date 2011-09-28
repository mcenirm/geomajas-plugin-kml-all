package edu.uah.mcenirm.geomajas.kml;

import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;

public abstract class KmlUtil {

	public static Feature findFeature(Feature startFeature,
			Class<? extends Feature> featureClass) {
		if (startFeature.getClass().equals(featureClass)) {
			return startFeature;
		}
		List<Feature> featureList = null;
		if (startFeature instanceof Document) {
			Document document = (Document) startFeature;
			featureList = document.getFeature();
		} else if (startFeature instanceof Folder) {
			Folder folder = (Folder) startFeature;
			featureList = folder.getFeature();
		}
		if (featureList != null) {
			for (Feature feature : featureList) {
				Feature foundFeature = findFeature(feature, featureClass);
				if (foundFeature != null) {
					return foundFeature;
				}
			}
		}
		return null;
	}

	public static GroundOverlay findGroundOverlay(Feature startFeature) {
		return (GroundOverlay) findFeature(startFeature, GroundOverlay.class);
	}
}
