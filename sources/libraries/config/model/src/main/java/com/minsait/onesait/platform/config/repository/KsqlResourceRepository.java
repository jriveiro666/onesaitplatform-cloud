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
package com.minsait.onesait.platform.config.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minsait.onesait.platform.config.model.KsqlResource;
import com.minsait.onesait.platform.config.model.KsqlResource.FlowResourceType;

public interface KsqlResourceRepository extends JpaRepository<KsqlResource, String> {

	KsqlResource findByIdentification(String identification);

	KsqlResource findByIdentificationAndIdNot(String identification, String id);

	KsqlResource findById(String identification);

	List<KsqlResource> findByKafkaTopic(String kafkaTopic);

	List<KsqlResource> findByResourceType(FlowResourceType resourceType);

	@Cacheable(cacheNames = "KsqlResourceRepositoryByOntology_IdentificationAndResourceType", unless = "#result == null")
	@Query("SELECT r.kafkaTopic FROM KsqlResource AS r WHERE r.ontology.identification = :ontology and r.resourceType = :resourceType")
	List<String> findByOntology_IdentificationAndResourceType(@Param("ontology") String ontology,
			@Param("resourceType") FlowResourceType resourceType);

	@Modifying
	@Transactional
	@CacheEvict(cacheNames = "KsqlResourceRepositoryByOntology_IdentificationAndResourceType", allEntries = true)
	void deleteByIdentification(String identification);

	@Override
	@CacheEvict(cacheNames = "KsqlResourceRepositoryByOntology_IdentificationAndResourceType", allEntries = true)
	@Modifying
	@Transactional
	void delete(String id);

	@Override
	@CacheEvict(cacheNames = "KsqlResourceRepositoryByOntology_IdentificationAndResourceType", allEntries = true)
	@Modifying
	@Transactional
	void delete(KsqlResource entity);

	@Override
	@CacheEvict(cacheNames = "KsqlResourceRepositoryByOntology_IdentificationAndResourceType", allEntries = true)
	KsqlResource save(KsqlResource datamodel);

	@Override
	@CacheEvict(cacheNames = "KsqlResourceRepositoryByOntology_IdentificationAndResourceType", allEntries = true)
	void flush();
}
