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
package com.minsait.onesait.platform.persistence.external.virtual.helper;

//import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
//import net.sf.jsqlparser.expression.LongValue;
//import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
//import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
//import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
//import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
//import net.sf.jsqlparser.statement.select.Select;
//import net.sf.jsqlparser.statement.select.Limit;
//import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
//import net.sf.jsqlparser.util.deparser.SelectDeParser;

@Component("MysqlVirtualOntologyHelper")
@Slf4j
public class MysqlVirtualOntologyHelper implements VirtualOntologyHelper {

	private final static String LIST_TABLES_QUERY = "SHOW TABLES";

	@Override
	public String getAllTablesStatement() {
		return LIST_TABLES_QUERY;
	}
	
	@Override
	public String addLimit(String query, final long limit) {
		return this.addLimit(query, limit, 0);
	}

	@Override
	public String addLimit(String query, final long limit, final long offset) {
		try {
			Statement statement = CCJSqlParserUtil.parse(query);
			if(statement instanceof Select) {
				PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
				final Boolean hasLimit = (select.getLimit() != null);
				
				if ( !hasLimit ) {
					Limit qlimit = new Limit();
					qlimit.setRowCount(new LongValue( (limit > 1 ? limit : 1) ));
					qlimit.setOffset(new LongValue( (offset > 0 ? offset : 0) ));
 					select.setLimit(qlimit);
				} else {
					final long nlimit = Math.min( ((LongValue) select.getLimit().getRowCount()).getValue(), limit );
					select.getLimit().setRowCount(new LongValue( (nlimit > 1 ? nlimit : 1) ));
					if(select.getLimit().getOffset() == null) 
						select.getLimit().setOffset(new LongValue( (offset >= 0 ? offset : 0) ));
				}
				
			}

			return statement.toString();
		} catch (JSQLParserException e) {
			log.error("Error adding LIMIT to query, returning the original. "+e.getMessage(), e);
			return query;
		}
	}
	
	

}
