/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.util.DartModelUtil;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A number of routines for working with JavaElements in editors. Use 'isOpenInEditor' to test if an
 * element is already open in a editor Use 'openInEditor' to force opening an element in a editor
 * With 'getWorkingCopy' you get the working copy (element in the editor) of an element
 */
public class EditorUtility {

  private static final String ID_ORG_ECLIPSE_UI_DEFAULT_TEXT_EDITOR = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$

  /**
   * Maps the localized modifier name to a code in the same manner as #findModifier.
   *
   * @param modifierName the modifier name
   * @return the SWT modifier bit, or <code>0</code> if no match was found
   */
  public static int findLocalizedModifier(String modifierName) {
    if (modifierName == null) {
      return 0;
    }

    if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.CTRL))) {
      return SWT.CTRL;
    }
    if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.SHIFT))) {
      return SWT.SHIFT;
    }
    if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.ALT))) {
      return SWT.ALT;
    }
    if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.COMMAND))) {
      return SWT.COMMAND;
    }

    return 0;
  }

  /**
   * If the current active editor edits a java element return it, else return null
   */
  public static DartElement getActiveEditorJavaInput() {
    IWorkbenchPage page = DartToolsPlugin.getActivePage();
    if (page != null) {
      IEditorPart part = page.getActiveEditor();
      if (part != null) {
        IEditorInput editorInput = part.getEditorInput();
        if (editorInput != null) {
          return DartUI.getEditorInputJavaElement(editorInput);
        }
      }
    }
    return null;
  }

  /**
   * Returns an array of all editors that have an unsaved content. If the identical content is
   * presented in more than one editor, only one of those editor parts is part of the result.
   *
   * @return an array of all dirty editor parts.
   */
  public static IEditorPart[] getDirtyEditors() {
    Set<IEditorInput> inputs = new HashSet<IEditorInput>();
    List<IEditorPart> result = new ArrayList<IEditorPart>(0);
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int x = 0; x < pages.length; x++) {
        IEditorPart[] editors = pages[x].getDirtyEditors();
        for (int z = 0; z < editors.length; z++) {
          IEditorPart ep = editors[z];
          IEditorInput input = ep.getEditorInput();
          if (inputs.add(input)) {
            result.add(ep);
          }
        }
      }
    }
    return result.toArray(new IEditorPart[result.size()]);
  }

  /**
   * Returns the editors to save before performing global Java-related operations.
   *
   * @param saveUnknownEditors <code>true</code> iff editors with unknown buffer management should
   *          also be saved
   * @return the editors to save
   */
  public static IEditorPart[] getDirtyEditorsToSave(boolean saveUnknownEditors) {
    Set<IEditorInput> inputs = new HashSet<IEditorInput>();
    List<IEditorPart> result = new ArrayList<IEditorPart>(0);
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int x = 0; x < pages.length; x++) {
        IEditorPart[] editors = pages[x].getDirtyEditors();
        for (int z = 0; z < editors.length; z++) {
          IEditorPart ep = editors[z];
          IEditorInput input = ep.getEditorInput();
          if (!mustSaveDirtyEditor(ep, input, saveUnknownEditors)) {
            continue;
          }

          if (inputs.add(input)) {
            result.add(ep);
          }
        }
      }
    }
    return result.toArray(new IEditorPart[result.size()]);
  }

  public static String getEditorID(IEditorInput input) throws PartInitException {
    Assert.isNotNull(input);
    IEditorDescriptor editorDescriptor;
    if (input instanceof IFileEditorInput) {
      editorDescriptor = IDE.getEditorDescriptor(((IFileEditorInput) input).getFile());
    } else {
      String name = input.getName();
      if (name == null) {
        throwPartInitException(DartEditorMessages.EditorUtility_could_not_find_editorId);
      }
      editorDescriptor = IDE.getEditorDescriptor(name);
    }
    return editorDescriptor.getId();
  }

  public static IEditorInput getEditorInput(Object input) throws DartModelException {
    if (input instanceof DartElement) {
      return getEditorInput((DartElement) input);
    }

    if (input instanceof IFile) {
      return new FileEditorInput((IFile) input);
    }

    if (DartModelUtil.isOpenableStorage(input)) {
      return new JarEntryEditorInput((IStorage) input);
    }

    return null;
  }

  /**
   * Returns the given editor's input as Java element.
   *
   * @param editor the editor
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   * @return the given editor's input as Java element or <code>null</code> if none
   */
  public static DartElement getEditorInputJavaElement(IEditorPart editor, boolean primaryOnly) {
    Assert.isNotNull(editor);
    IEditorInput editorInput = editor.getEditorInput();
    if (editorInput == null) {
      return null;
    }

    DartElement je = DartUI.getEditorInputJavaElement(editorInput);
    if (je != null || primaryOnly) {
      return je;
    }

    return DartToolsPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput, false);
  }

  /**
   * Returns the Java project for a given editor input or <code>null</code> if no corresponding Java
   * project exists.
   *
   * @param input the editor input
   * @return the corresponding Java project
   */
  public static DartProject getJavaProject(IEditorInput input) {
    DartProject jProject = null;
    if (input instanceof IFileEditorInput) {
      IProject project = ((IFileEditorInput) input).getFile().getProject();
      if (project != null) {
        jProject = DartCore.create(project);
        if (!jProject.exists()) {
          jProject = null;
        }
      }
    }
    return jProject;
  }

  /**
   * Returns the modifier string for the given SWT modifier modifier bits.
   *
   * @param stateMask the SWT modifier bits
   * @return the modifier string
   */
  public static String getModifierString(int stateMask) {
    String modifierString = ""; //$NON-NLS-1$
    if ((stateMask & SWT.CTRL) == SWT.CTRL) {
      modifierString = appendModifierString(modifierString, SWT.CTRL);
    }
    if ((stateMask & SWT.ALT) == SWT.ALT) {
      modifierString = appendModifierString(modifierString, SWT.ALT);
    }
    if ((stateMask & SWT.SHIFT) == SWT.SHIFT) {
      modifierString = appendModifierString(modifierString, SWT.SHIFT);
    }
    if ((stateMask & SWT.COMMAND) == SWT.COMMAND) {
      modifierString = appendModifierString(modifierString, SWT.COMMAND);
    }

    return modifierString;
  }

  /**
   * Tests if a CU is currently shown in an editor
   *
   * @return the IEditorPart if shown, null if element is not open in an editor
   */
  public static IEditorPart isOpenInEditor(Object inputElement) {
    IEditorInput input = null;

    try {
      input = getEditorInput(inputElement);
    } catch (DartModelException x) {
      DartToolsPlugin.log(x.getStatus());
    }

    if (input != null) {
      IWorkbenchPage p = DartToolsPlugin.getActivePage();
      if (p != null) {
        return p.findEditor(input);
      }
    }

    return null;
  }

  /**
   * Opens a Java editor for an element such as <code>DartElement</code>, <code>IFile</code>, or
   * <code>IStorage</code>. The editor is activated by default.
   *
   * @return an open editor or <code>null</code> if an external editor was opened
   * @throws PartInitException if the editor could not be opened or the input element is not valid
   */
  public static IEditorPart openInEditor(Object inputElement) throws DartModelException,
      PartInitException {
    return openInEditor(inputElement, true);
  }

  /**
   * Opens the editor currently associated with the given element (DartElement, IFile, IStorage...)
   *
   * @return an open editor or <code>null</code> if an external editor was opened
   * @throws PartInitException if the editor could not be opened or the input element is not valid
   */
  public static IEditorPart openInEditor(Object inputElement, boolean activate)
      throws DartModelException, PartInitException {

    if (inputElement instanceof IFile) {
      return openInEditor((IFile) inputElement, activate);
    }
    if (inputElement instanceof HTMLFile) {
      return openInEditor(((HTMLFile) inputElement).getCorrespondingResource(), activate);
    }
    DartX.todo();
//    if (inputElement instanceof DartElement
//        && ((DartElement) inputElement).isVirtual()) {
//
//      URI hostElementPath = ((DartElement) inputElement).getHostPath();
//
//      if (hostElementPath != null) {
//        /* See if we can resolve the URI on the workspace */
//        IResource realFile = ((DartElement) inputElement).getDartProject().getProject().getWorkspace().getRoot().getFileForLocation(
//            new Path(hostElementPath.getPath()));
//        if (realFile == null || !realFile.exists()) {
//          realFile = ((DartElement) inputElement).getDartProject().getProject().getWorkspace().getRoot().findMember(
//              hostElementPath.getPath());
//        }
//        if (realFile != null)
//          return openInEditor((IFile) realFile, activate);
//        return openInEditor(hostElementPath, activate);
//      }
//
//    }

    /*
     * Support to navigate inside non-primary working copy. For now we only support to navigate
     * inside the currently active editor. XXX: once we have FileStoreEditorInput as API, see
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=111887 we can fix this code by creating the
     * correct editor input in getEditorInput(Object)
     */
    if (inputElement instanceof DartElement) {
      CompilationUnit cu = ((DartElement) inputElement).getAncestor(CompilationUnit.class);
      if (cu != null && !DartModelUtil.isPrimary(cu)) {
        IWorkbenchPage page = DartToolsPlugin.getActivePage();
        if (page != null) {
          IEditorPart editor = page.getActiveEditor();
          if (editor != null) {
            DartElement editorCU = EditorUtility.getEditorInputJavaElement(editor, false);
            if (cu.equals(editorCU)) {
              if (activate && page.getActivePart() != editor) {
                page.activate(editor);
              }
              return editor;
            }
          }
        }
      }
    }

    IEditorInput input = getEditorInput(inputElement);
    if (input == null) {
      throwPartInitException(DartEditorMessages.EditorUtility_no_editorInput);
    }

    return openInEditor(input, getEditorID(input), activate);
  }

  /**
   * Selects a Java Element in an editor
   */
  public static void revealInEditor(IEditorPart part, DartElement element) {
    if (element == null) {
      return;
    }

    if (part instanceof DartEditor) {
      ((DartEditor) part).setSelection(element);
      return;
    }

    // Support for non-Java editor
    try {
      SourceRange range = null;
      DartX.todo();
      if (element instanceof CompilationUnit) {
        range = null;
      } else if (element instanceof TypeMember) {
        range = ((TypeMember) element).getNameRange();
      } else if (element instanceof DartVariableDeclaration) {
        range = ((DartVariableDeclaration) element).getNameRange();
      } else if (element instanceof SourceReference) {
        range = ((SourceReference) element).getSourceRange();
      }

      if (range != null) {
        revealInEditor(part, range.getOffset(), range.getLength());
      }
    } catch (DartModelException e) {
      // don't reveal
    }
  }

  /**
   * Selects and reveals the given offset and length in the given editor part.
   */
  public static void revealInEditor(IEditorPart editor, final int offset, final int length) {
    if (editor instanceof ITextEditor) {
      ((ITextEditor) editor).selectAndReveal(offset, length);
      return;
    }

    // Support for non-text editor - try IGotoMarker interface
    if (editor instanceof IGotoMarker) {
      final IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput) {
        final IGotoMarker gotoMarkerTarget = (IGotoMarker) editor;
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
          @Override
          protected void execute(IProgressMonitor monitor) throws CoreException {
            IMarker marker = null;
            try {
              marker = ((IFileEditorInput) input).getFile().createMarker(IMarker.TEXT);
              marker.setAttribute(IMarker.CHAR_START, offset);
              marker.setAttribute(IMarker.CHAR_END, offset + length);

              gotoMarkerTarget.gotoMarker(marker);

            } finally {
              if (marker != null) {
                marker.delete();
              }
            }
          }
        };

        try {
          op.run(null);
        } catch (InvocationTargetException ex) {
          // reveal failed
        } catch (InterruptedException e) {
          Assert.isTrue(false, "this operation can not be canceled"); //$NON-NLS-1$
        }
      }
      return;
    }

    /*
     * Workaround: send out a text selection XXX: Needs to be improved, see
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=32214
     */
    if (editor != null && editor.getEditorSite().getSelectionProvider() != null) {
      IEditorSite site = editor.getEditorSite();
      if (site == null) {
        return;
      }

      ISelectionProvider provider = editor.getEditorSite().getSelectionProvider();
      if (provider == null) {
        return;
      }

      provider.setSelection(new TextSelection(offset, length));
    }
  }

  /**
   * Selects and reveals the given region in the given editor part.
   */
  public static void revealInEditor(IEditorPart part, IRegion region) {
    if (part != null && region != null) {
      revealInEditor(part, region.getOffset(), region.getLength());
    }
  }

  /**
   * Appends to modifier string of the given SWT modifier bit to the given modifierString.
   *
   * @param modifierString the modifier string
   * @param modifier an int with SWT modifier bit
   * @return the concatenated modifier string
   */
  private static String appendModifierString(String modifierString, int modifier) {
    if (modifierString == null) {
      modifierString = ""; //$NON-NLS-1$
    }
    String newModifierString = Action.findModifierString(modifier);
    if (modifierString.length() == 0) {
      return newModifierString;
    }
    return Messages.format(DartEditorMessages.EditorUtility_concatModifierStrings, new String[] {
        modifierString, newModifierString});
  }

  private static IEditorInput getEditorInput(DartElement element) throws DartModelException {
    SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
    while (element != null) {
      if (element instanceof CompilationUnit) {
        CompilationUnit unit = ((CompilationUnit) element).getPrimary();
        IResource resource = unit.getResource();
        if (resource instanceof IFile) {
          return new FileEditorInput((IFile) resource);
        }
        if (unit instanceof ExternalCompilationUnitImpl) {
          ExternalCompilationUnitImpl cu = (ExternalCompilationUnitImpl) unit;
          URI uri = libraryManager.resolveDartUri(cu.getUri());
          if (JarEntryStorage.isJarUri(uri)) {
            return new JarEntryEditorInput(new JarEntryStorage(uri));
          }
          IFileStore fileStore = EFS.getLocalFileSystem().getStore(uri);
          return new ExternalCompilationUnitEditorInput(fileStore, cu);
        }
//      } else if (element instanceof LibraryConfigurationFileImpl) {
//        // external libraries
//        DartElement dartElement = ((LibraryConfigurationFileImpl) element).getParent();
//        if (dartElement instanceof DartLibraryImpl) {
//          LibrarySource librarySource = ((DartLibraryImpl) dartElement).getLibrarySourceFile();
//          if (librarySource instanceof UrlLibrarySource) {
//            URI uri = libraryManager.resolveDartUri(((UrlLibrarySource) librarySource).getUri());
//            if (JarEntryStorage.isJarUri(uri)) {
//              return new JarEntryEditorInput(new JarEntryStorage(uri));
//            }
//            IFileStore fileStore = EFS.getLocalFileSystem().getStore(uri);
//            return new FileStoreEditorInput(fileStore);
//          }
//        }
      }
      element = element.getParent();
    }
    return null;
  }

  private static void initializeHighlightRange(IEditorPart editorPart) {
    if (editorPart instanceof ITextEditor) {
      IAction toggleAction = editorPart.getEditorSite().getActionBars().getGlobalActionHandler(
          ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY);
      boolean enable = toggleAction != null;
      if (enable && editorPart instanceof DartEditor) {
        enable = DartToolsPlugin.getDefault().getPreferenceStore().getBoolean(
            PreferenceConstants.EDITOR_SHOW_SEGMENTS);
      } else {
        enable = enable && toggleAction.isEnabled() && toggleAction.isChecked();
      }
      if (enable) {
        if (toggleAction instanceof TextEditorAction) {
          // Reset the action
          ((TextEditorAction) toggleAction).setEditor(null);
          // Restore the action
          ((TextEditorAction) toggleAction).setEditor((ITextEditor) editorPart);
        } else {
          // Uncheck
          toggleAction.run();
          // Check
          toggleAction.run();
        }
      }
    }
  }

  private static String maybeSwapDefaultEditorDescriptor(String editorId) {
    if (editorId.equals(ID_ORG_ECLIPSE_UI_DEFAULT_TEXT_EDITOR)) {
      /*
       * TODO (rdayal): Once we modify Eclipse's default text editor so that it does not add a bunch
       * of context menu contributions that we do not need (and can't get rid of via activies), we
       * can get rid of the SimpleTextEditor.
       */
      return SimpleTextEditor.ID;
    }
    return editorId;
  }

  /**
	 *
	 */
  private static boolean mustSaveDirtyEditor(IEditorPart ep, IEditorInput input,
      boolean saveUnknownEditors) {
    /*
     * Goal: save all editors that could interfere with refactoring operations. If
     * <code>saveUnknownEditors</code> is <code>false</code>, save all editors for compilation units
     * that are not working copies. If <code>saveUnknownEditors</code> is <code>true</code>, save
     * all editors whose implementation is probably not based on file buffers.
     */
    IResource resource = (IResource) input.getAdapter(IResource.class);
    if (resource == null) {
      return saveUnknownEditors;
    }

    DartElement javaElement = DartCore.create(resource);
    if (javaElement instanceof CompilationUnit) {
      CompilationUnit cu = (CompilationUnit) javaElement;
      if (!cu.isWorkingCopy()) {
        return true;
      }
    }

    if (!(ep instanceof ITextEditor)) {
      return saveUnknownEditors;
    }

    ITextEditor textEditor = (ITextEditor) ep;
    IDocumentProvider documentProvider = textEditor.getDocumentProvider();
    if (!(documentProvider instanceof TextFileDocumentProvider)) {
      return saveUnknownEditors;
    }

    return false;
  }

  private static IEditorPart openInEditor(IEditorInput input, String editorID, boolean activate)
      throws PartInitException {
    Assert.isNotNull(input);
    Assert.isNotNull(editorID);

    IWorkbenchPage p = DartToolsPlugin.getActivePage();
    if (p == null) {
      throwPartInitException(DartEditorMessages.EditorUtility_no_active_WorkbenchPage);
    }

    IEditorPart editorPart = p.openEditor(input, editorID, activate);
    initializeHighlightRange(editorPart);
    return editorPart;
  }

  private static IEditorPart openInEditor(IFile file, boolean activate) throws PartInitException {
    if (file == null) {
      throwPartInitException(DartEditorMessages.EditorUtility_file_must_not_be_null);
    }

    IWorkbenchPage p = DartToolsPlugin.getActivePage();
    if (p == null) {
      throwPartInitException(DartEditorMessages.EditorUtility_no_active_WorkbenchPage);
    }

    IEditorDescriptor desc = IDE.getEditorDescriptor(file, true);

    IEditorPart editorPart = IDE.openEditor(p, file,
        maybeSwapDefaultEditorDescriptor(desc.getId()), activate);
    initializeHighlightRange(editorPart);
    return editorPart;
  }

  @SuppressWarnings("unused")
  private static IEditorPart openInEditor(URI file, boolean activate) throws PartInitException {
    if (file == null) {
      throwPartInitException(DartEditorMessages.EditorUtility_file_must_not_be_null);
    }

    IWorkbenchPage p = DartToolsPlugin.getActivePage();
    if (p == null) {
      throwPartInitException(DartEditorMessages.EditorUtility_no_active_WorkbenchPage);
    }

    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(
        file.getPath());
    if (desc == null) {
      throwPartInitException(DartEditorMessages.EditorUtility_cantFindEditor + file.toString());
    }

    IEditorPart editorPart = IDE.openEditor(p, file,
        maybeSwapDefaultEditorDescriptor(desc.getId()), activate);
    initializeHighlightRange(editorPart);
    return editorPart;
  }

  private static void throwPartInitException(String message) throws PartInitException {
    IStatus status = new Status(IStatus.ERROR, DartUI.ID_PLUGIN, IStatus.OK, message, null);
    throw new PartInitException(status);
  }

}
