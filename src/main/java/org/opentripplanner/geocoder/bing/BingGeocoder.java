/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.geocoder.bing;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import flexjson.JSONDeserializer;
import javax.ws.rs.core.UriBuilder;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;
import org.opentripplanner.geocoder.google.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Envelope;

/**
 * A geocoder using the data.gouv.fr API of BANO (Base Nationale d'Adresse Ouverte), the official
 * open-data address source covering the whole of France.
 *
 * The returned data is rather simple to use, as it returns a GeoJSON features collection.
 * 
 * Obviously, this geocoder will only work in France.
 *
 * @author laurent
 */
public class BingGeocoder implements Geocoder {

    private static final String BING_URL = "http://dev.virtualearth.net/REST/v1/Locations";
    
    private ObjectMapper mapper;
    private GoogleJsonDeserializer googleJsonDeserializer = new GoogleJsonDeserializer();

    private String key="";

    public BingGeocoder() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     */
    @Override
    public GeocoderResults geocode(String address, Envelope bbox) {

	String content = null;
        List<GeocoderResult> geocoderResults = new ArrayList<GeocoderResult>();
	
	try {
	    URL url = getBingGeocoderUrl(address, key);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            
            StringBuilder sb = new StringBuilder(128);
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            content = sb.toString();

  	    BingJsonDeserializer bingJsonDeserializer = new BingJsonDeserializer();

            BingGeocoderResourceSets resourceSets = bingJsonDeserializer.parseResults(content);
            for(BingGeocoderResourceSet recources : resourceSets.getResourceSets())
            {
		for(BingGeocoderResource resource : recources.getResources())
		{
                    String formattedAddress = resource.getName();
                    BingGeocoderPoint point = resource.getPoint();
                    Double lat = point.coordinates.get(0);
                    Double lng = point.coordinates.get(1);
                    GeocoderResult geocoderResult = new GeocoderResult(lat, lng, formattedAddress);
                    geocoderResults.add(geocoderResult);
		}

            }

  	    return new GeocoderResults(geocoderResults);

	} catch(Exception e)
	{
  	   return noGeocoderResult("Error parsing geocoder response (2)");
	}
    }

    private URL getBingGeocoderUrl(String address, String key) throws IOException {
        UriBuilder uriBuilder = UriBuilder.fromUri(BING_URL);
        uriBuilder.queryParam("query", address);
        uriBuilder.queryParam("key", key);
        URI uri = uriBuilder.build();
        return new URL(uri.toString());
    }

    private GeocoderResults noGeocoderResult(String error) {
	return new GeocoderResults(error);
    }

    public static class BingJsonDeserializer {
    private JSONDeserializer<BingGeocoderResourceSets> jsonDeserializer;

    public BingJsonDeserializer() 
    {
	jsonDeserializer = new JSONDeserializer<BingGeocoderResourceSets>().use(null, BingGeocoderResourceSets.class);
    }

    public BingGeocoderResourceSets parseResults(String content)
    {
	return (BingGeocoderResourceSets) jsonDeserializer.deserialize(content);
    }
    }

	public static class BingGeocoderResourceSets {
        private List<BingGeocoderResourceSet> resourceSets;

        public List<BingGeocoderResourceSet> getResourceSets() {
                return resourceSets;
        }

        public void setResourceSets(List<BingGeocoderResourceSet> resourceSets) {
                this.resourceSets = resourceSets;
        }
	}

	public static class BingGeocoderResourceSet {
        private List<BingGeocoderResource> resources;

        public List<BingGeocoderResource> getResources() {
                return resources;
        }

        public void setResources(List<BingGeocoderResource> resources) {
                this.resources = resources;
        }
	}

	public static class BingGeocoderResource {
        private String name;
        private BingGeocoderPoint point;

        public String getName() {
                return name;
        }
        public void setName(String name) {
                this.name = name;
        }

        public BingGeocoderPoint getPoint() {
                return point;
        }
        public void setPoint(BingGeocoderPoint point) {
                this.point = point;
        }
	}

	public static class BingGeocoderPoint {
        private List<Double> coordinates;
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<Double> getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(List<Double> coordinates) {
            this.coordinates = coordinates;
        }
    }
}
