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
package com.minsait.onesait.platform.persistence.mongodb.services;

import java.io.IOException;
import java.util.UUID;

import javax.persistence.PersistenceException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minsait.onesait.platform.commons.model.ContextData;
import com.minsait.onesait.platform.commons.testing.IntegrationTest;
import com.minsait.onesait.platform.persistence.interfaces.BasicOpsDBRepository;
import com.minsait.onesait.platform.persistence.mongodb.template.MongoDbTemplateImpl;

@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(IntegrationTest.class)
public class QueryAsTextMongoDBImplIntegrationTest {

	@Autowired
	QueryAsTextMongoDBImpl queryTool;

	@Autowired
	MongoDbTemplateImpl connect;

	@Autowired
	@Qualifier("MongoBasicOpsDBRepository")
	BasicOpsDBRepository repository;

	@Autowired
	MongoTemplate nativeTemplate;
	static final String ONT_NAME = "contextData";
	static final String DATABASE = "onesaitplatform_rtdb";

	String refOid = "";

	@Before
	public void setUp() throws PersistenceException, IOException {
		if (!connect.collectionExists(DATABASE, ONT_NAME))
			connect.createCollection(DATABASE, ONT_NAME);
		// 1º
		ContextData data = ContextData
				.builder("user", UUID.randomUUID().toString(), UUID.randomUUID().toString(), System.currentTimeMillis(), "Testing")
				.clientConnection(UUID.randomUUID().toString()).deviceTemplate(UUID.randomUUID().toString())
				.device(UUID.randomUUID().toString()).clientSession(UUID.randomUUID().toString()).build();
		ObjectMapper mapper = new ObjectMapper();
		refOid = repository.insert(ONT_NAME, "", mapper.writeValueAsString(data));
		// 2º
		data = ContextData
				.builder("admin", UUID.randomUUID().toString(), UUID.randomUUID().toString(),
						System.currentTimeMillis(), "Testing")
				.clientConnection(UUID.randomUUID().toString()).deviceTemplate(UUID.randomUUID().toString())
				.device(UUID.randomUUID().toString()).clientSession(UUID.randomUUID().toString()).build();
		mapper = new ObjectMapper();
		refOid = repository.insert(ONT_NAME, "", mapper.writeValueAsString(data));
		// 3º
		data = ContextData
				.builder("other", UUID.randomUUID().toString(), UUID.randomUUID().toString(),
						System.currentTimeMillis(), "Testing")
				.clientConnection(UUID.randomUUID().toString()).deviceTemplate(UUID.randomUUID().toString())
				.device(UUID.randomUUID().toString()).clientSession(UUID.randomUUID().toString()).build();
		mapper = new ObjectMapper();
		refOid = repository.insert(ONT_NAME, "", mapper.writeValueAsString(data));
	}

	@After
	public void tearDown() {
		connect.dropCollection(DATABASE, ONT_NAME);
	}

	@Test
	public void test1_remove() {
		try {
			//
			String json = queryTool.querySQLAsJson(ONT_NAME, "select count(contextData._id) from contextData", 0);
			Assert.assertTrue(json.indexOf("3") != -1);
			json = queryTool.queryNativeAsJson(ONT_NAME, "db." + ONT_NAME + ".remove({})");
			json = queryTool.querySQLAsJson(ONT_NAME, "select count(*) from contextData", 0);
			Assert.assertTrue(json.indexOf("3") == -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test1_QueryNativeLimit() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME, "db." + ONT_NAME + ".find({'user':'user'}).limit(2)", 0,
					0);
			Assert.assertTrue(json.indexOf("user") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test1_QueryNativeProjections() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME,
					"db." + ONT_NAME + ".find({'user':'user'},{user:1,_id:0})", 0, 0);
			Assert.assertTrue(json.indexOf("user") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test1_QuerySort() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME, "db." + ONT_NAME + ".find().sort({'user':-1})", 0, 0);
			Assert.assertTrue(json.indexOf("user") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test1_QuerySkip() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME, "db." + ONT_NAME + ".find().skip(2)", 0, 0);
			Assert.assertTrue(json.indexOf("other") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test1_QueryNativeType4() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME, "db." + ONT_NAME + ".find()", 0, 0);
			Assert.assertTrue(json.indexOf("user") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test_QueryNativeType1() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME, "{}", 0, 0);
			Assert.assertTrue(json.indexOf("user") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test_QueryNativeType2() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME, "{'user':'user'}", 0, 0);
			Assert.assertTrue(json.indexOf("user") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test_QueryNativeType3() {
		try {
			String json = queryTool.queryNativeAsJson(ONT_NAME, "db." + ONT_NAME + ".find({\"user\":\"admin\"})", 0, 0);
			Assert.assertTrue(json.indexOf("admin") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QueryNative" + e.getMessage());
		}
	}

	@Test
	public void test_QuerySQL() {
		try {
			String json = queryTool.querySQLAsJson(ONT_NAME, "select * from contextData", 0);
			Assert.assertTrue(json.indexOf("user") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QuerySQL" + e.getMessage());
		}
	}

	@Test
	public void test_QueryCountSQL() {
		try {
			String json = queryTool.querySQLAsJson(ONT_NAME, "select count(contextData._id) from contextData", 0);
			Assert.assertTrue(json.indexOf("3") != -1);
		} catch (Exception e) {
			Assert.fail("Error test_QuerySQL" + e.getMessage());
		}
	}

	@Test
	public void test_createAndDropIndex() {
		try {
			String result = queryTool.queryNativeAsJson(ONT_NAME,
					"db.contextData.createIndex({'user':1},{'name':'user_i'})");
			Assert.assertTrue(result.indexOf("Created index") != -1);
			result = queryTool.queryNativeAsJson(ONT_NAME, "db.contextData.getIndexes()");
			Assert.assertTrue(result.indexOf("user_i") != -1);
			result = queryTool.queryNativeAsJson(ONT_NAME, "db.contextData.dropIndex('user_i')");
			Assert.assertTrue(result.indexOf("Dropped index") != -1);
		} catch (Exception e) {
			Assert.fail("test1_createIndex:" + e.getMessage());
		}
	}

	@Test
	public void test_InsertAndUpdateAndRemove() {
		try {
			String result = queryTool.queryNativeAsJson(ONT_NAME, "db.contextData.count()");
			Assert.assertTrue(result.indexOf("0") == -1);
			//
			result = queryTool.queryNativeAsJson(ONT_NAME,
					"db.contextData.insert({\"user\":\"user_temp_1\",\"deviceTemplate\":\"1\"})");
			Assert.assertTrue(result.indexOf("Inserted row") != -1);
			result = queryTool.queryNativeAsJson(ONT_NAME, "db.contextData.remove({\"user\":\"user_temp_1\"})");
			Assert.assertTrue(result.indexOf("Deleted {\"count\":1} rows") != -1);
			//
			result = queryTool.queryNativeAsJson(ONT_NAME,
					"db.contextData.insert({'user':'user_temp_2','deviceTemplate':'2'})");
			Assert.assertTrue(result.indexOf("Inserted row") != -1);
			result = queryTool.queryNativeAsJson(ONT_NAME,
					"db.contextData.update({'user':'user_temp_2'},{'deviceTemplate':'3'})");
			Assert.assertTrue(result.indexOf("Updated {\"count\":1} rows") != -1);
			//

			result = queryTool.queryNativeAsJson(ONT_NAME,
					"db.contextData.remove({'user':'user_temp_2','deviceTemplate':'3'})");
			Assert.assertTrue(result.indexOf("Deleted {\"count\":1} rows") != -1);

		} catch (Exception e) {
			Assert.fail("test_InsertAndUpdateAndRemove:" + e.getMessage());
		}
	}

}
