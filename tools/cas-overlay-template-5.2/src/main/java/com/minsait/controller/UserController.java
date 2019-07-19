package com.minsait.controller;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import com.minsait.custom.SSLUtil;
import com.minsait.dto.UserDTO;

@Controller
@RequestMapping("users")
public class UserController {

	@Value("${onesaitplatform.api_key}")
	private String apiKey;
	@Value("${onesaitplatform.api_key_header}")
	private String apiHeader;
	@Value("${onesaitplatform.base_url}")
	private String baseUrl;

	private RestTemplate restTemplate;
	private HttpHeaders headers;

	@PostConstruct
	public void onInit() {
		restTemplate = SSLUtil.restTemplate();
		headers = new HttpHeaders();
		headers.add(apiHeader, apiKey);
	}

	@GetMapping("create")
	public String createForm(Model model) {
		return "users/create";
	}

	@GetMapping
	public ResponseEntity<List<UserDTO>> findAll() {
		final HttpEntity<?> httpEntity = new HttpEntity<>(headers);
		final ResponseEntity<List<UserDTO>> users = restTemplate.exchange(baseUrl.concat("/api/users"), HttpMethod.GET,
				httpEntity, new ParameterizedTypeReference<List<UserDTO>>() {
				});
		return new ResponseEntity<>(users.getBody(), HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<String> create(@RequestBody UserDTO user) {
		final HttpEntity<?> httpEntity = new HttpEntity<>(user, headers);
		final ResponseEntity<?> response = restTemplate.exchange(baseUrl.concat("/api/users"), HttpMethod.POST,
				httpEntity, String.class);
		if (response.getStatusCode().equals(HttpStatus.CREATED))
			return new ResponseEntity<>("User created", HttpStatus.OK);
		else
			return new ResponseEntity<>((String) response.getBody(), response.getStatusCode());
	}

}
