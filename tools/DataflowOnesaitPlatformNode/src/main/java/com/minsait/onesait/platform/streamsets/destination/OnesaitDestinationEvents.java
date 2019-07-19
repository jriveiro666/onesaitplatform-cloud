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
package com.minsait.onesait.platform.streamsets.destination;

import com.minsait.onesait.platform.streamsets.EventCreator;

public class OnesaitDestinationEvents {
	  public static final String DOCUMENT_MODIFIED_TAG = "document-modified";
	  public static final String DOCUMENT_NOT_MODIFIED_TAG = "document-not-modified";
	  
	  public static EventCreator DOCUMENT_MODIFIED = new EventCreator.Builder(OnesaitDestinationEvents.DOCUMENT_MODIFIED_TAG, 2)
			    .withOptionalField("data-updated")
			    .withOptionalField("data-inserted")
			    .withOptionalField("duplicated-keys")
			    .build();
	 
	  public static EventCreator DOCUMENT_NOT_MODIFIED = new EventCreator.Builder(OnesaitDestinationEvents.DOCUMENT_NOT_MODIFIED_TAG, 2)
			    .withOptionalField("data-not-updated")
			    .withOptionalField("data-not-inserted")
			    .withOptionalField("duplicated-keys")
			    .build();

	  private OnesaitDestinationEvents() {
	    // instantiation not permitted.
	  }

}