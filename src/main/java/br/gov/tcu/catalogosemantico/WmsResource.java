package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.apache.http.client.ClientProtocolException;
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

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("wms")
public class WmsResource extends ResourceOgc {

	private Map<String, String> mapaRecursosWms = new HashMap<String, String>();

	public WmsResource() {
		mapaRecursosWms.put("IBGE",
				"http://www.geoservicos.ibge.gov.br/geoserver/wms");
		mapaRecursosWms.put("INDE",
				"http://www.geoservicos.inde.gov.br/geoserver/wms");
		mapaRecursosWms
				.put("IBAMA", "http://siscom.ibama.gov.br/geoserver/wms");
		mapaRecursosWms.put("DATAGEO-SP",
				"http://datageo.ambiente.sp.gov.br/geoserver/ows");
		mapaRecursosWms
				.put("CPRM", "http://sace-cai.cprm.gov.br/geoserver/ows");
		mapaRecursosWms.put("GEOSIURB-BH",
				"http://geosiurbe.pbh.gov.br/geosiurbe/ows");
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("recursos")
	public Response getRecursos() {
		StringBuffer bufout = new StringBuffer();
		int codStatus = 200;
		bufout.append("{\"recursos\"\u003A[");
		Iterator<String> iter = mapaRecursosWms.keySet().iterator();
		while (iter.hasNext()) {
			String chave = iter.next();
			String getCapabilities = mapaRecursosWms.get(chave).concat(
					"?request=GetCapabilities&service=WMS");
			Document doc = null;
			try {
				doc = recuperaDocCapacidades(getCapabilities);
			} catch (ClientProtocolException e) {
				codStatus = 500;
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
			int qtd = 0;
			NodeList descNodes = (doc == null ? null : doc
					.getElementsByTagName("Layer"));
			if (descNodes != null) {
				for (int i = 0; i < descNodes.getLength(); i++) {
					if (!"1".equals(recuperaValorAtributoDoNo(
							descNodes.item(i), "queryable"))) {
						continue;
					} else {
						qtd++;
					}
				}
			}
			bufout.append("{\"id\"\u003A\"").append(chave).append("\"");
			bufout.append(",\"url\"\u003A\"").append(getCapabilities)
					.append("\",\"qtd\":\"")
					.append(descNodes == null ? 0 : qtd).append("\"}");
			if (iter.hasNext()) {
				bufout.append(",");
			} else {
				bufout.append("]}");
			}
		}
		return Response.status(codStatus).entity(bufout.toString()).build();

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

		Logger.getLogger("Tempo WFMS - Inicio Carga").info(
				Calendar.getInstance().getTime());
		for (String chave : mapaRecursosWms.keySet()) {
			String getCapabilities = mapaRecursosWms.get(chave)
					+ "?request=GetCapabilities&service=WMS";

			atualizaRecursoWms(chave, getCapabilities);
		}
		Logger.getLogger("Tempo WMS - Termino Carga").info(
				Calendar.getInstance().getTime());
		ResponseBuilder response = Response.ok();
		return response.build();
	}

	private int atualizaRecursoWms(String chave, String getCapabilities) {
		int naoachou = 0;
		int achou = 0;
		Response resp = null;
		try {

			Document doc = recuperaDocCapacidades(getCapabilities);
			NodeList descNodes = doc.getElementsByTagName("Layer");

			for (int i = 0; i < descNodes.getLength(); i++) {
				if (!"1".equals(recuperaValorAtributoDoNo(descNodes.item(i),
						"queryable"))) {
					continue;
				}
				resp = updateResourceWms(descNodes.item(i),
						mapaRecursosWms.get(chave), chave);
				if (resp.getStatus() == 200 && resp.getEntity() != null
						&& !resp.getEntity()
								.toString()
								.contains(
										recuperaValorNo(descNodes.item(i)
												.getChildNodes(), "Name"))) {
					achou++;
				} else {
					naoachou++;
				}
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
		return achou;
	}

	private Document recuperaDocCapacidades(String getCapabilities)
			throws IOException, ClientProtocolException,
			UnsupportedEncodingException, ParserConfigurationException,
			SAXException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(getCapabilities);
		HttpResponse httpResponse = client.execute(request);
		HttpEntity httpEntity = httpResponse.getEntity();

		String xml = new String(EntityUtils.toString(httpEntity).getBytes(),
				"UTF-8");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource();
		StringReader xmlstring = new StringReader(xml);
		is.setCharacterStream(xmlstring);
		is.setEncoding("UTF-8");
		// Code Stops here !
		Document doc = db.parse(is);
		return doc;
	}

	private Response updateResourceWms(Node node, String urlServer, String fonte) {

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
		String bbox_sul = recuperaValorAtributo(node.getChildNodes(),
				"BoundingBox", "maxy");
		String bbox_norte = recuperaValorAtributo(node.getChildNodes(),
				"BoundingBox", "miny");
		String bbox_leste = recuperaValorAtributo(node.getChildNodes(),
				"BoundingBox", "minx");
		String bbox_oeste = recuperaValorAtributo(node.getChildNodes(),
				"BoundingBox", "maxx");
		String bbox = bbox_leste.concat(",") + bbox_norte.concat(",")
				+ bbox_oeste.concat(",") + bbox_sul;
		String download = urlServer
				+ "?service=wms&request=getMap&crs=CRS:84&width=1000&height=1000&format=image/png&bbox="
				+ bbox + "&layers=" + nome;
		String protocol = "WMS";
		String source = fonte;
		String subject = recuperaValorNo(node.getChildNodes(), "ows:Keyword");

		// System.out.println("1. " +nome + " "+title + " "+ resumo );
		// System.out.println("2. " +lowerCorner + " "+ upperCorner + " "+
		// download );
		// System.out.println("3. " +subject );
		// System.out.println("4. " + download);

		String query = "{\"query\":\"match (n:Recurso:Ofertado{idRecurso:'"
				+ nome + "'})-[]->(r:RecursoSemantico:Ofertado) "
				+ "return n.idRecurso \"}";
		Response resp = executaPesquisa(query);
		if (resp.getEntity() != null
				&& !resp.getEntity().toString().contains(nome)) {
			query = "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})"
					.concat("CREATE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado")
					.concat("{idRecurso: '").concat(nome)
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

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("update/{idrec}")
	public Response atualiza(@PathParam("idrec") String idrec) {
		StringBuffer bufout = new StringBuffer();

		Response resp = null;
		int codStatus = 200;
		String chave = idrec;
		Logger.getLogger("Tempo WMS - Inicio Carga " + chave).info(
				Calendar.getInstance().getTime());

		String getCapabilities = mapaRecursosWms.get(chave)
				+ "?request=GetCapabilities&service=WMS";

		int qtd = atualizaRecursoWms(chave, getCapabilities);
		if (qtd == 0) {
			codStatus = 500;
		}

		bufout.append("{\"recursos\"\u003A").append("\"" + qtd + "\"}");
		Logger.getLogger("Tempo WMS - Termino Carga " + chave).info(
				Calendar.getInstance().getTime());
		return Response.status(codStatus).entity(bufout.toString()).build();
	}

	private String recuperaValorAtributo(NodeList childNodes, String nomeTag,
			String nomeAtributo) {
		String valor = "";
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n != null && n.getNodeName() != null
					&& n.getNodeName().equals(nomeTag)) {
				String atrib = recuperaValorAtributoDoNo(n, nomeAtributo);
				if (atrib != null) {
					return atrib;
				}
			}

		}
		return valor;
	}

}
