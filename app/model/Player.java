package model;

import java.util.Timer;
import java.util.TimerTask;

import play.Logger;
import play.mvc.WebSocket;

public class Player {
	private final int id;
	private final String name;
	private WebSocket<String> webSocket;
	private WebSocket.Out<String> outStream;
	private Match match;
	private String wantedGameName;
	private final Timer watchDog;
	private TimerTask task;
	private static final long WATCHDOG_TIMEOUT = 40000;
	
	public Player(int id, String name) {
		this.name = name;
		this.id = id;
		this.watchDog = new Timer(true);
	}
	
	public void setWantedGameName(String wantedGameName){
		this.wantedGameName = wantedGameName;
	}
	
	public String getWantedGameName(){
		return this.wantedGameName;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setWebSocket(WebSocket<String> webSocket) {
        this.webSocket = webSocket;
        resetWatchdog();
    }

    public void setOutStream(WebSocket.Out<String> outStream) {
        this.outStream = outStream;
    }
    
    public WebSocket<String> getWebsocket(){
		return this.webSocket;
	}
    
    public void setMatch(Match match) {
        this.match = match;
    }
    
    public Match getMatch() {
        return this.match;
    }
    
    public void resetWatchdog() {
        Logger.debug("resert Timer from " + this);
        if (task != null) {
            Logger.debug("cancel old task");
            task.cancel();
        }
        task = new TimerTask() {
            
            @Override
            public void run() {
                Logger.debug("send ping to " + Player.this.toString());
                outStream.write("ping");
                
            }
        };
        
        watchDog.schedule(task, WATCHDOG_TIMEOUT, WATCHDOG_TIMEOUT);
    }
    
    public void cancelWatchdog() {
        Logger.debug("Cancel Watchdog of " + this);
        watchDog.cancel();
    }
	
    /**
     * Send the Player a message to reload the "refresh" page.
     */
	public void reload(){
		reload("/refresh");
	}

	/**
	 * Send the Player a message with a page which should be reloaded.
	 * @param url URL to the page which should be reloaded
	 */
	public void reload(String url){
		outStream.write(url);
	}
	
	/**
	 * Let this Player give up his current Game.
	 * Instructs the Match, if existing, to remove the Player.
	 */
	public void giveUp() {
	    Logger.debug("in give up");
	    if (match != null) {
	        Logger.debug("match is not null");
	        match.playerGaveUp(this);
	    }
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Player)) {
            return false;
        }
        Player other = (Player) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Player [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
}
