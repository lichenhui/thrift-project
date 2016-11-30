package cn.lichenhui.rpc.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AuxiliaryController implements ErrorController {

	private static final Logger log = LoggerFactory.getLogger(AuxiliaryController.class);
	private static final String ERROR_PATH = "/error";

	@Autowired
	private ErrorAttributes errorAttributes;

	@RequestMapping(ERROR_PATH)
	public ResponseEntity<?> error(HttpServletRequest request, HttpServletResponse response) {
		HttpStatus httpStatus = getStatus(request);
		String message;
		Throwable throwable = getException(request);
		if (throwable == null) {
			message = httpStatus.getReasonPhrase();
		} else {
			message = throwable.getMessage();
		}
		Map<String, Object> errorMessage = new HashMap<>();
		if (httpStatus.is5xxServerError()) {
			log.error(message, throwable);
			message = "后台错误";
		}
		errorMessage.put("httpStatus", httpStatus.value());
		errorMessage.put("message", message);
		return new ResponseEntity<Object>(errorMessage, httpStatus);
	}

	@Override
	public String getErrorPath() {
		return ERROR_PATH;
	}

	private Throwable getException(HttpServletRequest request) {
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		return errorAttributes.getError(requestAttributes);
	}

	private HttpStatus getStatus(HttpServletRequest request) {
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		Integer status = (Integer) requestAttributes.getAttribute("javax.servlet.error.status_code",
				RequestAttributes.SCOPE_REQUEST);
		return HttpStatus.valueOf(status);
	}
}
