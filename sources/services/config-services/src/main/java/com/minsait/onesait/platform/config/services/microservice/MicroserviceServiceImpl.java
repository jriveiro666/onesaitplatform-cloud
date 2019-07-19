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
package com.minsait.onesait.platform.config.services.microservice;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.model.Role;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.repository.MicroserviceRepository;
import com.minsait.onesait.platform.config.services.exceptions.MicroserviceException;

@Service
public class MicroserviceServiceImpl implements MicroserviceService {

	@Autowired
	private MicroserviceRepository microserviceRepository;

	@PostConstruct
	void updateJenkinsQueues() {
		microserviceRepository.updateMicroserviceSetJenkinsQueueIdNull();
	}

	@Override
	public Microservice create(Microservice service) {
		if (microserviceRepository.findByIdentificationAndActiveTrue(service.getIdentification()) != null)
			throw new MicroserviceException(
					"Microservice with identification " + service.getIdentification() + " already exists.");
		else
			return microserviceRepository.save(service);
	}

	@Override
	public List<Microservice> getAllMicroservices() {
		return microserviceRepository.findByActiveTrue();
	}

	@Override
	public List<Microservice> getMicroservices(User user) {
		return microserviceRepository.findByUserAndActiveTrue(user);
	}

	@Override
	public Microservice save(Microservice service) {
		return microserviceRepository.save(service);
	}

	@Override
	public Microservice getByIdentification(String identification) {
		return microserviceRepository.findByIdentificationAndActiveTrue(identification);
	}

	@Override
	public Microservice getById(String id) {
		return microserviceRepository.findOne(id);
	}

	@Override
	public void delete(Microservice service) {
		service.setActive(false);
		microserviceRepository.save(service);

	}

	@Override
	public boolean hasUserPermission(Microservice microservice, User user) {
		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.name()) || microservice.getUser().equals(user))
			return true;
		return false;
	}

	@Override
	public Microservice update(Microservice service) {
		final Microservice mDB = getById(service.getId());
		mDB.setGitlabConfiguration(service.getGitlabConfiguration());
		mDB.setJenkinsConfiguration(service.getJenkinsConfiguration());
		mDB.setRancherConfiguration(service.getRancherConfiguration());
		if (!StringUtils.isEmpty(service.getContextPath()))
			mDB.setContextPath(service.getContextPath());
		return save(mDB);
	}

}
