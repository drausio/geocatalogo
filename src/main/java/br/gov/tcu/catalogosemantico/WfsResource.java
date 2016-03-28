package br.gov.tcu.catalogosemantico;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Path("wfs")
public class WfsResource extends ResourceOgc {

	private Map<String, String> mapaRecursosWfs = new HashMap<String, String>();

	public WfsResource() {
		mapaRecursosWfs.put("IBGE",
				"http://www.geoservicos.ibge.gov.br/geoserver/wfs");
		mapaRecursosWfs.put("INDE",
				"http://www.geoservicos.inde.gov.br/geoserver/wfs");
		mapaRecursosWfs
				.put("IBAMA", "http://siscom.ibama.gov.br/geoserver/wfs");
		mapaRecursosWfs.put("DATAGEO-SP",
				"http://datageo.ambiente.sp.gov.br/geoserver/ows");
		mapaRecursosWfs
				.put("CPRM", "http://sace-cai.cprm.gov.br/geoserver/ows");
		mapaRecursosWfs.put("GEOSIURB-BH",
				"http://geosiurbe.pbh.gov.br/geosiurbe/ows");

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("recursos")
	public Response getRecursos() {
		StringBuffer bufout = new StringBuffer();
		bufout.append("{\"recursos\"\u003A[");
		Iterator<String> iter = mapaRecursosWfs.keySet().iterator();
		while (iter.hasNext()) {
			String chave = iter.next();
			bufout.append("{\"id\"\u003A\"").append(chave).append("\"");
			bufout.append(",\"url\"\u003A\"")
					.append(mapaRecursosWfs.get(chave))
					.append("?request=GetCapabilities&service=WFS")
					.append("\"}");
			if (iter.hasNext()) {
				bufout.append(",");
			}

		}
		bufout.append("]}");
		return Response.status(200).entity(bufout.toString()).build();
	}

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

		Logger.getLogger("Tempo WFS - Inicio Carga").info(
				Calendar.getInstance().getTime());
		for (String chave : mapaRecursosWfs.keySet()) {
			String getCapabilities = mapaRecursosWfs.get(chave)
					+ "?request=GetCapabilities&service=WFS";

			atualizaRecursoWfs(chave, getCapabilities);
		}
		Logger.getLogger("Tempo WFS - Termino Carga").info(
				Calendar.getInstance().getTime());
		ResponseBuilder response = Response.ok();
		return response.build();
	}

	private void atualizaRecursoWfs(String chave, String getCapabilities) {
		Response resp;
		try {

			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(getCapabilities);
			HttpResponse httpResponse = client.execute(request);
			HttpEntity httpEntity = httpResponse.getEntity();

			String xml = new String(EntityUtils.toString(httpEntity)
					.getBytes(), "UTF-8");
			DocumentBuilderFactory dbf = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			StringReader xmlstring = new StringReader(xml);
			is.setCharacterStream(xmlstring);
			is.setEncoding("UTF-8");
			// Code Stops here !
			Document doc = db.parse(is);
			NodeList descNodes = doc.getElementsByTagName("FeatureType");
			long achou = 0;
			long naoachou = 0;
			for (int i = 0; i < descNodes.getLength(); i++) {
				resp = updateResourceWfs(descNodes.item(i),
						mapaRecursosWfs.get(chave), chave);
				if (!resp
						.getEntity()
						.toString()
						.contains(
								recuperaValorNo(descNodes.item(i)
										.getChildNodes(), "Name"))) {
					naoachou++;
				} else {
					achou++;
				}
			}
			System.out.println(chave + "  " + naoachou);

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
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("update/{idrec}")
	public Response atualiza(@PathParam("idrec") String idrec) {
		Response resp = null;
		String chave = idrec;
		Logger.getLogger("Tempo WFS - Inicio Carga "+ chave).info(
				Calendar.getInstance().getTime());
		
		String getCapabilities = mapaRecursosWfs.get(chave)
				+ "?request=GetCapabilities&service=WFS";

		atualizaRecursoWfs(chave, getCapabilities);

		Logger.getLogger("Tempo WFS - Termino Carga " + chave).info(
				Calendar.getInstance().getTime());
		ResponseBuilder response = Response.ok();
		return response.build();
	}

	private Response updateResourceWfs(Node node, String urlServer, String fonte) {

		String nome = recuperaValorNo(node.getChildNodes(), "Name");
		String language = "pt-BR";
		String title = recuperaValorNo(node.getChildNodes(), "Title") + "("
				+ nome + ")";
		String resumo = recuperaValorNo(node.getChildNodes(), "Abstract");
		String lowerCorner = recuperaValorNo(node.getChildNodes(),
				"ows:LowerCorner");
		String upperCorner = recuperaValorNo(node.getChildNodes(),
				"ows:UpperCorner");
		String link = "";
		String download = urlServer
				+ "?request=GetFeature&service=wfs&acceptFormats=application%2Fxml&typename="
				+ nome;
		String protocol = "WFS";
		String source = fonte;
		String subject = recuperaValorNo(node.getChildNodes(), "ows:Keyword");

		// System.out.println("1. " + resumo );
		// System.out.println("2. " +lowerCorner + " "+ upperCorner + " "+
		// download );
		// System.out.println("3. " +subject );

		String query = "{\"query\":\"match (n:Recurso:Ofertado{idRecurso:'"
				+ nome + "'})-[]->(r:RecursoSemantico:Ofertado) "
				+ "return n.idRecurso \"}";
		Response resp = executaPesquisa(query);
		if (!resp.getEntity().toString().contains(nome)) {
			query = "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})"
					+ "CREATE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado"
					+ "{idRecurso: '"
					+ nome
					+ "', nome:'"
					+ title
					+ "',link:'"
					+ link
					+ "',download:'"
					+ download
					+ "',protocol:'"
					+ protocol
					+ "',fonte:'"
					+ source
					+ "',datetime:'"
					+ Calendar.getInstance().getTime()
					+ "'})"
					+ "CREATE (a)-[:CONECTA]->(:RecursoSemantico:ModeloDeProjeto:Ofertado"
					+ "{language: '"
					+ language
					+ "',"
					+ "abstract: '"
					+ resumo
					+ "',source:'"
					+ source
					+ "',lowerCorner:'"
					+ lowerCorner
					+ "',upperCorner:'"
					+ upperCorner
					+ "',"
					+ "protocol:'"
					+ protocol
					+ "',subject:'"
					+ subject
					+ "',datetime:'"
					+ Calendar.getInstance().getTime() + "'}); \"}";
			resp = executaPesquisa(query);
		} else {

			query = "{\"query\":\"match (m)-[r]->(n:Recurso:Ofertado{idRecurso:'"
					+ nome + "'})-[s]->(o)" + "delete r,s,n,o \"}";
			resp = executaPesquisa(query);
			query = "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})"
					+ "MERGE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado"
					+ "{idRecurso: '"
					+ nome
					+ "', nome:'"
					+ title
					+ "',link:'"
					+ link
					+ "',download:'"
					+ download
					+ "',protocol:'"
					+ protocol
					+ "',fonte:'"
					+ source
					+ "',datetime:'"
					+ Calendar.getInstance().getTime()
					+ "'})"
					+ "MERGE (a)-[:CONECTA]->(:RecursoSemantico:ModeloDeProjeto:Ofertado"
					+ "{language: '"
					+ language
					+ "',"
					+ "abstract: '"
					+ resumo
					+ "',source:'"
					+ source
					+ "',lowerCorner:'"
					+ lowerCorner
					+ "',upperCorner:'"
					+ upperCorner
					+ "',"
					+ "protocol:'"
					+ protocol
					+ "',subject:'"
					+ subject
					+ "',datetime:'"
					+ Calendar.getInstance().getTime() + "'}); \"}";
			resp = executaPesquisa(query);

		}
		return resp;
	}

}
