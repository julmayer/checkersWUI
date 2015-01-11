package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.Match;
import model.Player;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.play.java.JavaController;
import org.pac4j.play.java.RequiresAuthentication;

import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.mvc.Http.Cookie;
import play.mvc.Result;
import play.mvc.WebSocket;
import de.htwg.checkers.controller.GameController;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.controller.State;
import de.htwg.checkers.models.Cell;

public class Application extends JavaController {
	private static Map<String, Match> openMatches = new HashMap<String, Match>();
	private static Map<String, Match> runningMatches = new HashMap<String, Match>();
	private static final String COOKIE_MATCH_ID = "CheckersMatchID";
	private static final String COOKIE_PLAYER_ID = "CheckersPlayerID";
	private static int playerCount = 0;
	private static Map<Integer, Player> playerMap = new HashMap<>();
	private static List<Player> playerListIdle = new LinkedList<>();
	
    public static Result gamecenter() {
        // check if logged in via OAuth 2.0
        CommonProfile googleProfile = getUserProfile();
        Logger.debug("gamecenter Google Profile: " + googleProfile);
        if (googleProfile != null) {
            Logger.debug("Firstname " + googleProfile.getFirstName() + " Lastname " + googleProfile.getFamilyName());
            session("loggedIn", "true");
        }
    	//check if user is logged in
    	if(session("loggedIn") != null){
    		
	        Player player = getCurrentPlayer();
	        // if player has no cookie or it is expired, create new player
	        if (player == null) {
	            synchronized (playerMap) {
	                String playerId = String.valueOf(playerCount);
	                player = new Player(playerCount, request().remoteAddress());
	                setCookieID(COOKIE_PLAYER_ID, playerId);
	                createWebSocketForPlayer(player);
	                playerMap.put(playerCount, player);
	                playerListIdle.add(player);
	                playerCount++;
	            }
	            Logger.info("Created new " + player);
	        } else {
	            Logger.debug(player + " in gamecenter");
	        }
	        
	        if (!playerListIdle.contains(player)) {
	            player.setMatch(null);
	            
	            // Player has reload the page or came from a match, now idle again.
	            playerListIdle.add(player);
	            Logger.debug(player + " is now idle again");
	        }
	        
	        return ok(views.html.gamecenter.render(openMatches));
	        
    	} else {
    	    String url = getRedirectAction("Google2Client").getLocation();
    		return ok(views.html.login.render(url));
    	}
    }

	public static Result join(String matchId) {
		Match match;
		synchronized (openMatches) {
		    match = openMatches.remove(matchId);
        }
		
		Player joiner = getCurrentPlayer();
		match.join(joiner);
		
		synchronized (runningMatches) {
		    runningMatches.put(match.getId(), match);
        }

		setCookieID(COOKIE_MATCH_ID,matchId);
		synchronized (playerListIdle) {
		    playerListIdle.remove(joiner);
        }
		Logger.info(joiner + " joined " + match);
		return playGame(8, true, 0, match);
	}

	public static Result create(String type) {
		Player hoster = getCurrentPlayer();
		Match match = new Match(new GameController(), hoster);

		setCookieID(COOKIE_MATCH_ID, match.getId());
		
		Result result;
		if (type.equals("Multi")) {
			synchronized (openMatches) {
			    openMatches.put(match.getId(), match);
            }
			Logger.info(hoster + " create multiplayer game " + match);
			result = renderPage(match);
		} else {
			synchronized (runningMatches) {
			    runningMatches.put(match.getId(), match);
            }
			Logger.info(hoster + " created singleplayer game " + match);
			result = playGame(8, false, 0, match);
		}
		
		// remove hoster from ide players
		playerListIdle.remove(hoster);
		
		informIdlePlayer();
		
		return result;
	}
	
	private static void informIdlePlayer() {
	    // inform all idle player of new multiplayer game
        for (Player player : new LinkedList<Player>(playerListIdle)){
            Logger.debug("Refresh idle " + player);
            player.reload("/play");
        }
	}

	private static void setCookieID(String cookie, String id) {
		response().setCookie(cookie, id);
	}

	public static Result refresh() {
	    Match currentMatch = getCurrentMatch();
	    Logger.debug(getCurrentPlayer() + " asks for refresh of " + currentMatch);
		return renderPage(currentMatch);
	}

	public static Result playGame(int size, boolean multiplayer,
		int difficulty, Match currentMatch) {
		StringBuilder initCommandBuilder = new StringBuilder();
		initCommandBuilder.append(size);
		if (multiplayer) {
			initCommandBuilder.append(" M ");
		} else {
			initCommandBuilder.append(" S ");
		}
		initCommandBuilder.append(difficulty);
		currentMatch.getGameController().input(initCommandBuilder.toString());
		return renderPage(currentMatch);
	}

	public static Result input(String move) {
		Match match = getCurrentMatch();
		Player player = getCurrentPlayer();

		if (isPlayerOnTurn(player, match)) {
		    match.getGameController().input(move);
		}
		
		return renderPage(match);
	}

	private static Result renderPage(Match currentMatch) {
		Result result;
		IGameController gameController = null;
		
		if (currentMatch != null) {
		    gameController = currentMatch.getGameController();
		}
		
		if (gameController == null) {
		    result = ok(views.html.waitGame.render("Waiting for creation of game"));
		    getCurrentPlayer().reload();
		} else if (gameController.getCurrentState() != State.RUNNING) {
		    result = ok(views.html.waitGame.render("Waiting for other player"));
		} else if (gameController.checkIfWin()) {
		    currentMatch.leave();
		    if (currentMatch.isEmpty()) {
		        runningMatches.remove(currentMatch);
		    }
		    response().discardCookie(COOKIE_MATCH_ID);
		    result = ok(views.html.finish.render(gameController.getInfo()));
	    } else {
			Player player = getCurrentPlayer();
			
			List<List<String>> data = updateData(gameController.getField()
					.getField());

			String nextPlayer = "Waiting for opponent.";
			if (isPlayerOnTurn(player, currentMatch)) {
				nextPlayer = "Your turn";
			}

			String error = "";
			if (gameController.getError() != null) {
				error = gameController.getError();
				if(!isPlayerOnTurn(player, currentMatch)){
					error = "";
				}
			}
			
			int moveCount = gameController.getMoveCount();
			boolean playerIsBlack = currentMatch.getHoster().equals(player);
			
			result = ok(views.html.playGame.render(data, data.size(),
					nextPlayer, error, moveCount, playerIsBlack));
	    }
	
		return result;
	}

	private static List<List<String>> updateData(Cell[][] matrix) {
		List<List<String>> rows = new ArrayList<List<String>>(matrix.length);
		for (int x = 0; x < matrix.length; ++x) {
			for (int y = 0; y < matrix.length; ++y) {
				if (y >= rows.size() || rows.get(y) == null) {
					rows.add(y, new ArrayList<String>());
				}

				Cell currentCell = matrix[x][y];
				String s;
				if (currentCell.isOccupied()) {
					s = currentCell.getOccupier().toString();
				} else {
					s = "";
				}
				rows.get(y).add(x, s);
			}
		}

		Collections.reverse(rows);

		return rows;
	}

	public static Result rules() {
		return ok(views.html.rules.render());
	}

	public static Result index() {
        return ok(views.html.index.render());
	}
	
	@RequiresAuthentication(clientName = "Google2Client")
	public static Result protectedIndex() {
	  // profile
	  final CommonProfile profile = getUserProfile();
	  return ok(views.html.protectedIndex.render(profile));
	}
	
	private static synchronized void createWebSocketForPlayer(final Player player) {
	    Logger.debug("Create WebSocker for " + player);
	    WebSocket<String> ws = new WebSocket<String>() {
            public void onReady(WebSocket.In<String> in,final WebSocket.Out<String> out) {
                player.setOutStream(out);
                
                in.onClose(new Callback0() {
                    public void invoke() {
                        // Player has left. Remove player
                        Logger.debug("WebSocket of " + player + " is closed");
                        Application.removePlayer(player);
                    }
                });
                in.onMessage(new Callback<String>() {
                    public void invoke(String event) {
	                    // Log events to the console
                    	//getCurrentPlayer().setWantedGameName(event);
                    	Logger.info(player + " has sent gamename:" + event);   
                    	player.setWantedGameName(event);
                    } 
                });
                
            }
        };
        player.setWebSocket(ws);
	}
	
	private static synchronized void removePlayer(Player player) {
	    Logger.debug("remove " + player + " from playerMap");
        playerMap.remove(player.getId());
	    
	    if (!playerListIdle.remove(player)) {
	        Logger.debug(player + " gave up");
	        // playe who left wasn't idle, kick him out of match
	        player.giveUp();
	        Logger.debug("after give up");
	        Match match = player.getMatch();
	        Logger.debug("check if match is empty " + match);
	        if (match != null && match.isEmpty()) {
	            Logger.debug("remove empty match and inform idle Player");
	            Match removed = openMatches.remove(match.getId());
	            Logger.debug("removed " + removed);
	            Logger.debug("Number of open matches: " + openMatches.size());
	            informIdlePlayer();
	        }
	    }
	}
	
	public static WebSocket<String> socket() {
	    Player currentPlayer = getCurrentPlayer();
	    Logger.debug(currentPlayer + " asks for socket");
		return currentPlayer.getWebsocket();
	}
	
	private static Player getCurrentPlayer() {
	    String playerId;
	    try {
	        playerId = request().cookie(COOKIE_PLAYER_ID).value();
	    } catch (NullPointerException e)  {
	        return null;
	    }
	    Integer playerIdKey = Integer.parseInt(playerId);
	    return playerMap.get(playerIdKey);
	}
	
	private static Match getCurrentMatch() {
	    String matchID = null;
	    Cookie requestCookie= request().cookie(COOKIE_MATCH_ID);
	    if (requestCookie != null) {
	        matchID = requestCookie.value();
	    } else {
	        // first request, cookie wasn't set yet
	        for (Cookie cookie : response().cookies()) {
	            if (cookie.name() == COOKIE_MATCH_ID) {
	                matchID = cookie.value();
	                break;
	            }
	        }
	    }
        Match match = runningMatches.get(matchID);
        if (match == null) {
            match = openMatches.get(matchID);
        }
        return match;
	}
	
	private static boolean isPlayerOnTurn(Player player, Match match) {
	    boolean playerIsBlack = match.getHoster().equals(player);
	    boolean blackIsOnTurn = match.getGameController().isBlackTurn();
	    boolean playerOnTurn;
	    
	    if (playerIsBlack && blackIsOnTurn) {
	        playerOnTurn = true;
	    } else if (!playerIsBlack && !blackIsOnTurn) {
	        playerOnTurn = true;
	    } else {
	        playerOnTurn = false;
	    }
	    
	    return playerOnTurn;
    }
	
	
	public static Result loginSubmit(){
		DynamicForm dF = Form.form().bindFromRequest();
		Logger.debug(dF.get("username")+" logged in with password: "+dF.get("password"));
		
		if ((dF.get("username").equals("test") && dF.get("password").equals("123"))) {
			session("loggedIn","true");
			return gamecenter();
		}
		
		String url = getRedirectAction("Google2Client").getLocation();
		return ok(views.html.login.render(url));
	}

}
