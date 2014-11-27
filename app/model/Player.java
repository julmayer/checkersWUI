package model;

public class Player {
	private final String name;
	private final int id;
	
	public Player(String name, int id) {
	    // TODO give the player a WebSocket
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getId() {
		return this.id;
	}
}
