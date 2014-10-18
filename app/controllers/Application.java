package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import play.mvc.Controller;
import play.mvc.Result;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.htwg.checkers.CheckersModule;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.models.Cell;
import de.htwg.checkers.util.observer.Observer;

public class Application extends Controller {
    private static IGameController gameController;
	    
    public static Result sindex() {
    	return index(8, false, 0);
    }

    public static Result index(int size, boolean multiplayer, int difficulty) {
    	Injector injector = Guice.createInjector(new CheckersModule(size, multiplayer, difficulty));
    	gameController = injector.getInstance(IGameController.class);
    	gameController.gameInit();
    	
    	return renderPage();
    }
    
    public static Result input(String move) {
    	gameController.input(move);
    	
    	return renderPage();
    }
    
    private static Result renderPage() {
    	List<List<String>> data = updateData(gameController.getField().getField());
    	
    	String nextPlayer = "White";
    	if (gameController.isBlackTurn()) {
    		nextPlayer = "Black";
    	}
    	
    	String error = "";
    	if (gameController.getError() != null) {
    		error = gameController.getError();
    	}
    	
    	return ok(views.html.index.render(data, data.size(), nextPlayer, error));
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
}
