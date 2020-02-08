package ca.utoronto.utm.mcs;

import java.io.IOException;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Value;
import static org.neo4j.driver.v1.Values.parameters;
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
    
    public void handlePut(HttpExchange r) throws IOException, JSONException{
        /* TODO: Implement this.
           Hint: This is very very similar to the get just make sure to save
                 your result in memory instead of returning a value.*/
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String id = null;
        String name = null;

        if (deserialized.has("actorId") && deserialized.has("name"))
        {
        	id = deserialized.getString("actorId");
        	name = deserialized.getString("name");
        }
        // if query doesn't have these, it's improperly formatted or missing info
        else
        	r.sendResponseHeaders(400, -1);
        
        // should not have to worry about extra data since only checks body for these two keys
        
        /* TODO: Implement the logic */
    	try
		{
    		/*
    		 * old code ignore
				Session s = App.driver.session();
				Transaction t = s.beginTransaction();
				//String command = "CREATE (:Actor {name: \" + name + \", actorId:\" + id + \"});";
				StatementResult result = t.run("CREATE (:Actor {name: \" $a \", actorId:\" $b \"});", parameters("a", name, "b", id));
				
				//use StatementResult result = t.run(command) for getting
			
			 * "CREATE (a:Greeting) " + "SET a.message = $message " + "RETURN a.message + ', from node ' + id(a)", parameters( "message", message ) 
			 */
    		
    		//start the session which uses driver imported from app.java
    		Session s = App.driver.session();
    		//create cypher query
			String command = "CREATE (:Actor {name: \"" + name + "\", actorId:\"" + id + "\"});";
			//write/run cypher query
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

        String id = memory.getString();

        if (deserialized.has("actorId"))
            id = deserialized.getString("actorId");
        else
        	r.sendResponseHeaders(400, -1);

        /* TODO: Implement the logic */
        try
		{    	
        	/* old code ignore
        	 * //Transaction t = s.beginTransaction();
				//String command = "MATCH (a:Actor {actorId:\"" + id + "\"}) RETURN a;";
	    		//StatementResult result = t.run(command);
				// shortest path method in neo4j to calculate degrees of bacon
				// count # of movies to return number
        	 */
        	
        	// start session
    		Session s = App.driver.session();
    		// create query
    		String command = "MATCH (a:Actor {actorId: \"" + id + "\"})-[r]-(b) RETURN  a.name, b.name;";
    		// read this time instead of write
			StatementResult result = s.readTransaction(tx -> tx.run(command));		
			
			Boolean first = true;
			String name=null;
			String ret=null;			
			// result is all the matches we got, iterate through while there are still matches
			while (result.hasNext()){
				String line = result.next().toString();
				if (first==true) {
					first=false;
					name=line.substring(17);
					name=name.split("\"")[0];
					ret = "\"actorId\": \"" + id + "\", \"name\": \"" + name +"\", \"movies\": [";
				}
				
				int cut = name.length()+28;
				String movie = line.substring(cut);
				movie = movie.split("\"")[1];
				ret=ret+"\""+movie+"\", ";
			}
			ret=ret.substring(0, ret.length() - 2);
			ret=ret+"]";
			
			// everything worked correctly			
			r.sendResponseHeaders(200, ret.length());
	        OutputStream os = r.getResponseBody();
	        os.write(ret.getBytes());
	        os.close();
        } catch (Exception e){
        	//error
        	r.sendResponseHeaders(500, -1);
        } finally {
        	//filler
        }       
    }
}
