var shapesMap = null;
var shapes = null;
var erroRetorno = null;
var reloadShapes = true; // ver shapesmap.js/loadShapes
var urlWsCarregar, urlWsPersistir, chavePai, resultadoPersistencia, mensagemSucesso, postBackCarregar, postBackSalvar;
var retanguloPesquisa = null;

function initialize() {
	shapesMap = new MapaGoogle(document.getElementById("googleMap"), document
			.getElementById("clear-button"));	
}

function limpaBusca(){
	retanguloPesquisa.setMap(null);
	$("#lat1").val("");
	$("#lat2").val("");		
	$("#long1").val("");
	$("#long2").val("");
}


google.maps.event.addDomListener(window, 'load', initialize);


function MapaGoogle(_mapContainer, _clearButton) {
	
	var DEFAULT_FILL_COLOR = 'blue';

	// state

	var _selection = null;
	var _map = null;
	var _drawingManager = null;
	var _newShapeNextId = 0;
	var _shapes = Array();

	// types

	var RECTANGLE = google.maps.drawing.OverlayType.RECTANGLE;
	var CIRCLE = google.maps.drawing.OverlayType.CIRCLE;
	var POLYGON = google.maps.drawing.OverlayType.POLYGON;
	var POLYLINE = google.maps.drawing.OverlayType.POLYLINE;
	var MARKER = google.maps.drawing.OverlayType.MARKER;

	

	// initialization

	onCreate();

	return _map;
	
	
	function onCreate() {
		
		_map = createMap(_mapContainer);
		_map.jsonMake = jsonMake;
		_map.shapesDeleteAll = shapesDeleteAll;
		_drawingManager = drawingManagerCreate(_map);

		google.maps.event.addDomListener(_clearButton, 'click',	limpaBusca);

		setInterval(shapesLoad, 100);
	}
	
	function createMap(mapContainer) {
		
		var center = new google.maps.LatLng(-7.792254, -57.887452);

		var mapOptions = {
			zoom : 5,
			center : center,
			mapTypeControl : true,
			mapTypeControlOptions : {
				position : google.maps.ControlPosition.UPPER_CENTER,
			},
			mapTypeId : google.maps.MapTypeId.HYBRID,
			disableDefaultUI : true,
			zoomControl : true
		};

		var map = new google.maps.Map(mapContainer, mapOptions);

		//google.maps.event.addListener(map, 'click', onMapClicked);

		initAutocomplete(map);

		return map;
	}
	
	function jsonMake() {
		var buf = '{"shapes":[';
		for (i = 0; i < _shapes.length; i++) {
			switch (_shapes[i].type) {
			case RECTANGLE:
				buf += comma(i) + '{' + jsonMakeRectangle(_shapes[i]) + '}';
				break;

			case MARKER:
				buf += comma(i) + '{' + jsonMakeMarker(_shapes[i]) + '}';
				break;

			case CIRCLE:
				buf += comma(i) + '{' + jsonMakeCircle(_shapes[i]) + '}';
				break;

			case POLYLINE:
				buf += comma(i) + '{' + jsonMakePolyline(_shapes[i]) + '}';
				break;

			case POLYGON:
				buf += comma(i) + '{' + jsonMakePolygon(_shapes[i]) + '}';
				break;
			}
		}
		buf += ']}';

		return buf;
	}

	
	function onMapClicked() {
		selectionClear();
	}
	
	function selectionClear() {
		selectionSet(null);
	}
	
	function initAutocomplete(map) {
		// Create the search box and link it to the UI element.
		var input = document.getElementById('pac-input');
		var searchBox = new google.maps.places.SearchBox(input);
		map.controls[google.maps.ControlPosition.TOP_RIGHT].push(input);

		// Bias the SearchBox results towards current map's viewport.
		map.addListener('bounds_changed', function() {
			searchBox.setBounds(map.getBounds());
		});

		var markers = [];
		// Listen for the event fired when the user selects a prediction and
		// retrieve
		// more details for that place.
		searchBox.addListener('places_changed', function() {
			var places = searchBox.getPlaces();

			if (places.length == 0) {
				return;
			}

			// Clear out the old markers.
			markers.forEach(function(marker) {
				marker.setMap(null);
			});
			markers = [];

			// For each place, get the icon, name and location.
			var bounds = new google.maps.LatLngBounds();
			places.forEach(function(place) {
				var icon = {
					url : place.icon,
					size : new google.maps.Size(71, 71),
					origin : new google.maps.Point(0, 0),
					anchor : new google.maps.Point(17, 34),
					scaledSize : new google.maps.Size(20, 20)
				};

				// Create a marker for each place.
				markers.push(new google.maps.Marker({
					map : map,
					icon : icon,
					title : place.name,
					position : place.geometry.location
				}));

				if (place.geometry.viewport) {
					// Only geocodes have viewport.
					bounds.union(place.geometry.viewport);
				} else {
					bounds.extend(place.geometry.location);
				}
			});
			map.fitBounds(bounds);
		});
	}
	
	// drawing manager creation

	function drawingManagerCreate() {

		// create drawing manager

		var drawingModes = new Array(RECTANGLE);

		var drawingControlOptions = {
			drawingModes : drawingModes,
			position : google.maps.ControlPosition.TOP_CENTER
		};

		var polyOptions = {
			strokeWeight : 0,
			editable : true,
			fillColor : DEFAULT_FILL_COLOR
		};

		drawingManagerOptions = {
			drawingMode : null,
			drawingControlOptions : drawingControlOptions,
			markerOptions : {
				draggable : true
			},
			polylineOptions : {
				editable : true
			},
			rectangleOptions : polyOptions,
			circleOptions : polyOptions,
			polygonOptions : polyOptions,
			map : _map
		};

		drawingManager = new google.maps.drawing.DrawingManager(
				drawingManagerOptions);

		// tie events to map

		google.maps.event.addListener(drawingManager, 'overlaycomplete',
				onNewShape);
		google.maps.event.addListener(drawingManager, 'drawingmode_changed',
				onDrawingModeChanged);

		// print initial drawing mode, selection

		//printDrawingMode(drawingManager);
		selectionPrint();

		return drawingManager;
	}

	
	// selection

	function selectionPrint() {
		if (_selection == null || _selection == undefined) {
			console.log("selection cleared\n");
		} else {
			console.log(_selection.appId + ": selected\n");
		}
	}

	function selectionIsSet() {
		return _selection != null;
	}

	function selectionSet(newSelection) {
		if (newSelection == _selection) {
			return;
		}

		if (_selection != null) {
			if (_selection.type != 'marker')
				_selection.setEditable(false);
			_selection = null;
		}

		if (newSelection != null) {
			_selection = newSelection;
			if (_selection.type != 'marker')
				_selection.setEditable(true);
		}

		selectionPrint();
	}

	

	function selectionDelete() {
		if (_selection != null) {
			_selection.setMap(null);
			selectionClear();
		}
	}

	function jsonMakePaths(paths) {
		var n = paths.getLength();

		var buf = '"paths":[';
		for (var i = 0; i < n; i++) {
			var path = paths.getAt(i);

			buf += comma(i) + '{' + jsonMakePath(path) + '}';
		}
		buf += ']';

		return buf;
	}

	function jsonMakeRectangle(rectangle) {
		var buf = jsonMakeId(rectangle.id) + ',' + jsonMakeType(RECTANGLE)
				+ ',' + jsonMakeColor(rectangle.fillColor) + ','
				+ jsonMakeBounds(rectangle.bounds);

		return buf;
	}

	function jsonMakeMarker(mkr) {
		var buf = jsonMakeId(mkr.id) + ',' + jsonMakeType(MARKER) + ','
				+ jsonMakePosition(mkr.position);

		return buf;
	}

	function jsonMakeCircle(circle) {
		var buf = jsonMakeId(circle.id) + ',' + jsonMakeType(CIRCLE) + ','
				+ jsonMakeColor(circle.fillColor) + ','
				+ jsonMakeCenter(circle.center) + ','
				+ jsonMakeRadius(circle.radius);

		return buf;
	}

	function jsonMakePolyline(polyline) {
		var buf = jsonMakeId(polyline.id) + ',' + jsonMakeType(POLYLINE) + ','
				+ jsonMakeColor(polyline.strokeColor) + ','
				+ jsonMakePath(polyline.getPath());

		return buf;
	}

	function jsonMakePolygon(polygon) {
		var buf = jsonMakeId(polygon.id) + ',' + jsonMakeType(POLYGON) + ','
				+ jsonMakeColor(polygon.fillColor) + ','
				+ jsonMakePaths(polygon.getPaths());

		return buf;
	}

	function shapesHideAll() {
		for (var i = 0; i < _shapes.length; i++) {
			_shapes[i].setMap(null);
		}
	}

	function shapesDeleteAll() {
		shapesHideAll();
		_shapes.splice(0, _shapes.length);
	}

	// event capture

	function onNewShape(event) {		
		retanguloPesquisa = event.overlay;
		$("#lat1").val(retanguloPesquisa.bounds.getNorthEast().lat());
		$("#lat2").val(retanguloPesquisa.bounds.getSouthWest().lat());		
		$("#long2").val(retanguloPesquisa.bounds.getSouthWest().lng());
		$("#long1").val(retanguloPesquisa.bounds.getNorthEast().lng());
	}
	
	function onDrawingModeChanged() {
		//alert("arrastar");
		selectionClear();
	}
	
	
	
	function typeDesc(type) {
		switch (type) {
		case RECTANGLE:
			return "rectangle";

		case CIRCLE:
			return "circle";

		case POLYGON:
			return "polygon";

		case POLYLINE:
			return "polyline";

		case MARKER:
			return "marker";

		case null:
			return "null";

		default:
			return "UNKNOWN GOOGLE MAPS OVERLAY TYPE";
		}
	}
	
	function shapesLoad() {
		if (reloadShapes) {
			reloadShapes = false;
			shapesHideAll();
			shapesDeleteAll();
			if (shapes != null && shapes != undefined) {
				//jsonRead(shapes);
				// mostrando no mapa
				//posicionaMapaTodosShapes();
			}
			
		}
	}
	
	function shapesDelete(shape) {
		var found = false;

		for (var i = 0; i < _shapes.length && !found; i++) {
			if (_shapes[i] === shape) {
				_shapes.splice(i, 1);
				found = true;
			}
		}
	}

	
	function shapesSave() {
		var shapes = jsonMake();
		parent.shapes = shapes;

		/*
		 * var expirationDate = new Date();
		 * expirationDate.setDate(expirationDate.getDate + 365);
		 * 
		 * var value = escape(shapes) + "; expires=" +
		 * expirationDate.toUTCString(); document.cookie = "shapes=" + value;
		 */
	}
	
	function jsonMakeId(id) {
		var buf = '"id":"' + id + '"';
		return buf;
	}
	
	function jsonMakeType(type) {
		var buf = '"type":"' + typeDesc(type) + '"';

		return buf;
	}

	

	function jsonMakeColor(color) {
		var buf = '"color":"' + color + '"';

		return buf;
	}
	
	function jsonMakeBounds(bounds) {
		var buf = '"bounds":{' + '"northEast":{'
				+ jsonMakeLatlon(bounds.getNorthEast()) + '},'
				+ '"southWest":{' + jsonMakeLatlon(bounds.getSouthWest()) + '}'
				+ '}';

		return buf;
	}
	
	function jsonMakeLatlon(latlon) {
		var buf = '"lat":"' + digitsAfterDot(latlon.lat(), 6) + '","lon":"'
				+ digitsAfterDot(latlon.lng(), 6) + '"';

		return buf;
	}

	function digitsAfterDot(s, n) {
		s = s.toString();
		if (s.indexOf(".") == -1)
			return s;
		return s.substring(0, s.indexOf(".") + n + 1);
	}


	// json reading

	/*function jsonReadPath(jsonPath) {
		var path = new google.maps.MVCArray();

		for (var i = 0; i < jsonPath.path.length; i++) {
			var latlon = new google.maps.LatLng(jsonPath.path[i].lat,
					jsonPath.path[i].lon);
			path.push(latlon);
		}

		return path;
	}

	function corPadrao(cor) {
		if (cor == undefined || cor == null || cor.length == 0)
			return DEFAULT_FILL_COLOR;
		else
			return cor;
	}

	function jsonReadRectangle(jsonRectangle) {
		var jr = jsonRectangle;
		var southWest = new google.maps.LatLng(jr.bounds.southWest.lat,
				jr.bounds.southWest.lon);
		var northEast = new google.maps.LatLng(jr.bounds.northEast.lat,
				jr.bounds.northEast.lon);
		var bounds = new google.maps.LatLngBounds(southWest, northEast);

		var rectangleOptions = {
			bounds : bounds,
			strokeWeight : 0,
			editable : false,
			fillColor : corPadrao(jr.color),
			map : _map
		};

		var rectangle = new google.maps.Rectangle(rectangleOptions);

		return rectangle;
	}

	function jsonReadCircle(jsonCircle) {
		var jc = jsonCircle;

		var center = new google.maps.LatLng(jc.center.lat, jc.center.lon);

		var circleOptions = {
			center : center,
			radius : parseFloat(jc.radius),
			strokeWeight : 0,
			editable : false,
			fillColor : corPadrao(jc.color),
			map : _map
		};

		var circle = new google.maps.Circle(circleOptions);

		return circle;
	}

	function jsonReadMarker2(lat, lon) {
		print("criando marker " + lat + " " + lon + "\n");
		var jc = new Object();
		jc.position = new Object();
		jc.position.lat = lat;
		jc.position.lon = lon;
		jsonReadMarker(jc);
	}

	function jsonReadMarker(jsonMarker) {
		var jc = jsonMarker;

		var point = new google.maps.LatLng(jc.position.lat, jc.position.lon);

		var mkOptions = {
			position : point,
			map : _map
		};

		var mk = new google.maps.Marker(mkOptions);

		return mk;
	}

	function jsonReadPolyline(jsonPolyline) {
		var path = jsonReadPath(jsonPolyline);

		var polylineOptions = {
			path : path,
			editable : false,
			strokeColor : jsonPolyline.color,
			map : _map
		};

		var polyline = new google.maps.Polyline(polylineOptions);

		return polyline;
	}

	function jsonReadPolygon(jsonPolygon) {
		var jc = jsonPolygon;
		var paths = new google.maps.MVCArray();

		for (var i = 0; i < jc.paths.length; i++) {
			var path = jsonReadPath(jc.paths[i]);
			paths.push(path.j[0]);
		}

		var polygonOptions = {
			paths : paths,
			strokeWeight : 0,
			editable : false,
			fillColor : corPadrao(jc.color),
			map : _map
		};

		var polygon = new google.maps.Polygon(polygonOptions);

		return polygon;
	}

	function jsonRead(json) {
		var jsonObject = eval("(" + json + ")");

		if (jsonObject.shapes) {

			for (var i = 0; i < jsonObject.shapes.length; i++) {
				switch (jsonObject.shapes[i].type) {
				case RECTANGLE:
					print("loading rectangle \n");
					var rectangle = jsonReadRectangle(jsonObject.shapes[i]);
					rectangle.id = jsonObject.shapes[i].id;
					newShapeSetProperties(rectangle, RECTANGLE);
					newShapeAddListeners(rectangle);
					shapesAdd(rectangle);
					break;

				case MARKER:
					print("loading marker \n");
					var mkr = jsonReadMarker(jsonObject.shapes[i]);
					mkr.id = jsonObject.shapes[i].id;
					newShapeSetProperties(mkr, MARKER);
					newShapeAddListeners(mkr);
					shapesAdd(mkr);
					break;

				case CIRCLE:
					print("loading circle \n");
					var circle = jsonReadCircle(jsonObject.shapes[i]);
					circle.id = jsonObject.shapes[i].id;
					newShapeSetProperties(circle, CIRCLE);
					newShapeAddListeners(circle);
					shapesAdd(circle);
					break;

				case POLYLINE:
					print("loading polyline \n");
					var polyline = jsonReadPolyline(jsonObject.shapes[i]);
					polyline.id = jsonObject.shapes[i].id;
					newShapeSetProperties(polyline, POLYLINE);
					newShapeAddListeners(polyline);
					shapesAdd(polyline);
					break;

				case POLYGON:
					print("loading polygon \n");
					var polygon = jsonReadPolygon(jsonObject.shapes[i]);
					polygon.id = jsonObject.shapes[i].id;
					newShapeSetProperties(polygon, POLYGON);
					newShapeAddListeners(polygon);
					shapesAdd(polygon);
					break;
				}
			}
			print(jsonObject.shapes.length + " shapes loaded \n");
		}
	}

	// json writing

	function comma(i) {
		return (i > 0) ? ',' : '';
	}

	

	

	
	function jsonMakeCenter(center) {
		var buf = '"center":{' + jsonMakeLatlon(center) + '}';

		return buf;
	}

	function jsonMakePosition(position) {
		var buf = '"position":{' + jsonMakeLatlon(position) + '}';

		return buf;
	}

	function jsonMakeRadius(radius) {
		var buf = '"radius":"' + radius + '"';

		return buf;
	}

	function jsonMakePath(path) {
		var n = path.getLength();

		var buf = '"path":[';
		for (var i = 0; i < n; i++) {
			var latlon = path.getAt(i);

			buf += comma(i) + '{' + jsonMakeLatlon(latlon) + '}';
		}
		buf += ']';

		return buf;
	}

	
	// storage

	function shapesAdd(shape) {
		_shapes.push(shape);
	}


	
	

	function posicionaMapaTodosShapes() {
		if (_shapes.length == 0)
			return;
		var bounds = new google.maps.LatLngBounds();
		for (var i = 0; i < _shapes.length; i++) {
			if (_shapes[i].position) {
				bounds.extend(_shapes[i].position);
			} else if (_shapes[i].bounds || _shapes[i].center) {
				bounds.union(_shapes[i].getBounds());
			} else if (_shapes[i].paths) {
				var p = _shapes[i].getPaths().getArray()[0].getArray();
				for (var j = 0; j < p.length; j++) {
					bounds.extend(p[j]);
				}
			} else if (_shapes[i].latLngs) {
				var p = _shapes[i].latLngs.getArray()[0].getArray();
				for (var j = 0; j < p.length; j++) {
					bounds.extend(p[j]);
				}
			}
		}
		_map.fitBounds(bounds);
	}

	// printing

	function print(string) {
		_console.innerHTML += string;
		_console.scrollTop = _console.scrollHeight;
	}

	
	
	// new shape integration

	function newShapeAddPathListeners(shape, path) {
		google.maps.event.addListener(path, 'insert_at', function() {
			onShapeEdited(shape)
		});
		google.maps.event.addListener(path, 'remove_at', function() {
			onShapeEdited(shape)
		});
		google.maps.event.addListener(path, 'set_at', function() {
			onShapeEdited(shape)
		});
	}

	function newShapeAddListeners(shape) {
		google.maps.event.addListener(shape, 'click', function() {
			onShapeClicked(shape);
		});

		switch (shape.type) {
		case RECTANGLE:
			google.maps.event.addListener(shape, 'bounds_changed', function() {
				onShapeEdited(shape);
			});
			break;

		case CIRCLE:
			google.maps.event.addListener(shape, 'center_changed', function() {
				onShapeEdited(shape);
			});
			google.maps.event.addListener(shape, 'radius_changed', function() {
				onShapeEdited(shape);
			});
			break;

		case POLYLINE:
			var path = shape.getPath();
			newShapeAddPathListeners(shape, path);
			break;

		case POLYGON:
			var paths = shape.getPaths();

			var n = paths.getLength();
			for (var i = 0; i < n; i++) {
				var path = paths.getAt(i);
				newShapeAddPathListeners(shape, path);
			}
			break;
		}
	}

	function newShapeSetProperties(shape, type) {
		shape.type = type;
		shape.appId = _newShapeNextId;

		_newShapeNextId++;
	}

	// map creation

	


	
	

	function onShapeEdited(shape) {
		print(shape.appId + ": shape edited\n");
		shapesSave();
	}

	function onShapeClicked(shape) {
		print(shape.appId + ": shape clicked\n");
		selectionSet(shape);
	}

*/	

	function onDeleteButtonClicked() {
		print("delete button clicked\n");

		if (selectionIsSet()) {
			shapesDelete(_selection);
			selectionDelete();
			shapesSave();
		}
	}

	function onClearButtonClicked() {
		selectionClear();
		shapesHideAll();
		shapesDeleteAll();
		shapesSave();
	}	
}


