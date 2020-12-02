package com.csc301.songmicroservice;

import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.mongodb.client.result.DeleteResult;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
	  Song returned = null;
	  DbQueryStatus status;
	  try {
		returned = db.insert(songToAdd);
		status = new DbQueryStatus("Song added successfully.", DbQueryExecResult.QUERY_OK);
	  } catch (Exception e) {
	    status = new DbQueryStatus("Song could not be added.", DbQueryExecResult.QUERY_ERROR_GENERIC);
	  }
	  status.setData(returned.getJsonRepresentation());
      return status;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		Song returned = null;
		DbQueryStatus status;
		try {
		  returned = db.findById(new ObjectId(songId), Song.class, "songs");
		  if (returned != null) { // status: song found
		    status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		    status.setData(returned.getJsonRepresentation());
		  } else { // status: song not found
		    status = new DbQueryStatus("Song not found.", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		  }
		} catch (Exception e) { // status: something went wrong
		  status = new DbQueryStatus("Error finding song by id.", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
        return status;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		DbQueryStatus status = findSongById(songId);
		if (status.getData() != null) { // song found
		  status.setData(((Map<?,?>)status.getData()).get("songName"));
		}
		return status;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
	    DbQueryStatus status;
	    try {
	      DeleteResult res = db.remove(new Query(Criteria.where("_id").is(songId)), Song.class);
	      if (res.getDeletedCount() == 1) {
	        status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	      } else {
	        status = new DbQueryStatus("Song was unable to be deleted", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
	      }
	    } catch (Exception e) {
	      status = new DbQueryStatus("Song was unable to be deleted.", DbQueryExecResult.QUERY_ERROR_GENERIC);
	    }
		return status;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		return null;
	}
}