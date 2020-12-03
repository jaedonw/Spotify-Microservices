package com.csc301.profilemicroservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
    @Override
    public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
      String queryStr;
      DbQueryStatus status;
      boolean invalid = userName == null || fullName == null || password == null
          || userName.length() < 1 || fullName.length() < 1 || password.length() < 1; // checking for valid parameters
                                                                                      
      if (invalid) { // invalid parameters -> profile cannot be created
        status = new DbQueryStatus("Profile was unable to be created.", DbQueryExecResult.QUERY_ERROR_GENERIC);
      } else {
        try (Session session = ProfileMicroserviceApplication.driver.session()) {
          try (Transaction trans = session.beginTransaction()) {
            // check if profile already exists, if so return an error
            queryStr = String.format("MATCH (nProfile:profile {userName:'%s'}) RETURN nProfile", userName);
            StatementResult result = trans.run(queryStr);
            if (result.hasNext()) {
              status = new DbQueryStatus("Profile was unable to be created.", DbQueryExecResult.QUERY_ERROR_GENERIC);
            } else {
              // create a new profile with username: userName, full name: fullName, password: password
              // which is related to (created) a playlist with property plName: userName-favorites
              queryStr = String.format("CREATE (nProfile:profile {userName:'%s', fullName:'%s', "
                  + "password:'%s'})-[:created]->(nPlaylist:playlist {plName:'%s-favorites'}) RETURN nProfile", 
                  userName, fullName, password, userName);
              result = trans.run(queryStr);
              status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
            }
            trans.success();
          } catch (Exception e) { // general error catch
            status = new DbQueryStatus("Profile was unable to be created.", DbQueryExecResult.QUERY_ERROR_GENERIC);
          } 
          session.close();
         } catch (Exception e) { // general error catch
           status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
         }
      }
      return status;
    }

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
	  String queryStr;
	  DbQueryStatus status = null;
	  boolean invalid = userName.equals(frndUserName); // invalid if a user tries to follow themselves
      try (Session session = ProfileMicroserviceApplication.driver.session()) {
          try (Transaction trans = session.beginTransaction()) {
            queryStr = String.format("MATCH (user1:profile {userName:'%s'}), "
                + "(user2:profile {userName:'%s'}) RETURN user1, user2", userName, frndUserName);
            StatementResult result = trans.run(queryStr); // check if both users exist
            boolean dne = !result.hasNext() || result.next().size() != 2; // invalid if both users aren't returned
            
            queryStr = String.format("MATCH (user1:profile {userName:'%s'})"
                + "-[r:follows]->(user2:profile {userName:'%s'}) RETURN r", userName, frndUserName);
            result = trans.run(queryStr); // does user already follow friend?
            invalid = invalid || result.hasNext(); // invalid if the relationship already exists
            
            if (invalid) {
              status = new DbQueryStatus("Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
            } else if (dne) {
              status = new DbQueryStatus("One or more users not found.", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            } else {
              queryStr = String.format("MATCH (user1:profile),(user2:profile) " + 
                  "WHERE user1.userName = '%s' AND user2.userName = '%s' " + 
                  "CREATE (user1)-[:follows]->(user2)", userName, frndUserName);
              trans.run(queryStr);
              status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
            }
            trans.success();
          } catch (Exception e) { // general error catch
            status = new DbQueryStatus("Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
          }
          session.close();
      } catch (Exception e) { // general error catch
        status = new DbQueryStatus("Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      return status;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
	  String queryStr;
      DbQueryStatus status = null;
      boolean invalid = userName.equals(frndUserName); // invalid if a user tries to unfollow themselves
      try (Session session = ProfileMicroserviceApplication.driver.session()) {
          try (Transaction trans = session.beginTransaction()) {
            queryStr = String.format("MATCH (user1:profile {userName:'%s'}), "
                + "(user2:profile {userName:'%s'}) RETURN user1, user2", userName, frndUserName);
            StatementResult result = trans.run(queryStr); // check if both users exist
            boolean dne = !result.hasNext() || result.next().size() != 2; // invalid if both users aren't returned
            
            queryStr = String.format("MATCH (user1:profile {userName:'%s'})"
                + "-[r:follows]->(user2:profile {userName:'%s'}) RETURN r", userName, frndUserName);
            result = trans.run(queryStr); // does user follow friend?
            invalid = invalid || !result.hasNext(); // invalid if there is no relationship
            
            if (dne) {
              status = new DbQueryStatus("One or more users not found.", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            } else if (invalid) {
              status = new DbQueryStatus("Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
              
            } else {
              queryStr = String.format("MATCH (user1:profile {userName:'%s'})"
                  + "-[r:follows]->(user2:profile {userName:'%s'}) DELETE r", 
                  userName, frndUserName);
              trans.run(queryStr);
              status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
            }
            trans.success(); 
          } catch (Exception e) { // general error catch
            status = new DbQueryStatus("Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
          }
          session.close();
      } catch (Exception e) { // general error catch
        status = new DbQueryStatus("Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      return status;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
	  String queryStr;
	  Map<String, Object> userToSongs = new HashMap<String, Object>();
	  DbQueryStatus status = null;
	  List<Record> usernames;
	  
      try (Session session = ProfileMicroserviceApplication.driver.session()) {
          try (Transaction trans = session.beginTransaction()) {
              if (PlaylistDriverImpl.checkIfUserExists(userName)) { // ensure userName represents a valid user
                usernames = getUserFriendUsernames(userName); // get the usernames of all the people user follows
                for (Record record : usernames) { // iterate over the user's friends' usernames
                  String friendUserName = record.get(0).asString(); // get the friend's username as a string
                  String[] friendsSongs = getSongTitlesInUserPlaylist(friendUserName); //get the friend's favourite songs
                  userToSongs.put(friendUserName, friendsSongs); //map the friend's username to their favourite songs
                }
                status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
                status.setData(userToSongs);
              } else { // no user with username: userName, return error
                status = new DbQueryStatus("User not found.", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
              }
              trans.success();
          } catch (Exception e) { // general error catch
            status = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
          }
          session.close();
      } catch (Exception e) { // general error catch
        status = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      return status;
	}
	
	// HELPER FUNCTIONS
    private List<Record> getUserFriendUsernames(String userName) {
      String queryStr;
      List<Record> usernames;

      try (Session session = ProfileMicroserviceApplication.driver.session()) {
        try (Transaction trans = session.beginTransaction()) {
          queryStr = String.format("MATCH (:profile{userName:'%s'})-[:follows]->(r:profile) RETURN r.userName", userName);
          StatementResult result = trans.run(queryStr);
          usernames = result.list(); 
          trans.success();
        }
        session.close();
      }
      
      return usernames;
    }  
    
    private String[] getSongTitlesInUserPlaylist(String userName) throws IOException {
      String queryStr;
      List<String> songTitlesList = new ArrayList<String>();
      String[] songTitleArr = new String[songTitlesList.size()];

      try (Session session = ProfileMicroserviceApplication.driver.session()) {
        try (Transaction trans = session.beginTransaction()) {
          queryStr = String.format("MATCH (:playlist{plName:'%s-favorites'})-[:includes]->(s:song) RETURN s.songId", userName);
          StatementResult result = trans.run(queryStr);
          List<Record> songs = result.list(); 
          
          for (Record record : songs) { // iterate over the songId's
            String songId = record.get(0).asString();
            songTitlesList.add(getSongTitleById(songId));
          }
          
          trans.success();
        }
        session.close();
      }
      
      return songTitlesList.toArray(songTitleArr);
    }
    
    private String getSongTitleById(String songId) throws IOException {
      OkHttpClient client = new OkHttpClient();
    
      HttpUrl.Builder urlBuilder = 
      HttpUrl.parse("http://localhost:3001/getSongTitleById/" + songId).newBuilder();
      String url = urlBuilder.build().toString();
      
      Request request = new Request.Builder()
          .url(url)
          .method("GET", null)
          .build();
      
      Call call = client.newCall(request);
      Response responseFromCall = null;

      String serviceBody = "{}";
      
      responseFromCall = call.execute();
      serviceBody = responseFromCall.body().string();
      return (String) new JSONObject(serviceBody).get("data");
    }
}
