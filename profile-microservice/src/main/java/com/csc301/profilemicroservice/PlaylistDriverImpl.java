package com.csc301.profilemicroservice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

  Driver driver = ProfileMicroserviceApplication.driver;

  public static void InitPlaylistDb() {
    String queryStr;

    try (Session session = ProfileMicroserviceApplication.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
        trans.run(queryStr);
        trans.success();
      }
      session.close();
    }
  }

  @Override
  public DbQueryStatus likeSong(String userName, String songId) {
    String queryStr;
    DbQueryStatus status = null;

    try (Session session = ProfileMicroserviceApplication.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        if (checkIfUserExists(userName) && checkIfSongIdExists(songId)) { // ensure both the user and song exist
          // check if user has already liked the song
          queryStr = String.format("MATCH (:playlist {plName:'%s-favorites'})"
              + "-[:includes]->(plSong:song {songId:'%s'}) RETURN plSong", userName, songId);
          StatementResult result = trans.run(queryStr);
          if (!result.hasNext()) { // if the song is not already in the user's favorites playlist, add it. otherwise, do nothing.
            queryStr = String.format("MATCH (pl:playlist {plName:'%s-favorites'}) "
                + "CREATE (pl)-[:includes]->(:song {songId:'%s'})", userName, songId);
            trans.run(queryStr);
            updateSongFavouritesCount(songId, false);
          }
          status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK); // song was either liked, or nothing happened
        } else { // the user, song, or both could not be found
          status = new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
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
  public DbQueryStatus unlikeSong(String userName, String songId) {
    String queryStr;
    DbQueryStatus status = null;

    try (Session session = ProfileMicroserviceApplication.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        if (checkIfUserExists(userName) && checkIfSongIdExists(songId)) { // ensure both the user and song exist
          // check if user has the song in their favorites
          queryStr = String.format("MATCH (:playlist {plName:'%s-favorites'})"
              + "-[:includes]->(plSong:song {songId:'%s'}) RETURN plSong", userName, songId);
          StatementResult result = trans.run(queryStr);
          if (result.hasNext()) { // if the song is already in the user's favorites playlist, unlike it.
            queryStr = String.format("MATCH (:playlist {plName:'%s-favorites'})-[:includes]->"
                + "(plSong:song {songId:'%s'}) DETACH DELETE plSong", userName, songId);
            trans.run(queryStr);
            updateSongFavouritesCount(songId, true);
            status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
          } else {
            status = new DbQueryStatus("Error", DbQueryExecResult.QUERY_ERROR_GENERIC); //user tried to unlike the song that cannot be unliked
          }
        } else { // the user, song, or both could not be found
          status = new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
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
  public DbQueryStatus deleteSongFromDb(String songId) {

    return null;
  }
  
  // HELPER FUNCTIONS
  private boolean checkIfSongIdExists(String songId) throws IOException {
      // communicate with SongMicroserviceApplication to check if there exists a song with _id songId in the MongoDB
      OkHttpClient client = new OkHttpClient();
      Map<String, Object> response = new HashMap<String, Object>();
    
      HttpUrl.Builder urlBuilder = 
      HttpUrl.parse("http://localhost:3001/getSongById/" + songId).newBuilder();
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
      return new JSONObject(serviceBody).get("status").equals("OK"); // if status of request is "OK", then song exists
  }
  
  private void updateSongFavouritesCount(String songId, Boolean shouldDecrement) throws IOException {
    // communicate with SongMicroserviceApplication to check if there exists a song with _id songId in the MongoDB
    OkHttpClient client = new OkHttpClient();
    Map<String, Object> response = new HashMap<String, Object>();
  
    HttpUrl.Builder urlBuilder = 
    HttpUrl.parse("http://localhost:3001/updateSongFavouritesCount/" + songId).newBuilder();
    urlBuilder.addQueryParameter("shouldDecrement", shouldDecrement.toString());
    String url = urlBuilder.build().toString();
    
    RequestBody body = RequestBody.create(null, new byte[0]);
    
    Request request = new Request.Builder()
        .url(url)
        .method("PUT", body)
        .build();
    
    Call call = client.newCall(request);
    call.execute();
  }
  
  private boolean checkIfUserExists(String userName) {
    String queryStr;
    
    try (Session session = ProfileMicroserviceApplication.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        queryStr = String.format("MATCH (user:profile {userName:'%s'}) RETURN user", userName); // match user with username: userName
        StatementResult result = trans.run(queryStr); 
        return result.hasNext(); // true if the result contains a record of the user, otherwise false
      }
    }
  }
}
