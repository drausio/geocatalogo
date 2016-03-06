package br.gov.tcu.catalogosemantico;

import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
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

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("update/{primeiro}")
	public String getIt(@PathParam("primeiro") String primeiro) {

		String out = "OK!";
		int inicio = Integer.parseInt(primeiro);
		int maxRegister = STEP + inicio;

		Logger.getLogger("Geocatalogo").info("CSW INDE - Início ");
		for (int i = inicio; (i + STEP) <= maxRegister + 1; i += STEP) {
			long comeco = new Date().getTime();
			int termino = i + STEP;
			Document doc = recuperaDocumentoXml(i, termino, maxRegister,
					"ISO-8859-1");
			NodeList descNodes = doc.getElementsByTagName("csw:Record");
			NodeList resultadoBusca = doc
					.getElementsByTagName("csw:SearchResults");
			String numMax = recuperaValorAtributoDoNo(resultadoBusca.item(0),
					"numberOfRecordsMatched");
			maxRegister = numMax == null ? 0 : Integer.parseInt(numMax);
			Logger.getLogger("Geocatalogo").info(
					"CSW INDE - Documento " + (new Date().getTime() - comeco));

			atualizaRecursoCsw(maxRegister, descNodes, i);

		}

		return out;
	}

	private void atualizaRecursoCsw(int maxRegister, NodeList descNodes,
			int parte) {
		Response resp;
		long naoachou = 0;
		long comeco = new Date().getTime();
		for (int i = 0; i < descNodes.getLength(); i++) {
			resp = updateResourceCsw(descNodes.item(i), "INDE");

			if (resp != null && resp.getEntity() != null) {
				naoachou++;
			}
		}
		Logger.getLogger("Geocatalogo").info(
				"CSW INDE - Updates " + (new Date().getTime() - comeco));
		comeco = new Date().getTime();
		resp = atualizaCamposPeloCsv(parte);
		Logger.getLogger("Geocatalogo").info(
				"CSW INDE - CSV " + (new Date().getTime() - comeco));
		Logger.getLogger("Geocatalogo").info(
				"CSW INDE - parte ".concat(String.valueOf(parte)).concat(" :")
						.concat(String.valueOf(naoachou))
						.concat(" de um total de: ")
						.concat(String.valueOf(maxRegister)));
	}

	private Response atualizaCamposPeloCsv(int parte) {
		String url = Conexao.LOCAL ? Conexao.URI_APP_LOCAL
				: Conexao.URI_APP_REMOTE;
		url = url.concat("servico/csw/csv/").concat(String.valueOf(parte));

		String query = "{\"query\":\" LOAD CSV WITH HEADERS FROM '"
				+ url
				+ "' AS csvLine MATCH (n{idRecurso:csvLine.identifier}) "
				+ " SET n.description=csvLine.description, n.abstract=csvLine.abstract,"
				+ " n.source=csvLine.source, n.subject=csvLine.subject   RETURN n.idRecurso \"}";
		return executaPesquisa(query);
	}

	private Document recuperaDocumentoXml(int inicio, int termino,
			int maxRegister, String encode) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(getCapabilities(inicio, termino,
				maxRegister));
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

	private String getCapabilities(int inicio, int termino, int maxRegister) {
		if (maxRegister < termino) {
			termino = maxRegister;
		}
		String capabilities = "http://www.metadados.inde.gov.br/geonetwork/srv/eng/csw?"
				+ "request=GetRecords&service=CSW&version=2.0.2&typeNames=csw%3ARecord&constraintLanguage="
				+ "CQL_TEXT&namespace=xmlns%28csw%3Dhttp%3A%2F%2Fwww.opengis.net%2Fcat%2Fcsw%2F2.0.2%29%2C"
				+ "xmlns%28gmd%3Dhttp%3A%2F%2Fwww.isotc211.org%2F2005%2Fgmd%29&"
				+ "constraint_language_version=1.1.0&resultType=results&"
				+ "startPosition="
				+ inicio
				+ "&maxRecords="
				+ STEP
				+ "&ElementSetName=full";
		return capabilities;
	}

	private Response updateResourceCsw(Node node, String string) {
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
		String source = "INDE";
		String descricao = recuperaValorNo(node.getChildNodes(),
				"dc:description").replace("\"", "||");
		String resumo = recuperaValorNo(node.getChildNodes(), "dct:abstract")
				.replace("\"", "||");
		String descrsource = recuperaValorNo(node.getChildNodes(), "dc:source")
				.replace("\"", "||");
		String subject = recuperaValorNo(node.getChildNodes(),"dc:subject");
		
		for (String key : links.keySet()) {
			if (key == null) {
				continue;
			}
			if (key.contains("OGC:WMS")) {
				protocol = "WMS";
			} else if (key.contains("OGC:WCS")) {
				protocol = "WCS";
			}
			if (key.contains("OGC:WFS")) {
				protocol = "WFS";
			}
			if (key.contains("-http--link")) {
				link = links.get(key);
			}
			if (key.contains("-http-get-map") && !links.get(key).isEmpty()) {
				download = download.isEmpty() ? links.get(key) : download;
			} else if (key.contains("image/png") && !links.get(key).isEmpty()) {
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
				//+ " ,r.description='".concat(descricao).concat("'")
				//+ " ,r.abstract='".concat(resumo).concat("'")
				//+ " ,r.source='".concat(descrsource).concat("'")
				//+ " ,r.subject='".concat(subject).concat("'")
				+ " ,n.datetime='".concat(
						Calendar.getInstance().getTime().toString())
						.concat("'")
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
		} /*else {

			query = "{\"query\":\"match (m)-[r]->(n:Recurso:Ofertado{idRecurso:'"
					+ nome + "'})-[s]->(o)" + "delete r,s,n,o \"}";
			resp = executaPesquisa(query);
			query = "{\"query\":\"MATCH (mmRecurso:Metamodelo {nome:'Recurso'})"
					+ " MERGE (mmRecurso)-[:INSTANCIA]->(a:Recurso:ModeloDeProjeto:Ofertado"
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
					+ " MERGE (a)-[:CONECTA]->(:RecursoSemantico:ModeloDeProjeto:Ofertado"
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

		}*/
		return resp;
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
	@Path("csv/{primeiro}")
	public String getCsv(@PathParam("primeiro") String primeiro) {
		String out = "\"identifier\",\"description\",\"abstract\",\"source\",\"subject\"\n";
		int inicio = Integer.parseInt(primeiro);
		int maxRegister = 100;
		String valores = null;

		int termino = inicio + STEP;
		Document doc = recuperaDocumentoXml(inicio, termino, maxRegister,
				"ISO-8859-1");
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
