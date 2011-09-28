package edu.uah.mcenirm.geomajas.kml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.geomajas.configuration.RasterLayerInfo;
import org.geomajas.geometry.Bbox;
import org.geomajas.geometry.Crs;
import org.geomajas.geometry.CrsTransform;
import org.geomajas.global.ExceptionCode;
import org.geomajas.global.GeomajasException;
import org.geomajas.layer.LayerException;
import org.geomajas.layer.RasterLayer;
import org.geomajas.layer.tile.RasterTile;
import org.geomajas.layer.tile.TileCode;
import org.geomajas.service.GeoService;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import com.vividsolutions.jts.geom.Envelope;

import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LatLonBox;

public class KmlGroundOverlayLayer implements RasterLayer {
	private static final String EPSG_4326 = "EPSG:4326";

	private String id;
	private Crs crs;
	private RasterLayerInfo layerInfo;

	private String url;

	@Autowired
	private GeoService geoService;

	private Kml kml;
	private GroundOverlay groundOverlay;

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public Crs getCrs() {
		return crs;
	}

	public RasterLayerInfo getLayerInfo() {
		return layerInfo;
	}

	public List<RasterTile> paint(CoordinateReferenceSystem boundsCrs,
			Envelope bounds, double scale) throws GeomajasException {
		LatLonBox latLonBox = groundOverlay.getLatLonBox();
		double west = latLonBox.getWest();
		double east = latLonBox.getEast();
		double south = latLonBox.getSouth();
		double north = latLonBox.getNorth();
		Bbox layerBox = new Bbox(west, south, east - west, north - south);

		CrsTransform layerToMap = geoService.getCrsTransform(getCrs(),
				boundsCrs);
		Bbox worldBox = geoService.transform(layerBox, layerToMap);

		Bbox screenBox = new Bbox(Math.round(scale * worldBox.getX()),
				-Math.round(scale * worldBox.getMaxY()), Math.round(scale
						* worldBox.getMaxX())
						- Math.round(scale * worldBox.getX()), Math.round(scale
						* worldBox.getMaxY())
						- Math.round(scale * worldBox.getY()));

		RasterTile tile = new RasterTile(screenBox, getId());
		tile.setCode(new TileCode(0, 0, 0));
		tile.setUrl(groundOverlay.getIcon().getHref());

		List<RasterTile> tiles = new ArrayList<RasterTile>(1);
		tiles.add(tile);
		return tiles;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	@PostConstruct
	protected void postConstruct() throws GeomajasException {
		if (null == url) {
			throw new GeomajasException(ExceptionCode.PARAMETER_MISSING, "url");
		}
		if (null != layerInfo) {
			throw new GeomajasException(ExceptionCode.PARAMETER_INVALID_VALUE,
					"layerInfo");
		}
		layerInfo = new RasterLayerInfo();

		refreshKml();
		groundOverlay = KmlUtil.findGroundOverlay(kml.getFeature());
		if (null == groundOverlay) {
			throw new GeomajasException(ExceptionCode.UNEXPECTED_PROBLEM,
					"no ground overlay found");
		}

		crs = geoService.getCrs2(EPSG_4326);
		layerInfo.setCrs(EPSG_4326);
	}

	protected void refreshKml() throws LayerException {
		InputStream stream = null;
		kml = null;
		try {
			URLConnection connection = ResourceUtils.getURL(url)
					.openConnection();
			stream = connection.getInputStream();
			kml = Kml.unmarshal(stream);
		} catch (Exception e) {
			throw new LayerException(e, ExceptionCode.UNEXPECTED_PROBLEM);
		} finally {
			if (null != stream) {
				try {
					stream.close();
				} catch (IOException e) {
					throw new LayerException(e,
							ExceptionCode.UNEXPECTED_PROBLEM);
				}
			}
		}
	}

}
