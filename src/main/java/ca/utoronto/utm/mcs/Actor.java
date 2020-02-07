package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Value;
import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Actor implements HttpHandler
{
    private static Memory memory;

    public Actor(Memory mem) {
        memory = mem;
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String first = memory.getString();
        String second = memory.getString();

        if (deserialized.has("actorId"))
            first = deserialized.getString("actorId");
        if (deserialized.has("name"))
            second = deserialized.getString("name");

        /* TODO: Implement the logic */
        System.out.println(first);
        String response = first + "\n";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public void handlePut(HttpExchange r) throws IOException, JSONException{
        /* TODO: Implement this.
           Hint: This is very very similar to the get just make sure to save
                 your result in memory instead of returning a value.*/
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String id = null;
        String name = null;

        if (deserialized.has("actorId"))
        	id = deserialized.getString("actorId");
        else
        	r.sendResponseHeaders(200, -1);
        if (deserialized.has("name"))
            name = deserialized.getString("name");
        else
        	r.sendResponseHeaders(400, -1);
        // should not have to worry about extra data since only checks body for these two keys
        
        /* TODO: Implement the logic */
		try 
		{
			Session s = App.driver.session();
			Transaction t = s.beginTransaction();
			String command = "CREATE (a:actor {Name: " + name + ", id: " + id + "})";
			StatementResult result = t.run(command);
			r.sendResponseHeaders(200, -1);
        } catch (Exception e){
        	r.sendResponseHeaders(500, -1);
        } finally {
        	App.driver.close();
        }            
        
		/*
         * check format to see if matches following format, else throw 400 BAD REQUEST
         * name : string
         * actorId: string
         * match(p:person {name : "String" )...
         * after match should call statement result r
         */
        
        /*
         * check to see if actor already exists in database since each 500 INTERNAL SERVER ERROR
         * if (actor already exists or other error)...
         */
        
        /*
         * If everything functioned, send 200 OK
         */
        
    }
}
