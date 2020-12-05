package com.csc301.songmicroservice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicUpdate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.mongodb.client.result.DeleteResult;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

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
		returned = db.insert(songToAdd); // insert song
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
		  if (returned != null) { // if song found
		    status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		    status.setData(returned.getJsonRepresentation());
		  } else { // if song not found
		    status = new DbQueryStatus("Song not found.", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		  }
		} catch (Exception e) { // something went wrong
		  status = new DbQueryStatus("Error finding song by id.", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
        return status;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		DbQueryStatus status = findSongById(songId); // get the entire song
		if (status.getData() != null) { // song found
		  status.setData(((Map<?,?>)status.getData()).get("songName")); // extract songName
		}
		return status;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
	    DbQueryStatus status;
	    try {
	      deleteAllSongsFromDb(songId); // remove song from Neo4j database
	      DeleteResult res = db.remove(new Query(Criteria.where("_id").is(songId)), Song.class); // remove song from MongoDB
	      if (res.getDeletedCount() == 1) { // songId represented a valid song, which was deleted
	        status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	      } else { // nothing was deleted
	        status = new DbQueryStatus("Song was unable to be deleted", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
	      }
	    } catch (Exception e) {
	      status = new DbQueryStatus("Song was unable to be deleted.", DbQueryExecResult.QUERY_ERROR_GENERIC);
	    }
		return status;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
	    DbQueryStatus status = null;
	    try {
	      Song song = db.findById(new ObjectId(songId), Song.class, "songs"); // attempt to find the song
	      long favs = song.getSongAmountFavourites(); // attempt to get # of favourites
	      if (shouldDecrement) { // decrease # of song favourites by 1
	        if (favs > 0) { // execute decrement if # of favourites is not 0 alread 
	          song.setSongAmountFavourites(favs-1); 
	          db.save(song);
	          status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	        } else { 
	          status = new DbQueryStatus("Song favourite count is already 0.", DbQueryExecResult.QUERY_ERROR_GENERIC);
	        }
	      } else { // increase # of song favourites by 1
	        song.setSongAmountFavourites(favs+1);
	        db.save(song);
	        status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	      }
	    } catch (NullPointerException e) { //song wasn't found
	      status = new DbQueryStatus("Song could not be found.", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
	    } catch (Exception e) {
	      status = new DbQueryStatus("Song favourite count could not be updated.", DbQueryExecResult.QUERY_ERROR_GENERIC);
	    }
		return status;
	}
	
	// HELPER FUNCTIONS
	private void deleteAllSongsFromDb(String songId) throws IOException {
	  // send a request to ProfileMicroServiceApplication to delete matching songs from Neo4j DB 
	  OkHttpClient client = new OkHttpClient();

      HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3002/deleteAllSongsFromDb/" + songId).newBuilder();
      String url = urlBuilder.build().toString();

      RequestBody body = RequestBody.create(null, new byte[0]);

      Request request = new Request.Builder()
          .url(url)
          .method("PUT", body)
          .build();

      Call call = client.newCall(request);
      call.execute();
	}
}