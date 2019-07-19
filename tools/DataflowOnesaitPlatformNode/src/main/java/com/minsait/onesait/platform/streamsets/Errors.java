/**
 * Copyright minsait by Indra Sistemas, S.A.
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
/*******************************************************************************
 * © Indra Sistemas, S.A.
 * 2013 - 2014  SPAIN
 * 
 * All rights reserved
 ******************************************************************************/
package com.minsait.onesait.platform.streamsets;

import com.streamsets.pipeline.api.ErrorCode;
import com.streamsets.pipeline.api.GenerateResourceBundle;

@GenerateResourceBundle
public enum Errors implements ErrorCode {

	// General errors
  ERROR_00("Incorrect configuration: {}"),
  ERROR_01("Error writting record: {}, generated error is {}"),
  ERROR_02("Error reading file: {}"),
  ERROR_03("Error IO: {} in recover excel book from file: {}"),
  ERROR_04("Error IO: {} hadoop configuration: {}"),
  ERROR_05("Error joining to broker: {}"),
  ERROR_06("Error leaving from broker: {}"),
  
  // Origin errors
  ERROR_20("Error doing query: {}"),
  ERROR_21("Error parsing: {}"),
  
  //Destination errors
  ERROR_30("Error inserting: {}"),
  ERROR_31("Error updating: {}"),
  ERROR_32("Error doing join: {}"),
  ERROR_33("Error generating SQL: {}"),
  
  // Processor errors
  ERROR_40("Error adding data to record: {}"),
  ERROR_41("Error parsing json data: {}"),
  ERROR_42("Record not found: {}"),
  ERROR_43("Error generating SQL: {}"),
  ERROR_44("Error doing query: {}"),
  ;
  private final String msg;

  Errors(String msg) {
    this.msg = msg;
  }

  /** {@inheritDoc} */
  @Override
  public String getCode() {
    return name();
  }

  /** {@inheritDoc} */
  @Override
  public String getMessage() {
    return msg;
  }
}
