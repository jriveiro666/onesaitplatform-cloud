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
package com.minsait.onesait.platform.rtdbmaintainer.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.minsait.onesait.platform.config.model.Configuration;
import com.minsait.onesait.platform.config.services.configuration.ConfigurationService;
import com.minsait.onesait.platform.scheduler.SchedulerType;
import com.minsait.onesait.platform.scheduler.scheduler.bean.TaskInfo;
import com.minsait.onesait.platform.scheduler.scheduler.bean.TaskOperation;
import com.minsait.onesait.platform.scheduler.scheduler.service.TaskService;

@Component
public class RtdbMaintainerConfiguration implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	ConfigurationService configurationService;
	@Autowired
	TaskService taskService;

	private static final String JOB_NAME = "RtdbMaintainerJob";

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		final Configuration configuration = configurationService.getConfiguration(Configuration.Type.SCHEDULING,
				"default", null);
		@SuppressWarnings("unchecked")
		final Map<String, Object> ymlConfig = (Map<String, Object>) configurationService
				.fromYaml(configuration.getYmlConfig()).get("RtdbMaintainer");
		final String cron = (String) ymlConfig.get("cron");
		final TimeUnit timeUnit = TimeUnit.valueOf((String) ymlConfig.get("timeUnit"));
		final long timeout = ((Integer) ymlConfig.get("timeout")).longValue();

		final TaskOperation taskOperation = new TaskOperation();
		taskOperation.setJobName(JOB_NAME + "-" + SchedulerType.BATCH.toString());
		if (!taskService.checkExists(taskOperation)) {
			final TaskInfo task = new TaskInfo();
			task.setSchedulerType(SchedulerType.BATCH);
			task.setCronExpression(cron);
			task.setSingleton(true);
			task.setJobName(JOB_NAME);
			task.setUsername("administrator");
			final Map<String, Object> jobContext = new HashMap<String, Object>();
			jobContext.put("timeout", timeout);
			jobContext.put("timeUnit", timeUnit);
			task.setData(jobContext);
			taskService.addJob(task);
		}

	}

}
