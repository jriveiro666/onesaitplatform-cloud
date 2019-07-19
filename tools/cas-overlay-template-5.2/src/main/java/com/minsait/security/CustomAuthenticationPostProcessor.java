package com.minsait.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apereo.cas.authentication.AuthenticationBuilder;
import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.AuthenticationPostProcessor;
import org.apereo.cas.authentication.AuthenticationTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minsait.dto.UserDTO;
import com.minsait.service.UserManagementService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomAuthenticationPostProcessor implements AuthenticationPostProcessor {

	@Autowired
	private UserManagementService userManagementService;
	private static final String ROLE_USER = "ROLE_USER";
	private static final String DEFAULT_PASSWORD = "changeIt2019!";
	private final ObjectMapper mapper = new ObjectMapper();

	@SuppressWarnings("unchecked")
	@Override
	public void process(AuthenticationBuilder builder, AuthenticationTransaction transaction)
			throws AuthenticationException {
		final String username = builder.getPrincipal().getId();
		if (builder.getSuccesses().keySet().iterator().next().toLowerCase().contains("ldap")) {
			if (!userManagementService.userExists(username)) {
				try {

					final String name = ((ArrayList<String>) builder.getPrincipal().getAttributes().get("commonName"))
							.get(0) + " "
							+ ((ArrayList<String>) builder.getPrincipal().getAttributes().get("simpleName")).get(0);

					final String email = ((ArrayList<String>) builder.getPrincipal().getAttributes().get("email"))
							.get(0);

					final Map<String, Object> attributes = new HashMap<>(builder.getPrincipal().getAttributes());
					attributes.put("authSource", "LDAP");
					final UserDTO user = UserDTO.builder().username(builder.getPrincipal().getId())
							.extraFields(mapper.writeValueAsString(attributes)).role(ROLE_USER)
							.password(DEFAULT_PASSWORD).mail(email).fullName(name).build();

					userManagementService.createUser(user);
				} catch (final Exception e) {
					System.out.println("Could not create user dto " + e.getMessage());
				}
			}

		}
	}

}
