package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import model.Match;
import model.Player;
import play.mvc.Controller;
import play.mvc.Http.Cookie;
import play.mvc.Result;
import play.mvc.WebSocket;
import de.htwg.checkers.controller.GameController;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.controller.State;
import de.htwg.checkers.models.Cell;

public class Application extends Controller {
	private static Map<String, Match> openMatches = new HashMap<String, Match>();
	private static Map<String, Match> runningMatches = new HashMap<String, Match>();
	private static final String COOKIE_MATCH_ID = "CheckersMatchID";
	private static final String COOKIE_PLAYER_ID = "CheckersPlayerID";
	private static int playerCount = 0;
	private static Map<Integer, Player> playerMap = new HashMap<>();
	private static List<Player> playerListIdle = new LinkedList<>();

    public synchronized static Result gamecenter() {
        String playerId = String.valueOf(playerCount);
        setCookieID(COOKIE_PLAYER_ID, playerId);
        Player player = new Player(playerCount, request().remoteAddress());
        playerMap.put(playerCount, player);
        playerListIdle.add(player);
        playerCount++;
        return ok(views.html.gamecenter.render(openMatches));
    }

	public static Result join(String matchId) {
		System.out.println("join: " + matchId);
		Match match;
		synchronized (openMatches) {
		    match = openMatches.remove(matchId);
        }
		System.out.println("found match: " + match);
		
		Player joiner = getCurrentPlayer();
		System.out.println(joiner + " is joining game");
		match.join(joiner);
		
		synchronized (runningMatches) {
		    runningMatches.put(match.getId(), match);
        }

		System.out.println("create cookie with: " + matchId);
		setCookieID(COOKIE_MATCH_ID,matchId);
		playerListIdle.remove(joiner);
		return playGame(8, true, 0, match);
	}

	public static Result create(String type) {
		Player hoster = getCurrentPlayer();
		Match match = new Match(new GameController(), hoster);
		System.out.println("new Match = " + match.getId() + " hostet by " + hoster);

		setCookieID(COOKIE_MATCH_ID, match.getId());
		
		Result result;
		if (type.equals("Multi")) {
			System.out.println("Open Match");
			synchronized (openMatches) {
			    openMatches.put(match.getId(), match);
            }
			result = renderPage(match);
		} else {
			System.out.println("start Single");
			synchronized (runningMatches) {
			    runningMatches.put(match.getId(), match);
            }
			result = playGame(8, false, 0, match);
		}
		
		System.out.println("hoster: "+hoster);
				
		
		System.out.println(playerListIdle.remove(hoster));
		
		for (Player player : new LinkedList<Player>(playerListIdle)){
			System.out.println(player);
			player.reload("/play");
		}
		
		
		return result;
	}

	private static void setCookieID(String cookie, String id) {
		response().setCookie(cookie, id);
	}

	public static Result refresh() {
	    Match currentMatch = getCurrentMatch();
	    System.out.println("Refresh from " + getCurrentPlayer() + " for match " + currentMatch);
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

		System.out.println("Input from " + player + " for match " + match);
		
		if (isPlayerOnTurn(player, match)) {
		    System.out.println("Player is on Turn");
		    match.getGameController().input(move);
		} else {
		    System.out.println("It's NOT your turn");
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
		    result = ok(views.html.waitGame.render("Wait for creation of game"));
		    getCurrentPlayer().reload();
		} else if (gameController.getCurrentState() != State.RUNNING) {
		    result = ok(views.html.waitGame.render("Wait for other player"));
		} else if (gameController.checkIfWin()) {
		    runningMatches.remove(currentMatch);
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
	
	public static WebSocket<String> socket() {
	    Player currentPlayer = getCurrentPlayer();
		System.out.println("Websocket from Player:" + currentPlayer);
		
		return currentPlayer.getWebsocket();
	}
	
	private static Player getCurrentPlayer() {
	    String playerId = request().cookie(COOKIE_PLAYER_ID).value();
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
}
