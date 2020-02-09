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
    private static boolean validActor;
    private static Memory memory;

    public Actor(Memory mem) {
        memory = mem;
    }

    public void handle(HttpExchange r) {
        try {
        	validActor = false;
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
    	try {
    		String body = Utils.convert(r.getRequestBody());
    		JSONObject deserialized = new JSONObject(body);
    		
	        String id = null;
	        String name = null;
	        
	        // should not have to worry about extra data since only checks body for these two keys
	    	if (deserialized.has("actorId") && deserialized.has("name"))
	        {
	        	id = deserialized.getString("actorId");
	        	name = deserialized.getString("name");
	        }
	        else // if query doesn't have these, it's improperly formatted or missing info
	        	r.sendResponseHeaders(400, -1);
	        
	        try
			{
	    		//start the session which uses driver imported from app.java
	    		Session s = App.driver.session();
	    		// check for existing actor in database
//	    		String readCommand = "MATCH (a:Actor {actorId: \"" + id + "\"})-[r]-(b) RETURN a.name, b.movieId;";
	    		String readCommand = "MATCH (a:Actor) WHERE (a.actorId)=\"" + id + "\" RETURN a;";
				StatementResult result = s.readTransaction(tx -> tx.run(readCommand));
				if (result.hasNext()) {
					//actor already in database
		        	System.out.println("[gm] next: " + result.next().toString());
		        	System.out.println("[gm] ERROR 400: Actor already in database!");
					r.sendResponseHeaders(400, -1);
				} else {
					//create cypher query
					String writeCommand = "CREATE (:Actor {name: \"" + name + "\", actorId:\"" + id + "\"});";
					//write/run cypher query
					s.writeTransaction(tx -> tx.run(writeCommand));
					//successful so return 200
					r.sendResponseHeaders(200, -1);
				}
	        } catch (Exception e){
	        	//something went wrong so 500
	        	r.sendResponseHeaders(500, -1);
	        } finally {
	        	//this is just filler since we don't need to do anything in both success and failure states
	        }
    	} catch (Exception e) {
        	//bad request 
        	r.sendResponseHeaders(400, -1);
    	} finally {
    		//filler
    	}
    }

    public void handleGet(HttpExchange r) throws IOException, JSONException {
    	try {
	        String body = Utils.convert(r.getRequestBody());
	        JSONObject deserialized = new JSONObject(body);
	        
	        String id = memory.getString();
	        
	        if (deserialized.has("actorId"))
	            id = deserialized.getString("actorId");
	        else
	        	r.sendResponseHeaders(400, -1);
	        
	        try
			{
	        	// start session
	    		Session s = App.driver.session();
	    		String readCommand = "MATCH (a:Actor) WHERE (a.actorId)=\"" + id + "\" RETURN a.name;";
	    		// read this time instead of write
	    		StatementResult readResult = s.readTransaction(tx -> tx.run(readCommand));
				
	    		if (!validActor) {
	        		// query for actors of actorId
	    			s.writeTransaction(tx -> tx.run(readCommand));
					if (!readResult.hasNext()) {
		    			//error 404 actor not found
		    			r.sendResponseHeaders(404, -1);
					} else
						validActor = true;
	    		}
	    		
	    		if (validActor) {
		    		// create query
		    		String writeCommand = "MATCH (a:Actor {actorId: \"" + id + "\"})-[r]-(b) RETURN a.name, b.movieId;";
		    		// read this time instead of write
					StatementResult writeResult = s.readTransaction(tx -> tx.run(writeCommand));
					
					Boolean first = true;
					String name = null;
					String ret = null;			
					// result is all the matches we got, iterate through while there are still matches
					if (!writeResult.hasNext()) {
						String line = readResult.next().toString();
						name = line.substring(17);
						name = name.split("\"")[0];
						ret = "\"actorId\": \"" + id + "\", \"name\": \"" + name +"\", \"movies\": []";
					} else while (writeResult.hasNext()) {
						String line = writeResult.next().toString();
						if (first) {
							first = false;
							name = line.substring(17);
							name = name.split("\"")[0];
							ret = "\"actorId\": \"" + id + "\", \"name\": \"" + name +"\", \"movies\": [";
						}
						
						int cut = name.length()+28;
						String movie = line.substring(cut);
						movie = movie.split("\"")[1];
						if (!ret.contains(movie)) {
							ret=ret+"\""+movie+"\", ";
						}
					}

					ret=ret.substring(0, ret.length() - 2);
					ret="{"+ret+"]}";
					// everything worked correctly			
					r.sendResponseHeaders(200, ret.length());
			        OutputStream os = r.getResponseBody();
			        os.write(ret.getBytes());
			        os.close();
	    		}
	        } catch (Exception e){
	        	//error
	        	r.sendResponseHeaders(500, -1);
	        } finally {
	        	//filler
	        }
        } catch (Exception e) {
        	//bad request 
        	r.sendResponseHeaders(400, -1);
    	} finally {
    		//filler
    	}
    }
}
