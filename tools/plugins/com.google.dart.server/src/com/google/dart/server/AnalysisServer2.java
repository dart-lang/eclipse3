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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server;

import java.util.List;
import java.util.Map;

/**
 * The interface {@code AnalysisServer} defines the behavior of objects that interface to an
 * analysis server.
 *
 * @coverage dart.server
 */
public interface AnalysisServer2 {

  /**
   * Add the given listener to the list of listeners that will receive notification when new
   * analysis results become available.
   *
   * @param listener the listener to be added
   */
  public void addAnalysisServerListener(AnalysisServerListener listener);
  /**
   * Start the analysis server.
   *
   * @param millisToRestart the number of milliseconds to wait for an unresponsive server before
   *          restarting it, or zero if the server should not be restarted.
   */
  public void start(long millisToRestart) throws Exception;

  /**
   * {@code analysis.getErrors}
   *
   * Return the errors associated with the given file. If the errors for the given file have not yet
   * been computed, or the most recently computed errors for the given file are out of date, then the
   * response for this request will be delayed until they have been computed. If some or all of the
   * errors for the file cannot be computed, then the subset of the errors that can be computed will
   * be returned and the response will contain an error to indicate why the errors could not be
   * computed.
   *
   * This request is intended to be used by clients that cannot asynchronously apply updated error
   * information. Clients that can apply error information as it becomes available should use the
   * information provided by the 'analysis.errors' notification.
   *
   * @param file The file for which errors are being requested.
   */
  // public void analysis_getErrors(String file, GetErrorsConsumer consumer);

  /**
   * {@code analysis.getHover}
   *
   * Return the hover information associate with the given location. If some or all of the hover
   * information is not available at the time this request is processed the information will be
   * omitted from the response.
   *
   * @param file The file in which hover information is being requested.
   * @param offset The offset for which hover information is being requested.
   */
  // public void analysis_getHover(String file, int offset, GetHoverConsumer consumer);

  /**
   * {@code analysis.reanalyze}
   *
   * Force the re-analysis of everything contained in the existing analysis roots. This will cause
   * all previously computed analysis results to be discarded and recomputed, and will cause all
   * subscribed notifications to be re-sent.
   */
  // public void analysis_reanalyze();

  /**
   * {@code analysis.setAnalysisRoots}
   *
   * Sets the root paths used to determine which files to analyze. The set of files to be analyzed
   * are all of the files in one of the root paths that are not also in one of the excluded paths.
   *
   * Note that this request determines the set of requested analysis roots. The actual set of
   * analysis roots at any given time is the intersection of this set with the set of files and
   * directories actually present on the filesystem. When the filesystem changes, the actual set of
   * analysis roots is automatically updated, but the set of requested analysis roots is unchanged.
   * This means that if the client sets an analysis root before the root becomes visible to server in
   * the filesystem, there is no error; once the server sees the root in the filesystem it will start
   * analyzing it. Similarly, server will stop analyzing files that are removed from the file system
   * but they will remain in the set of requested roots.
   *
   * If an included path represents a file, then server will look in the directory containing the
   * file for a pubspec.yaml file. If none is found, then the parents of the directory will be
   * searched until such a file is found or the root of the file system is reached. If such a file is
   * found, it will be used to resolve package: URI’s within the file.
   *
   * @param included A list of the files and directories that should be analyzed.
   * @param excluded A list of the files and directories within the included directories that should
   * not be analyzed.
   */
  // public void analysis_setAnalysisRoots(List<String> included, List<String> excluded);

  /**
   * {@code analysis.setPriorityFiles}
   *
   * Set the priority files to the files in the given list. A priority file is a file that is given
   * priority when scheduling which analysis work to do first. The list typically contains those
   * files that are visible to the user and those for which analysis results will have the biggest
   * impact on the user experience. The order of the files within the list is significant: the first
   * file will be given higher priority than the second, the second higher priority than the third,
   * and so on.
   *
   * Note that this request determines the set of requested priority files. The actual set of
   * priority files is the intersection of the requested set of priority files with the set of files
   * currently subject to analysis. (See analysis.setSubscriptions for a description of files that
   * are subject to analysis.)
   *
   * If a requested priority file is a directory it is ignored, but remains in the set of requested
   * priority files so that if it later becomes a file it can be included in the set of actual
   * priority files.
   *
   * @param files The files that are to be a priority for analysis.
   */
  // public void analysis_setPriorityFiles(List<String> files);

  /**
   * {@code analysis.setSubscriptions}
   *
   * Subscribe for services. All previous subscriptions are replaced by the current set of
   * subscriptions. If a given service is not included as a key in the map then no files will be
   * subscribed to the service, exactly as if the service had been included in the map with an
   * explicit empty list of files.
   *
   * Note that this request determines the set of requested subscriptions. The actual set of
   * subscriptions at any given time is the intersection of this set with the set of files currently
   * subject to analysis. The files currently subject to analysis are the set of files contained
   * within an actual analysis root but not excluded, plus all of the files transitively reachable
   * from those files via import, export and part directives. (See analysis.setAnalysisRoots for an
   * explanation of how the actual analysis roots are determined.) When the actual analysis roots
   * change, the actual set of subscriptions is automatically updated, but the set of requested
   * subscriptions is unchanged.
   *
   * If a requested subscription is a directory it is ignored, but remains in the set of requested
   * subscriptions so that if it later becomes a file it can be included in the set of actual
   * subscriptions.
   *
   * It is an error if any of the keys in the map are not valid services. If there is an error, then
   * the existing subscriptions will remain unchanged.
   *
   * @param subscriptions A table mapping services to a list of the files being subscribed to the
   * service.
   */
  // public void analysis_setSubscriptions(Map<String, List<String>> subscriptions);

  /**
   * {@code analysis.updateContent}
   *
   * Update the content of one or more files. Files that were previously updated but not included in
   * this update remain unchanged. This effectively represents an overlay of the filesystem. The
   * files whose content is overridden are therefore seen by server as being files with the given
   * content, even if the files do not exist on the filesystem or if the file path represents the
   * path to a directory on the filesystem.
   *
   * @param files A table mapping the files whose content has changed to a description of the content
   * change. Each value should be one of the following types: AddContentOverlay,
   * ChangeContentOverlay, or RemoveContentOverlay.
   */
  // public void analysis_updateContent(Map<String, Object> files);

  /**
   * {@code analysis.updateOptions}
   *
   * Update the options controlling analysis based on the given set of options. Any options that are
   * not included in the analysis options will not be changed. If there are options in the analysis
   * options that are not valid an error will be reported but the values of the valid options will
   * still be updated.
   *
   * @param options The options that are to be used to control analysis.
   */
  // public void analysis_updateOptions(AnalysisOptions options);

  /**
   * {@code completion.getSuggestions}
   *
   * Request that completion suggestions for the given offset in the given file be returned.
   *
   * @param file The file containing the point at which suggestions are to be made.
   * @param offset The offset within the file at which suggestions are to be made.
   */
  // public void completion_getSuggestions(String file, int offset, GetSuggestionsConsumer consumer);

  /**
   * {@code debug.createContext}
   *
   * Create a debugging context for the executable file with the given path. The context that is
   * created will persist until debug.deleteContext is used to delete it. Clients, therefore, are
   * responsible for managing the lifetime of debugging contexts.
   *
   * @param contextRoot The path of the Dart or HTML file that will be launched.
   */
  // public void debug_createContext(String contextRoot, CreateContextConsumer consumer);

  /**
   * {@code debug.deleteContext}
   *
   * Delete the debugging context with the given identifier. The context id is no longer valid after
   * this command. The server is allowed to re-use ids when they are no longer valid.
   *
   * @param id The identifier of the debugging context that is to be deleted.
   */
  // public void debug_deleteContext(String id);

  /**
   * {@code debug.mapUri}
   *
   * Map a URI from the debugging context to the file that it corresponds to, or map a file to the
   * URI that it corresponds to in the debugging context.
   *
   * Exactly one of the file and uri fields must be provided.
   *
   * @param id The identifier of the debugging context in which the URI is to be mapped.
   * @param file The path of the file to be mapped into a URI.
   * @param uri The URI to be mapped into a file path.
   */
  // public void debug_mapUri(String id, String file, String uri, MapUriConsumer consumer);

  /**
   * {@code debug.setSubscriptions}
   *
   * Subscribe for services. All previous subscriptions are replaced by the given set of services.
   *
   * It is an error if any of the elements in the list are not valid services. If there is an error,
   * then the current subscriptions will remain unchanged.
   *
   * @param subscriptions A list of the services being subscribed to.
   */
  // public void debug_setSubscriptions(List<String> subscriptions);

  /**
   * {@code edit.getAssists}
   *
   * Return the set of assists that are available at the given location. An assist is distinguished
   * from a refactoring primarily by the fact that it affects a single file and does not require user
   * input in order to be performed.
   *
   * @param file The file containing the code for which assists are being requested.
   * @param offset The offset of the code for which assists are being requested.
   * @param length The length of the code for which assists are being requested.
   */
  // public void edit_getAssists(String file, int offset, int length, GetAssistsConsumer consumer);

  /**
   * {@code edit.getAvailableRefactorings}
   *
   * Get a list of the kinds of refactorings that are valid for the given selection in the given
   * file.
   *
   * @param file The file containing the code on which the refactoring would be based.
   * @param offset The offset of the code on which the refactoring would be based.
   * @param length The length of the code on which the refactoring would be based.
   */
  // public void edit_getAvailableRefactorings(String file, int offset, int length, GetAvailableRefactoringsConsumer consumer);

  /**
   * {@code edit.getFixes}
   *
   * Return the set of fixes that are available for the errors at a given offset in a given file.
   *
   * @param file The file containing the errors for which fixes are being requested.
   * @param offset The offset used to select the errors for which fixes will be returned.
   */
  // public void edit_getFixes(String file, int offset, GetFixesConsumer consumer);

  /**
   * {@code edit.getRefactoring}
   *
   * Get the changes required to perform a refactoring.
   *
   * @param kindId The identifier of the kind of refactoring to be performed.
   * @param file The file containing the code involved in the refactoring.
   * @param offset The offset of the region involved in the refactoring.
   * @param length The length of the region involved in the refactoring.
   * @param validateOnly True if the client is only requesting that the values of the options be
   * validated and no change be generated.
   * @param options Data used to provide values provided by the user. The structure of the data is
   * dependent on the kind of refactoring being performed. The data that is expected is documented in
   * the section titled Refactorings, labeled as “Options”. This field can be omitted if the
   * refactoring does not require any options or if the values of those options are not known.
   */
  // public void edit_getRefactoring(String kindId, String file, int offset, int length, boolean validateOnly, Object options, GetRefactoringConsumer consumer);

  /**
   * {@code search.findElementReferences}
   *
   * Perform a search for references to the element defined or referenced at the given offset in the
   * given file.
   *
   * An identifier is returned immediately, and individual results will be returned via the
   * search.results notification as they become available.
   *
   * @param file The file containing the declaration of or reference to the element used to define
   * the search.
   * @param offset The offset within the file of the declaration of or reference to the element.
   * @param includePotential True if potential matches are to be included in the results.
   */
  // public void search_findElementReferences(String file, int offset, boolean includePotential, FindElementReferencesConsumer consumer);

  /**
   * {@code search.findMemberDeclarations}
   *
   * Perform a search for declarations of members whose name is equal to the given name.
   *
   * An identifier is returned immediately, and individual results will be returned via the
   * search.results notification as they become available.
   *
   * @param name The name of the declarations to be found.
   */
  // public void search_findMemberDeclarations(String name, FindMemberDeclarationsConsumer consumer);

  /**
   * {@code search.findMemberReferences}
   *
   * Perform a search for references to members whose name is equal to the given name. This search
   * does not check to see that there is a member defined with the given name, so it is able to find
   * references to undefined members as well.
   *
   * An identifier is returned immediately, and individual results will be returned via the
   * search.results notification as they become available.
   *
   * @param name The name of the references to be found.
   */
  // public void search_findMemberReferences(String name, FindMemberReferencesConsumer consumer);

  /**
   * {@code search.findTopLevelDeclarations}
   *
   * Perform a search for declarations of top-level elements (classes, typedefs, getters, setters,
   * functions and fields) whose name matches the given pattern.
   *
   * An identifier is returned immediately, and individual results will be returned via the
   * search.results notification as they become available.
   *
   * @param pattern The regular expression used to match the names of the declarations to be found.
   */
  // public void search_findTopLevelDeclarations(String pattern, FindTopLevelDeclarationsConsumer consumer);

  /**
   * {@code search.getTypeHierarchy}
   *
   * Return the type hierarchy of the class declared or referenced at the given location.
   *
   * @param file The file containing the declaration or reference to the type for which a hierarchy
   * is being requested.
   * @param offset The offset of the name of the type within the file.
   */
  // public void search_getTypeHierarchy(String file, int offset, GetTypeHierarchyConsumer consumer);

  /**
   * {@code server.getVersion}
   *
   * Return the version number of the analysis server.
   */
  // public void server_getVersion(GetVersionConsumer consumer);

  /**
   * {@code server.setSubscriptions}
   *
   * Subscribe for services. All previous subscriptions are replaced by the given set of services.
   *
   * It is an error if any of the elements in the list are not valid services. If there is an error,
   * then the current subscriptions will remain unchanged.
   *
   * @param subscriptions A list of the services being subscribed to.
   */
  // public void server_setSubscriptions(List<String> subscriptions);

  /**
   * {@code server.shutdown}
   *
   * Cleanly shutdown the analysis server. Requests that are received after this request will not be
   * processed. Requests that were received before this request, but for which a response has not yet
   * been sent, will not be responded to. No further responses or notifications will be sent after
   * the response to this request has been sent.
   */
  // public void server_shutdown();
}
