package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.http.client.ClientProtocolException;
import org.jboss.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ResourceOgc {

	private Conexao conexao = new Conexao();

	protected String recuperaValorAtributoDoNo(Node node, String nomeAtributo) {
		String valor = null;
		if (node == null) {
			return valor;
		}
		Node n = node;
		Node atrib = n.getAttributes().getNamedItem(nomeAtributo);
		if (atrib != null) {
			return atrib.getNodeValue();
		}
		return valor;
	}

	protected String recuperaValorNo(NodeList childNodes, String nomeTag) {
		String valor = "";
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n.hasChildNodes()) {
				String valorLocalizado = recuperaValorNo(n.getChildNodes(),
						nomeTag);
				if (valorLocalizado != null && !valorLocalizado.isEmpty()) {
					return valorLocalizado;
				}
			}
			if (n != null && n.getNodeName() != null
					&& n.getNodeName().equals(nomeTag)) {
				valor = valor
						+ (n.getFirstChild() == null ? "" : ","
								+ n.getFirstChild().getNodeValue());
				if (valor.startsWith(",")) {
					valor = valor.substring(1);
				}
			}

		}
		return valor;
	}

	protected String recuperaValorTag(NodeList childNodes, String nomeTag) {
		String valor = "";
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n != null && n.getNodeName() != null
					&& n.getNodeName().equals(nomeTag)) {
				valor = valor.concat((n.getFirstChild() == null ? "" : n
						.getFirstChild().getNodeValue()));
				break;
			}

		}
		return valor;
	}

	protected Map<String, String> recuperaMapaProtocoloUri(NodeList childNodes,
			String nomeTag) {
		Map<String, String> mapa = new HashMap<String, String>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n.getNodeName().equalsIgnoreCase(nomeTag)) {
				mapa.put(this.recuperaValorAtributoDoNo(n, "protocol"), n
						.getFirstChild() == null ? "" : n.getFirstChild()
						.getNodeValue());
			}
		}
		return mapa;
	}

	protected List<Node> recuperaNo(NodeList childNodes, String nomeTag) {
		List<Node> valor = new ArrayList<Node>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n.getNodeName().equals(nomeTag)) {
				valor.add(n);
			}

		}
		return valor;
	}

	protected Response executaPesquisa(String query) {
		int status = 200;
		StringBuffer bufout = new StringBuffer();
		try {

			Response response = conexao.executaQuery(encodeUnicode(query));

			if (response.getStatus() != 200) {
				Logger.getLogger(this.getClass()).error(
						"Failed : HTTP error code : " + response.getStatus()
								+ " query :" + encodeUnicode(query));
				status = response.getStatus();
			} else {
				colocaNoBuffer(bufout, response);
			}

		} catch (ClientProtocolException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		} catch (Exception e) {

			e.printStackTrace();

		}

		return Response.status(status).entity(bufout.toString()).build();

	}

	protected void colocaNoBuffer(StringBuffer bufout, Response response)
			throws IOException {
		String output;

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(
						((String) response.getEntity()).getBytes())));

		while ((output = br.readLine()) != null) {
			bufout.append(output);
		}
	}

	protected String encodeUnicode(String palavra) {
		palavra = palavra.replace("á", "\\u00e1").replace("ç", "\\u00e7")
				.replace("ã", "\\u00e3").replace("á", "\\u00e1")
				.replace("à", "\\u00e0").replace("â", "\\u00e2")
				.replace("ä", "\\u00e4").replace("Á", "\\u00c1")
				.replace("À", "\\u00c0").replace("Â", "\\u00c2")
				.replace("Ã", "\\u00c3").replace("Ä", "\\u00c4")
				.replace("é", "\\u00e9").replace("è", "\\u00e8")
				.replace("ê", "\\u00ea").replace("É", "\\u00c9")
				.replace("È", "\\u00c8").replace("Ê", "\\u00ca")
				.replace("Ë", "\\u00cb").replace("í", "\\u00ed")
				.replace("ì", "\\u00ec").replace("î", "\\u00ee")
				.replace("ï", "\\u00ef").replace("Í", "\\u00cd")
				.replace("Ì", "\\u00cc").replace("Î", "\\u00ce")
				.replace("Ï", "\\u00cf").replace("ó", "\\u00f3")
				.replace("ò", "\\u00f2").replace("ô", "\\u00f4")
				.replace("õ", "\\u00f5").replace("ö", "\\u00f6")
				.replace("Ó", "\\u00d3").replace("Ò", "\\u00d2")
				.replace("Ô", "\\u00d4").replace("Õ", "\\u00d5")
				.replace("Ö", "\\u00d6").replace("ú", "\\u00fa")
				.replace("ù", "\\u00f9").replace("û", "\\u00fb")
				.replace("ü", "\\u00fc").replace("Ú", "\\u00da")
				.replace("Ù", "\\u00d9").replace("Û", "\\u00db")
				.replace("ç", "\\u00e7").replace("Ç", "\\u00c7")
				.replace("ñ", "\\u00f1").replace("Ñ", "\\u00d1")
				.replace("&", "\\u0026").replace("'", "\\u0027")
				.replace("\n", " ").replace("\t", " ").replace("\r", " ");

		return palavra;
	}

}
