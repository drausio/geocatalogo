package br.gov.tcu.catalogosemantico;

import java.io.IOException;
import java.io.Serializable;
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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.ows.ServiceException;
import org.xml.sax.SAXException;


/**
 * Root resource (exposed at "myresource" path)
 */
@Path("wfs")
public class WfsResource {

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
    	String getCapabilities = "http://www.geoservicos.inde.gov.br:80/geoserver/wfs?"
    			+ "request=GetCapabilities&service=WFS";
    	URL url = null;
    	try {
    	  url = new URL(getCapabilities);
    	} catch (MalformedURLException e) {
    	  //will not happen
    	}

    	SimpleFeatureCollection result = null;
        try{
            
            String wfsGetCap = "http://localhost:8080/geoserver/ows?service=WFS&version=1.0.0&request=GetCapabilities";
            
            // use the WFS Datastore
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("URL", wfsGetCap);
            params.put("TIMEOUT", new Integer(60000));
            
            DataStore datastore = DataStoreFinder.getDataStore(params);
            
            SimpleFeatureSource featureSource = datastore.getFeatureSource("mylayer");
            
            
            result = featureSource.getFeatures();
            
        }catch(Exception e){
            new RuntimeException("Unable to get the target collection", e);
        }
        
        System.out.println(result.size());
        return out;
    }
}
