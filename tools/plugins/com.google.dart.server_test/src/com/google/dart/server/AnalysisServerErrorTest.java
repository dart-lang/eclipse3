/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.server;

import junit.framework.TestCase;

public class AnalysisServerErrorTest extends TestCase {
  public void test_getMessage() throws Exception {
    AnalysisServerError error = new AnalysisServerError(
        AnalysisServerErrorCode.INVALID_CONTEXT_ID,
        "my-id");
    assertEquals("Cannot find a context with the id 'my-id'", error.getMessage());
    assertEquals(
        "[code=INVALID_CONTEXT_ID, message=Cannot find a context with the id 'my-id']",
        error.toString());
  }
}