package br.gov.tcu.catalogosemantico;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

@Path("csw")
public class CswResource extends ResourceOgc {

	private static int STEP = 100;

	private Map<String, String> mapaRecursosCsw = new HashMap<String, String>();

	public CswResource() {
		mapaRecursosCsw
				.put("IBGE",
						"http://www.metadados.geo.ibge.gov.br/geonetwork_ibge/srv/por/csw");
		mapaRecursosCsw.put("INDE",
				"http://www.metadados.inde.gov.br/geonetwork/srv/eng/csw");

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("recursos")
	public Response getRecursos() {
		StringBuffer bufout = new StringBuffer();
		int codStatus = 200;
		bufout.append("{\"recursos\"\u003A[");
		Iterator<String> iter = mapaRecursosCsw.keySet().iterator();
		while (iter.hasNext()) {
			String chave = iter.next();
			String getCapabilities = mapaRecursosCsw
					.get(chave)
					.concat("?request=GetRecords&service=CSW")
					.concat("&version=2.0.2&constraintLanguage=CQL_TEXT")
					.concat("&namespace=xmlns%28csw%3Dhttp%3A%2F%2Fwww.opengis.net")
					.concat("%2Fcat%2Fcsw%2F2.0.2%29%2Cxmlns%28gmd%3Dhttp%3A%2F%2F")
					.concat("www.isotc211.org%2F2005%2Fgmd%29");
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
					.getElementsByTagName("csw:SearchResults"));
			if (descNodes != null) {
				String strQtd = recuperaValorAtributoDoNo(descNodes.item(0),
						"numberOfRecordsMatched");
				qtd = Integer.valueOf(strQtd);
			}
			bufout.append("{\"id\"\u003A\"").append(chave).append("\"");
			bufout.append(",\"url\"\u003A\"")
					.append(mapaRecursosCsw.get(chave)).append("\",\"qtd\":\"")
					.append(qtd).append("\"}");
			if (iter.hasNext()) {
				bufout.append(",");
			} else {
				bufout.append("]}");
			}
		}
		return Response.status(codStatus).entity(bufout.toString()).build();

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("update/fonte/{idrec}")
	public Response atualiza(@PathParam("idrec") String idrec) {
		StringBuffer bufout = new StringBuffer();

		Response resp = null;
		int codStatus = 200;
		String chave = idrec;
		Logger.getLogger("Tempo CSW - Inicio Carga " + chave).info(
				Calendar.getInstance().getTime());

		String getCapabilities = mapaRecursosCsw.get(chave);

		int qtd = atualizaRecursoCsw(chave, getCapabilities);
		if (qtd == 0) {
			codStatus = 500;
		}

		bufout.append("{\"recursos\"\u003A").append("\"" + qtd + "\"}");
		Logger.getLogger("Tempo CSW - Termino Carga " + chave).info(
				Calendar.getInstance().getTime());
		return Response.status(codStatus).entity(bufout.toString()).build();
	}

	private int atualizaRecursoCsw(String chave, String getCapabilities) {

		int inicio = 1;
		int maxRegister = STEP + inicio;
		int totalizador = 0;

		Logger.getLogger("Geocatalogo").info("CSW " + chave + " - Início ");
		for (int i = inicio; (i + STEP) <= maxRegister + 1; i += STEP) {
			long comeco = new Date().getTime();
			int termino = i + STEP;
			Document doc = recuperaDocumentoXml(i, termino, maxRegister,
					"UTF-8", getCapabilities);
			if (doc != null) {
				NodeList descNodes = doc.getElementsByTagName("csw:Record");
				NodeList resultadoBusca = doc
						.getElementsByTagName("csw:SearchResults");
				String numMax = recuperaValorAtributoDoNo(
						resultadoBusca.item(0), "numberOfRecordsMatched");
				maxRegister = numMax == null ? 0 : Integer.parseInt(numMax);
				Logger.getLogger("Geocatalogo").info(
						"CSW " + chave + " - Tempo em mseg "
								+ (new Date().getTime() - comeco));

				totalizador = totalizador
						+ atualizaRecursoCsw(maxRegister, descNodes, i, chave);
			}

		}

		return totalizador;
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
		int posi = xml.indexOf("??gua");
		if (posi != -1) {
			Logger.getLogger("??" + String.valueOf(posi));
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource();
		StringReader xmlstring = new StringReader(xml);
		is.setCharacterStream(xmlstring);
		is.setEncoding("UTF-8");
		Document doc = db.parse(is);
		return doc;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("update/{primeiro}")
	public int getIt(@PathParam("primeiro") String primeiro) {

		String out = "OK!";
		int inicio = Integer.parseInt(primeiro);
		int maxRegister = STEP + inicio;
		int totalizador = 0;

		Logger.getLogger("Geocatalogo").info("CSW INDE - Início ");
		for (int i = inicio; (i + STEP) <= maxRegister + 1; i += STEP) {
			long comeco = new Date().getTime();
			int termino = i + STEP;
			Document doc = recuperaDocumentoXml(i, termino, maxRegister,
					"ISO-8859-1", mapaRecursosCsw.get("INDE"));
			NodeList descNodes = doc.getElementsByTagName("csw:Record");
			NodeList resultadoBusca = doc
					.getElementsByTagName("csw:SearchResults");
			String numMax = recuperaValorAtributoDoNo(resultadoBusca.item(0),
					"numberOfRecordsMatched");
			maxRegister = numMax == null ? 0 : Integer.parseInt(numMax);
			Logger.getLogger("Geocatalogo").info(
					"CSW INDE - Documento " + (new Date().getTime() - comeco));

			totalizador = totalizador
					+ atualizaRecursoCsw(maxRegister, descNodes, i, "INDE");

		}

		return totalizador;
	}

	private int atualizaRecursoCsw(int maxRegister, NodeList descNodes,
			int parte, String fonte) {
		Response resp;
		int qtdSucesso = 0;
		long comeco = new Date().getTime();
		for (int i = 0; i < descNodes.getLength(); i++) {
			resp = updateResourceCsw(descNodes.item(i), fonte);

			if (resp != null && resp.getStatus() == 200
					&& resp.getEntity() != null) {
				qtdSucesso++;
			}
		}
		Logger.getLogger("Geocatalogo").info(
				"CSW " + fonte + " - Updates "
						+ (new Date().getTime() - comeco));
		comeco = new Date().getTime();
		resp = atualizaCamposPeloCsv(parte, fonte);
		Logger.getLogger("Geocatalogo").info(
				"CSW " + fonte + " - CSV - Tempo mseg."
						+ (new Date().getTime() - comeco));
		Logger.getLogger("Geocatalogo").info(
				"CSW "
						+ fonte
						+ " - parte ".concat(String.valueOf(parte))
								.concat(" :")
								.concat(String.valueOf(qtdSucesso))
								.concat(" de um total de: ")
								.concat(String.valueOf(maxRegister)));
		return qtdSucesso;
	}

	private Response atualizaCamposPeloCsv(int parte, String fonte) {
		String url = Conexao.LOCAL ? Conexao.URI_APP_LOCAL
				: Conexao.URI_APP_REMOTE;
		url = url.concat("servico/csw/csv/").concat(fonte + "/")
				.concat(String.valueOf(parte));

		String query = "{\"query\":\" LOAD CSV WITH HEADERS FROM '"
				+ url
				+ "' AS csvLine MATCH (n{idRecurso:csvLine.identifier})-[]->(m:RecursoSemantico) "
				+ " SET m.description=csvLine.description, m.abstract=csvLine.abstract,"
				+ " m.source=csvLine.source, m.subject=csvLine.subject   RETURN n.idRecurso \"}";
		return executaPesquisa(query);
	}

	private Document recuperaDocumentoXml(int inicio, int termino,
			int maxRegister, String encode, String urlfonte) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(getCapabilities(inicio, termino,
				maxRegister, urlfonte));
		HttpResponse httpResponse;
		Document doc = null;
		try {
			httpResponse = client.execute(request);
			HttpEntity httpEntity = httpResponse.getEntity();
			String xml = new String(
					EntityUtils.toString(httpEntity).getBytes(), encode);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			StringReader xmlstring = new StringReader(xml);
			is.setCharacterStream(xmlstring);
			is.setEncoding(encode);
			doc = db.parse(is);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return doc;
	}

	private String getCapabilities(int inicio, int termino, int maxRegister,
			String urlfonte) {
		if (maxRegister < termino) {
			termino = maxRegister;
		}
		String capabilities = urlfonte
				+ "?request=GetRecords&service=CSW&version=2.0.2&typeNames=csw%3ARecord&constraintLanguage="
				+ "CQL_TEXT&namespace=xmlns%28csw%3Dhttp%3A%2F%2Fwww.opengis.net%2Fcat%2Fcsw%2F2.0.2%29%2C"
				+ "xmlns%28gmd%3Dhttp%3A%2F%2Fwww.isotc211.org%2F2005%2Fgmd%29&"
				+ "constraint_language_version=1.1.0&resultType=results&"
				+ "startPosition=" + inicio + "&maxRecords=" + STEP
				+ "&ElementSetName=full";
		return capabilities;
	}

	private Response updateResourceCsw(Node node, String fonte) {
		String nome = recuperaValorNo(node.getChildNodes(), "dc:identifier");
		String language = "pt-BR";
		String title = recuperaValorNo(node.getChildNodes(), "dc:title");
		String lowerCorner = recuperaValorNo(node.getChildNodes(),
				"ows:LowerCorner");
		String upperCorner = recuperaValorNo(node.getChildNodes(),
				"ows:UpperCorner");
		Map<String, String> links = recuperaMapaProtocoloUri(
				node.getChildNodes(), "dc:URI");
		String protocol = "CSW";
		String link = "";
		String download = "";
		String source = fonte;
		String descricao = recuperaValorNo(node.getChildNodes(),
				"dc:description").replace("\"", "||");
		String resumo = recuperaValorNo(node.getChildNodes(), "dct:abstract")
				.replace("\"", "||");
		String descrsource = recuperaValorNo(node.getChildNodes(), "dc:source")
				.replace("\"", "||");
		String subject = recuperaValorNo(node.getChildNodes(), "dc:subject");

		for (String key : links.keySet()) {
			if (key == null) {
				continue;
			}
			/*
			 * if (key.contains("OGC:WMS")) { protocol = "WMS"; } else if
			 * (key.contains("OGC:WCS")) { protocol = "WCS"; } if
			 * (key.contains("OGC:WFS")) { protocol = "WFS"; }
			 */
			if (key.contains("-http--link")) {
				link = links.get(key);
			} else if (key.contains("image/png")) {
				link = download.isEmpty() ? links.get(key) : download;
			}
			if (key.contains("-http-get-map") && !links.get(key).isEmpty()) {
				download = download.isEmpty() ? links.get(key) : download;
			} else if (key.contains("-ftp--download")
					&& !links.get(key).isEmpty()) {
				download = download.isEmpty() ? links.get(key) : download;
			} else if (key.contains("-http--download")
					&& !links.get(key).isEmpty()) {
				download = download.isEmpty() ? links.get(key) : download;
			}

		}

		// System.out.println("8.subject " +subject );

		String query = "{\"query\":\"match (n:Recurso:Ofertado{idRecurso:'"
				+ nome
				+ "'})-[]->(r:RecursoSemantico:Ofertado) "
				+ " set n.nome='".concat(encodeUnicode(title)).concat("'")
				+ " ,n.link='".concat(link).concat("'")
				+ " ,n.download='".concat(download).concat("'")
				+ " ,n.protocol='".concat(protocol).concat("'")
				+ " ,n.fonte='".concat(source).concat("'")
				// + " ,r.description='".concat(descricao).concat("'")
				// + " ,r.abstract='".concat(resumo).concat("'")
				// + " ,r.source='".concat(descrsource).concat("'")
				// + " ,r.subject='".concat(subject).concat("'")
				+ " ,n.datetime='".concat(
						String.valueOf(Calendar.getInstance().getTime()
								.getTime())).concat("'")
				+ " ,r.lowerCorner='".concat(lowerCorner).concat("'")
				+ " ,r.upperCorner='".concat(upperCorner).concat("'")
				+ " ,r.protocol='".concat(protocol).concat("'")
				+ " ,r.datetime='".concat(
						Calendar.getInstance().getTime().toString())
						.concat("'") + " return n.idRecurso \"}";
		Response resp = executaPesquisa(query);

		if (!resp.getEntity().toString().contains(nome)) {
			query = "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})"
					+ " CREATE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado"
					+ "{idRecurso: '"
					+ nome
					+ "', nome:'"
					+ encodeUnicode(title)
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
					+ " CREATE (a)-[:CONECTA]->(:RecursoSemantico:ModeloDeProjeto:Ofertado"
					+ "{language: '"
					+ language
					+ "',lowerCorner:'"
					+ lowerCorner
					+ "',upperCorner:'"
					+ upperCorner
					+ "',protocol:'"
					+ protocol
					+ "',datetime:'"
					+ Calendar.getInstance().getTime() + "'}); \"}";
			resp = executaPesquisa(query);
		} /*
		 * else {
		 * 
		 * query = "{\"query\":\"match (m)-[r]->(n:Recurso:Ofertado{idRecurso:'"
		 * + nome + "'})-[s]->(o)" + "delete r,s,n,o \"}"; resp =
		 * executaPesquisa(query); query =
		 * "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})" +
		 * " MERGE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado"
		 * + "{idRecurso: '" + nome + "', nome:'" + encodeUnicode(title) +
		 * "',link:'" + link + "',download:'" + download + "',protocol:'" +
		 * protocol + "',fonte:'" + source + "',datetime:'" +
		 * Calendar.getInstance().getTime() + "'})" +
		 * " MERGE (a)-[:CONECTA]->(:RecursoSemantico:ModeloDeProjeto:Ofertado"
		 * + "{language: '" + language + "',lowerCorner:'" + lowerCorner +
		 * "',upperCorner:'" + upperCorner + "',protocol:'" + protocol +
		 * "',datetime:'" + Calendar.getInstance().getTime() + "'}); \"}"; resp
		 * = executaPesquisa(query);
		 * 
		 * }
		 */
		return resp;
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
	@Path("csv/{fonte}/{primeiro}")
	public String getCsv(@PathParam("primeiro") String primeiro,
			@PathParam("fonte") String fonte) {
		String out = "\"identifier\",\"description\",\"abstract\",\"source\",\"subject\"\n";
		int inicio = Integer.parseInt(primeiro);
		int maxRegister = 100;
		String valores = null;

		int termino = inicio + STEP;
		Document doc = recuperaDocumentoXml(inicio, termino, maxRegister,
				"UTF-8", mapaRecursosCsw.get(fonte));
		NodeList descNodes = doc.getElementsByTagName("csw:Record");
		NodeList resultadoBusca = doc.getElementsByTagName("csw:SearchResults");
		String numMax = recuperaValorAtributoDoNo(resultadoBusca.item(0),
				"numberOfRecordsMatched");
		maxRegister = numMax == null ? 0 : Integer.parseInt(numMax);
		valores = recuperaCsv(maxRegister, descNodes);
		out = out.concat(valores);

		return out;

	}

	private String recuperaCsv(int maxRegister, NodeList descNodes) {
		String resp = "";
		for (int i = 0; i < descNodes.getLength(); i++) {
			resp = resp.concat(recuperaLinhaCsv(descNodes.item(i)));
		}
		return resp;
	}

	private String recuperaLinhaCsv(Node node) {
		String identifier = encodeUnicode(recuperaValorNo(node.getChildNodes(),
				"dc:identifier"));
		String descricao = recuperaValorNo(node.getChildNodes(),
				"dc:description").replace("\"", "||");
		String resumo = recuperaValorNo(node.getChildNodes(), "dct:abstract")
				.replace("\"", "||");
		String descrsource = recuperaValorNo(node.getChildNodes(), "dc:source")
				.replace("\"", "||");
		String subject = encodeUnicode(recuperaValorNo(node.getChildNodes(),
				"dc:subject"));

		return "\"".concat(identifier).concat("\",\"").concat(descricao)
				.concat("\",\"").concat(resumo).concat("\",\"")
				.concat(descrsource).concat(subject).concat("\"\n");
	}

}
