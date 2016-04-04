package br.gov.tcu.catalogosemantico;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.client.ClientProtocolException;
import org.jboss.resteasy.client.ClientResponse;





@SuppressWarnings("deprecation")
@Path("search")
public class Search {

	
	private Conexao conexao = new Conexao();

	private static String stopWords = "a, agora, ainda, alguém, algum, alguma, algumas, alguns, ampla, amplas, amplo, "
			+ "amplos, ante, antes, ao, aos, após, aquela, aquelas, aquele, aqueles, aquilo, as, até, através, cada"
			+ ", coisa, coisas, com, como, contra, contudo, da, daquele, daqueles, das, de, dela, delas, dele, deles"
			+ ", depois, dessa, dessas, desse, desses, desta, destas, deste, deste, destes, deve, devem, devendo, "
			+ "dever, deverá, deverão, deveria, deveriam, devia, deviam, disse, disso, disto, dito, diz, dizem, do, "
			+ "dos, e, é, ela, elas, ele, eles, em, enquanto, entre, era, essa, essas, esse, esses, esta, está, "
			+ "estamos, estão, estas, estava, estavam, estávamos, este, estes, estou, eu, fazendo, fazer, feita, "
			+ "feitas, feito, feitos, foi, for, foram, fosse, fossem, grande, grandes, há, isso, isto, já, la, lá, "
			+ "lhe, lhes, lo, mas, me, mesma, mesmas, mesmo, mesmos, meu, meus, minha, minhas, muita, muitas, muito,"
			+ " muitos, na, não, nas, nem, nenhum, nessa, nessas, nesta, nestas, ninguém, no, nos, nós, nossa, nossas, "
			+ "nosso, nossos, num, numa, nunca, o, os, ou, outra, outras, outro, outros, para, pela, pelas, pelo, "
			+ "pelos, pequena, pequenas, pequeno, pequenos, per, perante, pode, pude, podendo, poder, poderia, poderiam,"
			+ "podia, podiam, pois, por, porém, porque, posso, pouca, poucas, pouco, poucos, primeiro, primeiros, própria, "
			+ "próprias, próprio, próprios, quais, qual, quando, quanto, quantos, que, quem, são, se, seja, sejam, sem, "
			+ "sempre, sendo, será, serão, seu, seus, si, sido, só, sob, sobre, sua, suas, talvez, também, tampouco, te, "
			+ "tem, tendo, tenha, ter, teu, teus, ti, tido, tinha, tinham, toda, todas, todavia, todo, todos, tu, tua, tuas,"
			+ "tudo, última, últimas, último, últimos, um, uma, umas, uns, vendo, ver, vez, vindo, vir, vos, vós ";

	

	/**
	 * Method handling HTTP GET requests. The returned object will be sent to
	 * the client as "text/plain" media type.
	 *
	 * @return String that will be returned as a text/plain response.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("nuvem")
	public Response getNuvem() {
		
		String query = "{\"query\":\"MATCH (r:Recurso:ModeloDeProjeto:Requisitado{codRecurso:'RR_001'}) "
				+ " WITH r  UNWIND r.palavras_chave AS pc  "
				+ " MATCH (n:Recurso:Ofertado)-[*1..2]->(a:RecursoSemantico)"
				+ " WHERE   a.description=~ tostring('(?i).*'+ pc +'.*') "
				+ " OR a.source =~ tostring('(?i).*'+ pc +'.*') "
				+ " OR a.abstract =~ tostring('(?i).*'+ pc +'.*') "
				+ " MERGE r-[z:PUBLISH{qtd:1,termo:pc}]->n  return sum(z.qtd) as total ,pc\"}";

		return executaPesquisa(query);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("ontologia/vcge")
	public Response getVcge() {
		
		String query = "{\"query\":\"match (n:Ontologia:VCGE)-[]->(r:Ontologia:VCGE) "
				+ "return n.id as id ,n.nome as nome,n.descricao as descricao , r.id, r.nome order by n.nome\"}";

		return executaPesquisa(query);
	}


	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("ontologia/vce")
	public Response getVce() {
		
		String query = "{\"query\":\"match (n:Ontologia:VCE)-[]->(r:Ontologia:VCE) "
				+ "return id(n) , n.nome, n.descricao,  id(r), r.nome order by n.nome\"}";

		return executaPesquisa(query);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("ontologia/edgv")
	public Response getEdgv() {		

		String query = "{\"query\":\"match (n:Ontologia:EDGV) where n.nome=~'Categoria .*' "
				+ "return id(n),n.nome order by n.nome\"}";
		
		return executaPesquisa(query);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("ontologia/edgv/filhos")
	public Response getEdgvFilhos() {
	
		String query = "{\"query\":\"match (m:Ontologia:EDGV)-[]->(n:Ontologia:EDGV) "
				+ "return id(m),m.nome,id(n),n.nome order by n.nome,m.nome\"}";		
		
		return executaPesquisa(query);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("ranking/{id}")
	public Response getRecurso(@PathParam("id") String id) {
	
		String query = "{\"query\":\"match (m:Recurso:Ofertado)-[r]->(s:RecursoSemantico) where id(m)="+id+" "
				+ "return id(m),m.nome,s.description,s.subject,s.source,s.abstract,m.link,m.download\"}";		
		
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

	
	@POST
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("ranking")
	public Response getRanking(@FormParam("pc1") String pc1,
			@FormParam("descr") String descr, 
			@FormParam("cbx_vcge_n1") List<String> cbx_vcge,
			@FormParam("cbx_edgv") List<String> cbx_edgv,
			@FormParam("cbx_vce") List<String> cbx_vce,
			@FormParam("qtditenspag") String qtditenspag,
			@FormParam("pagcorrente") String pagcorrente,
			@FormParam("slider1") String peso_descricao,
			@FormParam("slider2") String peso_palavraschave,
			@FormParam("slider3") String peso_vcge_direto,
			@FormParam("slider4") String peso_vce_direto,
			@FormParam("slider5") String peso_edgv_direto,
			@FormParam("slider6") String peso_vcge_indireto,
			@FormParam("slider7") String peso_vce_indireto,
			@FormParam("slider8") String peso_edgv_indireto,
			@FormParam("slider9") String peso_metadados_nome,
			@FormParam("slider10") String peso_metadados_descricao,
			@FormParam("slider11") String peso_metadados_assunto,
			@FormParam("slider12") String peso_metadados_resumo,
			@FormParam("slider13") String peso_metadados_fonte,
			@FormParam("anoInicio") String anoInicio,
			@FormParam("anoTermino") String anoTermino,
			@FormParam("checkboxlink") String checkboxlink,
			@FormParam("checkboxdownload") String checkboxdownload,
			@FormParam("checkboxdetalhe") String checkboxdetalhe,
			@FormParam("lat1") String lat1,
			@FormParam("lat2") String lat2,
			@FormParam("long1") String long1,
			@FormParam("long2") String long2,
			@FormParam("origem") String fonte){
		
		conexao.token = getToken();
		
		PesoMetadados pmeta = new PesoMetadados(peso_metadados_nome,peso_metadados_descricao,peso_metadados_assunto,
				peso_metadados_resumo,peso_metadados_fonte);
		StringBuffer bufout = new StringBuffer();
		StringTokenizer palavrasChaves = new StringTokenizer(pc1, ",");
		StringTokenizer palavrasDescricao = new StringTokenizer(descr==null?"":descr, " ");		
		StringTokenizer stopw = new StringTokenizer(stopWords, ",");
		
		Set<String> conjuntoStopWords = new HashSet<String>();
		while (stopw.hasMoreElements()) {
			conjuntoStopWords.add(stopw.nextToken().trim());
		}

		try {
			
			Long maxPontPalavraChave = palavrasChaves.countTokens()*Long.valueOf(peso_palavraschave)* pmeta.getSomaDosPesos();
			criaPontuacaoPalavrasChaves(palavrasChaves , Long.valueOf(peso_palavraschave) , pmeta);
			
			Long maxPontCampoDescricao = palavrasDescricao.countTokens()*Long.valueOf(peso_descricao) 
					* pmeta.getSomaDosPesos();
			
			criaPontuacaoCampoDescricao(palavrasDescricao, conjuntoStopWords, Long.valueOf(peso_descricao), pmeta);			
			
			Long maxPontVcge = cbx_vcge.size()*Long.valueOf(peso_vcge_direto) 
					* pmeta.getSomaDosPesos();
			criaPontuacaoListaPalavras(cbx_vcge, conjuntoStopWords , Long.valueOf(peso_vcge_direto), pmeta);
			
			List<String> assoc_vcge = recuperaTermosAssociadosVcge(cbx_vcge);			
			Long maxPontVcgeIndireto = assoc_vcge.size()*Long.valueOf(peso_vcge_indireto)* pmeta.getSomaDosPesos();
			criaPontuacaoListaPalavras(assoc_vcge, conjuntoStopWords , Long.valueOf(peso_vcge_indireto), pmeta);
						
			Long maxPontVce = cbx_vce.size()*Long.valueOf(peso_vce_direto)* pmeta.getSomaDosPesos();
			criaPontuacaoListaPalavras(cbx_vce, conjuntoStopWords , Long.valueOf(peso_vce_direto), pmeta);	
			
			List<String> assoc_vce = recuperaTermosAssociadosVce(cbx_vce);
			Long maxPontVceIndireto = assoc_vce.size()*Long.valueOf(peso_vce_indireto)* pmeta.getSomaDosPesos();
			criaPontuacaoListaPalavras(assoc_vce, conjuntoStopWords , Long.valueOf(peso_vce_indireto), pmeta);
			
			Long maxPontEdgv = cbx_edgv.size()*Long.valueOf(peso_edgv_direto)* pmeta.getSomaDosPesos();
			criaPontuacaoListaPalavras(cbx_edgv, conjuntoStopWords , Long.valueOf(peso_edgv_direto), pmeta);
			
			List<String> assoc_edgv = recuperaTermosAssociadosEdgv(cbx_edgv);
			Long maxPontEdgvIndireto = assoc_edgv.size()*Long.valueOf(peso_edgv_indireto)* pmeta.getSomaDosPesos();
			criaPontuacaoListaPalavras(assoc_edgv, conjuntoStopWords , Long.valueOf(peso_edgv_indireto), pmeta);
			
			montaRanking(bufout,qtditenspag,pagcorrente,
					montaListaAnos(anoInicio,anoTermino),
					checkboxlink, checkboxdownload, checkboxdetalhe, 
					lat1,lat2,long1,long2,fonte);
			
			exclueConexoes();
			
			Long maxpont = maxPontPalavraChave + maxPontCampoDescricao + maxPontVcge 
					+ maxPontVcgeIndireto + maxPontVce + maxPontVceIndireto + maxPontEdgv 
					+ maxPontEdgvIndireto;
			
			bufout = bufout.insert(1, " \"maxpont\" : "+maxpont+" ,");
			
			return Response.status(200).entity(bufout.toString()).build();
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(400).build();
		}
		
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("configuracao")
	public Response salvaConfiguracao(
			@FormParam("slider1") String peso_descricao,
			@FormParam("slider2") String peso_palavraschave,
			@FormParam("slider3") String peso_vcge_direto,
			@FormParam("slider4") String peso_vce_direto,
			@FormParam("slider5") String peso_edgv_direto,
			@FormParam("slider6") String peso_vcge_indireto,
			@FormParam("slider7") String peso_vce_indireto,
			@FormParam("slider8") String peso_edgv_indireto,
			@FormParam("slider9") String peso_metadados_nome,
			@FormParam("slider10") String peso_metadados_descricao,
			@FormParam("slider11") String peso_metadados_assunto,
			@FormParam("slider12") String peso_metadados_resumo,
			@FormParam("slider13") String peso_metadados_fonte) {
		
		
		StringBuffer bufout = new StringBuffer();
		try {
			
			String query = "{\"query\":\"MATCH (n:Configuracao{nome:'Config'}) "
					+ " set n.peso_descricao = " + peso_descricao
					+ " set n.peso_palavra_chave = " + peso_palavraschave
					+ " set n.peso_vcge_direto = " + peso_vcge_direto
					+ " set n.peso_vce_direto = " + peso_vce_direto
					+ " set n.peso_edgv_direto = " + peso_edgv_direto
					+ " set n.peso_vcge_indireto = " + peso_vcge_indireto
					+ " set n.peso_vce_indireto = " + peso_vce_indireto
					+ " set n.peso_edgv_indireto = " + peso_edgv_indireto
					+ " set n.peso_campo_nome = " + peso_metadados_nome
					+ " set n.peso_campo_descricao = " + peso_metadados_descricao
					+ " set n.peso_campo_assunto = " + peso_metadados_assunto
					+ " set n.peso_campo_resumo = " + peso_metadados_resumo
					+ " set n.peso_campo_fonte = " + peso_metadados_fonte					
					+ " return n  \"}";
			ClientResponse<String> cr =conexao.executaQuery(query);
			colocaNoBuffer(bufout, cr);
			
		} catch (Exception e) {
			return Response.status(400).build();
		}	
		return Response.status(200).entity(bufout.toString()).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("configuracao")
	public Response getConfiguracao() {

		StringBuffer bufout = new StringBuffer();
		try {

			String query = "{\"query\":\"MATCH (n:Configuracao{nome:'Config'}) "
					+ " return n.peso_descricao, n.peso_palavra_chave, n.peso_vcge_direto , n.peso_vce_direto,"
					+ " n.peso_edgv_direto, n.peso_vcge_indireto, n.peso_vce_indireto, n.peso_edgv_indireto, "
					+ " n.peso_campo_nome, n.peso_campo_descricao, n.peso_campo_assunto, n.peso_campo_resumo,"
					+ " n.peso_campo_fonte  \"}";
			ClientResponse<String> cr = conexao.executaQuery(query);
			colocaNoBuffer(bufout, cr);

		} catch (Exception e) {
			return Response.status(400).build();
		}
		return Response.status(200).entity(bufout.toString()).build();
	}

	
	private List<String> montaListaAnos(String anoInicio, String anoTermino) {
		List<String> lista = new ArrayList<String>();
		if(!anoInicio.isEmpty() && anoTermino.isEmpty()){
			lista.add(anoInicio);
		}else if(!anoInicio.isEmpty() && !anoTermino.isEmpty()){
			Long anoI = Long.valueOf(anoInicio);
			Long anoT = Long.valueOf(anoTermino);
			if(anoT < anoI) return lista;
			for(Long ano = anoI; ano <= anoT ; ano++){
				lista.add(ano.toString());
			}
		}else if(anoInicio.isEmpty() && !anoTermino.isEmpty()){
			
			Long anoT = Long.valueOf(anoTermino);
			Long anoI = Long.valueOf(anoT-10);
			if(anoT < anoI) return lista;
			for(Long ano = anoI; ano <= anoT ; ano++){
				lista.add(ano.toString());
			}
		}
		return lista;
	}

	private List<String> recuperaTermosAssociadosEdgv(List<String> cbx_edgv) {
		List<String> lista = new ArrayList<String>();
		for(String termo:cbx_edgv){
			lista.addAll(recuperaTermosFilhosEdgv(encodeUnicode(termo)));
			lista.add(recuperaTermoPaiEdgv(encodeUnicode(termo)));
		}
		
		return lista;
	}

	private String recuperaTermoPaiEdgv(String termo) {
		List<String> lista = new ArrayList<String>();
		String query = "{\"query\":\"match (n:Ontologia:EDGV{nome:'"+termo+"'})-[]->(r) "
				+ "return r.nome as nome order by r.nome\"}";

		executaQueryPreencheLista(lista, query);
		return lista.isEmpty()?"":lista.get(0);
	}

	private Collection<? extends String> recuperaTermosFilhosEdgv(String termo) {
		List<String> lista = new ArrayList<String>();
		String query = "{\"query\":\"match (r)-[]->(n:Ontologia:EDGV{nome:'"+termo+"'}) "
				+ "return r.nome as nome order by r.nome\"}";

		executaQueryPreencheLista(lista, query);
		return lista;
	}

	private List<String> recuperaTermosAssociadosVce(List<String> cbx_vce) {
		List<String> lista = new ArrayList<String>();
		for(String termo:cbx_vce){
			lista.addAll(recuperaTermosFilhosVce(encodeUnicode(termo)));
			lista.add(recuperaTermoPaiVce(encodeUnicode(termo)));
		}
		
		return lista;
	}

	private String recuperaTermoPaiVce(String termo) {
		List<String> lista = new ArrayList<String>();
		String query = "{\"query\":\"match (n:Ontologia:VCE{nome:'"+termo+"'})-[:TERMO_ESPECIFICO_DE]->(r) "
				+ "return r.nome as nome order by r.nome\"}";

		executaQueryPreencheLista(lista, query);
		return lista.isEmpty()?"":lista.get(0);
	}

	private void executaQueryPreencheLista(List<String> lista, String query) {
		try {
			String resp = conexao.executaQuery(query).getEntity();
			resp = resp.replace("\"data\" :", "").replace("[", "")
					.replace("]", "").replace("\"", "")
					.replace("columns :  nome ,", "").replace("{", "")
					.replace("}", "");
			StringTokenizer tkn = new StringTokenizer(resp,",");
			while(tkn.hasMoreTokens()){
				lista.add(tkn.nextToken().trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Collection<? extends String> recuperaTermosFilhosVce(
			String termo) {
		List<String> lista = new ArrayList<String>();
		String query = "{\"query\":\"match (r)-[]->(n:Ontologia:VCE{nome:'"+termo+"'}) "
				+ "return r.nome as nome order by r.nome\"}";

		executaQueryPreencheLista(lista, query);
		return lista;
	}

	private List<String> recuperaTermosAssociadosVcge(List<String> cbx_vcge) {
		List<String> lista = new ArrayList<String>();
		for(String termo:cbx_vcge){
			lista.addAll(recuperaTermosFilhosVcge(encodeUnicode(termo)));
			lista.add(recuperaTermoPaiVcge(encodeUnicode(termo)));
		}
		
		return lista;
	}

	private String recuperaTermoPaiVcge(String termo) {
		List<String> lista = new ArrayList<String>();
		String query = "{\"query\":\"match (n:Ontologia:VCGE{nome:'"+termo+"'})-[:EH_UM]->(r) "
				+ "return r.nome as nome order by r.nome\"}";

		executaQueryPreencheLista(lista, query);
		return lista.isEmpty()?"":lista.get(0);
	}

	private Collection<? extends String> recuperaTermosFilhosVcge(String termo) {
		
		List<String> lista = new ArrayList<String>();
		String query = "{\"query\":\"match (r)-[]->(n:Ontologia:VCGE{nome:'"+termo+"'}) "
				+ "return r.nome as nome order by r.nome\"}";

		executaQueryPreencheLista(lista, query);
		return lista;
	}

	private void criaPontuacaoCampoDescricao(StringTokenizer palavrasDescricao,
			Set<String> conjuntoStopWords , Long peso, PesoMetadados pmeta) throws Exception {
		while (palavrasDescricao.hasMoreElements()) {
			String palavra = palavrasDescricao.nextToken();
			if (!conjuntoStopWords.contains(palavra.toLowerCase().trim())) {
				criaConexaoProPalavraChave(palavra,peso,pmeta);
			}
		}
	}

	private void criaPontuacaoPalavrasChaves(StringTokenizer palavrasChaves, Long peso, PesoMetadados pmeta)
			throws Exception {
		while (palavrasChaves.hasMoreElements()) {
			criaConexaoProPalavraChave(palavrasChaves.nextToken(),peso,pmeta);
		}
	}

	private void criaPontuacaoListaPalavras(List<String> lista,
			Set<String> conjuntoStopWords, Long peso, PesoMetadados pmeta) throws Exception {
		for(String id:lista){
			if(id == null) continue;
			//StringTokenizer palavrasDescricaoVce =new StringTokenizer(id," ");
			//while (palavrasDescricaoVce.hasMoreElements()) {
				String palavra = id ;//palavrasDescricaoVce.nextToken();
				if (!conjuntoStopWords.contains(palavra.toLowerCase().trim())) {
					palavra = encodeUnicode(palavra);
					criaConexaoProPalavraChave(palavra,peso,pmeta);
				}
			//}
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

	private void exclueConexoes() throws Exception {
		String query0 = "{\"query\":\"MATCH (:Recurso{codRecurso:'RR_001'})-[r:PUBLISH]->() "
				+ " WHERE r.token='"+conexao.token+"' delete (r)\"}";
		conexao.executaQuery(query0);
	}

	private void montaRanking(StringBuffer bufout, String qtditenspag, String pagcorrente , List<String> anos, 
			String checkboxlink, String checkboxdownload, String checkboxdetalhe, 
			String lat1, String lat2, String long1, String long2, String fonte) throws Exception,
			IOException {
		String campos = " return sum(p.qtd) as peso , b.nome, b.link, b.download, b.fonte,c.protocol, id(b) "
				+ " order by sum(p.qtd) desc skip "
				+ String.valueOf(Long.valueOf(qtditenspag)*(Long.valueOf(pagcorrente)-1)).toString() 
				+" limit "+qtditenspag+"\"}";
		String periodos = montaCriteriosPeriodo(anos);
		String filtroslinks = "";
		String filtrosEspaciais="";
		if(lat1 != null && !lat1.isEmpty() && lat2 != null && !lat2.isEmpty() 
				&& long1 != null && !long2.isEmpty()){
			filtrosEspaciais = montaCriteriosEspaciais(lat1, lat2, long1, long2);
		}
		if(checkboxdetalhe == null){
			String str = " and ( (exists(b.link) and size(b.link)>0) "
					+ " or (exists(b.download) and size(b.download)>0) and not b.download =~ '.*access=private.*' )";
				filtroslinks = filtroslinks.concat(str);
		}
		if(checkboxdownload == null){
			String str = " and not(exists(b.download) and size(b.download)>0 and not b.download =~ '.*access=private.*' ) ";
			filtroslinks = filtroslinks.concat(str);
		}
		
		if(checkboxlink == null){
			String str = " and not( exists(b.link) and size(b.link)>0 ) ";
			filtroslinks = filtroslinks.concat(str);
		}
		/*if(tipo != null && !tipo.isEmpty()){
			String str = " and ( b.protocol =~ '.*" +tipo+".*') " ;
			filtroslinks = filtroslinks.concat(str);
		}*/
		if(fonte != null && !fonte.isEmpty()){
			String str = " and ( b.fonte =~ '.*" +fonte+".*') " ;
			filtroslinks = filtroslinks.concat(str);
		}
				String query1 = "{\"query\":\"match (a:Recurso{codRecurso:'RR_001'})-"
				+ "[p:PUBLISH]->(b:Recurso:Ofertado)-[r]->(c:RecursoSemantico) where p.qtd > 0 "
				+ " and p.token='"+conexao.token+"' "
				.concat(periodos).concat(filtroslinks).concat(filtrosEspaciais).concat(campos);				

		ClientResponse<String> response1 = conexao.executaQuery(query1);

		colocaNoBuffer(bufout, response1);
	}
	
	
	private String montaCriteriosPeriodo(List<String> anos) {
		String termino	= " ) " ;
		String query = "";
		if(anos != null && !anos.isEmpty()){
			query = "".concat(" and (b.nome is null ");
			for(String ano:anos){
			query = query.concat("or b.nome =~ '.*"+ano+".*'  or c.abstract =~ '.*"+ano+".*' or c.subject =~ '.*"+ano+".*' "
					+ " or c.source =~ '.*"+ano+".*' or c.description = '.*"+ano+".*' ");
			}
			query = query.concat(termino);
		}
		return query;
	}
	
	private String montaCriteriosEspaciais(String lat1, String lat2, String long1, String long2){
		String query = "";
		String latnortemaiorouiguallat1 = 
				"toFloat(split(replace(c.upperCorner,',','.'), ' ')[1]) >= " + lat1;
		String latsulmenorouiguallat1 = 
				"toFloat(split(replace(c.lowerCorner,',','.'), ' ')[1]) <= " + lat1;
		String latnortemaiorouiguallat2 = 
				"toFloat(split(replace(c.upperCorner,',','.'), ' ')[1]) >= " + lat2;
		String latsulmenorouiguallat2 = 
				"toFloat(split(replace(c.lowerCorner,',','.'), ' ')[1]) <= " + lat2;
		String latnortemenorouiguallat1 = 
				"toFloat(split(replace(c.upperCorner,',','.'), ' ')[1]) <= " + lat1;
		String latsulmaiorouiguallat2 = 
				"toFloat(split(replace(c.lowerCorner,',','.'), ' ')[1]) >= " + lat2;
		String lnglestemaiorouiguallng1 = 
				"toFloat(split(replace(c.lowerCorner,',','.'), ' ')[0]) >= " + long1;
		String lngoestemenorouiguallng1 = 
				"toFloat(split(replace(c.upperCorner,',','.'), ' ')[0]) <= " + long1;
		String lnglestemaiorouiguallng2 = 
				"toFloat(split(replace(c.lowerCorner,',','.'), ' ')[0]) >= " + long2;
		String lngoestemenorouiguallng2 = 
				"toFloat(split(replace(c.upperCorner,',','.'), ' ')[0]) <= " + long2;
		String lnglestemenorouiguallng1 = 
				"toFloat(split(replace(c.lowerCorner,',','.'), ' ')[0]) <= " + long1;
		String lngoestemaiorouiguallng2 = 
				"toFloat(split(replace(c.upperCorner,',','.'), ' ')[0]) >= " + long2;
		String lngoestemmenorouiguallng1 = 
				"toFloat(split(replace(c.upperCorner,',','.'), ' ')[0]) <= " + long1;
		String lnglestemmaiorouiguallng1 = 
				"toFloat(split(replace(c.lowerCorner,',','.'), ' ')[0]) >= " + long1;
		
		query = query.concat(" AND (" +
				"("+ latnortemaiorouiguallat1 + " AND "+ latsulmenorouiguallat1 + ") "+
				" OR "+
				"("+ latnortemaiorouiguallat2 + " AND "+ latsulmenorouiguallat2 + ") "+
				" OR "+
				"("+ latnortemenorouiguallat1 + " AND "+ latsulmaiorouiguallat2 + ") "+
				")"+
				" AND NOT ( "+ latnortemaiorouiguallat1 + " AND " + latsulmenorouiguallat1 + 
				 " AND " + latnortemaiorouiguallat2 + " AND " + latsulmenorouiguallat2 +
				") "+
			    " AND ( "+
				"("+ lnglestemaiorouiguallng1 + " AND " + lngoestemenorouiguallng1 + ")"+ 
			  	" OR "+
				"(" + lnglestemaiorouiguallng2 + " AND "+ lngoestemenorouiguallng2 + ")"+
				" OR "+
				"("+ lnglestemenorouiguallng1  + " AND "+ lngoestemaiorouiguallng2 + ")"+
				") "+
				" AND NOT ( "+ lngoestemmenorouiguallng1 + " AND " + lnglestemmaiorouiguallng1 + 
				" AND " + lngoestemenorouiguallng2 + " AND " + lnglestemaiorouiguallng2 +
				" )");
		return query;
	}

	private void criaConexaoProPalavraChave(String pc1 , Long peso, PesoMetadados pmeta) throws Exception {
		if (pc1 == null || pc1.isEmpty())
			return;
		pc1=encodeUnicode(pc1);
		Long pesoTotal = 0L;
		Long pesoDescricao = Long.valueOf(pmeta.getPeso_metadados_descricao()) * peso ;
		criaConexaoCampoRecursoSemantico(pc1, "description", pesoDescricao.toString());
		//pesoTotal  = pesoTotal + localizaRecursoSemantico(pc1, "description" , pesoDescricao.toString());
		Long pesoFonte = Long.valueOf(pmeta.getPeso_metadados_fonte()) * peso ;
		criaConexaoCampoRecursoSemantico(pc1, "source",pesoFonte.toString());
		//pesoTotal  = pesoTotal + localizaRecursoSemantico(pc1, "source" , pesoDescricao.toString());
		Long pesoResumo = Long.valueOf(pmeta.getPeso_metadados_resumo()) * peso ;
		criaConexaoCampoRecursoSemantico(pc1, "abstract",pesoResumo.toString());
		//pesoTotal  = pesoTotal + localizaRecursoSemantico(pc1, "abstract" , pesoDescricao.toString());
		Long pesoAssunto = Long.valueOf(pmeta.getPeso_metadados_assunto()) * peso ;
		criaConexaoCampoRecursoSemantico(pc1, "subject",pesoAssunto.toString());
		//pesoTotal  = pesoTotal + localizaRecursoSemantico(pc1, "subject" , pesoDescricao.toString());
		Long pesoNome = Long.valueOf(pmeta.getPeso_metadados_nome()) * peso ;
		pesoTotal = pesoNome + pesoTotal;
		criaConexaoCampoRecursoOfertado(pc1, "nome", pesoTotal.toString());
		
		
		
	}

	private void criaConexaoCampoRecursoSemantico(String pc1, String campo , String peso) throws Exception {
		String query = "{\"query\":\"MATCH (n:Recurso:Ofertado)-[]->(a:RecursoSemantico) "
				+ " WHERE a."+campo+"=~ tostring('(?i).*"+ " ".concat(pc1)+ ".*') WITH n "
				+ " MATCH (r:Recurso{codRecurso:'RR_001'}) "
				+ " MERGE (r)-[z:PUBLISH{qtd:"+peso+",termo:'"
				+ pc1 + "',campo:'"+campo+"' ,token:'"+conexao.token+"'}]->n"
				+" \"}";
		conexao.executaQuery(query);
	}
	
	private long localizaRecursoSemantico(String pc1, String campo , String peso) throws Exception {
		String query = "{\"query\":\"MATCH (n:Recurso:Ofertado)-[]->(a:RecursoSemantico) "
				+ " WHERE a."+campo+"=~ tostring('(?i).*"+ " ".concat(pc1)+ ".*') RETURN id(n) "
				+" \"}";
		ClientResponse<String> resp = conexao.executaQuery(query);
		//System.out.println(resp.getEntity().contains("\"data\" : [ ]"));
		String dados = resp.getEntity().substring(resp.getEntity().indexOf("\"data\" :"));
		return (dados.contains("\"data\" : [ ]"))?0:Long.parseLong(peso);
	}
	
	
	private void criaConexaoCampoRecursoOfertado(String pc1, String campo , String peso) throws Exception {
		String query = "{\"query\":\"MATCH (n:Recurso:Ofertado)"
				+ " WHERE n."+campo+" =~ tostring('(?i).*"+ " ".concat(pc1)+ ".*') WITH n"
				+ " MATCH (r:Recurso{codRecurso:'RR_001'}) "
				+ " MERGE (r)-[z:PUBLISH{qtd:"+peso+",termo:'"
				+ pc1+ "',campo:'"+campo+"' ,token:'"+conexao.token+"'}]->n \"}";
		conexao.executaQuery(query);
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

	private String getToken(){
		StringTokenizer stk = new StringTokenizer(this.toString(),"@");
		String tkn = "";
		while(stk.hasMoreTokens()){
			tkn=stk.nextToken();
		}
		return tkn+Calendar.getInstance().getTimeInMillis();		
		
	}

}
