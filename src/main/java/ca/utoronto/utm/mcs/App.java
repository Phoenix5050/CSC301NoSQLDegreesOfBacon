package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        Memory mem = new Memory();
        server.createContext("api/v1/addActor", new Actor(mem));
        server.createContext("/api/v1/addMovie", new Movie(mem));
        server.createContext("api/v1/addRelationship", new Relation(mem));
        server.createContext("/api/v1/getActor", new Actor(mem));
        server.createContext("/api/v1/getMovie", new Movie(mem));
        server.createContext("/api/v1/hasRelationship", new Relation(mem));
        server.createContext("/api/v1/computeBaconNumber", new Relation(mem));
        server.createContext("/api/v1/computeBaconPath", new Relation(mem));
        
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}