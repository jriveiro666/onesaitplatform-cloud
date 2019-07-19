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
package com.minsait.onesait.platform.controlpanel.helper.digitaltwin.device;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.minsait.onesait.platform.config.components.GitlabConfiguration;
import com.minsait.onesait.platform.config.model.DigitalTwinDevice;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.model.PropertyDigitalTwinType;
import com.minsait.onesait.platform.config.repository.DigitalTwinDeviceRepository;
import com.minsait.onesait.platform.config.repository.PropertyDigitalTwinTypeRepository;
import com.minsait.onesait.platform.config.services.utils.ZipUtil;
import com.minsait.onesait.platform.controlpanel.services.microservice.MicroserviceBusinessService;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DigitalTwinDeviceHelper {

	@Autowired
	private DigitalTwinDeviceRepository digitalTwinDeviceRepo;

	@Autowired
	private PropertyDigitalTwinTypeRepository propDigitalTwinTypeRepo;

	@Autowired
	private MicroserviceBusinessService microservicesBusinessService;

	@Autowired
	private ZipUtil zipUtil;

	@Value("${digitaltwin.temp.dir}")
	private String tempDir;

	@Value("${digitaltwin.maven.exec.path}")
	private String mavenExecPath;

	private Template digitalTwinStatusTemplate;
	private Template pomTemplate;
	private Template deviceApplicationTemplate;
	private Template deviceConfigurationTemplate;
	private Template swaggerConfigTemplate;
	private Template dockerfileTemplate;

	private static final String RESOURCES_STR = "resources";
	private static final String ERROR_CLOSING = "Error closing File object";

	@PostConstruct
	public void init() {
		final Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		try {
			final TemplateLoader templateLoader = new ClassTemplateLoader(getClass(), "/digitaltwin/templates");

			cfg.setTemplateLoader(templateLoader);
			digitalTwinStatusTemplate = cfg.getTemplate("DigitalTwinStatusTemplate.ftl");
			pomTemplate = cfg.getTemplate("pomTemplate.ftl");
			deviceApplicationTemplate = cfg.getTemplate("DeviceApplicationTemplate.ftl");
			deviceConfigurationTemplate = cfg.getTemplate("applicationPropertiesTemplate.ftl");
			swaggerConfigTemplate = cfg.getTemplate("SwaggerConfigTemplate.ftl");
			dockerfileTemplate = cfg.getTemplate("Dockerfile.ftl");
		} catch (final IOException e) {
			log.error("Error configuring the template loader.", e);
		}
	}

	public File generateProject(String identificacion, Boolean compile, Boolean sensehat) {

		final List<PropertiesDTO> properties = new ArrayList<>();
		final List<PropertiesDTO> statusProperties = new ArrayList<>();
		final List<String> inits = new ArrayList<>();
		final List<PropertiesDTO> cls = new ArrayList<>();

		final DigitalTwinDevice device = digitalTwinDeviceRepo.findByIdentification(identificacion);
		final List<PropertyDigitalTwinType> propsDigitalTwin = propDigitalTwinTypeRepo.findByTypeId(device.getTypeId());
		String logic = device.getTypeId().getLogic();

		if (logic.startsWith("\"")) {
			logic = logic.substring(1, logic.length() - 1);
			logic = logic.replace("\\\"", "\"");
		}
		final String wot = device.getTypeId().getJson();
		final String projectDirectory = tempDir + File.separator + UUID.randomUUID();

		final File src = new File(projectDirectory + File.separator + device.getIdentification() + File.separator
				+ "src" + File.separator + "main");
		if (!src.exists()) {
			final Boolean success = src.mkdirs();
			if (!success) {
				log.error("Creating project for Digital Twin Device falied");
				return null;
			}
		} else {
			log.error("Creating project for Digital Twin Device falied, the temporary directory don't exist: "
					+ src.getAbsolutePath());
			return null;
		}

		final File docker = new File(
				projectDirectory + File.separator + device.getIdentification() + File.separator + "docker");

		if (!docker.exists()) {
			final Boolean success = docker.mkdirs();
			if (!success) {
				log.error("Creating project for Digital Twin Device falied");
				return null;
			}
		} else {
			log.error("Creating project for Digital Twin Device falied, the temporary directory don't exist: "
					+ docker.getAbsolutePath());
			return null;
		}

		for (final PropertyDigitalTwinType prop : propsDigitalTwin) {
			properties.add(new PropertiesDTO(prop.getName(), GeneratorJavaTypesMapper.mapPropertyName(prop.getType())));
			statusProperties
					.add(new PropertiesDTO(prop.getName(), GeneratorJavaTypesMapper.mapPropertyName(prop.getType())));
			properties.add(new PropertiesDTO(
					"operation" + prop.getName().substring(0, 1).toUpperCase() + prop.getName().substring(1),
					"OperationType"));
			inits.add("setOperation" + prop.getName().substring(0, 1).toUpperCase() + prop.getName().substring(1)
					+ "(OperationType." + prop.getDirection().toUpperCase() + ");");
			cls.add(new PropertiesDTO(prop.getName(), GeneratorJavaTypesMapper.mapPropertyName(prop.getType())));
		}

		// Status Template properties
		final Map<String, Object> dataStatusMap = new HashMap<>();
		dataStatusMap.put("properties", properties);
		dataStatusMap.put("statusProperties", statusProperties);
		dataStatusMap.put("inits", inits);
		dataStatusMap.put("package", "digitaltwin.device.status;");
		dataStatusMap.put("mapClass", cls);

		final Map<String, Object> dataApplicationPropertiesMap = new HashMap<>();
		dataApplicationPropertiesMap.put("serverPort", device.getPort());
		dataApplicationPropertiesMap.put("serverContextPath", device.getContextPath());
		dataApplicationPropertiesMap.put("applicationName", identificacion);
		dataApplicationPropertiesMap.put("apiKey", device.getDigitalKey());
		dataApplicationPropertiesMap.put("deviceId", device.getIdentification());
		dataApplicationPropertiesMap.put("deviceRestLocalSchema", device.getUrlSchema());
		dataApplicationPropertiesMap.put("deviceLocalInterface", device.getIntrface());
		dataApplicationPropertiesMap.put("deviceIpv6", device.getIpv6());
		dataApplicationPropertiesMap.put("onesaitplatformBrokerEndpoint", device.getUrl());

		// pom.xml Template properties
		final Map<String, Object> dataPomMap = new HashMap<>();
		dataPomMap.put("ProjectName", identificacion);
		dataPomMap.put("sensehat", sensehat);

		// dockerfile props
		final Map<String, Object> dataDockerfile = new HashMap<>();
		dataDockerfile.put("dtName", identificacion);

		Writer writerDeviceApplication = null;
		Writer writerTwinStatus = null;
		Writer writerApplicationProperties = null;
		Writer writerDockerfile = null;
		Writer writerPom = null;
		Writer writerSwagger = null;
		PrintWriter outLogic = null;
		PrintWriter outWot = null;
		File zipFile = null;

		try {
			zipFile = File.createTempFile(device.getIdentification(), ".zip");

			// Create DeviceApplication.java
			log.info("New file is going to be generate on: " + src.getAbsolutePath() + File.separator + "java"
					+ File.separator + "digitaltwin" + File.separator + "device");
			final File app = new File(src.getAbsolutePath() + File.separator + "java" + File.separator + "digitaltwin"
					+ File.separator + "device");
			if (!app.isDirectory()) {
				app.mkdirs();
			}
			writerDeviceApplication = new FileWriter(app + File.separator + "DeviceApplication.java");
			deviceApplicationTemplate.process(new HashMap<>(), writerDeviceApplication);
			writerDeviceApplication.flush();

			// create Dockerfile
			writerDockerfile = new FileWriter(docker + File.separator + "Dockerfile");
			dockerfileTemplate.process(dataDockerfile, writerDockerfile);
			writerDockerfile.flush();

			// Create DigitalTwinStatus.java
			final File fileJava = new File(app + File.separator + "status");
			if (!fileJava.isDirectory()) {
				fileJava.mkdirs();
			}
			writerTwinStatus = new FileWriter(fileJava + File.separator + "DigitalTwinStatus.java");
			digitalTwinStatusTemplate.process(dataStatusMap, writerTwinStatus);
			writerTwinStatus.flush();

			// Create logic.js
			final File fileStatic = new File(src.getAbsolutePath() + File.separator + RESOURCES_STR + File.separator
					+ "static" + File.separator + "js");
			if (!fileStatic.isDirectory()) {
				fileStatic.mkdirs();
			}
			outLogic = new PrintWriter(fileStatic + File.separator + "logic.js");
			outLogic.println(logic.replace("\\n", System.getProperty("line.separator")));
			outLogic.flush();

			// Create Wot.json
			final File fileJson = new File(src.getAbsolutePath() + File.separator + RESOURCES_STR + File.separator
					+ "static" + File.separator + "json");
			if (!fileJson.isDirectory()) {
				fileJson.mkdirs();
			}
			outWot = new PrintWriter(fileJson + File.separator + "wot.json");
			outWot.println(wot);
			outWot.flush();

			// Create application.yml
			writerApplicationProperties = new FileWriter(
					src.getAbsolutePath() + File.separator + RESOURCES_STR + File.separator + "application.yml");
			deviceConfigurationTemplate.process(dataApplicationPropertiesMap, writerApplicationProperties);
			writerApplicationProperties.flush();

			// Create pom.xml
			writerPom = new FileWriter(
					projectDirectory + File.separator + device.getIdentification() + File.separator + "pom.xml");
			pomTemplate.process(dataPomMap, writerPom);
			writerPom.flush();

			// Create SwaggerConfig.java
			writerSwagger = new FileWriter(fileJava + File.separator + "SwaggerConfig.java");
			final Map<String, Object> dataSwaggerMap = new HashMap<>();
			swaggerConfigTemplate.process(dataSwaggerMap, writerSwagger);
			writerSwagger.flush();

		} catch (final Exception e) {
			log.error("Error generating Digital Twin project", e);
		} finally {
			try {
				if (null != writerDeviceApplication) {
					writerDeviceApplication.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

			try {
				if (null != writerTwinStatus) {
					writerTwinStatus.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

			try {
				if (null != writerApplicationProperties) {
					writerApplicationProperties.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

			try {
				if (null != writerPom) {
					writerPom.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

			try {
				if (null != writerSwagger) {
					writerSwagger.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

			try {
				if (null != outLogic) {
					outLogic.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

			try {
				if (null != outWot) {
					outWot.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

			try {
				if (null != writerDockerfile) {
					writerDockerfile.close();
				}
			} catch (final Exception e) {
				log.error(ERROR_CLOSING, e);
			}

		}
		if (compile) {
			buildProjectMaven(projectDirectory + File.separator + device.getIdentification());
		}

		final File fileProjectDirectory = new File(projectDirectory);
		try {
			zipUtil.zipDirectory(fileProjectDirectory, zipFile);
		} catch (final IOException e) {
			log.error("Zip file deviceTwin failed", e);
		}

		// Removes the directory
		deleteDirectory(fileProjectDirectory);

		return zipFile;
	}

	private boolean deleteDirectory(File directoryToBeDeleted) {
		final File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (final File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

	private void buildProjectMaven(String projectPath) {
		try {
			final File pathToExecutable = new File(mavenExecPath);
			final ProcessBuilder builder = new ProcessBuilder(pathToExecutable.getAbsolutePath(), "clean", "package");
			final File workingDirectory = new File(projectPath);
			log.info("Sets working directory: {}", workingDirectory);
			log.info("Absolute path: {}", workingDirectory);

			builder.directory(workingDirectory); // this is where you set the root folder for the executable to run with
			builder.redirectErrorStream(true);
			final Process process = builder.start();

			final Scanner s = new Scanner(process.getInputStream());
			final StringBuilder text = new StringBuilder();
			while (s.hasNextLine()) {
				text.append(s.nextLine());
				text.append("\n");
			}
			s.close();

			final int result = process.waitFor();
			log.info("Process exited with result {} and output {}", result, text);
		} catch (final Exception e) {
			log.error("Error compiling project", e);
		}

	}

	public String createMicroservice(String identification, Boolean sensehat, String gitlabUrl, String gitlabToken) {
		final File file = generateProject(identification, false, sensehat);
		final Microservice microservice = microservicesBusinessService.createMicroserviceFromDigitalTwin(
				digitalTwinDeviceRepo.findByIdentification(identification), file,
				GitlabConfiguration.builder().site(gitlabUrl).privateToken(gitlabToken).build(), identification + "/",
				identification + "/docker/");
		return microservice.getIdentification();
	}
}
