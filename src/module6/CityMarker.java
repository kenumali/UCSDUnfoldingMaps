package module6;

import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;

/** Implements a visual marker for cities on an earthquake map
 * 
 * @author UC San Diego Intermediate Software Development MOOC team
 * @author Kenneth
 *
 */
// TODO: Change SimplePointMarker to CommonMarker as the very first thing you do 
// in module 5 (i.e. CityMarker extends CommonMarker).  It will cause an error.
// That's what's expected.
public class CityMarker extends CommonMarker {

	public static int TRI_SIZE = 5;  // The size of the triangle marker
	PImage img;

	public CityMarker(Location location) {
		super(location);
	}

	public CityMarker(Feature city, PImage img) {
		super(((PointFeature)city).getLocation(), city.getProperties());
		this.img = img;
	}

	public CityMarker(Feature city) {
		super(((PointFeature)city).getLocation(), city.getProperties());
		// Cities have properties: "name" (city name), "country" (country name)
		// and "population" (population, in millions)
	}


	/**
	 * Implementation of method to draw marker on the map.
	 */
	@Override
	public void drawMarker(PGraphics pg, float x, float y) {
		// Save previous drawing style
		pg.pushStyle();

		// IMPLEMENT: drawing triangle for each city
		//		pg.fill(150, 30, 30);
		//		pg.triangle(x, y-TRI_SIZE, x-TRI_SIZE, y+TRI_SIZE, x+TRI_SIZE, y+TRI_SIZE);

		pg.imageMode(PConstants.CORNER);
		//The image is drawn in object coordinates, i.e. the marker's origin (0,0) is at its geo-location.

		pg.image(img, x-8, y-15, 30, 30);
		// Restore previous drawing style
		pg.popStyle();
	}

	/** Show the title of the city if this marker is selected */
	public void showTitle(PGraphics pg, float x, float y)
	{

		// TODO: Implement this method
		pg.pushStyle();

		String title = getCity() + ", " + getCountry();
		String pop =  "Population: " + getPopulation() + " million";
		float nameWidth = pg.textWidth(title);
		float popWidth = pg.textWidth(pop);

		pg.rectMode(PConstants.CORNER);
		pg.fill(255);
		pg.textSize(12);
		pg.rect(x+15, y-8, Math.max(nameWidth, popWidth) + 10, 35, 5, 5, 5, 5);
		pg.textAlign(PConstants.LEFT, PConstants.TOP);
		pg.fill(20, 24, 35);
		pg.text(title, x+22, y-5);
		pg.text(pop, x+22, y+10);
		pg.popStyle();
	}



	/* Local getters for some city properties.  
	 */
	public String getCity()
	{
		return getStringProperty("name");
	}

	public String getCountry()
	{
		return getStringProperty("country");
	}

	public float getPopulation()
	{
		return Float.parseFloat(getStringProperty("population"));
	}
}
