$(function() {
	$("#content").on("input","#gamename",function(event) {
		if($("#gamename").val() == ""){
			$("#createMPButton").prop('disabled', true);
		} else {
			$("#createMPButton").prop('disabled', false);
		}
	});
});

function loadContentFrom(url) {
	$('#content').load(url + " #content");
}
