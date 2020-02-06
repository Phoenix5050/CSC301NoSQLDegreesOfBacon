package ca.utoronto.utm.mcs;
import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
//import org.neo4j.driver.v1.Result;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

public class App 
{
    static int PORT = 8080;
    
    private static Driver driver;
    
    public static void Login(String uri){
        driver = GraphDatabase.driver(uri, AuthTokens.basic("neo4j", "1234"));
    }
    
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        Memory mem = new Memory();
        server.createContext("/api/v1/addActor", new Actor(mem));
        server.createContext("/api/v1/addMovie", new Movie(mem));
        server.createContext("/api/v1/addRelationship", new Relation(mem));
        server.createContext("/api/v1/getActor", new Actor(mem));
        server.createContext("/api/v1/getMovie", new Movie(mem));
        server.createContext("/api/v1/hasRelationship", new Relation(mem));
        server.createContext("/api/v1/computeBaconNumber", new BaconNumber(mem));
        server.createContext("/api/v1/computeBaconPath", new BaconPath(mem));
        
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}