package net.limework.core.managers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.limework.core.LimeworkSpigotCore;
import org.bukkit.configuration.Configuration;

public class DataSourcesManager {

    private MongoClient mongoClient;


    public DataSourcesManager(LimeworkSpigotCore plugin) {
        Configuration config = plugin .getConfig();
        if (config.getBoolean("Mongodb.enabled")){
            mongoClient = MongoClients.create(config.getString("Mongodb.url"));
        }
    }

    public void shutdown(){
        mongoClient.close();
    }



    public MongoClient getMongoClient() {
        return mongoClient;
    }
}
