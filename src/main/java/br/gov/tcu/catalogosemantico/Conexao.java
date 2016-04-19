package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;


import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;



@SuppressWarnings("deprecation")
public class Conexao {
	
	public String token;
	protected static boolean LOCAL = false;
	private static String URI_SERVER_LOCAL = "http://localhost:7474/db/data/";
	protected static String URI_APP_LOCAL = "http://localhost:8080/geocatalogo/";
	protected static String URI_APP_REMOTE = "http://ec2-54-233-110-78.sa-east-1.compute.amazonaws.com:8085/geocatalogo/";
	private static String URI_SERVER_REMOTE 
		="http://geocatalogo.sb02.stations.graphenedb.com:24789/db/data/";
	private static String AUTORIZATION_LOCAL = "Basic bmVvNGo6Z2VvY2F0YWxvZ28=";
	private static String AUTORIZATION_REMOTE = "Basic Z2VvY2F0YWxvZ286Wm5aUUpqOHN3WjZNOTY2MDN2N1k=";
	public Conexao() {
	}

	
	ClientResponse<String> executaQuery(String query) throws Exception {
		String uri ="";
		String auth = "";
		if(LOCAL){
			uri = URI_SERVER_LOCAL;
			auth=AUTORIZATION_LOCAL;
		}else{
			uri=URI_SERVER_REMOTE;
			auth=AUTORIZATION_REMOTE;
		}
		ClientRequest request = new ClientRequest(uri + "cypher");
		request.accept("application/json;charset=UTF-8");
		request.header("Authorization",auth);
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