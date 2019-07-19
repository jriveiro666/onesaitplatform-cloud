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
package com.minsait.onesait.platform.streamsets.destination.beans;

import com.streamsets.pipeline.api.Label;

public enum TimeseriesTime implements Label {
  FIFTEENMINUTES("15 minutes"),
  TENMINUTES("10 minutes"),
  FIVEMINUTES("5 minutes"),
  MINUTES("1 minute"),
  THIRTYSECONDS("30 seconds"),
  TENSECONDS("10 seconds"),
  FIVESECONDS("5 seconds"),
  FOURSECONDS("4 seconds"),
  ONESECOND("1 second");

  private final String label;

  TimeseriesTime(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}