package model;

import de.htwg.checkers.controller.GameController;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.util.observer.Observer;

public class Match implements Observer {
	private final IGameController gameController;
	private Player hoster;
	private Player joiner;
	private final String id;
	
	public Match(IGameController gameController, Player hoster) {
		this.gameController = gameController;
		this.gameController.addObserver(this);
		this.hoster = hoster;
		this.id = Integer.toHexString(System.identityHashCode(gameController));
	}
	
	public void join(Player joiner) {
		this.joiner = joiner;
	}
	
	public Player getHoster() {
		return this.hoster;
	}
	
	public IGameController getGameController() {
		return this.gameController;
	}
	
	public String getId() {
		return this.id;
	}

	@Override
	public void update() {
		// Diese Methode wird vom Controller aufgerufen, wenn es was zu aktualisieren gibt
		// TODO inform players with websocket
		
	}
}
