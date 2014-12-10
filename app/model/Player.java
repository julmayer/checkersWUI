package model;

import play.mvc.WebSocket.*;
import play.mvc.*;
import play.libs.F.Callback;
import play.libs.F.Callback0;

public class Player {
	private final int id;
	private final String name;
	
	private WebSocket<String> ws;
	
	private WebSocket.In<String> inStream;
	private WebSocket.Out<String> outStream;
	
	
	public Player(int id, String name) {
		this.name = name;
		this.id = id;
		this.ws = createNewWebsocket();
	}
	
	private  WebSocket<String> createNewWebsocket(){
		this.ws = new WebSocket<String>() {
			public void onReady(WebSocket.In<String> in,final WebSocket.Out<String> out) {
				
				inStream = in;
				outStream = out;
				
				in.onMessage(new Callback<String>() {
					public void invoke(String event) {
						System.out.println(event);
						//out.write("i'm server, received: "+event);
					}
				});

				in.onClose(new Callback0() {
					public void invoke() {
						System.out.println("Closed");
					}
				});
			}

		};
		return ws;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public WebSocket<String> getWebsocket(){
		return this.ws;
	}
	
	public void reload(){
		outStream.write("update");
	}
}
