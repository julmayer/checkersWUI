import org.pac4j.core.client.Clients;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.play.Config;

import play.Application;
import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {

    @Override
    public void onStart(final Application app) {
      Logger.info("Application has started");
      // OAuth
      final Google2Client googleClient = new Google2Client(
              "564983107101-n3uk0vplk9fup152ff527dkugmgfo4ph.apps.googleusercontent.com",
              "w1NIxGY1q7WjIl5EaP2Rst7V");

      final Clients clients = new Clients("https://htwg-checkers.herokuapp.com/callback", googleClient);
      Config.setClients(clients);
    }  
    
    @Override
    public void onStop(Application app) {
      Logger.info("Application shutdown...");
    }  
      
}