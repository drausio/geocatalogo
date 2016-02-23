package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.data.wfs.impl.WFSContentComplexFeatureSource;
import org.geotools.data.wfs.internal.v2_0.FeatureTypeInfoImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.ows.ServiceException;
import org.jboss.resteasy.client.ClientResponse;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


@Path("wfs")
public class WfsResource {

	private Conexao conexao = new Conexao();
	/**
	 * Method handling HTTP GET requests. The returned object will be sent to
	 * the client as "text/plain" media type.
	 *
	 * @return String that will be returned as a text/plain response.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("update")
	public Response getIt() {
		String out = "Got it!";
		String getCapabilities = "http://www.geoservicos.ibge.gov.br/geoserver/wfs?"
				+ "request=GetCapabilities&service=WFS";

		Collection<SimpleFeatureSource> result = new ArrayList<SimpleFeatureSource>();
		
			
			URL url;
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			try {
				
				DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpPost = new HttpGet(getCapabilities);
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                
                String xml = new String(EntityUtils.toString(httpEntity).getBytes(),"UTF-8");
                //System.out.println(xml);
                
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                StringReader xmlstring=new StringReader(xml);
                is.setCharacterStream(xmlstring);
                is.setEncoding("UTF-8");
                        //Code Stops here !
                Document doc = db.parse(is); 
                NodeList nl = doc.getElementsByTagName("FeatureType");
				NodeList descNodes = doc.getElementsByTagName("FeatureType");
				Response resp = null;
		        for(int i=0; i<descNodes.getLength();i++)
		        {
		            System.out.println(descNodes.item(i).getFirstChild().getFirstChild().getNodeValue());
		            resp = updateResourceWfs(descNodes.item(i));
		        }
				/*url = new URL(getCapabilities);	
				URLConnection connection = url.openConnection();
				
				InputSource is = connection.getInputStream();
				Document doc = db.parse(is);
				
				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer xform = factory.newTransformer();

				// that’s the default xform; use a stylesheet to get a real one
				xform.transform(new DOMSource(doc), new StreamResult(System.out));
				NodeList nl = doc.getElementsByTagName("FeatureType");
				NodeList descNodes = doc.getElementsByTagName("Abstract");
		        for(int i=0; i<descNodes.getLength();i++)
		        {
		            //System.out.println(descNodes.item(i).getNodeName());
		        }
				params.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", url);
				//System.out.println("OK " + new WFSDataStoreFactory().canProcess(params));*/
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} /*catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/ catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/*WFSDataStore datastore;
			
			String[] types;
			
				try {
					datastore = (WFSDataStore) DataStoreFinder.getDataStore(params);
					types = datastore.getTypeNames();
					for(String t : types){
						
						SimpleFeatureSource fs = datastore.getFeatureSource(t);
						FeatureTypeInfoImpl fi =(FeatureTypeInfoImpl)fs.getInfo();
						//Response res=updateResourceWfs(fi);
						//System.out.println("Description: " + (String)res.getEntity());
						//System.out.println("Description: " + fi.getDescription());
						System.out.println("Name: " + fi.getName());
						System.out.println("Name: " + fs.getInfo().getName());
						//System.out.println("Title: " +fi.getTitle());
						//System.out.println("Title: " +fi.);
						//if(fs.getInfo().getCRS()!=null){
						//System.out.println("Abstract: " + fi.getAbstract());
						//System.out.println("SRS: " + fi.getDefaultSRS());
						////System.out.println("BBOX MaxX: " + fi.getWGS84BoundingBox().getMaxX());
						//}
						//System.out.println("Keyword: " + fi.getKeywords());
						
					}
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
					*/
		ResponseBuilder response = ClientResponse.ok();
		return response.build();
	}
	private Response updateResourceWfs(Node node) {
		
		String nome = node.getFirstChild().getFirstChild().getNodeValue();
		String query = "{\"query\":\"match (n:Recurso:Ofertado{idRecurso:'"+ nome +"'})-[]->(r:RecursoSemantico:Ofertado) "
				+ "return n.download \"}";
		return executaPesquisa(query);
	}
	
	private Response executaPesquisa(String query) {
		StringBuffer bufout = new StringBuffer();
		try {			

			ClientResponse<String> response = conexao.executaQuery(query);

			colocaNoBuffer(bufout, response);

		} catch (ClientProtocolException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		} catch (Exception e) {

			e.printStackTrace();

		}

		return Response.status(200).entity(bufout.toString()).build();
	}
	
	private void colocaNoBuffer(StringBuffer bufout,
			ClientResponse<String> response) throws IOException {
		String output;
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatus());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(response.getEntity().getBytes())));

		while ((output = br.readLine()) != null) {
			bufout.append(output);
		}
	}

}
