package controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.htwg.checkers.CheckersModule;
import de.htwg.checkers.controller.GameController;
import de.htwg.checkers.controller.IGameController;
import de.htwg.checkers.util.observer.Observer;
import de.htwg.checkers.view.tui.TUI;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller implements Observer {
    private static IGameController gameController;
	    
    public static Result index(int size, boolean multiplayer, int difficulty) {
    	Injector injector = Guice.createInjector(new CheckersModule(size, multiplayer, difficulty));
    	gameController = injector.getInstance(IGameController.class);
    	gameController.gameInit();
    	
    	return ok(views.html.index.render(gameController.getField().toString()));
    }
    
    /*public Application(IGameController gameController) {
		this.gameController = gameController;
		this.gameController.gameInit();
		this.gameController.addObserver(this);
	}*/
    
    public static Result input(String move) {
    	gameController.input(move);
    	return ok(views.html.index.render(gameController.getField().toString()));    	
    }

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}
    
}
