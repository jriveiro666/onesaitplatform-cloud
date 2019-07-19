package com.minsait.security;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration("RegisterCustomAuthenticationPostProcessor")
@EnableConfigurationProperties(CasConfigurationProperties.class)

public class AuthenticationConfigurer implements AuthenticationEventExecutionPlanConfigurer {

	@Autowired
	private CustomAuthenticationPostProcessor customAuthenticationPostProcessor;

	@Override
	public void configureAuthenticationExecutionPlan(AuthenticationEventExecutionPlan plan) {
		System.out.println("Configuring plan");
		plan.registerAuthenticationPostProcessor(customAuthenticationPostProcessor);

	}

}
