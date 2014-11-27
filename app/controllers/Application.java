package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.Match;
import model.Player;
import play.api.mvc.Session;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Result;

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
    private static IGameController gameController;
	private static GameFrame gui;    
    private static TUI tui;
    private static Map<String, Match> openMatches = new HashMap<String, Match>();
    private static Map<String, Match> runningMatches = new HashMap<String, Match>();
    private static final String COOKIE_NAME = "CheckersID";
    
    public static Result sPlayGame() {
    	gameController = new GameController();
    	return playGame(8, true, Bot.SIMPLE_BOT.ordinal(), gameController);
    }
    
    public static Result gamecenter() {
    	return ok(views.html.gamecenter.render(openMatches));
    }
    
    public static Result join(String matchId) {
        // ToDO check for nullpointer Exception, looks like match == null
        System.out.println("join: " + matchId);
    	Match match = openMatches.remove(matchId);
    	System.out.println("foundc match: " + match);
    	Player joiner = new Player(request().remoteAddress(), 2);
    	match.join(joiner);
    	runningMatches.put(match.getId(), match);
    	
    	String id = generateId(match, joiner);
    	System.out.println("create cookie with: " + id);
    	setCookieID(id);
    	return playGame(8, true, 0, match.getGameController());
    }
    
    public static Result create(String type) {
    	// TODO generate Websocket and assign it to player
    	Match match = new Match(new GameController(), new Player(request().remoteAddress(), 1));
    	System.out.println("new Match = " + match.getId());
    	
    	String id = generateId(match, match.getHoster());
    	System.out.println("GameId = " + id);
    	
    	setCookieID(id);
    	Result result;
    	if (type.equals("Multi")) {
    		System.out.println("Open Match");
    		openMatches.put(match.getId(), match); 
    		result = renderPage(match.getGameController());
    	} else {
    		System.out.println("start Single");
    		runningMatches.put(match.getId(), match);
    		result = playGame(8, type.equals("Multi"), 0, match.getGameController());
    	}
    	
    	return result;
    }
    
    private static void setCookieID(String id) {
    	response().setCookie(COOKIE_NAME, id);
    }
    
    private static String generateId(Match match, Player player) {
    	StringBuilder buildId = new StringBuilder();
    	buildId.append(match.getId());
    	buildId.append("_");
    	buildId.append(player.getId());
    	
    	return buildId.toString();
    }
    
    public static Result refresh() {
        String checkersId = request().cookie(COOKIE_NAME).value();
        System.out.println("Refresh from " + checkersId);
        String[] ids = checkersId.split("_");
        Match match = runningMatches.get(ids[0]);
        if (match == null) {
            match = openMatches.get(ids[0]);
        }
        return renderPage(match.getGameController());
    }
    
    public static Result playGame(int size, boolean multiplayer, int difficulty, IGameController gameController) {
		Injector injector = Guice.createInjector(new CheckersModule());
    	//gameController = injector.getInstance(IGameController.class);
    	//gui = injector.getInstance(GameFrame.class);
    	//tui = injector.getInstance(TUI.class);
    	//gameController.gameInit(size, multiplayer, Bot.valueOf(difficulty));
    	StringBuilder buildInput = new StringBuilder();
    	buildInput.append(size);
    	if (multiplayer) {
    		buildInput.append(" M ");
    	} else {
    		buildInput.append(" S ");
    	}
    	buildInput.append(difficulty);
    	gameController.input(buildInput.toString());
    	return renderPage(gameController);
    }
    
    public static Result input(String move) {
    	String checkersId = request().cookie(COOKIE_NAME).value();
    	System.out.println("Input from " + checkersId);
    	String[] ids = checkersId.split("_");
	    Match match = runningMatches.get(ids[0]);
	    if ((match.getGameController().isBlackTurn() && ids[1].equals("1")) ||
	            (!match.getGameController().isBlackTurn() && ids[1].equals("2"))) {
	        System.out.println("Player is on Turn");
	        match.getGameController().input(move);
	    } else {
	        System.out.println("It's NOT your turn");
	    }
    	
    	return renderPage(match.getGameController());
    }
    
    private static Result renderPage(IGameController gameController) {
        String checkersId = request().cookie(COOKIE_NAME).value();
        String[] ids = checkersId.split("_");
        String player = ids[1];
        Result result;
        if (gameController.getCurrentState() == State.RUNNING) {
        	List<List<String>> data = updateData(gameController.getField().getField());
        	
        	String nextPlayer = "Wait for opponent.";
        	if ((gameController.isBlackTurn() && player.equals("1")) || 
        	        (!gameController.isBlackTurn() && player.equals("2"))) {
        		nextPlayer = "Your turn";
        	}
        	
        	String error = "";
        	if (gameController.getError() != null) {
        		error = gameController.getError();
        	}
        	
        	result = ok(views.html.playGame.render(data, data.size(), nextPlayer, error));
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
    
    public static Result rules(){
    	return ok(views.html.rules.render());
    }
    
    public static Result index(){
    	return ok(views.html.index.render());
    }
}
