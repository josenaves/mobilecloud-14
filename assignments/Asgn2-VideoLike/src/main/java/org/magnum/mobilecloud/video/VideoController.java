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
package org.magnum.mobilecloud.video;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.exception.VideoNotFoundException;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class VideoController {
	
	@Autowired
	private VideoRepository repo;
	
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

	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		Collection<Video> videos = (Collection<Video>) repo.findAll();
		return videos;
	}

	@RequestMapping(value="/video/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideoById(@PathVariable("id") long id) {
		Video video = repo.findOne(id);
		if (null == video) throw new VideoNotFoundException();
		return video;
	}

	@RequestMapping(value="/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video) {
		// new videos don't have likes
		video.setLikes(0);
		// persist the video
		video = repo.save(video);
		return video;
	}

	@RequestMapping(value="/video/{id}/like", method=RequestMethod.POST)
	public void likeVideo(@PathVariable("id") long id, Principal principal, HttpServletResponse response) {
		Video video = repo.findOne(id);
		if (null == video) throw new VideoNotFoundException();
		
		// get the user associated with this request
		String user = principal.getName();
		
		Set<String> users = video.getLikesUsernames();
		
		// find if the user already liked the video
		if (users.contains(user)) {
			// Status code 400
			response.setStatus(org.springframework.http.HttpStatus.BAD_REQUEST.value());
			return;
		}
		
		// add the user in the set
		users.add(user);
		video.setLikesUsernames(users);
		video.setLikes(users.size());
		
		video = repo.save(video);
	}
	
	@RequestMapping(value="/video/{id}/unlike", method=RequestMethod.POST)
	public void unlikeVideo(@PathVariable("id") long id, Principal principal, HttpServletResponse response) {
		Video video = repo.findOne(id);
		if (null == video) throw new VideoNotFoundException();
		
		// get the user associated with this request
		String user = principal.getName();
		
		Set<String> users = video.getLikesUsernames();
		
		// find if the user really liked the video
		if (!users.contains(user)) {
			// Status code 400
			response.setStatus(org.springframework.http.HttpStatus.BAD_REQUEST.value());
			return;
		}
		
		// REMOVE the user in the set
		users.remove(user);
		video.setLikesUsernames(users);
		video.setLikes(users.size());
		
		video = repo.save(video);
	}

	//@RequestMapping(value="/video/search/findByName?title={title}", method=RequestMethod.GET)
	@RequestMapping(value="/video/search/findByName", method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(@RequestParam("title") String title) {
		Collection<Video> videos = (Collection<Video>) repo.findByName(title);
		if (null == videos) videos = new ArrayList<Video>();
		return videos;
	}
	
	//@RequestMapping(value="/video/search/findByDurationLessThan?duration={duration}", method=RequestMethod.GET)	
	@RequestMapping(value="/video/search/findByDurationLessThan", method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan(@RequestParam("duration") long duration) {
		Collection<Video> videos = (Collection<Video>) repo.findByDurationLessThan(duration);
		if (null == videos) videos = new ArrayList<Video>();
		return videos;
	}

	@RequestMapping(value="/video/{id}/likedby", method=RequestMethod.GET)	
	public @ResponseBody Collection<String> getUsersWhoLikedVideo(@PathVariable("id") long id) {
		Video video = repo.findOne(id);
		if (null == video) throw new VideoNotFoundException();
		
		return video.getLikesUsernames();
	}
	
}
