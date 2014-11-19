//globals
var fromX;
var fromY;
var toX;
var toY;
var clickCount = 0;

$(function() {
	
	$(".contentContainer ").on("click",".myCell",function(event) {
		if(clickCount == 0){
	        fromX = decodePosition(event.target.id, "x");
	    	fromY = decodePosition(event.target.id, "y");
	    	clickCount = 1;
		} else {
			toX = decodePosition(event.target.id, "x");
	    	toY = decodePosition(event.target.id, "y");
	    	clickCount = 0;
	    	var input = "input/" + fromX + "%20" + fromY + "%20" + toX + "%20" + toY + " #content";
	    	$( "#content" ).load(input);
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
	
	var input = "input/" + fromX + "%20" + fromY + "%20" + toX + "%20" + toY + " #content";
	$( "#content" ).load(input);
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