import play.*;
import org.pac4j.core.client.Clients;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.play.Config;

public class Global extends GlobalSettings {

    @Override
    public void onStart(final Application app) {
      Logger.info("Application has started");
      // OAuth
      final Google2Client googleClient = new Google2Client(
              "564983107101-9dab0njruqo53lrjvf9kvq50sjgrggi9.apps.googleusercontent.com",
              "NeaA_m9GY-ecQ3LJgIyawrnj");

      final Clients clients = new Clients("https://htwg-checkers.herokuapp.com/callback", googleClient);
      Config.setClients(clients);
    }  
    
    @Override
    public void onStop(Application app) {
      Logger.info("Application shutdown...");
    }  
      
}