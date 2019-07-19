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
package com.minsait.onesait.platform.streamsets.origin;

import com.minsait.onesait.platform.streamsets.EventCreator;

public class OnesaitOriginEvents {
	  public static final String NO_MORE_DATA_TAG = "no-more-data";
	  public static final String INIT_TAG = "init";

	  public static EventCreator NO_MORE_DATA = new EventCreator.Builder(OnesaitOriginEvents.NO_MORE_DATA_TAG, 2)
	    .withOptionalField("record-count")
	    .withOptionalField("error-count")
	    .withOptionalField("file-count")
	    .build();
	  
	  public static EventCreator INIT = new EventCreator.Builder(OnesaitOriginEvents.INIT_TAG, 2)
			  .withOptionalField("init-pipeline")
			  .build();

	  private OnesaitOriginEvents() {
	    // instantiation not permitted.
	  }

}