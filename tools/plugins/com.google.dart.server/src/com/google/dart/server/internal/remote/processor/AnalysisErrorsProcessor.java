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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.ExtendedRequestErrorCode;
import com.google.dart.server.GetErrorsConsumer;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.RequestError;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Instances of {@code AnalysisErrorsConsumer} translate JSON result objects for a given
 * {@link GetErrorsConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class AnalysisErrorsProcessor extends ResultProcessor {

  private final GetErrorsConsumer consumer;

  public AnalysisErrorsProcessor(GetErrorsConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject, RequestError requestError) {

    if (resultObject != null) {
      try {
        ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
        Iterator<JsonElement> iter = resultObject.get("errors").getAsJsonArray().iterator();
        while (iter.hasNext()) {
          JsonObject errorJsonObject = iter.next().getAsJsonObject();
          errors.add(AnalysisError.fromJson(errorJsonObject));
        }
        consumer.computedErrors(errors.toArray(new AnalysisError[errors.size()]));
      } catch (Exception e) {
        // catch any exceptions in the formatting of this response
        String message = e.getMessage();
        String stackTrace = null;
        if (e.getStackTrace() != null) {
          stackTrace = ExceptionUtils.getStackTrace(e);
        }
        requestError = new RequestError(
            ExtendedRequestErrorCode.INVALID_SERVER_RESPONSE,
            message != null ? message : "",
            stackTrace);
      }
    }
    if (requestError != null) {
      consumer.onError(requestError);
    }
  }
}
