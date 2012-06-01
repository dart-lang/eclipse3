/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse License v1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * The context (saved on disk, editor buffer, refactoring) in which analysis occurs.
 */
class Context {

  private static final String END_CACHE_TAG = "</end-cache>";

  private static final Library[] NO_LIBRARIES = new Library[] {};

  private AnalysisServer server;

  /**
   * The libraries in this context, including imported libraries. This should only be accessed on
   * the background thread.
   */
  private final HashMap<File, Library> libraryCache;

  /**
   * A map of URI (as needed by DartC) to parsed but unresolved unit. Units are added to this
   * collection by {@link ParseFileTask} and {@link ParseLibraryFileTask}, and removed from this
   * collection by {@link ResolveLibraryTask} when it calls
   * {@link AnalysisUtility#resolve(AnalysisServer, Library, java.util.Map, java.util.Map)}
   */
  private final HashMap<URI, DartUnit> unresolvedUnits;

  Context(AnalysisServer server) {
    this.server = server;
    this.libraryCache = new HashMap<File, Library>();
    this.unresolvedUnits = new HashMap<URI, DartUnit>();
  }

  void cacheLibrary(Library library) {
    libraryCache.put(library.getFile(), library);
  }

  void cacheUnresolvedUnit(File file, DartUnit unit) {
    unresolvedUnits.put(file.toURI(), unit);
  }

  void discardLibraries() {
    libraryCache.clear();
    unresolvedUnits.clear();
  }

  void discardLibrary(Library library) {
    File libraryFile = library.getFile();
    libraryCache.remove(libraryFile);
    unresolvedUnits.remove(libraryFile.toURI());
    for (File sourcedFile : library.getSourceFiles()) {
      unresolvedUnits.remove(sourcedFile.toURI());
    }
  }

  void discardLibraryAndReferencingLibraries(Library library) {
    discardLibrary(library);
    for (Library cachedLibrary : getLibrariesImporting(library.getFile())) {
      discardLibraryAndReferencingLibraries(cachedLibrary);
    }
  }

  Collection<Library> getCachedLibraries() {
    return libraryCache.values();
  }

  /**
   * Answer the cached library or <code>null</code> if not cached
   */
  Library getCachedLibrary(File file) {
    return libraryCache.get(file);
  }

  /**
   * Answer a resolved or unresolved unit, or <code>null</code> if none
   */
  DartUnit getCachedUnit(Library library, File dartFile) {
    if (library != null) {
      DartUnit unit = library.getResolvedUnit(dartFile);
      if (unit != null) {
        return unit;
      }
    }
    return unresolvedUnits.get(dartFile.toURI());
  }

  /**
   * Answer the libraries containing the specified file or contain files in the specified directory
   * tree
   * 
   * @return an array of libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  Library[] getLibrariesContaining(File file) {

    // Quick check if the file is a library

    Library library = libraryCache.get(file);
    if (library != null) {
      return new Library[] {library};
    }
    Library[] result = NO_LIBRARIES;

    // If this is a file, then return the libraries that source the file

    if (file.isFile() || (!file.exists() && DartCore.isDartLikeFileName(file.getName()))) {
      for (Library cachedLibrary : libraryCache.values()) {
        if (cachedLibrary.getSourceFiles().contains(file)) {
          result = append(result, cachedLibrary);
        }
      }
      return result;
    }

    // Otherwise return the libraries containing files in the specified directory tree

    String prefix = file.getAbsolutePath() + File.separator;
    for (Library cachedLibrary : libraryCache.values()) {
      for (File sourceFile : cachedLibrary.getSourceFiles()) {
        if (sourceFile.getPath().startsWith(prefix)) {
          result = append(result, cachedLibrary);
          break;
        }
      }
    }
    return result;
  }

  /**
   * Answer the libraries importing the specified file
   */
  ArrayList<Library> getLibrariesImporting(File file) {
    ArrayList<Library> result = new ArrayList<Library>();
    for (Library cachedLibrary : libraryCache.values()) {
      if (cachedLibrary.getImportedFiles().contains(file)) {
        result.add(cachedLibrary);
      }
    }
    return result;
  }

  /**
   * Answer units that have been parsed by not resolved.
   */
  HashMap<URI, DartUnit> getUnresolvedUnits() {
    return unresolvedUnits;
  }

  /**
   * Reload cached libraries
   */
  void readCache(LineNumberReader reader) throws IOException {
    while (true) {
      String filePath = reader.readLine();
      if (filePath == null) {
        throw new IOException("Expected " + END_CACHE_TAG + " but found EOF");
      }
      if (filePath.equals(END_CACHE_TAG)) {
        break;
      }
      File libraryFile = new File(filePath);
      Library lib = Library.readCache(server, libraryFile, reader);
      libraryCache.put(libraryFile, lib);
    }
  }

  /**
   * Write information for each cached library. Don't include unresolved libraries so that listeners
   * will be notified when the cache is reloaded.
   */
  void writeCache(PrintWriter writer) {
    for (Entry<File, Library> entry : libraryCache.entrySet()) {
      Library library = entry.getValue();
      if (library.hasBeenResolved()) {
        writer.println(entry.getKey().getPath());
        library.writeCache(writer);
      }
    }
    writer.println(END_CACHE_TAG);
  }

  private Library[] append(Library[] oldArray, Library library) {
    if (oldArray.length == 0) {
      return new Library[] {library};
    }
    int oldLen = oldArray.length;
    Library[] newArray = new Library[oldLen + 1];
    System.arraycopy(oldArray, 0, newArray, 0, oldLen);
    newArray[oldLen] = library;
    return newArray;
  }
}
