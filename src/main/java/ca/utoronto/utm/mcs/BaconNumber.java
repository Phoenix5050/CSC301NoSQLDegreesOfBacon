package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class BaconNumber implements HttpHandler
{
    private static boolean validActor;
    private static Memory memory;

    public BaconNumber(Memory mem) {
        memory = mem;
    }

    public void handle(HttpExchange r) {
        try {
        	validActor = false;
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
	        
	        // check if bacon himself
	        if (id.equals("nm0000102")) {
	        	String ret = "{\"baconNumber\": \"0\"}";
				r.sendResponseHeaders(200, ret.length());
		        OutputStream os = r.getResponseBody();
		        os.write(ret.getBytes());
		        os.close();
	        }
	
	        /* TODO: Implement the logic */
	        try
			{    	     	
	        	// start session
	    		Session s = App.driver.session();
	    		// create query
	    		//MATCH path=shortestPath((station_44:STATION {id:44})-[*0..10]-(station_46:STATION {id:46}))
	    		//RETURN path
	    		// kevin bacon ID nm0000102
	    		String command = "MATCH p=shortestPath((bacon:Actor {actorId:\"nm0000102\"})-[*]-(meg:Actor {actorId:\"" + id + "\"}))  RETURN p";
	    		// read this time instead of write
				StatementResult result = s.readTransaction(tx -> tx.run(command));	
	
	    		if (!validActor) {
	        		// query for actors of actorId
	    			s.writeTransaction(tx -> tx.run(command));
					if (!result.hasNext()) {
		    			//error 404 actor not found
		    			r.sendResponseHeaders(404, -1);
					} else {
						validActor = true;
					}
	    		}
	
	    		if (validActor) {
					String baconNum = result.next().toString();
					// count number of commas (one for every connection from actor to movie or movie to actor)
					// add one to make even number
					// divide by two to get bacon number
					int num = baconNum.replaceAll("[^,]","").length();
					num=(num+1)/2;
					if (num > 6 || num < 0) {
						r.sendResponseHeaders(404, -1);
					} else {
						String ret = "{\"baconNumber\": \"" + num + "\"}";
						r.sendResponseHeaders(200, ret.length());
				        OutputStream os = r.getResponseBody();
				        os.write(ret.getBytes());
				        os.close();
					}
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
