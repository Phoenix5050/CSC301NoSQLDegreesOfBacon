package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Movie implements HttpHandler
{
    private static boolean validMovie;
    private static Memory memory;

    public Movie(Memory mem) {
        memory = mem;
    }

    public void handle(HttpExchange r) {
        try {
        	validMovie = false;
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
	        if (deserialized.has("movieId") && deserialized.has("name"))
	        {
	        	id = deserialized.getString("movieId");
	        	name = deserialized.getString("name");
	        }
	        else // if query doesn't have these, it's improperly formatted or missing info
	        	r.sendResponseHeaders(400, -1);
	        
	    	try
			{
	    		//start the session which uses driver imported from app.java
	    		Session s = App.driver.session();
	    		// check for existing actor in database
//	    		String readCommand = "MATCH (a:Movie {movieId: \"" + id + "\"})-[r]-(b) RETURN a.name, b.actorId;";
	    		String readCommand = "MATCH (m:Movie) WHERE (m.movieId)=\"" + id + "\" RETURN m;";
				StatementResult result = s.readTransaction(tx -> tx.run(readCommand));
				if (result.hasNext()) {
					//actor already in database
		        	System.out.println("[gm] next: " + result.next().toString());
		        	System.out.println("[gm] ERROR 400: Movie already in database!");
					r.sendResponseHeaders(400, -1);
				} else {
		    		//create cypher query
		    		String writeCommand = "CREATE (:Movie {name: \"" + name + "\", movieId:\"" + id + "\"});";
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
	
	        if (deserialized.has("movieId"))
	            id = deserialized.getString("movieId");
	        else
	        	r.sendResponseHeaders(400, -1);
	        
	        try
			{    	        	
	        	// start session
	    		Session s = App.driver.session();
	    		String readCommand = "MATCH (m:Movie) WHERE (m.movieId)=\"" + id + "\" RETURN m.name;";
	    		// read this time instead of write
				StatementResult readResult = s.readTransaction(tx -> tx.run(readCommand));
				
	    		if (!validMovie) {
	        		// query for movies of movieId
	    			s.writeTransaction(tx -> tx.run(readCommand));
					if (!readResult.hasNext()) {
		    			//error 404 movie not found
		    			r.sendResponseHeaders(404, -1);
					} else
						validMovie = true;
	    		}
	    		
	    		if (validMovie) {
		    		// create query
		    		String writeCommand = "MATCH (a:Movie {movieId: \"" + id + "\"})-[r]-(b) RETURN a.name, b.actorId;";
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
						ret = "\"actorId\": \"" + id + "\", \"name\": \"" + name +"\", \"actors\": []";
					} else while (writeResult.hasNext()){
						String line = writeResult.next().toString();
						if (first) {
							first = false;
							name = line.substring(17);
							name = name.split("\"")[0];
							ret = "\"actorId\": \"" + id + "\", \"name\": \"" + name +"\", \"actors\": [";
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
