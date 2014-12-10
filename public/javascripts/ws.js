var ws;

$(function() {
	//create websocket
	ws = new WebSocket("wss://"+window.document.location.host+"/socket");
	
	ws.onopen = function() {
        // Web Socket is connected, send data using send()
        //ws.send("Hello Server here is the client");
        //alert("socket is open");
    };
    
    ws.onclose = function()
    { 
       // websocket is closed.
       //alert("Connection is closed..."); 
    };
    
    ws.onmessage = function (evt) { 
        var received_msg = evt.data;
        //alert("Received: "+evt.data);
        
        $("#content").load("/refresh #content");
        
        /*if(evt.data == "BLABLA"){
        	$("#content").load("/refresh #content");
    	}*/
    };
});