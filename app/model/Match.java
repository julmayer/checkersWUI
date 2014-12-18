package model;

import de.htwg.checkers.controller.GameController;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.util.observer.Observer;

public class Match implements Observer {
	private final IGameController gameController;
	private Player hoster;
	private Player joiner;
	private final String id;
	private Integer playerCounter;
	
	public Match(IGameController gameController, Player hoster) {
		this.hoster = hoster;
		this.gameController = gameController;
		this.gameController.addObserver(this);
		this.id = Integer.toHexString(System.identityHashCode(gameController));
	}
	
	public void join(Player joiner) {
	    synchronized (playerCounter) {
	        ++playerCounter;
        }
		this.joiner = joiner;
	}
	
	public void leave() {
	    synchronized (playerCounter) {
	        --playerCounter;
        }
	}
	
	public synchronized boolean isEmpty() {
	    return playerCounter.intValue() == 0;
	}
	
	public Player getHoster() {
		return this.hoster;
	}
	
	public Player getJoiner() {
		return this.joiner;
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
		hoster.reload();
		if (joiner != null){
			joiner.reload();
		}
	}
}
