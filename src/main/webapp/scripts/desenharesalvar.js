var TAMANHO_LIMITE_SHAPES = 1500;

var loaded = true;
var botoesHabilitados = true;

function defineUrlsComunicacao(urlCarregar, urlPersistir, functionPostBackCarregar, functionPostBackSalvar) {
	urlWsCarregar  = urlCarregar;
	urlWsPersistir = urlPersistir;
	postBackSalvar = functionPostBackSalvar;
	postBackCarregar = functionPostBackCarregar;
}

/**
 * carrega os shapes descartando todos os outros que j� existiam
 * @param urlWsCarregar
 * @param chaveMestre
 */
function carregaShapes(urlWsCarregar, chaveMestre, msgSucesso) {
	chavePai = chaveMestre;
	mensagemSucesso = msgSucesso;
	$.getScript('carregarobjetos/' + chavePai + '/' + urlWsCarregar)
		.success(function( script, textStatus ) {
			reloadShapes = true;
		})
		.fail(function( script, textStatus ) {
			alert('Erro ao carregar objetos geogr�ficos (JS): ' + textStatus);
		});
}

/**
 * mescla shapes aos que já estão na tela
 * @param urlWsCarregar
 * @param chaveMestre
 */
function mesclaShapes(urlWsCarregar, chaveMestre, msgSucesso) {
	chavePai = chaveMestre;
	mensagemSucesso = msgSucesso;
	$.getScript('mesclarobjetos/' + chavePai + '/' + fullEncode(msgSucesso) + '/' + urlWsCarregar)
		.success(function( script, textStatus ) {
			reloadShapes = true;
		})		
		.fail(function( script, textStatus ) {
			alert('Erro ao mesclar objetos geogr�ficos (JS): ' + textStatus);
		});
}

var EXCECAO_NAO_PREVISTA = 'br.gov.tcu.arquitetura.util.excecao.ExcecaoNaoPrevista';

function trataErroRetorno(erro) {
	console.log(erro);
	var codErro = null;
	if (erro.indexOf(EXCECAO_NAO_PREVISTA) != -1) {
		codErro = erro.substring(erro.indexOf(EXCECAO_NAO_PREVISTA) + EXCECAO_NAO_PREVISTA.length + 1);
		codErro = codErro.substring(0, codErro.indexOf('<'));
		codErro = codErro.trim();
	}
	if (codErro == null) {
		alert("Houve erro ao recuperar objetos.");
	} else {
		alert("Houve erro ao recuperar objetos. Entre em contato com o suporte e informe o c�digo: " + codErro);
	}
	postBackCarregar(false, true);
}

var startShapes = '{"shapes":['; 
var endShapes = ']}';
var shapesMesclados = [];

function mesclarArrays(shapeArr, chaveMestre, msgSucesso) {
	var shapesNull = shapes == undefined || shapes == null || (shapes == (startShapes + endShapes)) || (shapes == (startShapes + '{}' + endShapes));
	if (shapesNull) {
		shapes = startShapes + endShapes;
	}
	var cntShapes = contaShapes(shapes);
	var cntShapeArr = contaShapes(shapeArr);
	if (cntShapes + cntShapeArr > TAMANHO_LIMITE_SHAPES) {
		postBackCarregar(false, false);
	} else {
		shapeArr = shapeArr.substring(shapeArr.indexOf(startShapes) + startShapes.length, shapeArr.lastIndexOf(endShapes));
		shapes = shapes.substring(0, shapes.lastIndexOf(endShapes)) + (shapesNull?'':',') + shapeArr + endShapes;
		updateShapesMesclados(chaveMestre, msgSucesso, shapeArr);
		verificaBotoesHabilitados();
		defineChavePai();
		postBackCarregar(true, false);
	}
}

function contaShapes(shp) {
	var obj = eval("(" + shp + ")");
	var qtd = 0;
	if (obj != null && obj.shapes != null && obj.shapes.length != null)
		qtd = obj.shapes.length;
	obj = null;
	return qtd;
}

function defineChavePai() {
	// verificando a chavepai
	if (getQuantidadePais() == 1) {
		setDadosPai(getPrimeiroPaiListaMesclados());
	} else {
		setDadosPai(null);
	}

}

function verificaBotoesHabilitados() {	
	var cnt = 0;
	for (var i = 0; i < shapesMesclados.length; i++) {
		if (shapesMesclados[i] != undefined && shapesMesclados[i] != null) {
			++cnt;
			if (cnt > 1) {
				break;
			}
		}
	}
	if (cnt > 1) {
		return false;
	} else {
		return true;
	}
}

function replaceAll(s, oque, poroque) {
	return s.split(oque).join(poroque);
}

function shapesDeleteAll(chaveMestre) {
	if (shapes != undefined && shapes != null) {
		if (chaveMestre == null) {
			shapes = null;
			shapesMesclados = [];
		} else {
			if (shapes.indexOf(',' + getShapesMescladosDaChave(chaveMestre)) != -1)
				shapes = shapes.replace(',' + getShapesMescladosDaChave(chaveMestre), '');
			else if (shapes.indexOf(getShapesMescladosDaChave(chaveMestre) + ',') != -1)
				shapes = shapes.replace(getShapesMescladosDaChave(chaveMestre) + ',', '');
			else
				shapes = shapes.replace(getShapesMescladosDaChave(chaveMestre), '');
			removeShapesMesclados(chaveMestre);
		}
	}
	reloadShapes = true; // ver shapesmap.js/loadShapes
	defineChavePai();

}
//teste
var textoOperacaoIndisponivel = 'Opera��o indispon�vel para m�ltiplos objetos.';
var textoObjetoNaoSelecionado = 'N�o h� objeto selecionado para associar os objetos geogr�ficos.';
var textoNenhumShapeCriado = 'N�o h� objetos para gravar.';

function salvarShapes() {
	if (!verificaBotoesHabilitados()) {
		alert(textoOperacaoIndisponivel);
		return;
	}
	if (chavePai == undefined || chavePai == null || chavePai.length == 0) {
		alert(textoObjetoNaoSelecionado);
		return;
	}
	var shp = shapesMap.jsonMake();
	if (shp == undefined || shp == null || shp.length == 0) {
		alert(textoNenhumShapeCriado);
		return;
	}
	parent.exibeTelaEsperaSemAjax();
	var formData = "shapeData=" + shp;
	$.post('persistirobjetos/' + chavePai + '/' + urlWsPersistir, formData, function( script, textStatus ) {
		parent.escondeTelaEsperaSemAjax();
		if (script.trim() == "resultadoPersistencia = \'OK\';") {
			alert(fullDecode(mensagemSucesso));
			if (postBackSalvar != null)
				postBackSalvar();
		} else
			trataErroRetorno(script);
	});
}

function encodeShapes() {
	var shp = shapesMap.jsonMake();
	shp = fullEncode(shp);
	return shp;
}

function importarShapes() {
	if (!verificaBotoesHabilitados()) {
		alert(textoOperacaoIndisponivel);
		return;
	}
	
	if (chavePai == undefined || chavePai == null || chavePai.length == 0) {
		alert(textoObjetoNaoSelecionado);
		return;
	}
	
	PF('widgetModalUpload').show();
}

function loadShapesInput(idInput) {
	var sh = encodeShapes();
	document.getElementById(idInput).value = sh;
}

function closeAndReloadMap() {
	var shapeArr = document.getElementById('frmUp:shapes').value;
	mesclarArrays(shapeArr, chavePai, mensagemSucesso);
	reloadShapes = true;
	PF('widgetModalUpload').hide();
}

var arquivoDownloadGerado = false;

function gerarArquivoDownload() {
	parent.exibeTelaEsperaSemAjax();
	var shp = shapesMap.jsonMake();
	var formData = {shapeData: shp, formatoArquivoData: document.getElementById('frmDown:somFormato_input').value};
	arquivoDownloadGerado = false;
	$.post('gerararquivodownload', formData, 
		function( script, textStatus ) {
			if (script.trim() == 'OK')
				arquivoDownloadGerado = true;
			else
				alert(script);
			parent.escondeTelaEsperaSemAjax();
		}
	);
	createCloseAndDownload();
}

var idInterval;

function createCloseAndDownload() {
	idInterval = setInterval(closeAndDownload, 500);
}

function closeAndDownload() {
	if (arquivoDownloadGerado) {
		clearInterval(idInterval);
		window.open('./SvlDownload', '_blank');
		PF('widgetModalDownload').hide();
	}
}

function exportarShapes() {
	PF('widgetModalDownload').show();
}

function apagarTodosShapes() {
	
	limpaBusca();
	//shapesMap.shapesDelete(shape);
	shapesMap._shape= Array();
	//shapesMap.shape.setMap(null);
	//shapes = null;
	//reloadShapes = true;	
	
	shapes = null;
	reloadShapes = true;
}

function getQuantidadePais() {
	var cnt = 0;
	for (var i= 0; i < shapesMesclados.length; i++) {
		if (shapesMesclados[i] != undefined && shapesMesclados[i] != null)
			++cnt;
	}
	return cnt;
}

function getPrimeiroPaiListaMesclados() {
	var cnt = 0;
	for (var i= 0; i < shapesMesclados.length; i++) {
		if (shapesMesclados[i] != undefined && shapesMesclados[i] != null) {
			return shapesMesclados[i][0];
		}
	}
	return null;
}

function setDadosPai(chave) {
	if (chave == undefined || chave == null) {
		chavePai = chave;
		mensagemSucesso = null;
	} else {
		var idx = findIndexShapesMescladosDaChave(chave);
		chavePai = shapesMesclados[idx][0];
		mensagemSucesso = shapesMesclados[idx][2];
	}
}

function getShapesMescladosDaChave(chave) {
	var idx = findIndexShapesMescladosDaChave(chave);
	return shapesMesclados[idx][1];
}

function findIndexShapesMescladosDaChave(chave) {
	var idxVazio = null;
	if (shapesMesclados == undefined || shapesMesclados == null || shapesMesclados.length == 0) 
		return 0;
	for (var i= 0; i < shapesMesclados.length; i++) {
		if (idxVazio == null && (shapesMesclados[i] == undefined || shapesMesclados[i] == null))
			idxVazio = i;
		if (shapesMesclados[i] != undefined && shapesMesclados[i] != null) {
			if (shapesMesclados[i][0] == chave) {
				return i;
			}
		}
	}
	if (idxVazio == null)
		return shapesMesclados.length;
	return idxVazio;
}

function updateShapesMesclados(chave, msgSucesso, shpArr) {
	var idx = findIndexShapesMescladosDaChave(chave);
	shapesMesclados[idx] = [];
	shapesMesclados[idx][0] = chave;
	shapesMesclados[idx][1] = shpArr;
	shapesMesclados[idx][2] = msgSucesso;
}


function removeShapesMesclados(chave) {
	var idx = findIndexShapesMescladosDaChave(chave);
	shapesMesclados.splice(idx, 1);
	defineChavePai();

}

function doubleEncode(s) {
	return encodeURIComponent(encodeURIComponent(s));
}

function fullEncode(s) {
	s = doubleEncode(s);
	s = replaceAll(s, '.', '%2E');
	return s;
}

function fullDecode(s) {
	return decodeURIComponent(s);
}