/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal.formatter;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceAction;
import org.eclipse.ui.ide.ResourceUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches the <code>dartfmt</code> process collecting stdout, stderr, and exit code information.
 */
public class DartFormatter {

//  /**
//   * Run the formatter on the given input path.
//   * 
//   * @param path the path to pass to the formatter
//   * @param monitor the monitor for displaying progress
//   * @param console the console to which output should be directed
//   * @throws IOException if an exception was thrown during execution
//   * @throws CoreException if an exception occurs in file refresh
//   */
//  public static void format(IPath path, IProgressMonitor monitor, MessageConsole console)
//      throws IOException, CoreException {
//
//    File dartfmt = DartSdkManager.getManager().getSdk().getDartFmtExecutable();
//    if (!dartfmt.canExecute()) {
//      return;
//    }
//
//    ProcessBuilder builder = new ProcessBuilder();
//
//    List<String> args = new ArrayList<String>();
//    args.add(dartfmt.getPath());
//    args.addAll(buildArguments(path));
//
//    builder.command(args);
//    builder.redirectErrorStream(true);
//
//    ProcessRunner runner = new ProcessRunner(builder);
//    runner.runSync(monitor);
//
//    if (runner.getExitCode() == 0) {
//      ResourcesPlugin.getWorkspace().getRoot().getFile(path).refreshLocal(
//          IResource.DEPTH_INFINITE,
//          monitor);
//    }
//
//    StringBuilder sb = new StringBuilder();
//
//    if (!runner.getStdOut().isEmpty()) {
//      sb.append(runner.getStdOut() + "\n");
//    }
//
//    //TODO (pquitslund): better error handling
//    if (!runner.getStdErr().isEmpty()) {
//      sb.append(runner.getStdErr() + "\n");
//    }
//
//    console.print(sb.toString());
//
//  }

  public static class FormatFileAction extends WorkspaceAction {

    private List<IFile> files = Arrays.asList(new IFile[0]);

    public FormatFileAction(IShellProvider provider) {
      super(provider, "Format");
      setId(DartEditorActionDefinitionIds.QUICK_FORMAT);
      setActionDefinitionId(DartEditorActionDefinitionIds.QUICK_FORMAT);
    }

    @Override
    public void run() {
      for (IFile file : files) {
        try {
          format(file, new NullProgressMonitor());
        } catch (Exception e) {
          DartCore.logError(e);
        }
      }
    }

    @Override
    protected String getOperationMessage() {
      return "Formatting;";
    }

    @Override
    protected List<IFile> getSelectedResources() {
      @SuppressWarnings("unchecked")
      List<Object> res = super.getSelectedResources();
      ArrayList<IFile> resources = new ArrayList<IFile>();
      for (Object r : res) {
        if (r instanceof IFile && DartCore.isDartLikeFileName(((IResource) r).getName())) {
          resources.add((IFile) r);
        }
      }
      return resources;
    }

    @Override
    protected boolean updateSelection(IStructuredSelection selection) {
      files = getSelectedResources();
      return !files.isEmpty();
    }

  }

  /**
   * Holder for formatted source and selection info.
   */
  public static class FormattedSource {
    public int selectionOffset;
    public int selectionLength;
    public String source;
  }

  private static final String ARGS_MACHINE_FORMAT_FLAG = "-m";
  private static final String ARGS_SOURCE_FLAG = "-s";

  private static final String JSON_LENGTH_KEY = "length";
  private static final String JSON_OFFSET_KEY = "offset";
  private static final String JSON_SELECTION_KEY = "selection";
  private static final String JSON_SOURCE_KEY = "source";

  /**
   * Run the formatter on the given input file.
   * 
   * @param file the source to pass to the formatter
   * @param selection the selection info to pass into the formatter
   * @param monitor the monitor for displaying progress
   * @throws IOException if an exception was thrown during execution
   * @throws CoreException if an exception occurs in file refresh
   */
  public static void format(IFile file, IProgressMonitor monitor) throws IOException, CoreException {

    if (file == null || DartCore.isPackagesResource(file)) {
      return;
    }

    DartEditor editor = getDirtyEditor(file);
    if (editor != null) {
      // Delegate to the editor if possible
      editor.doFormat();
    } else {
      formatFile(file, monitor);
    }
  }

  /**
   * Run the formatter on the given input source.
   * 
   * @param source the source to pass to the formatter
   * @param selection the selection info to pass into the formatter
   * @param monitor the monitor for displaying progress
   * @throws IOException if an exception was thrown during execution
   * @throws CoreException if an exception occurs in file refresh
   * @return the formatted source (or null in case formatting could not be executed)
   */
  public static FormattedSource format(final String source, final Point selection,
      IProgressMonitor monitor) throws IOException, CoreException {

    File dartfmt = DartSdkManager.getManager().getSdk().getDartFmtExecutable();
    if (!dartfmt.canExecute()) {
      return null;
    }

    if (source.length() == 0) {
      FormattedSource result = new FormattedSource();
      result.source = source;
      return result;
    }

    ProcessBuilder builder = new ProcessBuilder();

    List<String> args = new ArrayList<String>();
    args.add(dartfmt.getPath());
    if (selection != null) {
      args.add(ARGS_SOURCE_FLAG + " " + selection.x + "," + selection.y);
    }
    args.add(ARGS_MACHINE_FORMAT_FLAG);

    builder.command(args);
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder) {
      @Override
      protected void processStarted(Process process) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            process.getOutputStream(),
            "UTF-8"), source.length());
        writer.append(source);
        writer.close();
      }
    };

    runner.runSync(monitor);

    StringBuilder sb = new StringBuilder();

    if (!runner.getStdOut().isEmpty()) {
      sb.append(runner.getStdOut());
    }

    //TODO (pquitslund): better error handling
    if (runner.getExitCode() != 0) {
      sb.append(runner.getStdErr());
      throw new IOException(sb.toString());
    }

    String formattedSource = sb.toString();
    try {
      JSONObject json = new JSONObject(formattedSource);
      String sourceString = (String) json.get(JSON_SOURCE_KEY);
      JSONObject selectionJson = (JSONObject) json.get(JSON_SELECTION_KEY);
      //TODO (pquitslund): figure out why we (occasionally) need to remove an extra trailing NEWLINE
      if (sourceString.endsWith("\n\n")) {
        sourceString = sourceString.substring(0, sourceString.length() - 1);
      }
      FormattedSource result = new FormattedSource();
      result.source = sourceString;
      result.selectionOffset = selectionJson.getInt(JSON_OFFSET_KEY);
      result.selectionLength = selectionJson.getInt(JSON_LENGTH_KEY);
      return result;
    } catch (JSONException e) {
      DartToolsPlugin.log(e);
      throw new IOException(e);
    }

  }

  public static boolean isAvailable() {
    return DartSdkManager.getManager().getSdk().getDartFmtExecutable().canExecute();
  }

  private static void formatFile(IFile file, IProgressMonitor monitor)
      throws UnsupportedEncodingException, CoreException, IOException {

    Reader reader = new InputStreamReader(file.getContents(), file.getCharset());
    String contents = FileUtilities.getContents(reader);

    if (contents != null) {
      FormattedSource result = format(contents, null, monitor);
      if (!contents.equals(result.source)) {
        InputStream stream = new ByteArrayInputStream(result.source.getBytes("UTF-8"));
        file.setContents(stream, IResource.KEEP_HISTORY, monitor);
      }
    }
  }

  private static IWorkbenchPage getActivePage() {

    final IWorkbenchPage[] page = new IWorkbenchPage[1];

    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
          page[0] = window.getActivePage();
        }
      }
    });

    return page[0];
  }

  private static DartEditor getDirtyEditor(IFile file) {

    IWorkbenchPage activePage = getActivePage();
    if (activePage != null) {
      IEditorPart editor = ResourceUtil.findEditor(activePage, file);
      if (editor instanceof DartEditor) {
        if (editor.isDirty()) {
          return (DartEditor) editor;
        }
      }
    }
    return null;
  }

//  private static List<String> buildArguments(IPath path) {
//    ArrayList<String> args = new ArrayList<String>();
//    args.add("-w");
//    args.add(path.toOSString());
//    return args;
//  }

}
