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
		  returned = db.findById(new ObjectId(songId), Song.class);
		  if (returned != null) {
		    status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		  } else {
		    status = new DbQueryStatus("Song not found.", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		  }
	      status.setData(returned);
	      return status;
		} catch (Exception e) {
		  status = new DbQueryStatus("Error finding song by id.", DbQueryExecResult.QUERY_ERROR_GENERIC);
	      status.setData(returned);
	      return status;
		}
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		return null;
	}
}