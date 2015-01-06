package model;

import play.Logger;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.controller.State;
import de.htwg.checkers.util.observer.Observer;

public class Match implements Observer {
    private final IGameController gameController;
	private Player hoster;
	private Player joiner;
	private final String id;
	private Integer playerCounter = new Integer(0);
	
	public Match(IGameController gameController, Player hoster) {
		this.hoster = hoster;
		this.hoster.setMatch(this);
		this.gameController = gameController;
		this.gameController.addObserver(this);
		this.id = Integer.toHexString(System.identityHashCode(gameController));
		++playerCounter;
	}
	
	/**
	 * Let a Player join the Match.
	 * @param joiner Player to join the Match.
	 */
	public void join(Player joiner) {
	    synchronized (playerCounter) {
	        ++playerCounter;
        }
		this.joiner = joiner;
		this.joiner.setMatch(this);
	}
	
	/**
	 * Decrease player counter to determine when game is empty.
	 */
	public void leave() {
	    synchronized (playerCounter) {
	        --playerCounter;
        }
	}
	
	/**
	 * Checks if the game is empty.
	 * @return True if game is empty.
	 */
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
	
	/**
	 * Removes a given Player from the match and instructs
	 * the GameController to remove his Figures.
	 * If the given Player is not in this game, nothing happens.
	 * @param player Player who gave up.
	 */
	public void playerGaveUp(Player player) {
	    Logger.debug(player + " gave up " + this);
	    String controlInput = null;
	    if (player.equals(hoster)) {
	        Logger.debug("Player was hoster");
	        controlInput = "B";
	        hoster = null;
	    } else if (player.equals(joiner)) {
	        Logger.debug("Player was joiner");
	        controlInput = "W";
	        joiner = null;
	    }
	    
	    if (controlInput != null) {
	        Logger.debug("leave");
	        leave();
	        if (gameController.getCurrentState() == State.RUNNING) {
	            Logger.debug("Send input");
	            this.gameController.input(controlInput);
	            Logger.debug("Input sent");
	        }
	    }
	}

	/**
	 * Instruct players to reload the game.
	 */
	@Override
	public void update() {
		if (hoster != null) {
		    hoster.reload();
		}
		if (joiner != null){
			joiner.reload();
		}
	}
	
	   @Override
	    public String toString() {
	        StringBuilder builder = new StringBuilder();
	        builder.append("Match [hoster=");
	        builder.append(hoster);
	        builder.append(", joiner=");
	        builder.append(joiner);
	        builder.append(", id=");
	        builder.append(id);
	        builder.append(", playerCounter=");
	        builder.append(playerCounter);
	        builder.append("]");
	        return builder.toString();
	    }
}
