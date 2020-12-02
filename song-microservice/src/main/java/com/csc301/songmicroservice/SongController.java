package com.csc301.songmicroservice;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
	    
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.findSongById(songId);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
	    
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	
    @RequestMapping(value = "/addSong", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("path", String.format("POST %s", Utils.getUrl(request)));  
        
        try {
          // check that required parameters exist
          if (params.containsKey("songName") && params.containsKey("songArtistFullName") 
              && params.containsKey("songAlbum")) {
            String songName = params.get("songName");
            String artistName = params.get("songArtistFullName");
            String albumName = params.get("songAlbum");
            
            // checking that required parameters are non-empty 
            if (songName.length() > 0 && artistName.length() > 0 && albumName.length() > 0) {
              DbQueryStatus status = songDal.addSong(new Song(songName, artistName, albumName));
              Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());
            } else {
              response.put("status", "Song could not be added due to invalid parameters.");
            }
          } else {
            response.put("status", "Song could not be added due to missing parameters.");
          }
        } catch (Exception e) {
          response.put("status", "Song could not be added.");
        }
        return response;
    }

	
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("data", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.updateSongFavouritesCount(songId, shouldDecrement.equals("true"));
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);

		return response;
	}
}