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
package com.minsait.onesait.platform.rulesengine;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.minsait.onesait.platform.commons.testing.IntegrationTest;
import com.minsait.onesait.platform.config.model.DroolsRule;
import com.minsait.onesait.platform.config.model.DroolsRuleDomain;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.repository.DroolsRuleDomainRepository;
import com.minsait.onesait.platform.config.repository.DroolsRuleRepository;
import com.minsait.onesait.platform.config.repository.OntologyRepository;
import com.minsait.onesait.platform.config.repository.UserRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
@Category(IntegrationTest.class)
@Ignore
public class SpringBootRuleLoader {

	@Autowired
	private DroolsRuleDomainRepository ruleDomainRepository;
	@Autowired
	private DroolsRuleRepository ruleRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private OntologyRepository ontologyRepository;

	private static final String RULE_ROLE = "package com.minsait.onesait.platform.rulesengine;\n"
			+ "import com.minsait.onesait.platform.rulesengine.model.OntologyJsonWrapper;\n"
			+ "global com.minsait.onesait.platform.rulesengine.model.OntologyJsonWrapper input;\n"
			+ "global com.minsait.onesait.platform.rulesengine.model.OntologyJsonWrapper output;\n" + "/*\n"
			+ "Input JSON data is wrapped into an OntologyJsonWrapper\n" + "Here are the methods that you can invoke\n"
			+ "\n" + "input.getProperty(\"anyProperty\") -> gets a property from input JSON\n"
			+ "input.toJson() -> serializes input as a string\n" + "\n"
			+ "Results are extracted from output variable which is also an OntologyJsonWrapper\n"
			+ "Here are the methods that you can invoke\n" + "\n"
			+ "output.setRootNode(\"rootNode\") -> sets Json output root node for ontology validation\n"
			+ "output.setProperty(\"property\", anyValue) -> sets a new Property\n"
			+ "output.copyInputToOuput(input) -> copies al properties from input to output\n"
			+ "output.toJson() -> serializes output as a string\n" + "*/\n" + "dialect  \"mvel\"\n" + "\n"
			+ "rule \"Assign role\"\n" + "\n" + "    when\n"
			+ "        eval( input.getProperty(\"currentSalary\") < 1000000 && input.getProperty(\"experienceInYears\") > 10 )\n"
			+ "    then\n" + "    	\n" + "        output.setProperty(\"role\", \"Manager\");\n" + "end";

	@Test
	public void loadSampleRules() {
		final User admin = userRepository.findByUserId("administrator");
		if (ruleDomainRepository.findByUser(admin) == null) {
			final DroolsRuleDomain domain = new DroolsRuleDomain();
			domain.setActive(true);
			domain.setIdentification("admin_domain");
			domain.setUser(admin);
			ruleDomainRepository.save(domain);
		}

		final DroolsRule rule1 = new DroolsRule();
		rule1.setDRL(RULE_ROLE);
		rule1.setUser(admin);
		rule1.setIdentification("SET_ROLE_RULE");
		rule1.setSourceOntology(ontologyRepository.findByIdentification("Salarios"));
		rule1.setTargetOntology(ontologyRepository.findByIdentification("Salarios"));
		ruleRepository.save(rule1);

	}
}
