package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Calendar;

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
		String urlServer = "http://www.geoservicos.ibge.gov.br/geoserver/wfs";
		String fonte = "IBGE";
		String getCapabilities = urlServer+"?request=GetCapabilities&service=WFS";

		try {

			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(getCapabilities);
			HttpResponse httpResponse = client.execute(request);
			HttpEntity httpEntity = httpResponse.getEntity();

			String xml = new String(
					EntityUtils.toString(httpEntity).getBytes(), "UTF-8");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			StringReader xmlstring = new StringReader(xml);
			is.setCharacterStream(xmlstring);
			is.setEncoding("UTF-8");
			// Code Stops here !
			Document doc = db.parse(is);
			NodeList descNodes = doc.getElementsByTagName("FeatureType");
			long achou=0;
			long naoachou = 0;
			for (int i = 0; i < descNodes.getLength(); i++){
				resp = updateResourceWfs(descNodes.item(i),urlServer,fonte);
				if(!resp.getEntity().toString().contains(recuperaValorNo(descNodes.item(i).getChildNodes(),"Name"))){
					naoachou++;
				}else{
					achou++;
				}
			}
			System.out.println(achou + " " + naoachou);

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

	private Response updateResourceWfs(Node node,String urlServer, String fonte) {
		
		String nome = recuperaValorNo(node.getChildNodes(),"Name");
		String language="pt-BR";
		String title=recuperaValorNo(node.getChildNodes(),"Title")+"("+nome+")";
		String resumo=recuperaValorNo(node.getChildNodes(),"Abstract");
		String lowerCorner=recuperaValorNo(node.getChildNodes(),"ows:LowerCorner");
		String upperCorner=recuperaValorNo(node.getChildNodes(),"ows:UpperCorner");
		String link="";
		String download=urlServer+"?request=GetFeature&service=wfs&acceptFormats=application%2Fxml&typename="+nome;
		String protocol="WFS";
		String source=fonte;
		String subject=recuperaValorNo(node.getChildNodes(),"ows:Keyword");
		
		//System.out.println("1. " + resumo );
		//System.out.println("2. " +lowerCorner + " "+ upperCorner + " "+ download );
		//System.out.println("3. " +subject );
		
		
		String query = "{\"query\":\"match (n:Recurso:Ofertado{idRecurso:'"
				+ nome + "'})-[]->(r:RecursoSemantico:Ofertado) "
				+ "return n.idRecurso \"}";
		Response resp = executaPesquisa(query);
		if(!resp.getEntity().toString().contains(nome)){
			query = "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})"+
					"CREATE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado"+
					"{idRecurso: '"+nome+"', nome:'"+title+"',link:'"+link+"',download:'"+download+
					"',protocol:'"+protocol+"',fonte:'"+source+"',datetime:'"+Calendar.getInstance().getTime()+"'})"+
					"CREATE (a)-[:CONECTA]->(:RecursoSemantico:ModeloDeProjeto:Ofertado"+
					"{language: '"+language+"',"+
					"abstract: '"+resumo+"',source:'"+source+
					"',lowerCorner:'"+lowerCorner+"',upperCorner:'"+upperCorner+"',"+
					"protocol:'"+protocol+"',subject:'"+subject+"',datetime:'"+Calendar.getInstance().getTime()+"'}); \"}";
					resp = executaPesquisa(query);
		}else{
			
			query = "{\"query\":\"match (m)-[r]->(n:Recurso:Ofertado{idRecurso:'"
					+ nome + "'})-[s]->(o)"
					+ "delete r,s,n,o \"}";
					resp = executaPesquisa(query);
			query = "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})"+
					"MERGE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado"+
					"{idRecurso: '"+nome+"', nome:'"+title+"',link:'"+link+"',download:'"+download+
					"',protocol:'"+protocol+"',fonte:'"+source+"',datetime:'"+Calendar.getInstance().getTime()+"'})"+
					"MERGE (a)-[:CONECTA]->(:RecursoSemantico:ModeloDeProjeto:Ofertado"+
					"{language: '"+language+"',"+
					"abstract: '"+resumo+"',source:'"+source+
					"',lowerCorner:'"+lowerCorner+"',upperCorner:'"+upperCorner+"',"+
					"protocol:'"+protocol+"',subject:'"+subject+"',datetime:'"+Calendar.getInstance().getTime()+"'}); \"}";
					resp = executaPesquisa(query);
			
		}
		return resp;
	}

	private String recuperaValorNo(NodeList childNodes, String nomeTag) {
		String valor = "";
		for(int i = 0; i < childNodes.getLength(); i++){
			Node n = childNodes.item(i);
			if(n.hasChildNodes()){
				String valorLocalizado = recuperaValorNo(n.getChildNodes(), nomeTag);
				if(valorLocalizado != null && !valorLocalizado.isEmpty()){
					return valorLocalizado;
				}
			}
			if(n != null &&  n.getNodeName() != null && n.getNodeName().equals(nomeTag)){
				valor = valor + (n.getFirstChild()==null?"":","+n.getFirstChild().getNodeValue());
				if(valor.startsWith(",")){
					valor = valor.substring(1);
				}
			}
			
		}
		return valor;
	}

	private Response executaPesquisa(String query) {
		StringBuffer bufout = new StringBuffer();
		try {

			Response response = conexao.executaQuery(encodeUnicode(query));

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

	private void colocaNoBuffer(StringBuffer bufout, Response response)
			throws IOException {
		String output;
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatus());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(
						((String) response.getEntity()).getBytes())));

		while ((output = br.readLine()) != null) {
			bufout.append(output);
		}
	}
	
	private String encodeUnicode(String palavra) {
		palavra = palavra
				.replace("á", "\\u00e1")
				.replace("ç", "\\u00e7")
				.replace("ã", "\\u00e3")
				.replace("á", "\\u00e1")
				.replace("à", "\\u00e0")
				.replace("â", "\\u00e2")
				.replace("ä", "\\u00e4")
				.replace("Á", "\\u00c1")
				.replace("À", "\\u00c0")
				.replace("Â", "\\u00c2")
				.replace("Ã", "\\u00c3")
				.replace("Ä", "\\u00c4")
				.replace("é", "\\u00e9")
				.replace("è", "\\u00e8")
				.replace("ê", "\\u00ea")
				.replace("É", "\\u00c9")
				.replace("È", "\\u00c8")
				.replace("Ê", "\\u00ca")
				.replace("Ë", "\\u00cb")
				.replace("í", "\\u00ed")
				.replace("ì", "\\u00ec")
				.replace("î", "\\u00ee")
				.replace("ï", "\\u00ef")
				.replace("Í", "\\u00cd")
				.replace("Ì", "\\u00cc")
				.replace("Î", "\\u00ce")
				.replace("Ï", "\\u00cf")
				.replace("ó", "\\u00f3")
				.replace("ò", "\\u00f2")
				.replace("ô", "\\u00f4")
				.replace("õ", "\\u00f5")
				.replace("ö", "\\u00f6")
				.replace("Ó", "\\u00d3")
				.replace("Ò", "\\u00d2")
				.replace("Ô", "\\u00d4")
				.replace("Õ", "\\u00d5")
				.replace("Ö", "\\u00d6")
				.replace("ú", "\\u00fa")
				.replace("ù", "\\u00f9")
				.replace("û", "\\u00fb")
				.replace("ü", "\\u00fc")
				.replace("Ú", "\\u00da")
				.replace("Ù", "\\u00d9")
				.replace("Û", "\\u00db")
				.replace("ç", "\\u00e7")
				.replace("Ç", "\\u00c7")
				.replace("ñ", "\\u00f1")
				.replace("Ñ", "\\u00d1")
				.replace("&", "\\u0026")
				.replace("'", "\\u0027");

		return palavra;
	}

}
