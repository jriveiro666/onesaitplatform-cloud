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
package com.minsait.onesait.platform.config.services.user;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.minsait.onesait.platform.config.model.ClientPlatform;
import com.minsait.onesait.platform.config.model.Role;
import com.minsait.onesait.platform.config.model.Token;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.model.UserToken;
import com.minsait.onesait.platform.config.repository.ClientPlatformRepository;
import com.minsait.onesait.platform.config.repository.RoleRepository;
import com.minsait.onesait.platform.config.repository.TokenRepository;
import com.minsait.onesait.platform.config.repository.UserRepository;
import com.minsait.onesait.platform.config.repository.UserTokenRepository;
import com.minsait.onesait.platform.config.services.deletion.EntityDeletionService;
import com.minsait.onesait.platform.config.services.exceptions.UserServiceException;
import com.minsait.onesait.platform.config.services.usertoken.UserTokenService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private UserTokenRepository userTokenRepository;
	@Autowired
	private TokenRepository tokenRepository;
	@Autowired
	private ClientPlatformRepository clientPlatformRepository;
	@Autowired
	private EntityDeletionService entityDeletionService;
	@Autowired
	private UserTokenService userTokenService;

	@Override
	public boolean isUserAdministrator(User user) {
		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.name()))
			return true;
		if (user.getRole().getRoleParent() != null
				&& user.getRole().getRoleParent().getId().equals(Role.Type.ROLE_ADMINISTRATOR.name()))
			return true;
		return false;
	}

	@Override
	public boolean isUserDeveloper(User user) {
		if (user.getRole().getId().equals(Role.Type.ROLE_DEVELOPER.name()))
			return true;
		if (user.getRole().getRoleParent() != null
				&& user.getRole().getRoleParent().getId().equals(Role.Type.ROLE_DEVELOPER.name()))
			return true;
		return false;
	}

	@Override
	public Token getToken(String token) {
		return tokenRepository.findByToken(token);
	}

	@Override
	public UserToken getUserToken(String user, String token) {
		return userTokenRepository.findByUserAndToken(user, token);
	}

	@Override
	public User getUser(UserToken token) {
		return token.getUser();
	}

	@Override
	public User getUserByToken(String token) {
		final UserToken usertoken = userTokenRepository.findByToken(token);
		if (usertoken != null) {
			final User user = usertoken.getUser();
			return user;
		}
		return null;

	}

	@Override
	public User getUserByEmail(String email) {
		final User user = userRepository.findUserByEmail(email);
		return user;
	}

	@Override
	public User getUser(String userId) {
		return userRepository.findByUserId(userId);
	}

	@Override
	public List<Role> getAllRoles() {
		return roleRepository.findAll();
	}

	@Override
	public List<UserToken> getUserToken(User userId) {
		return userTokenRepository.findByUser(userId);
	}

	@Override
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@Override
	public List<User> getAllActiveUsers() {
		return userRepository.findAllActiveUsers();
	}

	@Override
	public List<User> getAllUsersByCriteria(String userId, String fullName, String email, String roleType,
			Boolean active) {
		List<User> users;

		if (active != null) {
			users = userRepository.findByUserIdOrFullNameOrEmailOrRoleTypeAndActive(userId, fullName, email, roleType,
					active);
		} else {
			users = userRepository.findByUserIdOrFullNameOrEmailOrRoleType(userId, fullName, email, roleType);
		}

		return users;

	}

	@Override
	public void createUser(User user) {
		if (!userExists(user)) {
			log.debug("User no exist, creating...");
			user.setRole(roleRepository.findByName(user.getRole().getName()));
			userRepository.save(user);

			try {
				userTokenService.generateToken(user);
			} catch (final Exception e) {
				log.debug("Error creating userToken");
			}
		} else {
			throw new UserServiceException("User already exists in Database");
		}
	}

	@Override
	public void registerRoleDeveloper(User user) {

		user.setRole(getRole(Role.Type.ROLE_DEVELOPER));
		user.setActive(true);
		log.debug("Creating user with Role Developer default");

		createUser(user);

	}

	@Override
	public void registerRoleUser(User user) {

		user.setActive(true);
		user.setRole(getRole(Role.Type.ROLE_USER));
		log.debug("Creating user with Role User default");

		createUser(user);

	}

	@Override
	public boolean userExists(User user) {
		if (userRepository.findByUserId(user.getUserId()) != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void updatePassword(User user) {
		if (userExists(user)) {
			final User userDb = userRepository.findByUserId(user.getUserId());
			userDb.setPassword(user.getPassword());
			try {
				userRepository.save(userDb);
			} catch (final RuntimeException e) {
				throw new UserServiceException("Could not update password", e);
			}
		}
	}

	@Override
	public void updateUser(User user) {
		if (userExists(user)) {
			log.info("User exists in configdb");
			final User userDb = userRepository.findByUserId(user.getUserId());
			userDb.setEmail(user.getEmail());

			if (user.getRole() != null)
				userDb.setRole(roleRepository.findByName(user.getRole().getName()));

			// Update dateDeleted for in/active user
			if (!userDb.isActive() && user.isActive()) {
				userDb.setDateDeleted(null);
			}
			if (userDb.isActive() && !user.isActive()) {
				userDb.setDateDeleted(new Date());
			}

			userDb.setActive(user.isActive());
			if (user.getDateDeleted() != null) {
				userDb.setDateDeleted(user.getDateDeleted());
			}
			userDb.setFullName(user.getFullName());
			// new features Avatar and extra fields
			if (user.getAvatar() != null)
				userDb.setAvatar(user.getAvatar());
			if (user.getExtraFields() != null)
				userDb.setExtraFields(user.getExtraFields());

			userRepository.save(userDb);

			log.info("User have been updated in configdb");
		} else {
			throw new UserServiceException("Cannot update user that does not exist");
		}
	}

	@Override
	public Role getUserRole(String role) {
		return roleRepository.findByName(role);
	}

	@Override
	public void deleteUser(String userId) {
		entityDeletionService.deactivateUser(userId);
	}

	@Override
	public void deleteUser(List<String> userIds) {
		entityDeletionService.deactivateUser(userIds);

	}

	Role getRole(Role.Type roleType) {
		return roleRepository.findById(roleType.name());
	}

	@Override
	public List<ClientPlatform> getClientsForUser(User user) {
		List<ClientPlatform> clients;
		clients = clientPlatformRepository.findByUser(user);
		return clients;
	}

	@Override
	public UserToken getUserToken(String token) {
		return userTokenRepository.findByToken(token);
	}

	@Override
	public boolean emailExists(User user) {

		if ((userRepository.findByEmail(user.getEmail())).size() != 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public User getUserByIdentification(String identification) {
		return userRepository.findByUserId(identification);
	}

	@Override
	public User saveExistingUser(User user) {
		return userRepository.save(user);
	}

	@Override
	public Role getUserRoleById(String roleId) {
		return roleRepository.findById(roleId);
	}

	@Override
	public void hardDeleteUser(String userId) {
		entityDeletionService.deleteUser(userId);
	}

}
