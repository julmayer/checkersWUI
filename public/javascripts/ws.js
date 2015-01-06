var ws;

$(function() {
	//create websocket
	if (window.location.protocol == "https:") {
		ws = new WebSocket("wss://"+window.document.location.host+"/socket");
	} else {
		ws = new WebSocket("ws://"+window.document.location.host+"/socket");
	}
	
	//expire player cookie if user closes window 
	window.onbeforeunload = function() {
		ws.onclose = function(){};
		document.cookie = "CheckersPlayerID" + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
		ws.close();
	};
	
	ws.onopen = function() {
        // Web Socket is connected, send data using send()
    };
    
    ws.onclose = function()
    { 
    	// websocket is closed, discard cookie.
    	document.cookie = "CheckersPlayerID" + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    };
    
    ws.onmessage = function (evt) { 
    	// load content of given page
    	var received_msg = evt.data;
        
    	$("#content").load(received_msg + " #content")
    };
});