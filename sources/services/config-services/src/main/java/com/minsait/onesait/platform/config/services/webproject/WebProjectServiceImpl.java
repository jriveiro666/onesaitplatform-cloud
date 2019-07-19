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
package com.minsait.onesait.platform.config.services.webproject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.minsait.onesait.platform.config.model.Role;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.model.WebProject;
import com.minsait.onesait.platform.config.repository.WebProjectRepository;
import com.minsait.onesait.platform.config.services.exceptions.WebProjectServiceException;
import com.minsait.onesait.platform.config.services.user.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WebProjectServiceImpl implements WebProjectService {

	@Autowired
	private WebProjectRepository webProjectRepository;

	@Autowired
	private UserService userService;

	private static final String USER_UNAUTH = "The user is not authorized";

	@Value("${onesaitplatform.webproject.baseurl:https://localhost:18080/web/}")
	private final String rootWWW = "";

	@Value("${onesaitplatform.webproject.rootfolder.path:/usr/local/webprojects/}")
	private String rootFolder;

	@Override
	public List<WebProject> getWebProjectsWithDescriptionAndIdentification(String userId, String identification,
			String description) {
		List<WebProject> webProjects;
		final User user = userService.getUser(userId);

		description = description == null ? "" : description;
		identification = identification == null ? "" : identification;

		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			webProjects = webProjectRepository.findByIdentificationContainingAndDescriptionContaining(identification,
					description);
		} else {
			webProjects = webProjectRepository.findByUserAndIdentificationContainingAndDescriptionContaining(user,
					identification, description);
		}

		return webProjects;
	}

	@Override
	public List<String> getWebProjectsIdentifications(String userId) {
		List<WebProject> webProjects;
		final List<String> identifications = new ArrayList<>();
		final User user = userService.getUser(userId);

		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			webProjects = webProjectRepository.findAllByOrderByIdentificationAsc();
		} else {
			webProjects = webProjectRepository.findByUserOrderByIdentificationAsc(user);
		}

		for (final WebProject webProject : webProjects) {
			identifications.add(webProject.getIdentification());
		}

		return identifications;
	}

	@Override
	public List<String> getAllIdentifications() {
		final List<WebProject> webProjects = webProjectRepository.findAll();
		final List<String> allIdentifications = new ArrayList<>();

		for (final WebProject webProject : webProjects) {
			allIdentifications.add(webProject.getIdentification());
		}

		return allIdentifications;
	}

	@Override
	public boolean webProjectExists(String identification) {
		if (webProjectRepository.findByIdentification(identification) != null)
			return true;
		else
			return false;
	}

	@Override
	public void createWebProject(WebProject webProject, String userId) {
		if (!webProjectExists(webProject.getIdentification())) {
			log.debug("Web Project does not exist, creating..");
			final User user = userService.getUser(userId);
			webProject.setUser(user);
			if (webProject.getMainFile().isEmpty()) {
				webProject.setMainFile("index.html");
			}
			createFolderWebProject(webProject.getIdentification(), userId);
			webProjectRepository.save(webProject);

		} else {
			throw new WebProjectServiceException(
					"Web Project with identification: " + webProject.getIdentification() + " already exists");
		}
	}

	@Override
	public WebProject getWebProjectById(String webProjectId, String userId) {
		final WebProject webProject = webProjectRepository.findById(webProjectId);
		final User user = userService.getUser(userId);
		if (webProject != null) {
			if (hasUserPermissionToEditWebProject(user, webProject)) {
				return webProject;
			} else {
				throw new WebProjectServiceException(USER_UNAUTH);
			}
		} else {
			return null;
		}

	}

	public boolean hasUserPermissionToEditWebProject(User user, WebProject webProject) {
		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			return true;
		} else if (webProject.getUser().getUserId().equals(user.getUserId())) {
			return true;
		} else {
			return false;
		}
	}

	public String getWebProjectURL(String identification) {
		final WebProject webProject = webProjectRepository.findByIdentification(identification);
		return rootWWW + webProject.getIdentification() + "/" + webProject.getMainFile();
	}

	@Override
	public void updateWebProject(WebProject webProject, String userId) {
		final WebProject wp = webProjectRepository.findById(webProject.getId());
		final User user = userService.getUser(userId);

		if (wp != null) {
			if (hasUserPermissionToEditWebProject(user, wp)) {
				if (webProjectExists(wp.getIdentification())) {
					wp.setDescription(webProject.getDescription());
					wp.setMainFile(webProject.getMainFile());
					updateFolderWebProject(webProject.getIdentification(), userId);
					webProjectRepository.save(wp);
				} else {
					throw new WebProjectServiceException(
							"Web Project with identification:" + webProject.getIdentification() + " not exist");
				}
			} else {
				throw new WebProjectServiceException(USER_UNAUTH);
			}
		} else
			throw new WebProjectServiceException("Web project does not exist");
	}

	@Override
	public void deleteWebProject(WebProject webProject, String userId) {
		final WebProject wp = webProjectRepository.findById(webProject.getId());
		final User user = userService.getUser(userId);

		if (hasUserPermissionToEditWebProject(user, wp)) {
			deleteFolder(rootFolder + wp.getIdentification() + "/");
			webProjectRepository.delete(webProject);
		} else {
			throw new WebProjectServiceException(USER_UNAUTH);
		}
	}

	@Override
	public void deleteWebProjectById(String id, String userId) {
		final WebProject wp = webProjectRepository.findById(id);
		final User user = userService.getUser(userId);

		if (hasUserPermissionToEditWebProject(user, wp)) {
			deleteFolder(rootFolder + wp.getIdentification() + "/");
			webProjectRepository.delete(wp);
		} else {
			throw new WebProjectServiceException(USER_UNAUTH);
		}
	}

	@Override
	public void uploadZip(MultipartFile file, String userId) {

		final String folder = rootFolder + userId + "/";

		deleteFolder(folder);
		uploadFileToFolder(file, folder);
		unzipFile(folder, file.getOriginalFilename());
	}

	private void uploadFileToFolder(MultipartFile file, String path) {

		final String fileName = file.getOriginalFilename();
		byte[] bytes;
		try {
			bytes = file.getBytes();

			final InputStream is = new ByteArrayInputStream(bytes);

			final File folder = new File(path);
			if (!folder.exists()) {
				folder.mkdirs();
			}

			final String fullPath = path + fileName;
			final OutputStream os = new FileOutputStream(new File(fullPath));

			IOUtils.copy(is, os);

			is.close();
			os.close();
		} catch (final IOException e) {
			throw new WebProjectServiceException("Error uploading files " + e);
		}

		log.debug("File: " + path + fileName + " uploaded");
	}

	public static void deleteFolder(String path) {
		final File folder = new File(path);
		final File[] files = folder.listFiles();
		if (files != null) {
			for (final File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f.getAbsolutePath());
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	private void createFolderWebProject(String identification, String userId) {

		final File file = new File(rootFolder + userId + "/");
		if (file.exists() && file.isDirectory()) {
			final File newFile = new File(rootFolder + identification + "/");
			if (!file.renameTo(newFile)) {
				throw new WebProjectServiceException("Cannot create web project folder " + identification);
			}
			log.debug("New folder for Web Project " + identification + " has been created");
		}
	}

	private void updateFolderWebProject(String identification, String userId) {

		final File file = new File(rootFolder + userId + "/");
		if (file.exists() && file.isDirectory()) {
			deleteFolder(rootFolder + identification + "/");
			final File newFile = new File(rootFolder + identification + "/");
			if (!file.renameTo(newFile)) {
				throw new WebProjectServiceException("Cannot create web project folder " + identification);
			}
			log.debug("Folder for Web Project " + identification + " has been created");
		}
	}

	private void unzipFile(String path, String fileName) {

		final File folder = new File(path + fileName);
		log.debug("Unzipping zip file: " + folder);
		if (folder.exists()) {
			try (ZipInputStream zis = new ZipInputStream(new FileInputStream(folder))) {
			    byte[] buffer = new byte[4];
                byte [] zipbf = new byte[] {0x50, 0x4B, 0x03, 0x04};

                FileInputStream is = new FileInputStream(folder);
                is.read(buffer);
                is.close();
                if(!Arrays.equals(buffer, zipbf)) {
                    throw new WebProjectServiceException("Error: Invalid file");
                }
                
				ZipEntry ze;

				while (null != (ze = zis.getNextEntry())) {

					if (ze.isDirectory()) {

						final File f = new File(path + ze.getName());
						f.mkdirs();

					} else {
						log.debug("Unzipping file: " + ze.getName());

						final FileOutputStream fos = new FileOutputStream(path + ze.getName());

						IOUtils.copy(zis, fos);

						fos.close();
						zis.closeEntry();
					}
				}

				if (folder.exists() && folder.delete()) {
					log.debug("Zip: " + folder + " deleted");
				}

			} catch (final IOException e) {
				throw new WebProjectServiceException("Error unzipping files " + e);
			}

		} else {
			throw new WebProjectServiceException("Folder : " + folder + " does not exist");
		}
	}

}
