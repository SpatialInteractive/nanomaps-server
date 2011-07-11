(function(global) {
/** Imports **/
var nanomaps=global.nanomaps;

/** Locals **/
var map,
	tapIw,
	toc,
	activeTocName='mqstreet',
	activeTocEntry,
	activeTocLayers,
	mapShowing,
	geoLocationWatchId,
	locationMarker=new nanomaps.ImgMarker({
			src: 'orb_blue.png'
		}),
	placeMarker=new nanomaps.ImgMarker({
			src: 'pin_pink.png'
		}),
	locationUncertaintyMarker=new nanomaps.EllipseMarker({
			className: 'errorHalo',
			unit: 'm'
		});

/** Global scope **/
function initialize() {
	var mapElt=$('#map').get(0);
	
	// Detect hi resolution display and bias zoom levels
	map=new nanomaps.MapSurface(mapElt, {
		//zoomBias: zoomBias
	});
	map.on('motion.longtap', function(motionEvent) {
		var latLng=map.getLocation(motionEvent.x, motionEvent.y);
		showDebugMessage('Long tap: ' + latLng.lat() + ',' + latLng.lng());
		motionEvent.handled=true;
	});
	map.on('motion.click', function(motionEvent) {
		if (motionEvent.count===1) {
			var latLng=map.getLocation(motionEvent.x, motionEvent.y);
			//showDebugMessage('Single tap: ' + latLng.lat() + ',' + latLng.lng() + ', Zoom=' + map.getZoom());
			motionEvent.handled=true;

			if (tapIw) map.detach(tapIw);
			tapIw=new nanomaps.InfoWindow();
			$(tapIw.getContent()).text('Location: (' + latLng.lat() + ', ' + latLng.lng() + ')');
			$(tapIw.element).click(function() {
				if (tapIw) map.detach(tapIw);
			});
			tapIw.setLocation(latLng);
			map.attach(tapIw);
		}
	});
	
    navigator.geolocation.getCurrentPosition(
        handleGeoLocation,
        handleGeoLocationError,
        {maximumAge:Infinity, timeout: 2000}
    );
	
    loadToc();
	setupControls();
	//showMap(null, 2);
}

function setupControls() {
	$('#zoomControl .zoomIn').click(function() {
		map.begin();
		map.zoomIn();
		map.commit(true);
	});
	$('#zoomControl .zoomOut').click(function() {
		map.begin();
		map.zoomOut();
		map.commit(true);
	});
	$('#btnRefresh').click(function() {
	   loadToc(); 
	});
	$('#slMap').change(function() {
	    var name=$(this).val();
	    focusTocEntry(name);
	});
}  

/**
 * Called on browser window resize.  Just have the map reset its
 * natural size
 */
function resize() {
	map.setSize();
}

function loadToc() {
    $.ajax({
       url: '/map/',
       dataType: 'json',
       success: handleTocResults
    });
}    

function handleTocResults(data) {
    var maps=data.maps||[], i, entry, options='';
    toc=maps;
    
    $('#slMap option').remove();
    for (i=0; i<toc.length; i++) {
        entry=toc[i];
        options+='<option';
        if (entry.name===activeTocName)
            options+=' selected';
        options+='>' + entry.name + '</option>';
    }
    $('#slMap').html(options);
    
    focusTocEntry(activeTocName);
}

function focusTocEntry(name) {
    var i, entry, props, name, layer, copyright, src;
    
    activeTocName=name;
    
    // Deactivate layers
    if (activeTocLayers) {
        for (i=0; i<activeTocLayers.length; i++) {
            map.detach(activeTocLayers[i]);
        }
        activeTocLayers=null;
    }
    
    $('#mapcopy').html('');
    
    // Find the active entry
    for (i=0; i<toc.length; i++) {
        entry=toc[i];
        name=entry.name;
        if (name===activeTocName) {
            activeTocEntry=entry;
        }
    }
    
    // Activate it
    activeTocLayers=[];
    if (activeTocEntry.tileSpec) {
    	src=activeTocEntry.tileSpec;
        layer=new nanomaps.TileLayer({
           tileSrc: src,
           autoPixelRatio: true
        });
        activeTocLayers.push(layer);
        map.attach(layer);
        
        props=activeTocEntry.properties||{};
        copyright=props.attributionHtml || props.attribution;
        if (copyright) $('#mapcopy').html(copyright);
    }
}

/**
 * Show an initial location on the map if the map is not already
 * showing.
 */
function showMap(initialPosition, initialLevel) {
	if (mapShowing) return;
	if (!initialPosition) initialPosition={
		// Seattle - my favorite city
		//lat: 47.604317, 
		//lng: -122.329773
		lat:0, lng: 0
	};
	if (initialLevel) map.setZoom(initialLevel);
	map.setLocation(initialPosition);
	
	// Show tiles
	mapShowing=true;
}

function handleGeoLocation(position) {
	var latLng={lat: position.coords.latitude, lng: position.coords.longitude };
	showMap(latLng, 15);

	// Make sure halo is below the marker
	locationUncertaintyMarker.settings.latitude=latLng.lat;
	locationUncertaintyMarker.settings.longitude=latLng.lng;
	locationUncertaintyMarker.settings.radius=position.coords.accuracy;
	map.update(locationUncertaintyMarker);
	
	locationMarker.setLocation(latLng);
	map.update(locationMarker);
	
	$(locationMarker.element).click(function() {
		if (tapIw) map.detach(tapIw);
		tapIw=new nanomaps.InfoWindow();
		$(tapIw.getContent()).text('My Location');
		tapIw.setLocation(latLng, {
			x: 0,
			y: locationMarker.element.clientHeight/2
		});
		$(tapIw.element).click(function() {
			if (tapIw) map.detach(tapIw);
		});
		map.attach(tapIw);
	});
	
	if (!geoLocationWatchId) {
		geoLocationWatchId=navigator.geolocation.watchPosition(handleGeoLocation, handleGeoLocationError);
	}
}

function handleGeoLocationError(error) {
	console.log('Could not get location: ' + error.message);
	showMap();
}


$(window).load(initialize);
$(window).resize(resize);
})(window);

