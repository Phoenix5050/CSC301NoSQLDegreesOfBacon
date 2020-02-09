package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Relation implements HttpHandler
{
    private static boolean validActor, validMovie;
    private static Memory memory;

    public Relation(Memory mem) {
        memory = mem;
    }

    public void handle(HttpExchange r) {
        try {
        	validActor = false;
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
	        
	    	try
			{
	    		//start the session which uses driver imported from app.java
	    		Session s = App.driver.session();
	    		
	    		if (!validActor) {
	        		String actorCommand = "MATCH (a:Actor) WHERE a.actorId=\"" + actorId + "\" RETURN a.name;";
	        		// query for actors of actorId
	    			StatementResult actorResult = s.readTransaction(tx -> tx.run(actorCommand));
	    			s.writeTransaction(tx -> tx.run(actorCommand));
					if (!actorResult.hasNext()) {
		    			//error 404 actor not found
		    			r.sendResponseHeaders(404, -1);
					} else
						validActor = true;
	    		}
	    		
	    		if (validActor && !validMovie) {
	        		String movieCommand = "MATCH (m:Movie) WHERE m.movieId=\"" + movieId + "\" RETURN m.name;";
	        		// query for movies of movieId
	    			StatementResult movieResult = s.readTransaction(tx -> tx.run(movieCommand));
	    			s.writeTransaction(tx -> tx.run(movieCommand));
					if (!movieResult.hasNext()) {
		    			//error 404 movie not found
		    			r.sendResponseHeaders(404, -1);
					} else
						validMovie = true;
	    		}
	    		
	    		if (validActor && validMovie) {
	    			String readCommand = "MATCH (a:Actor {actorId: \"" + actorId + "\"})-[r]-(b) RETURN a.name, b.movieId;";
					s.writeTransaction(tx -> tx.run(readCommand));
					StatementResult readResult = s.readTransaction(tx -> tx.run(readCommand));
					while (readResult.hasNext()) {
						String line = readResult.next().toString();
						if (line.contains(movieId)) {
							//relationship already in database
				        	System.out.println("[gm] ERROR 400: Relationship already in database!");
							r.sendResponseHeaders(400, -1);
							break;
						}
					}
					
					if (!readResult.hasNext()) {
			    		//create cypher query
			    		String command = "MATCH (a:Actor), (m:Movie) WHERE (\"" + movieId + "\"=m.movieId AND \"" + actorId + "\"=a.actorId) CREATE (a)-[:ActedIn]->(m);";
						//write/run cypher query
			    		System.out.println(command);
						s.writeTransaction(tx -> tx.run(command));
						//successful so return 200
						r.sendResponseHeaders(200, -1);						
					}
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
	
	        String movieId = null;
	        String actorId = null;
	
	        if (deserialized.has("movieId") && deserialized.has("actorId"))
	        {
	        	movieId = deserialized.getString("movieId");
	        	actorId = deserialized.getString("actorId");
	        }
	        else
	        	r.sendResponseHeaders(400, -1);
	        
	        try
			{    	        	
	        	// start session
	    		Session s = App.driver.session();
	
	    		if (!validActor) {
	        		String actorCommand = "MATCH (a:Actor) WHERE a.actorId=\"" + actorId + "\" RETURN a.name;";
	        		// query for actors of actorId
//	    			s.writeTransaction(tx -> tx.run(actorCommand));
	    			StatementResult result = s.readTransaction(tx -> tx.run(actorCommand));
					if (!result.hasNext()) {
		    			//error 404 actor not found
		    			r.sendResponseHeaders(404, -1);
					} else {
						validActor = true;
					}
	    		}
	    		
	    		if (validActor && !validMovie) {
	        		String movieCommand = "MATCH (m:Movie) WHERE m.movieId=\"" + movieId + "\" RETURN m.name;";
	        		// query for movies of movieId
//	    			s.writeTransaction(tx -> tx.run(movieCommand));
	    			StatementResult result = s.readTransaction(tx -> tx.run(movieCommand));
					if (!result.hasNext()) {
		    			//error 404 movie not found
		    			r.sendResponseHeaders(404, -1);
					} else {
						validMovie = true;
					}
	    		}
	    		
	    		if (validActor && validMovie) {
		    		// create query
		    		//MATCH  (a:Actor {actorId: "2"}), (m:Movie {movieId: "007"}) 
		    		//RETURN EXISTS((a)-[:ActedIn]-(m))
		    		String command = "MATCH (a:Actor {actorId: \"" + actorId + "\"}), (m:Movie {movieId: \"" + movieId + "\"}) RETURN  EXISTS((a)-[:ActedIn]-(m));";
		    		// read this time instead of write
					StatementResult result = s.readTransaction(tx -> tx.run(command));	
					String TorF = result.next().toString();
					TorF = TorF.substring(36, TorF.length()-2);
					if (TorF.equals("TRUE")) {
						String ret = "{\"actorId\": \"" + actorId + "\", \"movieId\": \"" + movieId +"\", \"hasRelationship\": true}";
						r.sendResponseHeaders(200, ret.length());
				        OutputStream os = r.getResponseBody();
				        os.write(ret.getBytes());
				        os.close();
					}
					else {
						String ret = "{\"actorId\": \"" + actorId + "\", \"movieId\": \"" + movieId +"\", \"hasRelationship\": false}";
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
