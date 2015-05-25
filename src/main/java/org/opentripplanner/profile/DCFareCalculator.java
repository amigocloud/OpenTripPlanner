package org.opentripplanner.profile;

import com.google.common.collect.Lists;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.profile.fares.FareTable;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

/**
 */
public class DCFareCalculator {

    private static RideType classify (Route route) {
        // NOTE the agencyId string of the route's agencyAndId is not the same as the one given by route.getAgency.
        // The former is the same for all routes in the feed. The latter is the true agency of the feed.

        String agency = route.getAgency().getId();
        String agency_url = route.getAgency().getUrl(); // this is used in single-agency feeds so it should work
        String short_name = route.getShortName();
        String long_name = route.getLongName();
        if ("VTA".equals(agency)) {
		return RideType.VTA;
        } else if ("caltrain-ca-us".equals(agency)) {
		return RideType.CALTRAIN;
	} else if("samtrans-ca-us".equals(agency)) {
		return RideType.SAMTRANS;
	} else if("Santa Cruz Metro".equals(agency)) {
                return RideType.SANTACRUZ;
        } else if("SFMTA".equals(agency)) {
                return RideType.SFMTA;
        } else if("BART".equals(agency)) {
                return RideType.BART;
        } else if("AC Transit".equals(agency)) {
                return RideType.AC_TRANSIT;
        } else if("Golden Gate Transit".equals(agency)) {
                return RideType.GG_TRANSIT;
        } else {
		System.out.println("classify agency: " + agency);
	}
        return null;
    }

    /**
     * Should we have exactly one fare per ride, where some fares may have zero cost if they are transfers from the same operator?
     * ...except that this doesn't work for MetroRail, where two legs combine into one.
     */
    public static List<Fare> calculateFares (List<Ride> rides) {
        List<FareRide> fareRides = Lists.newArrayList();
        FareRide prev = null;
        for (Ride ride : rides) {
            // Calculate the fare for a ride based on only one of its patterns. This is usually right as long as the
            // patterns are all the same mode and operator. That happens less naturally now that stops are being grouped.
            // FIXME cluster stops considering mode and operator
            PatternRide exemplarPatternRide = ride.patternRides.get(0);
            FareRide fareRide = new FareRide(exemplarPatternRide, prev);
            if (prev != null && prev.type == fareRide.type) {
                prev.to = fareRide.to;
                prev.calcFare(); // recalculate existing fare using new destination
            } else {
                fareRides.add(fareRide);
                prev = fareRide;
            }
        }
        List<Fare> fares = Lists.newArrayList();
        for (FareRide fareRide : fareRides) {
            fares.add(fareRide.fare);
        }
        return fares;
    }
    
    public static Rectangle2D.Double createFareZone(double min_lon, double max_lat, double max_lon, double min_lat) {
    	return new Rectangle2D.Double(min_lon, max_lat, max_lon - min_lon, max_lat - min_lat);
    }

    static class FareArea extends Rectangle2D.Double {
    	
    	public FareArea(double min_lon, double min_lat, double max_lon, double max_lat) {
    		super(min_lon, min_lat, max_lon - min_lon, max_lat - min_lat);
    	}
    	
    	public boolean containsStop(Stop stop) {
    		return super.contains(stop.getLon(), stop.getLat());
    	}
    }
    
    static class FareRide {
        Stop from;
        Stop to;
        Route route;
        RideType type;
        Fare fare;
        FareRide prev;
        public FareRide (PatternRide pRide, FareRide prevRide) {
            from = pRide.getFromStop();
            to = pRide.getToStop();
            // Problem: Rides no longer have a single fare because they may be on multiple routes.
            // TODO: make sure Patterns in Rides are all the same mode and operator.
            // This seems to happen naturally because different operators generally do not share stops.
            route = pRide.pattern.route;
            type = classify(route);
            prev = prevRide;
            calcFare();
        }
        private void setFare(double base, boolean transferReduction) {
            fare = new Fare(base);
            fare.transferReduction = transferReduction;
        }
        private void setFare(double low, double peak, double senior, boolean transferReduction) {
            fare = new Fare(peak);
            fare.low = low;
            fare.senior = senior;
            fare.transferReduction = transferReduction;
        }
        // TODO store rule-based Fares in a table keyed on (type, prevtype) instead of doing on the fly
        // automatically compose string using 'free' or 'discounted' and route name
        private void calcFare() {
            RideType prevType = (prev == null) ? null : prev.type;
            if (type == null)
                return;
            switch (type) {
            case VTA:
                setFare(2.00, false);
                break;
            case CALTRAIN:
                setFare(10.00, false);
                break;
            case SAMTRANS:
		setFare(4.00, false);
		break;        
            case SANTACRUZ:
                setFare(5.00, false);
                break;
            case SFMTA:
                setFare(6.00, false);
                break;
	    case BART:
                setFare(3.50, false);
                break;
 	    case AC_TRANSIT:
                setFare(5.00, false);
                break;
	    case GG_TRANSIT:
                setFare(7.50, false);
                break;
            default:
                setFare(0.00, false);
            }
            if (fare != null) fare.type = type;
        }
    }

    public static class Fare {

        public RideType type;
        public double low;
        public double peak;
        public double senior;
        public boolean transferReduction;

        public Fare (Fare other) {
            this.accumulate(other);
        }

        public Fare (double base) {
            low = peak = senior = base;
        }

        public Fare (double low, double peak, double senior) {
            this.low = low;
            this.peak = peak;
            this.senior = senior;
        }

        public void accumulate (Fare other) {
            if (other != null) {
                low    += other.low;
                peak   += other.peak;
                senior += other.senior;
            }
        }

        public void discount(double amount) {
            low    -= amount;
            peak   -= amount;
            senior -= amount;
            transferReduction = true;
        }

    }
    
    enum RideType {
	VTA,
	CALTRAIN,
	SAMTRANS,
	SANTACRUZ,
	SFMTA,
	BART,
	AC_TRANSIT,
	GG_TRANSIT
    }


}
