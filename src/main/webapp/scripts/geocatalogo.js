var myApp;
var vcgejson;
var vcejson;
var edgvjson;

$(document).ready(function() {
		
	$("#modal_detalhe").load("detalhe_recurso.html"); 
	$("#resultado_pesquisa").load("resultado_pesquisa.html");
	//$("#panel_config").load("configuracao_pesquisa.html");
	carregaTelaEspera();
	carregaConfiguracaoBusca();
	$("#pesquisar").click(function() {
		$("#table > tbody").html("");
		$("#pagcorrente").val(1);
		myApp.showPleaseWait();
		disparaPesquisa();
		
	});
	$("#salva_configuracao").click(function() {
		salvaConfiguracao();
	});
	
	$("button[data-target='#panel_vcge'").click( function(){
		if(!vcgejson){
			pesquisaVCGE();
		}
	});
	$("button[data-target='#panel_vce'").click( function(){
		if(!vcejson){
			pesquisaVCE();
		}
	});
	$("button[data-target='#panel_edgv'").click( function(){
		if(!edgvjson){
			pesquisaEDGV();
		}
	});
	$("input[id*='slider']").slider({
		tooltip: 'always'
	});
	$('[data-toggle="tooltip"]').tooltip(); 
	
});

function carregaConfiguracaoBusca(){
	$.ajax({
		type : "GET",
		url : "/geocatalogo/servico/search/configuracao",
		async : false,
		contentType : "application/json",
		dataType : 'json',
		success : function(data) {
			
			//console.log(data.data);
			$.each(	data.data,function(idx, obj) {
				$("#slider1").attr("data-slider-value",obj[0]);				
				$("#slider2").attr("data-slider-value",obj[1]);
				$("#slider3").attr("data-slider-value",obj[2]);
				$("#slider4").attr("data-slider-value",obj[3]);
				$("#slider5").attr("data-slider-value",obj[4]);
				$("#slider6").attr("data-slider-value",obj[5]);
				$("#slider7").attr("data-slider-value",obj[6]);
				$("#slider8").attr("data-slider-value",obj[7]);
				$("#slider9").attr("data-slider-value",obj[8]);
				$("#slider10").attr("data-slider-value",obj[9]);
				$("#slider11").attr("data-slider-value",obj[10]);
				$("#slider12").attr("data-slider-value",obj[11]);
				$("#slider13").attr("data-slider-value",obj[12]);
				
				$("#slider1").attr("value",obj[0]);
				$("#slider2").attr("value",obj[1]);
				$("#slider3").attr("value",obj[2]);
				$("#slider4").attr("value",obj[3]);
				$("#slider5").attr("value",obj[4]);
				$("#slider6").attr("value",obj[5]);
				$("#slider7").attr("value",obj[6]);
				$("#slider8").attr("value",obj[7]);
				$("#slider9").attr("value",obj[8]);
				$("#slider10").attr("value",obj[9]);
				$("#slider11").attr("value",obj[10]);
				$("#slider12").attr("value",obj[11]);
				$("#slider13").attr("value",obj[12]);
				
				//console.log(obj.value);
			});
		},
		error : function(e) {
			alert(e.message);
		}
	});
}

function carregaTelaEspera() {
	myApp = myApp
	|| (function() {
		var pleaseWaitDiv = $('<div class="modal hide" id="pleaseWaitDialog"' 
				+' data-backdrop="static" data-keyboard="false"><div class="modal-header"><h1>Processing...</h1>'
				+ '<img src="../img/loader.gif"/></div><div class="modal-body"><img src="../img/loader.gif"/><div class="progress progress-striped active">'
				+ '<div class="bar" style="width: 100%;"></div></div></div></div>');
		return {
			showPleaseWait : function() {
				$('#loading-indicator').show();
				pleaseWaitDiv.modal();
			},
			hidePleaseWait : function() {
				$('#loading-indicator').hide();
				pleaseWaitDiv.modal('hide');
			},

		};
	})();
	
}

function pesquisaEDGV() {

	$.ajax({
		type : "GET",
		url : "/geocatalogo/servico/search/ontologia/edgv",
		async : false,
		contentType : "application/json",
		dataType : 'json',
		success : function(data) {
			
			$.each(	data.data,function(idx, obj) {
				$("#panel_lista_edgv").append(
						"<li id='pedgv"+obj[0]+"'>"
						+"<a href='#' class='trigger right-caret'>"
						+ obj[1]+ "</a><ul></ul></li>");				
			});
			if(!edgvjson){
				pesquisaEDGVfilhos();
			}
			$("input[name='cbx_edgv']").click(function() {
				if($(this).is(':checked')){
					$("#termo_edgv").text($("#termo_edgv").text()+" | EDGV: "+ $(this).val() );
				}else{
					var txt = $("#termo_edgv").text();
					txt = txt.replace(" | EDGV: "+ $(this).val(),"");
					$("#termo_edgv").text(txt);
				};
			});

		},
		error : function(e) {
			alert(e.message);
		}
	});
}

function pesquisaEDGVfilhos() {
	$.ajax({
		type : "GET",
		url : "/geocatalogo/servico/search/ontologia/edgv/filhos",
		async : false,
		contentType : "application/json",
		dataType : 'json',
		success : function(data) {
			edgvjson = data
			//console.log(data);
			
			$.each(edgvjson.data,function(idx, obj) {
				$("li[id='pedgv"+obj[2]+"'] > ul").append("<li id='pedgv"+obj[0]
				+"'><a href='#'><input type='checkbox'" 
				+" name='cbx_edgv' value='"+obj[1]+"'/><small>"
				+ obj[1]+ "</small></a><ul></ul></li>");
				
			});
			
		},
		error : function(e) {
			alert(e.message);
		}
	});
}

function pesquisaVCGE() {

	$.ajax({
		type : "GET",
		url : "/geocatalogo/servico/search/ontologia/vcge",
		async : false,
		contentType : "application/json",
		dataType : 'json',
		success : function(data) {
			vcgejson = data;
			$.each(	vcgejson.data,function(idx, obj) {
				if(obj[4]=='VCGE'){
				$("#panel_lista_vcge").append(
						"<li id='pvcge"+obj[0]+"' class='dropdown-submenu'>"
						+"<a href='#' class='trigger right-caret'><input type='checkbox'" 
						+" name='cbx_vcge_n1' value='"+obj[1]+"'/>"
						+ obj[1]+ "</a><ul></ul></li>");
					pesquisaVCGEfilhos(obj[0]);
				}
			});
			$("input[name='cbx_vcge_n1']").click(function() {
				if($(this).is(':checked')){
					$("#termo_vcge").text($("#termo_vcge").text()+" | VCGE: "+ $(this).val() );
				}else{
					var txt = $("#termo_vcge").text();
					txt = txt.replace(" | VCGE: "+ $(this).val(),"");
					$("#termo_vcge").text(txt);
				};
			});
		},
		error : function(e) {
			alert(e.message);
		}
	});
}
	
function pesquisaVCGEfilhos(id) {
	$.each(	vcgejson.data,function(idx, obj) {		
		if(obj[3]== id){
			console.log(obj[3]+" "+id);
			$("li[id='pvcge"+id+"'] > ul").append("<li id='pvcge"+obj[0]
			+"'><a href='#'><input type='checkbox'" 
			+" name='cbx_vcge_n1' value='"+obj[1]+"'/><small>"
			+ obj[1]+ "</small></a><ul></ul></li>");
			pesquisaVCGEfilhos(obj[0]);
		}
	});
	
	
}

function pesquisaVCE() {

	$.ajax({
		type : "GET",
		url : "/geocatalogo/servico/search/ontologia/vce",
		async : false,
		contentType : "application/json",
		dataType : 'json',
		success : function(data) {
			vcejson = data;
			$.each(	vcejson.data,function(idx, obj) {
				if(obj[4]=='VCE'){
				$("#panel_lista_vce").append(
						"<li id='pvce"+obj[0]+"' >"
						+"<a href='#' class='trigger right-caret'><input type='checkbox'" 
						+" name='cbx_vce' value='"+obj[1]+"'/>"
						+ obj[1]+ "</a></li>");
					pesquisaVCEfilhos(obj[0]);
				}
			});
			$("input[name='cbx_vce']").click(function() {
				if($(this).is(':checked')){
					$("#termo_vce").text($("#termo_vce").text()+" | VCE: "+ $(this).val() );
				}else{
					var txt = $("#termo_vce").text();
					txt = txt.replace(" | VCE: "+ $(this).val(),"");
					$("#termo_vce").text(txt);
				};
			});
		},
		error : function(e) {
			alert(e.message);
		}
	});
}
	
function pesquisaVCEfilhos(id) {
	var i = $("li[id='pvce"+id+"']");
	i.append("<ul></ul>");
	$.each(	vcejson.data,function(idx, obj) {
		if(obj[3]==id){
			$("li[id='pvce"+id+"'] > ul").append("<li id='pvce"+obj[0]
			+"'><a href='#'><input type='checkbox'" 
			+" name='cbx_vce' value='"+obj[1]+"'/><small>"
			+ obj[1]+ "</small></a></li>");
			pesquisaVCEfilhos(obj[0]);
		}
	});
	
}





function disparaPesquisa() {
	$(".collapse").collapse('hide')
	$.ajax({
		type : "POST",
		url : "/geocatalogo/servico/search/ranking",
		data : $('#form').serialize(),
		success : function(data) {
			var maxpont = parseInt(data.maxpont);
			$.each(
					data.data,
					function(idx, obj) {						
						var i = parseInt(idx + 1);
						var ipp = parseInt($("#qtditenspag").val());
						var pc = parseInt($("#pagcorrente").val());
						$('#table > tbody:last-child')
						.append(
								"<tr onmousedown='desmarcaLinha(this)' onmouseup='marcaLinha(this)'><td>"
								+(i + (ipp*(pc-1)))
								+ "</td><td>"
								+ (obj[0]/maxpont).toFixed(2)
								+ "</td><td>"
								+ obj[0]
								+ "</td>"
								+ "<td><a target='_blank'  href='"+obj[2]+"'>"
								+ "<span class='glyphicon glyphicon-link'></span></a></td>"
								+ "<td><a target='_blank' href='"+obj[3]+"'>"
								+ "<span class='glyphicon glyphicon-download-alt'></span></a></td>"
								+ "<td>"
								+ "<a target='_blank'  id='"+obj[6]+"' href='#' name='link_detalhe' data-toggle='modal' data-target='#modal_detalhe'>"
								+ "<span class='glyphicon glyphicon-th-list'></span></a>" 
								+ "</td>"
								+"<td>"
								+ obj[1]
								+ "</td>"
								+ "<td>"
								+ obj[4]
								+ "</td>"
								+ "<td class='protocolo'>"
								+ recuperaTipo(obj[5])
								+ "</td>"
								+"<tr>");

					});
			$("a[href='']").hide();
			$("a[href*='access=private']:last-child").hide();
			$("a[name='link_detalhe']").click(
				function(ev){
					recuperaRecurso(ev.currentTarget.id);
				}	
			);
			myApp.hidePleaseWait();
		},
		error : function(e) {
			alert(e.message);
			myApp.hidePleaseWait();
		}
	});
}

function recuperaUrlMetadado(ev){
	$.ajax({
		type : "GET",
		url : "http://www.metadados.inde.gov.br/geonetwork/srv/por/main.search.embedded?any=" 
					+ ev +"&dummyfiel",
		success : function(data) {
			//console.log(data);
						
		},
		error : function() {
			alert("falha");				
		}
	});
	
}

function salvaConfiguracao() {
	$.ajax({
		type : "POST",
		url : "/geocatalogo/servico/search/configuracao",
		data : $('#form').serialize(),
		success : function(data) {			
			alert("Configuração de pesos salva com sucesso");
		},
		error : function() {
			alert("failure");
			myApp.hidePleaseWait();
		}
	});
}

	function recuperaRecurso(id){
		$.ajax({
			type : "GET",
			url : "/geocatalogo/servico/search/ranking/"+id,
			success : function(data) {
				//console.log(data.data);
				$.each(data.data,
						function(idx, obj) {
							var i = parseInt(idx + 1);
							$(".modal-title").text(obj[1]);
							$("#detalhe_descricao").text(obj[2]);
							$("#detalhe_subject").text(obj[3]);
							$("#detalhe_source").text(obj[4]);
							$("#detalhe_abstract").text(obj[5]);
							$("#detalhe_link").text(obj[6]);
							$("#detalhe_download").text(obj[7]);
						});
				
			},
			error : function() {
				alert("falha");				
			}
		});
		
	}
	function recuperaPalavras() {
		$.ajax({
			type : 'GET',
			url : 'http://localhost:8080/geocatalogo/servico/search/nuvem',
			async : false,
			contentType : "application/json",
			dataType : 'json',
			success : function(data) {
				//console.log(data.data);

				$.each(data.data, function(idx, obj) {
					//$( "#demo" ).append( "<span data-weight='99'>"+ obj[1] +"</span>" );
				});
				carregaNuvem();

			},
			error : function(e) {
				alert(e.message);
			}
		});
	}
	function carregaNuvem() {
		$("#demo")
				.awesomeCloud(
						{
							"size" : {
								"<a href='http://www.jqueryscript.net/tags.php?/grid/'>grid</a>" : 56, // word spacing, smaller is more tightly packed
								"factor" : 0, // font resize factor, 0 means automatic
								"normalize" : false
							// reduces outliers for more attractive output
							},
							"color" : {
								"background" : "rgba(255,255,255,0)", // background color, transparent by default
								"start" : "#20f", // color of the smallest font, if options.color = "gradient""
								"end" : "rgb(200,0,0)" // color of the largest font, if options.color = "gradient"
							},
							"options" : {
								"color" : "random-dark", // random-light, random-dark, gradient
								"rotationRatio" : 0.35, // 0 is all horizontal, 1 is all vertical
								"printMultiplier" : 3, // set to 3 for nice printer output; higher numbers take longer
								"sort" : "random" // highest, lowest or random
							},
							"font" : "'Times New Roman', Times, serif", //  the CSS font-family string
							"shape" : "square" // circle, square, star or a theta function describing a shape
						});
	}
	
	function recuperaTipo(protocolo){
		if(!protocolo){
			return '';
		}
		if(protocolo.indexOf("OGC:WMS") > -1){
			return 'WMS';
		}
		if(protocolo.indexOf("OGC:WFS") > -1){
			return 'WFS';
		}
		return protocolo;
	}
	
	function formataNome(nome){
		return nome;
	}
	
	function marcaLinha(elem){
		//alert("marca");
		$(elem).css("background-color", "#eee");
	}
	
	function desmarcaLinha(elem){
		//alert("des");
		$("tr").css("background-color", "white");
		
	}
	