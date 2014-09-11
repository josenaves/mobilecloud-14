package org.magnum.mobilecloud.video.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=org.springframework.http.HttpStatus.NOT_FOUND, reason="There's no such video")
public class VideoNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -3173711071653810546L;

}
