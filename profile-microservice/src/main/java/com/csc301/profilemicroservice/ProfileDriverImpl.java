package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
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
          } catch (Exception e) {
            status = new DbQueryStatus("Profile was unable to be created.", DbQueryExecResult.QUERY_ERROR_GENERIC);
          } 
          session.close();
         } catch (Exception e) {
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
              status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
            } else if (dne) {
              status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            } else {
              queryStr = String.format("MATCH (user1:profile),(user2:profile) " + 
                  "WHERE user1.userName = '%s' AND user2.userName = '%s' " + 
                  "CREATE (user1)-[:follows]->(user2)", userName, frndUserName);
              trans.run(queryStr);
              status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
            }
            trans.success();
          } catch (Exception e) {
            status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
          }
          session.close();
      } catch (Exception e) {
        status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      return status;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		return null;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
			
		return null;
	}
}
