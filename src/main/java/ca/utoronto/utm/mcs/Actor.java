package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
//import org.neo4j.driver.v1.Result;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

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
    	try {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String first = memory.getString();
        String second = memory.getString();

        if (deserialized.has("actorId"))
            first = deserialized.getString("actorId");
        if (deserialized.has("name"))
            second = deserialized.getString("name");

        /* TODO: Implement the logic */
        try (Session session = org.neo4j.driver.v1.Driver.session())
        {
        	
        }
        
        // write into a try catch 
        // catch block is a 500
               
        
        //memory.setString(first);
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
        r.sendResponseHeaders(200, -1);
    }

	private void Print(String first) {
		// TODO Auto-generated method stub
		
	}
}
