package org.opentripplanner.index;

import org.opentripplanner.common.LuceneIndex;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opentripplanner.geocoder.bing.*;

/**
 * OTP Bing geocoder.
 */
@Path("/routers/{routerId}/binggeocode")
@Produces(MediaType.APPLICATION_JSON)
public class BingGeocoderResource {

    private final BingGeocoder geocoder;

    public BingGeocoderResource (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
	geocoder = new BingGeocoder();
    }

    /**
     *
     * @param address The query string we want to geocode
     * @return list of results in in the format expected by GeocoderBuiltin.js in the OTP Leaflet client
     */
    @GET
    public Response textSearch (@QueryParam("address") String query,
				@QueryParam("key") @DefaultValue("") String key
                                ) {
        geocoder.setKey(key);
        return Response.status(Response.Status.OK).entity(geocoder.geocode(query, null)).build();
    }

}
