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
package com.minsait.onesait.platform.config.services.gis.viewer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.minsait.onesait.platform.config.model.BaseLayer;
import com.minsait.onesait.platform.config.model.Role;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.model.Viewer;
import com.minsait.onesait.platform.config.repository.BaseLayerRepository;
import com.minsait.onesait.platform.config.repository.OntologyRepository;
import com.minsait.onesait.platform.config.repository.ViewerRepository;
import com.minsait.onesait.platform.config.services.exceptions.ViewerServiceException;
import com.minsait.onesait.platform.config.services.user.UserService;

@Service
public class ViewerServiceImpl implements ViewerService {

	private static final String ANONYMOUSUSER = "anonymousUser";

	@Autowired
	private UserService userService;

	@Autowired
	private ViewerRepository viewerRepository;

	@Autowired
	OntologyRepository ontologyRepository;

	@Autowired
	BaseLayerRepository baseLayerRepository;

	@Override
	public List<Viewer> findAllViewers(String userId) {
		List<Viewer> viewers = new ArrayList<Viewer>();
		final User sessionUser = userService.getUser(userId);

		if (sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			viewers = viewerRepository.findAll();
		} else {
			viewers = viewerRepository.findByIsPublicTrueOrUser(sessionUser);
		}

		return viewers;
	}

	@Override
	public List<BaseLayer> findAllBaseLayers() {
		return baseLayerRepository.findAll();
	}

	@Override
	public List<BaseLayer> getBaseLayersByTechnology(String technology) {
		return baseLayerRepository.findByTechnologyOrderByIdentificationAsc(technology);
	}

	@Override
	public Viewer create(Viewer viewer, String baseMap) {

		BaseLayer baseLayer = baseLayerRepository.findByIdentification(baseMap).get(0);
		viewer.setBaseLayer(baseLayer);

		return viewerRepository.save(viewer);
	}

	@Override
	public Boolean hasUserViewPermission(String id, String userId, String userIdToken) {
		final User sessionUser = userService.getUser(userId);
		Viewer viewer = viewerRepository.findById(id);

		if (userIdToken != null) {
			if (viewer.isPublic() || userIdToken.equals(viewer.getUser().getUserId())
					|| sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.name())) {
				return true;
			} else {
				return false;
			}
		} else {
			if (viewer.isPublic()) {
				return true;
			} else if (sessionUser == null || userId.equals(ANONYMOUSUSER)) {
				if (!viewer.isPublic()) {
					return null;
				} else {
					return true;
				}
			} else if (sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())
					|| viewer.getUser().equals(sessionUser)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Viewer getViewerById(String id, String userId) {
		final User sessionUser = userService.getUser(userId);
		Viewer viewer = viewerRepository.findById(id);

		if (sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())
				|| viewer.getUser().equals(sessionUser) || viewer.isPublic()) {
			return viewer;
		} else {
			throw new ViewerServiceException("The user is not authorized");
		}
	}

	@Override
	public void deleteViewer(Viewer viewer, String userId) {
		final User sessionUser = userService.getUser(userId);

		if (sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())
				|| viewer.getUser().equals(sessionUser) || viewer.isPublic()) {
			viewerRepository.delete(viewer);
		} else {
			throw new ViewerServiceException("The user is not authorized");
		}
	}

}
