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



/**
 * Root resource (exposed at "myresource" path)
 */
@Path("csw")
public class CswResource {

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
    	String getCapabilities = "http://www.metadados.inde.gov.br/geonetwork/srv/eng/csw?"
    			+ "request=GetCapabilities&service=CSW&acceptVersions=2.0.2&acceptFormats=application%2Fxml  ";
    	URL url = null;
    	try {
    	  url = new URL(getCapabilities);
    	} catch (MalformedURLException e) {
    	  //will not happen
    	}

    	
       
        return out;
    }
}
