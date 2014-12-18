var ws;

$(function() {
	//create websocket
	if (window.location.protocol == "https:") {
		ws = new WebSocket("wss://"+window.document.location.host+"/socket");
	} else {
		ws = new WebSocket("ws://"+window.document.location.host+"/socket");
	}
	
	ws.onopen = function() {
        // Web Socket is connected, send data using send()
        //ws.send("Hello Server here is the client");
        //alert("socket is open");
    };
    
    ws.onclose = function()
    { 
    	// websocket is closed.
    	document.cookie = "checkersPlayerID" + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    	alert("Connection is closed..."); 
    };
    
    ws.onmessage = function (evt) { 
        var received_msg = evt.data;
        //alert("Received: "+evt.data);
        
    	$("#content").load(evt.data + " #content")
        
        
        /*if(evt.data == "BLABLA"){
        	$("#content").load("/refresh #content");
    	}*/
    };
});