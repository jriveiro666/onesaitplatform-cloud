package com.minsait.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.minsait.controller.UserController;
import com.minsait.custom.SSLUtil;
import com.minsait.dto.UserDTO;

@Service
public class UserManagementService {
	@Value("${onesaitplatform.api_key}")
	private String apiKey;
	@Value("${onesaitplatform.api_key_header}")
	private String apiHeader;
	@Value("${onesaitplatform.base_url}")
	private String baseUrl;

	private static final String API_USERS_PATH = "/api/users";

	@Autowired
	private UserController userController;
	private RestTemplate restTemplate;
	private HttpHeaders headers;

	@PostConstruct
	public void onInit() {
		restTemplate = SSLUtil.restTemplate();
		headers = new HttpHeaders();
		headers.add(apiHeader, apiKey);
	}

	public boolean userExists(String username) {
		final HttpEntity<?> httpEntity = new HttpEntity<>(headers);
		try {
			final ResponseEntity<UserDTO> user = restTemplate.exchange(
					baseUrl.concat(API_USERS_PATH).concat("/").concat(username), HttpMethod.GET, httpEntity,
					UserDTO.class);
			if (user.getBody() == null)
				return false;
		} catch (final Exception e) {
			return false;
		}
		return true;

	}

	public void createUser(UserDTO user) {

		System.out.println("Sending user create request");
		try {
			userController.create(user);
		} catch (final Exception e) {
			System.out.println("End request error: " + e);
		}
	}

}
