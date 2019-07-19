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
package com.minsait.onesait.platform.config.services.dashboard.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.minsait.onesait.platform.config.services.gadget.dto.GadgetDTO;
import com.minsait.onesait.platform.config.services.gadget.dto.GadgetDatasourceDTO;
import com.minsait.onesait.platform.config.services.gadget.dto.GadgetMeasureDTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class DashboardExportDTO implements Cloneable, Serializable {

	@Getter
	@Setter
	private String identification;

	@Getter
	@Setter
	private String user;

	@Getter
	@Setter
	private String category;

	@Getter
	@Setter
	private String subcategory;

	@Getter
	@Setter
	private Date createdAt;

	@Getter
	@Setter
	private Date modifiedAt;

	@Getter
	@Setter
	private String headerlibs;

	@Getter
	@Setter
	private String description;

	@Getter
	@Setter
	private boolean isPublic;

	@Getter
	@Setter
	private int nGadgets;

	@Getter
	@Setter
	private List<DashboardUserAccessDTO> dashboardAuths;

	@Getter
	@Setter
	private String model;

	@Getter
	@Setter
	private List<GadgetDTO> gadgets;

	@Getter
	@Setter
	private List<GadgetDatasourceDTO> gadgetDatasources;

	@Getter
	@Setter
	private List<GadgetMeasureDTO> gadgetMeasures;
}
