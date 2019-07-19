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
package com.minsait.onesait.platform.streamsets.processor;

import com.minsait.onesait.platform.streamsets.EventCreator;

public class OnesaitProcessorEvents {
	  public static final String RECORD_FOUND_TAG = "records-found";
	  public static final String RECORD_NOT_FOUND_TAG = "records-not-found";
	  
	  public static EventCreator RECORD_FOUND = new EventCreator.Builder(OnesaitProcessorEvents.RECORD_FOUND_TAG, 2)
			    .withOptionalField("records-found")
			    .build();
	 
	  public static EventCreator RECORD_NOT_FOUND = new EventCreator.Builder(OnesaitProcessorEvents.RECORD_NOT_FOUND_TAG, 2)
			    .withOptionalField("records-not-found")
			    .build();

	  private OnesaitProcessorEvents() {
	    // instantiation not permitted.
	  }

}