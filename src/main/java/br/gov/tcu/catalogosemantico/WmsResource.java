package br.gov.tcu.catalogosemantico;

import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;



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

    	
       
        return out;
    }
}
