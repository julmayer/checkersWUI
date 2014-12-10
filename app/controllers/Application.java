package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jdk.nashorn.internal.ir.RuntimeNode.Request;
import model.Match;
import model.Player;
import play.api.mvc.Session;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.*;
import play.mvc.WebSocket.*;
import play.libs.F.Callback;
import play.libs.F.Callback0;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.htwg.checkers.CheckersModule;
import de.htwg.checkers.controller.GameController;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.controller.State;
import de.htwg.checkers.controller.bot.Bot;
import de.htwg.checkers.models.Cell;
import de.htwg.checkers.util.observer.Observer;
import de.htwg.checkers.view.gui.GameFrame;
import de.htwg.checkers.view.tui.TUI;

public class Application extends Controller {
	private static GameFrame gui;
	private static TUI tui;
	private static Map<String, Match> openMatches = new HashMap<String, Match>();
	private static Map<String, Match> runningMatches = new HashMap<String, Match>();
	private static final String COOKIE_MATCH_ID = "CheckersMatchID";
	private static final String COOKIE_PLAYER_ID = "CheckersPlayerID";
	private static int playerCount = 0;
	private static Map<Integer, Player> playerMap = new HashMap<>();

//	public static Result sPlayGame() {
//		gameController = new GameController();
//		return playGame(8, true, Bot.SIMPLE_BOT.ordinal(), gameController);
//	}

	public synchronized static Result gamecenter() {
		String count = String.valueOf(playerCount);
		setCookieID(COOKIE_PLAYER_ID,count);
		Player player = new Player(playerCount, request().remoteAddress());
		playerMap.put(playerCount, player);
		playerCount++;
		return ok(views.html.gamecenter.render(openMatches));
	}

	public static Result join(String matchId) {
		System.out.println("join: " + matchId);
		Match match = openMatches.remove(matchId);
		System.out.println("foundc match: " + match);
		
		String playerID = request().cookie(COOKIE_PLAYER_ID).value();
		
		//Player joiner = new Player(request().remoteAddress(), 2);
		match.join(playerMap.get(Integer.parseInt(playerID)));
		
		runningMatches.put(match.getId(), match);

		System.out.println("create cookie with: " + matchId);
		setCookieID(COOKIE_MATCH_ID,matchId);
		return playGame(8, true, 0, match);
	}

	public static Result create(String type) {
		
		String playerID = request().cookie(COOKIE_PLAYER_ID).value();
		//playerList.get(Integer.parseInt(playerID));
		
		Match match = new Match(new GameController(), playerMap.get(Integer.parseInt(playerID)));
		System.out.println("new Match = " + match.getId());

		String id = match.getId();
		System.out.println("GameId = " + id);

		setCookieID(COOKIE_MATCH_ID,id);
		
		Result result;
		if (type.equals("Multi")) {
			System.out.println("Open Match");
			openMatches.put(match.getId(), match);
			result = renderPage(match);
		} else {
			System.out.println("start Single");
			runningMatches.put(match.getId(), match);
			result = playGame(8, false, 0, match);
		}

		return result;
	}

	private static void setCookieID(String cookie, String id) {
		response().setCookie(cookie, id);
	}

	public static Result refresh() {
		String matchID = request().cookie(COOKIE_MATCH_ID).value();
		System.out.println("Refresh from " + matchID);
		Match match = runningMatches.get(matchID);
		if (match == null) {
			match = openMatches.get(matchID);
		}
		return renderPage(match);
	}

	public static Result playGame(int size, boolean multiplayer,
		int difficulty, Match currentMatch) {
		Injector injector = Guice.createInjector(new CheckersModule());
		StringBuilder buildInput = new StringBuilder();
		buildInput.append(size);
		if (multiplayer) {
			buildInput.append(" M ");
		} else {
			buildInput.append(" S ");
		}
		buildInput.append(difficulty);
		currentMatch.getGameController().input(buildInput.toString());
		return renderPage(currentMatch);
	}

	public static Result input(String move) {
		String matchId = request().cookie(COOKIE_MATCH_ID).value();
		String playerId = request().cookie(COOKIE_PLAYER_ID).value();
		System.out.println("Input from " + playerId);
		Match match = runningMatches.get(matchId);
		Player player = playerMap.get(Integer.parseInt(playerId));
		
		if ((match.getGameController().isBlackTurn() && match.getHoster().equals(player) )
				|| (!match.getGameController().isBlackTurn() && match.getJoiner().equals(player))) {
			System.out.println("Player is on Turn");
			match.getGameController().input(move);
		} else {
			System.out.println("It's NOT your turn");
		}

		return renderPage(match);
	}

	private static Result renderPage(Match currentMatch) {
		Result result;
		IGameController gameController = currentMatch.getGameController();
		if (gameController.getCurrentState() == State.RUNNING) {
			String playerId = request().cookie(COOKIE_PLAYER_ID).value();
			Player player = playerMap.get(Integer.parseInt(playerId));
			
			List<List<String>> data = updateData(gameController.getField()
					.getField());

			String nextPlayer = "Wait for opponent.";
			if ((gameController.isBlackTurn() && currentMatch.getHoster().equals(player) )
					|| (!gameController.isBlackTurn() && currentMatch.getJoiner().equals(player))) {
				nextPlayer = "Your turn";
			}

			String error = "";
			if (gameController.getError() != null) {
				error = gameController.getError();
			}

			result = ok(views.html.playGame.render(data, data.size(),
					nextPlayer, error));
		} else {
			result = ok(views.html.waitGame.render());
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
		
		String playerID = request().cookie(COOKIE_PLAYER_ID).value();
		System.out.println("Websocket from PlayerID:" + playerID);
		
		return playerMap.get(Integer.parseInt(playerID)).getWebsocket();
		//return playerList.get(Integer.parseInt(playerID)).getWebsocket();
		
	}
	
	/*private static boolean isYourTurn(){
		//todo
		return null;
	}*/
}
