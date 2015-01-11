import play.*;
import org.pac4j.core.client.Clients;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.play.Config;

public class Global extends GlobalSettings {

    @Override
    public void onStart(final Application app) {
      Logger.info("Application has started");
      // OAuth
      final Google2Client googleClient = new Google2Client("564983107101-7lgkkdcv4t5fq0g9c5h93deapu89elnm.apps.googleusercontent.com", "6MjDi0CU910R5o2xZalUiQyl");

      final Clients clients = new Clients("http://localhost:9000/callback", googleClient);
      Config.setClients(clients);
    }  
    
    @Override
    public void onStop(Application app) {
      Logger.info("Application shutdown...");
    }  
      
}