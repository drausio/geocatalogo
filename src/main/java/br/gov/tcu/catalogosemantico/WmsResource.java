package br.gov.tcu.catalogosemantico;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultQuery;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wms.WMSUtils;
import org.geotools.data.wms.WebMapServer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.Filter;
import org.geotools.map.Layer;
import org.geotools.ows.ServiceException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.xml.sax.SAXException;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("wms")
public class WmsResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
    	String out = "Got it!";
    	String getCapabilities = "http://www.geoservicos.inde.gov.br:80/geoserver/wms?"
    			+ "request=GetCapabilities&service=WMS";
    	URL url = null;
    	try {
    	  url = new URL(getCapabilities);
    	} catch (MalformedURLException e) {
    	  //will not happen
    	}

    	WebMapServer wms = null;
    	try {
    	  wms = new WebMapServer(url);
    	  for(org.geotools.data.ows.Layer l:WMSUtils.getNamedLayers(wms.getCapabilities())){
    		  out=out.concat(", ").concat(l.get_abstract());
    		  out=out.concat(", ").concat(l.getName());
    		  out=out.concat(", ").concat(l.getTitle()).concat("| ");
    		  
    	  }
    	} catch (IOException e) {
    	  System.out.print("There was an error communicating with the server");
    	  //For example, the server is down
    	} catch (ServiceException e) {
    		System.out.print("The server returned a ServiceException (unusual in this case)");
    	} catch (SAXException e) {
    		System.out.print("//Unable to parse the response from the server");
    	  //For example, the capabilities it returned was not valid
    	}
       
        return out;
    }
}
