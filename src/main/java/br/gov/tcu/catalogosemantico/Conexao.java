package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.ws.rs.core.Context;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.HttpRequest;


@SuppressWarnings("deprecation")
public class Conexao {
	@Context
	public HttpRequest httpRequest;
	public String token;

	private static String SERVER_ROOT_URI = "http://localhost:7474/db/data/";
	private static String SERVER_REMOTE_ROOT_URI 
		="http://geocatalogo.sb02.stations.graphenedb.com:24789/db/data/";
	public Conexao() {
	}

	
	ClientResponse<String> executaQuery(String query) throws Exception {
		//ClientRequest request = new ClientRequest(SERVER_ROOT_URI + "cypher");
		ClientRequest request = new ClientRequest(SERVER_REMOTE_ROOT_URI + "cypher");
		request.accept("application/json;charset=windows-1252");
		request.header("Authorization","Basic Z2VvY2F0YWxvZ286Wm5aUUpqOHN3WjZNOTY2MDN2N1k="); // REMOTE
		//request.header("Authorization","Basic bmVvNGo6Z2VvY2F0YWxvZ28=");
		request.body("application/json", query);
		ClientResponse<String> response = request.post(String.class);
		return response;
	}

	
	ClientResponse<String> executaDelete(String query) throws Exception {
		//ClientRequest request = new ClientRequest(SERVER_ROOT_URI + "cypher");
		ClientRequest request = new ClientRequest(SERVER_REMOTE_ROOT_URI + "cypher");
		request.accept("application/json;charset=UTF-8");
		request.header("Authorization","Basic Z2VvY2F0YWxvZ286Wm5aUUpqOHN3WjZNOTY2MDN2N1k="); // REMOTE
		//request.header("Authorization","Basic bmVvNGo6Z2VvY2F0YWxvZ28=");
		request.body("application/json", query);
		ClientResponse<String> response = request.post(String.class);
		return response;
	}


	void colocaNoBuffer(StringBuffer bufout, ClientResponse<String> response) throws IOException {
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