/**
 * Copyright Indra Soluciones Tecnologías de la Información, S.L.U.
 * 2013-2019 SPAIN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.minsait.onesait.platform.controlpanel.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import com.minsait.onesait.platform.config.services.menu.MenuService;
import com.minsait.onesait.platform.config.services.user.UserService;
import com.minsait.onesait.platform.controlpanel.rest.management.login.LoginManagementController;
import com.minsait.onesait.platform.controlpanel.rest.management.login.model.RequestLogin;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesService;

import jline.internal.Log;

@Component
public class Securityhandler implements AuthenticationSuccessHandler {

	private final String BLOCK_PRIOR_LOGIN = "block_prior_login";
	private final String URI_CONTROLPANEL = "/controlpanel";
	private final String URI_MAIN = "/main";

	@Autowired
	private LoginManagementController controller;

	@Autowired
	private AppWebUtils utils;
	@Autowired
	private MenuService menuService;
	@Autowired
	private UserService userService;

	@Autowired
	private IntegrationResourcesService integrationResourcesService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {

		final HttpSession session = request.getSession();
		if (session != null) {
			loadMenuAndUrlsToSession(request);
			generateTokenOauth2ForControlPanel(request, authentication);
			final String redirectUrl = (String) session.getAttribute(BLOCK_PRIOR_LOGIN);
			if (redirectUrl != null) {
				// we do not forget to clean this attribute from session
				session.removeAttribute(BLOCK_PRIOR_LOGIN);
				// then we redirect
				response.sendRedirect(request.getContextPath() + redirectUrl.replace(URI_CONTROLPANEL, ""));
			} else {
				response.sendRedirect(request.getContextPath() + URI_MAIN);
			}

		} else {
			response.sendRedirect(request.getContextPath() + URI_MAIN);
		}

	}

	private void generateTokenOauth2ForControlPanel(HttpServletRequest request, Authentication authentication) {
		final String password = request.getParameter("password");
		final String username = request.getParameter("username");
		if (!StringUtils.isEmpty(password) && !StringUtils.isEmpty(username)) {
			final RequestLogin oauthRequest = new RequestLogin();
			oauthRequest.setPassword(password);
			oauthRequest.setUsername(username);
			try {
				request.getSession().setAttribute("oauthToken", controller.postLoginOauth2(oauthRequest).getBody());
			} catch (final Exception e) {

				Log.error(e.getMessage());
			}
		} else if (authentication != null && authentication.isAuthenticated()) {
			request.getSession().setAttribute("oauthToken", controller.postLoginOauthNopass(authentication));
		}
	}

	private void loadMenuAndUrlsToSession(HttpServletRequest request) {
		final String jsonMenu = menuService.loadMenuByRole(userService.getUser(utils.getUserId()));
		// Remove PrettyPrinted
		final String menu = utils.validateAndReturnJson(jsonMenu);
		utils.setSessionAttribute(request, "menu", menu);
		if (request.getSession().getAttribute("apis") == null)
			utils.setSessionAttribute(request, "apis", integrationResourcesService.getSwaggerUrls());
	}

}
