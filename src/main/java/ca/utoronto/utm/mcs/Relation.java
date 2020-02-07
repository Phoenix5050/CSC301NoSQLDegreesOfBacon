package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Session;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Relation implements HttpHandler
{
    private static Memory memory;

    public Relation(Memory mem) {
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
    
    public void handlePut(HttpExchange r) throws IOException, JSONException{
        /* TODO: Implement this.
           Hint: This is very very similar to the get just make sure to save
                 your result in memory instead of returning a value.*/
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String movieId = null;
        String actorId = null;

        if (deserialized.has("movieId") && deserialized.has("actorId"))
        {
        	movieId = deserialized.getString("movieId");
        	actorId = deserialized.getString("actorId");
        }
        // if query doesn't have these, it's improperly formatted or missing info
        else
        	r.sendResponseHeaders(400, -1);
        
        /* TODO: Implement the logic */
    	try
		{    		
    		//start the session which uses driver imported from app.java
    		Session s = App.driver.session();
    		//to do:
    		// add get for actor id and movie id and if one does not exist, throw 404 not found error
    		
    		//create cypher query
    		String command = "MATCH (a:Actor), (m:Movie) WHERE (\"" + movieId + "\"=m.movieId AND \"" + actorId + "\"=a.actorId) CREATE (a)-[:ActedIn]->(m);";
			//write/run cypher query
    		System.out.println(command);
			s.writeTransaction(tx -> tx.run(command));
			//successful so return 200
			r.sendResponseHeaders(200, -1);
        } catch (Exception e){
        	//something went wrong so 500
        	r.sendResponseHeaders(500, -1);
        } finally {
        	//this is just filler since we don't need to do anything in both success and failure states
        }            
    }

    public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        long first = memory.getValue();
        long second = memory.getValue();

        if (deserialized.has("firstNumber"))
            first = deserialized.getLong("firstNumber");

        if (deserialized.has("secondNumber"))
            second = deserialized.getLong("secondNumber");

        /* TODO: Implement the math logic */
        long answer = first - second;

        String response = Long.toString(answer) + "\n";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
