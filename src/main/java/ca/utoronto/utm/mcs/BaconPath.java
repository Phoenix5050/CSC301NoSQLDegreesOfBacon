package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class BaconPath implements HttpHandler
{
    private static Memory memory;

    public BaconPath(Memory mem) {
        memory = mem;
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        
        // check if bacon himself
        if (id.equals("nm0000102")) {
        	String ret = "{\"baconNumber\": \"0\" \"baconPath\":[]}";
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

			String baconNum= result.next().toString();
			// count number of commas (one for every connection from actor to movie or movie to actor)
			// add one to make even number
			// divide by two to get bacon number
			int num = baconNum.replaceAll("[^,]","").length();
			num=(num+1)/2;
			String ret = "{\"baconNumber\": \"" + num + "\" \"baconPath\":[";
			Boolean record = false;
			String currentId="";
			String prev = "";
			//iterate backwards through the return
			for (int i = baconNum.length() - 4; i >= 0; i--) {
				char letter = baconNum.charAt(i);
				if (String.valueOf(letter).equals(")")){
					record = true;
				}
				
				else if(String.valueOf(letter).equals("(")) {
					currentId=currentId.substring(1);
					String reverse = new StringBuffer(currentId).reverse().toString();
					String newCommand = "MATCH (s) WHERE ID(s) = " + reverse + " RETURN s.movieId, s.actorId";
					StatementResult newResult = s.writeTransaction(tx -> tx.run(newCommand));
					String thing = newResult.next().toString();
					String movieId = thing.substring(19);
					movieId = movieId.split("\\.", 3)[0];
					movieId = movieId.substring(0,movieId.length()-3);
					if (movieId.equals("NULL")) {
						String actorId = thing.substring(0,thing.length()-2);
						actorId=actorId.replaceAll(".+,", "");
						actorId=actorId.substring(12);
						if (prev.equals("")) {
							prev=actorId;
						}
						else {
							ret=ret+"{\"actorId\": " + actorId + ", \"movieId\": " + prev + "}, ";
							prev="";
						}
					}
					else {
						if (prev.equals("")) {
							prev=movieId;
						}
						else {
							ret=ret+"{\"actorId\": " + prev + ", \"movieId\": " + movieId + "}, ";
							prev="";
						}
					}
					currentId="";
					record = false;
				}
				if (record == true) {
					currentId=currentId+String.valueOf(letter);
				}
			}
			ret=ret.substring(0, ret.length() - 2)+"]}";
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
