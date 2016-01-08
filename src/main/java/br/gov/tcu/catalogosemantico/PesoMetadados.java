package br.gov.tcu.catalogosemantico;

import javax.ws.rs.FormParam;

public class PesoMetadados {

	String peso_metadados_nome;
	String peso_metadados_descricao;
	String peso_metadados_assunto;
	String peso_metadados_resumo;
	String peso_metadados_fonte;
	
	public PesoMetadados(String peso_metadados_nome,
			String peso_metadados_descricao, String peso_metadados_assunto,
			String peso_metadados_resumo, String peso_metadados_fonte) {
		this.peso_metadados_nome = peso_metadados_nome;
		this.peso_metadados_descricao = peso_metadados_descricao;
		this.peso_metadados_assunto = peso_metadados_assunto;
		this.peso_metadados_resumo = peso_metadados_resumo;
		this.peso_metadados_fonte = peso_metadados_fonte;
	}
	
	public Long getSomaDosPesos(){
		return Long.valueOf(peso_metadados_nome)+Long.valueOf(peso_metadados_descricao)
				+Long.valueOf(peso_metadados_assunto)+Long.valueOf(peso_metadados_resumo)
				+Long.valueOf(peso_metadados_fonte);
	}

	public String getPeso_metadados_nome() {
		return peso_metadados_nome;
	}

	public void setPeso_metadados_nome(String peso_metadados_nome) {
		this.peso_metadados_nome = peso_metadados_nome;
	}

	public String getPeso_metadados_descricao() {
		return peso_metadados_descricao;
	}

	public void setPeso_metadados_descricao(String peso_metadados_descricao) {
		this.peso_metadados_descricao = peso_metadados_descricao;
	}

	public String getPeso_metadados_assunto() {
		return peso_metadados_assunto;
	}

	public void setPeso_metadados_assunto(String peso_metadados_assunto) {
		this.peso_metadados_assunto = peso_metadados_assunto;
	}

	public String getPeso_metadados_resumo() {
		return peso_metadados_resumo;
	}

	public void setPeso_metadados_resumo(String peso_metadados_resumo) {
		this.peso_metadados_resumo = peso_metadados_resumo;
	}

	public String getPeso_metadados_fonte() {
		return peso_metadados_fonte;
	}

	public void setPeso_metadados_fonte(String peso_metadados_fonte) {
		this.peso_metadados_fonte = peso_metadados_fonte;
	}

}
