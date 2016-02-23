package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
		Response resp = null;
		String getCapabilities = "http://www.geoservicos.ibge.gov.br/geoserver/wfs?"
				+ "request=GetCapabilities&service=WFS";

			try {
				
				HttpClient client = HttpClientBuilder.create().build();
				HttpGet request = new HttpGet(getCapabilities);
				HttpResponse httpResponse = client.execute(request);
				HttpEntity httpEntity = httpResponse.getEntity();
                
                String xml = new String(EntityUtils.toString(httpEntity).getBytes(),"UTF-8");
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                StringReader xmlstring=new StringReader(xml);
                is.setCharacterStream(xmlstring);
                is.setEncoding("UTF-8");
                        //Code Stops here !
                Document doc = db.parse(is); 
                NodeList descNodes = doc.getElementsByTagName("FeatureType");
				for(int i=0; i<descNodes.getLength();i++)
				
		        {
		            System.out.println(descNodes.item(i).getFirstChild().getFirstChild().getNodeValue());
		            resp = updateResourceWfs(descNodes.item(i));
		        }
				
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		ResponseBuilder response = Response.ok();
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

			Response response = conexao.executaQuery(query);

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
			Response response) throws IOException {
		String output;
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatus());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(((String) response.getEntity()).getBytes())));

		while ((output = br.readLine()) != null) {
			bufout.append(output);
		}
	}

}
