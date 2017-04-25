package module6;

import java.util.ArrayList;
import java.util.List;

import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import processing.core.PConstants;
import processing.core.PGraphics;

/** 
 * A class to represent AirportMarkers on a world map.
 *   
 * @author Adam Setters and the UC San Diego Intermediate Software Development
 * MOOC team
 *
 */
public class AirportMarker extends CommonMarker {
	public List<SimpleLinesMarker> routes;
	
	public AirportMarker(Feature city) {
		super(((PointFeature)city).getLocation(), city.getProperties());
		routes = new ArrayList<SimpleLinesMarker>();
	}
	
	@Override
	public void drawMarker(PGraphics pg, float x, float y) {
		pg.fill(252, 103, 94);
		pg.noStroke();
		pg.ellipse(x, y, 5, 5);
	}
	
	@Override
	public void showTitle(PGraphics pg, float x, float y) {
		 // show rectangle with title
		String title = getName() + " - " + getCity() + ", " + getCountry();
		String code = "IATA code:" + getCode();
		String altitude = "Altitude: " + getAltitude();
		float latitude = getLocation().getLat();
		float longitude = getLocation().getLon();
		title = title.replace("\"", "");
		code = code.replace("\"", "");
		float width = pg.textWidth(title);
		String latlon = latitude +  ", " + longitude;
		pg.pushStyle();
		
		pg.rectMode(PConstants.CORNER);
		pg.fill(255);
		pg.textSize(12);
		pg.rect(x+15, y-8, Math.max(width, pg.textWidth(latlon)) + 12, 65, 5, 5, 5, 5);
		pg.textAlign(PConstants.LEFT, PConstants.TOP);
		pg.fill(20, 24, 35);
		pg.text(code, x+22, y - 5);
		pg.text(title, x+22, y + 8);
		pg.text(altitude, x+22, y + 22);
		pg.text(latlon, x+22, y + 37);
		
		pg.popStyle();
		// show routes
	}
	
	public void addRoute(SimpleLinesMarker sl) {
		routes.add(sl);
	}
	
	public String getName() {
		return (String) getProperty("name"); 
	}
	
	public String getCity() {
		return (String) getProperty("city");
	}
	
	public String getCountry() {
		return (String) getProperty("country");
	}
	
	public String getCode() {
		return (String) getProperty("code");
	}
	
	public String getAltitude() {
		return (String) getProperty("altitude");
	}
}
