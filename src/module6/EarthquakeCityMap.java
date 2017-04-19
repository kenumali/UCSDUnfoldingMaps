package module6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.AbstractMapProvider;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.providers.OpenStreetMap.OpenStreetMapProvider;
import de.fhpotsdam.unfolding.utils.GeoUtils;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;
import processing.core.PGraphics;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * Author: UC San Diego Intermediate Software Development MOOC team
 * @author Kenneth
 * Date: April 19, 2017
 * */
public class EarthquakeCityMap extends PApplet {

	// We will use member variables, instead of local variables, to store the data
	// that the setup and draw methods will need to access (as well as other methods)
	// You will use many of these variables, but the only one you should need to add
	// code to modify is countryQuakes, where you will store the number of earthquakes
	// per country.

	// You can ignore this.  It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;

	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";

	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";

	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";

	// The map
	private UnfoldingMap map;

	//Map providers
	private AbstractMapProvider provider1;
	private AbstractMapProvider provider2;
	private AbstractMapProvider provider3;
	private AbstractMapProvider provider4;
	private AbstractMapProvider provider5;

	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;

	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

	private PGraphics buffer;

	//fields for clicking the city marker
	private int nearbyEarthquake;
	private float averageMagnitude;
	private EarthquakeMarker mostRecent;

	public void setup() {		
		// (1) Initializing canvas and map tiles
		size(900, 700, OPENGL);
		buffer = createGraphics(900, 700);

		if (offline) {
			map = new UnfoldingMap(this, 200, 50, 650, 600, new MBTilesMapProvider(mbTilesString));
			earthquakesURL = "2.5_week.atom";  // The same feed, but saved August 7, 2015
		}
		else {
			//			map = new UnfoldingMap(this, 200, 50, 650, 600, new Google.GoogleMapProvider());
			provider1 = new Microsoft.HybridProvider();
			provider2 = new Google.GoogleMapProvider();
			provider3 = new Microsoft.AerialProvider();
			provider4 = new OpenStreetMap.OpenStreetMapProvider();
			provider5 = new Google.GoogleTerrainProvider();
			// IF YOU WANT TO TEST WITH A LOCAL FILE, uncomment the next line
			//earthquakesURL = "2.5_week.atom";
		}
		map = new UnfoldingMap(this, 200, 50, 650, 600, provider1);
		MapUtils.createDefaultEventDispatcher(this, map);

		// (2) Reading in earthquake data and geometric properties
		//     STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);

		//     STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
			cityMarkers.add(new CityMarker(city, loadImage("ui/marker_red.png")));
		}

		//     STEP 3: read in earthquake RSS feed
		List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
		quakeMarkers = new ArrayList<Marker>();

		for(PointFeature feature : earthquakes) {
			//check if LandQuake
			if(isLand(feature)) {
				quakeMarkers.add(new LandQuakeMarker(feature));
			}
			// OceanQuakes
			else {
				quakeMarkers.add(new OceanQuakeMarker(feature));
			}
		}

		// could be used for debugging
		printQuakes();

		// (3) Add markers to map
		//     NOTE: Country markers are not added to the map.  They are used
		//           for their geometric properties
		map.addMarkers(quakeMarkers);
		map.addMarkers(cityMarkers);

		//Prints the top 'x' numbers of earthquakes depending on magnitude
		//From large to smallest magnitude
		sortAndPrint(10);
	}  // End setup


	public void draw() {
		buffer.beginDraw();
		background(0);
		map.draw();
		buffer.endDraw();
		image(buffer, 0, 0);
		addKey();
		buffer.clear();

		//Draw latitude and longitude in a rectangle below the map
		Location location = map.getLocation(mouseX, mouseY);
		fill(0);
		text(location.getLat()+", "+location.getLon(), 685, 675);

		if(lastSelected != null) {
			lastSelected.drawTitleOnTop(buffer, mouseX, mouseY);
		}

		//show an info like the count of nearby earthquake, average magnitude, and recent earthquake
		if(lastClicked instanceof CityMarker) {
			popMenu();
		}
	}

	private void sortAndPrint(int numToPrint) {
		List<EarthquakeMarker> quakes = new ArrayList<EarthquakeMarker>();
		for(Marker marker : quakeMarkers) {
			quakes.add((EarthquakeMarker) marker);
		}
		Collections.sort(quakes);
		if(numToPrint > quakes.size()) {
			for(int i = 0; i < quakes.size(); i++) {
				System.out.println(quakes.get(i));
			}
		} else {
			for(int i = 0; i < numToPrint; i++) {
				System.out.println(quakes.get(i));
			}
		}
	}

	/** Event handler that gets called automatically when the 
	 * mouse moves.
	 */
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
	}

	// If there is a marker under the cursor, and lastSelected is null 
	// set the lastSelected to be the first marker found under the cursor
	// Make sure you do not select two markers.
	// 
	private void selectMarkerIfHover(List<Marker> markers)
	{
		if (lastSelected != null) {
			return;
		}
		
		for(Marker marker : markers) {
			if(marker.isInside(map, mouseX, mouseY) && lastSelected == null) {
				lastSelected = (CommonMarker) marker;
				lastSelected.setSelected(true);
				break;
			}
		}
	}

	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		// TODO: Implement this method
		// Hint: You probably want a helper method or two to keep this code
		// from getting too long/disorganized
		if(lastClicked != null) {
			lastClicked.setClicked(false);
			lastClicked = null;
			unhideMarkers();
			nearbyEarthquake = 0;
			averageMagnitude = 0.0f;
			mostRecent = null;
		} else {
			checkMarkersForClick(quakeMarkers);
			checkMarkersForClick(cityMarkers);
			if(lastClicked instanceof EarthquakeMarker) {
				hideOtherMarkers(quakeMarkers);
				hideCityMarkers(cityMarkers);
			} else if(lastClicked instanceof CityMarker) {
				hideOtherMarkers(cityMarkers);
				hideQuakeMarkers(quakeMarkers);
			}
		}
	}

	/**
	 * The event handler for keyboard press
	 * This will change the appearance of the map
	 */
	public void keyPressed() {
		if(key == '1') {
			map.mapDisplay.setProvider(provider1);
		} else if(key == '2') {
			map.mapDisplay.setProvider(provider2);
		} else if(key == '3') {
			map.mapDisplay.setProvider(provider3);
		} else if(key == '4') {
			map.mapDisplay.setProvider(provider4);
		} else if(key == '5') {
			map.mapDisplay.setProvider(provider5);
		} else if(key == '6') { //show Past Hour earthquakes
			showRecentEarthquakes();
		} else if(key == '7') { //show earthquake markers only
			unhideMarkers();
		}
	}

	private void showRecentEarthquakes() {
		for(Marker earthquake : quakeMarkers) {
			if(earthquake.getStringProperty("age").equals("Past Hour") || earthquake.getStringProperty("age").equals("Past Day")) {
				earthquake.setHidden(false);
			} else {
				earthquake.setHidden(true);
			}
		}
	}

	private void hideCityMarkers(List<Marker> cities) {
		for(Marker city : cities) {
			if(city.getDistanceTo(lastClicked.getLocation()) > ((EarthquakeMarker) lastClicked).threatCircle()) {
				city.setHidden(true);
			} else { 
				city.setHidden(false);
			}
		}
	}

	private void hideQuakeMarkers(List<Marker> earthquakes) {
		for(Marker earthquake : earthquakes) {
			if(earthquake.getDistanceTo(lastClicked.getLocation()) > ((EarthquakeMarker) earthquake).threatCircle()) {
				earthquake.setHidden(true);
			} else {
				earthquake.setHidden(false);
				nearbyEarthquake++;
				averageMagnitude += ((EarthquakeMarker) earthquake).getMagnitude();
				if(earthquake.getStringProperty("age").equals("Past Hour") || earthquake.getStringProperty("age").equals("Past Day")) {
					mostRecent = (EarthquakeMarker) earthquake;
				}
			}
		}
	}

	private void hideOtherMarkers(List<Marker> markers) {
		for(Marker marker : markers) {
			if(marker != lastClicked) {
				marker.setHidden(true);
			}
		}
	} 

	private void checkMarkersForClick(List<Marker> markers) {
		// TODO Auto-generated method stub
		for(Marker marker : markers) {
			if(lastClicked != null) {
				break;
			}
			if(!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;
				lastClicked.setClicked(true);
				break;
			}
		}
	}


	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}

		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}

	// helper method to draw key in GUI
	private void addKey() {	
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);

		int xbase = 25;
		int ybase = 50;

		rect(xbase, ybase, 150, 400);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);

		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		//		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE, 
		//				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE, 
		//				tri_ybase+CityMarker.TRI_SIZE);

		image(loadImage("ui/marker_red.png"), xbase+29, ybase+37, 25, 25);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);

		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+120);

		fill(255, 255, 255);
		ellipse(xbase+35, 
				ybase+70, 
				10, 
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);

		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Keyboard Shortcuts", xbase+17, ybase+230);
		text("1 Hybrid", xbase+17, ybase+250);
		text("2 GoogleMap", xbase+17, ybase+270);
		text("3 Aerial", xbase+17, ybase+290);
		text("4 OpenStreetMap", xbase+17, ybase+310);
		text("5 GoogleTerrain", xbase+17, ybase+330);
		text("6 Recent Earthquakes", xbase+17, ybase+350);
		text("7 Show All", xbase+17, ybase+370);

		fill(255);
		rect(xbase+650, ybase+610, 175, 30);
	}

	private void popMenu() {
		fill(255, 250, 240);

		int xbase = 25;
		int ybase = 460;

		rect(xbase, ybase, 150, 190);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(20);
		text(nearbyEarthquake, xbase+100, ybase+20);
		textSize(12);
		text("Nearby", xbase+17, ybase+15);
		text("Earthquakes", xbase+17, ybase+30);

		text("Average", xbase+17, ybase+55);
		text("Magnitude", xbase+17, ybase+70);
		textSize(20);
		float average = averageMagnitude / nearbyEarthquake;
		if(nearbyEarthquake == 0) {
			average = 0;
		}
		text(average, xbase+80, ybase+60);
		textSize(12);
		text("Most Recent", xbase+17, ybase+105);
		text("Earthquake", xbase+17, ybase+120);
		if(mostRecent != null) {
			String[] title = mostRecent.getTitle().split("-");
			String magnitude = title[0].trim();
			String distance = title[1].trim().substring(0, title[1].indexOf("of")+1);
			String loc = title[1].substring(title[1].indexOf("of")+2).trim();
			text(magnitude, xbase+17, ybase+140);
			text(distance, xbase+17, ybase+155);
			text(loc, xbase+17, ybase+170);
		}  else {
			textSize(14);
			text("None", xbase+17, ybase+140);
		}
	}


	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.	
	private boolean isLand(PointFeature earthquake) {

		// IMPLEMENT THIS: loop over all countries to check if location is in any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		// not inside any country
		return false;
	}

	// prints countries with number of earthquakes
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers)
			{
				EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				System.out.println(countryName + ": " + numQuakes);
			}
		}
		System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}



	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if 
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {

			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {

				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));

					// return if is inside one
					return true;
				}
			}
		}

		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));

			return true;
		}
		return false;
	}

}
