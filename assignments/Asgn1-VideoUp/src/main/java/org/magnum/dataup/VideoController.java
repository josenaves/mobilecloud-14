/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {
	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */

    private static final AtomicLong currentId = new AtomicLong(0L);
	
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	
	
	/*
	GET /video

	Returns the LIST of videos that have been added to the server as JSON. The list of videos does not have
	to be persisted across restarts of the server.
	 
	The list of Video objects should be able to be unmarshalled by the client into a Collection.
	
	The return content-type should be application/json, which will be the default if you use @ResponseBody
	*/
	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideos(Video videoData) throws NotFoundException {
		if (videos.values().isEmpty()) throw new NotFoundException("There's no videos available.");
		return videos.values();
	}
	
	/*
	POST /video

	The video metadata is provided as an application/json request body. The JSON should generate a valid instance of the Video class when 
	deserialized by Spring's default Jackson library.
	
	Returns the JSON representation of the Video object that was stored along with any updates to that object made by the server.
	The server should generate a unique identifier for the Video object and assign it to the Video by calling its setId(...) method.
	
	No video should have ID = 0. All IDs should be > 0.
	
	The returned Video JSON should include this server-generated identifier so that the client can refer to it when uploading the binary 
	mpeg video content for the Video.
	
	The server should also generate a "data url" for the Video. The "data url" is the url of the binary data for a Video 
	(e.g., the raw mpeg data). The URL should be the full URL for the video and not just the path (e.g., http://localhost:8080/video/1/data 
	would be a valid data url). See the Hints section for some ideas on how to generate this URL.
	*/
	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video) {
		checkAndSetId(video);
		videos.put(video.getId(), video);
		video.setDataUrl(getDataUrl(video.getId()));
		return video;
	}


	/*
	POST /video/{id}/data

	The binary mpeg data for the video should be provided in a multipart request as a part with the key "data". The id in the path should be replaced
	with the unique identifier generated by the server for the Video. A client MUST create a Video first by sending a POST to /video and getting the 
	identifier for the newly created Video object before sending a POST to /video/{id}/data.
	
	The endpoint should return a VideoStatus object with state=VideoState.READY if the request succeeds and the appropriate HTTP error status otherwise. 
	VideoState.PROCESSING is not used in this assignment but is present in VideoState.
	
	Rather than a PUT request, a POST is used because, by default, Spring does not support a PUT with multipart data due to design decisions in the 
	Commons File Upload library: https://issues.apache.org/jira/browse/FILEUPLOAD-197
	*/
	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable("id") long id, @RequestParam("data") MultipartFile videoData) throws NotFoundException, IOException {
		// look up for the video
		Video video = videos.get(id);
		if (video == null) throw new NotFoundException("Cannot find video with id [" + id + "]");

		// save binary data
		saveSomeVideo(video, videoData);
		
		return new VideoStatus(VideoState.READY);
	}
	
	/*
	GET /video/{id}/data

	Returns the binary mpeg data (if any) for the video with the given identifier. If no mpeg data has been uploaded for the specified video, 
	then the server should return a 404 status code.
	*/
	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public void getVideoData(@PathVariable("id") long id, HttpServletResponse response)  throws NotFoundException, IOException {
		// look up for the video
		Video video = videos.get(id);
		
		if (video == null) throw new NotFoundException("Cannot find video with id [" + id + "]");
		if (video.getDataUrl() == null) throw new NotFoundException("Cannot find mpeg data for video with id [" + id + "]");
		
		serveSomeVideo(video, response);
	}
	
	/*
	 * This will make an unique id for videos that has not has one.
	 */
	private void checkAndSetId(Video video) {
		if (video.getId() == 0){
			video.setId(currentId.incrementAndGet());
		}
	}

	private void saveSomeVideo(Video video, MultipartFile videoData) throws IOException {
		VideoFileManager.get().saveVideoData(video, videoData.getInputStream());
	}
	
	private void serveSomeVideo(Video video, HttpServletResponse response) throws IOException {
		VideoFileManager.get().copyVideoData(video, response.getOutputStream());
	}
	
	private String getDataUrl(long videoId) {
		return getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base ="http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
				+ request.getServerPort() : "");
		return base;
	}	
	
}
