//globals
var fromX;
var fromY;
var toX;
var toY;
var clickCount = 0;

//TODO: better error handling
$(function() {
	
	$(".myCell").click(function(event) {
		if(clickCount == 0){
	        fromX = decodePosition(event.target.id, "x");
	    	fromY = decodePosition(event.target.id, "y");
	    	clickCount = 1;
		} else {
			toX = decodePosition(event.target.id, "x");
	    	toY = decodePosition(event.target.id, "y");
	    	clickCount = 0;
	    	window.location = "http://localhost:9000/input/"+fromX +" "+ fromY +" "+ toX +" "+ toY;
		}
    });
	
});

function drag(event){
	fromX = decodePosition(event.target.id, "x");
	fromY = decodePosition(event.target.id, "y");
}

function drop(event){
	event.preventDefault();
	toX = decodePosition(event.target.id, "x");
	toY = decodePosition(event.target.id, "y");
	
	//TODO ajax reload, current: loads whole new site -> bad
	window.location = "http://localhost:9000/input/"+fromX +" "+ fromY +" "+ toX +" "+ toY;
}

function allowDrop(event){
	event.preventDefault();
}

function decodePosition(str, xy){
	var res = str.split("-"); 
	var numCells = Math.sqrt($('.myCell').length);
	
	res[1] = numCells - res [1] - 1;
	var dummy = res[2];
	res[2] = res[1];
	res[1] = dummy;
	
	if(xy == "x"){
		return res[1];
	} else {
		return res[2];
	}
}