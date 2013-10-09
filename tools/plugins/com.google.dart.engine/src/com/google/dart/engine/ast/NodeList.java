/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

import java.util.AbstractList;
import java.util.Collection;

/**
 * Instances of the class {@code NodeList} represent a list of AST nodes that have a common parent.
 * 
 * @coverage dart.engine.ast
 */
public class NodeList<E extends ASTNode> extends AbstractList<E> {
  /**
   * Create an empty list with the given owner. This is a convenience method that allows the
   * compiler to determine the correct value of the type argument {@link #E} without needing to
   * explicitly specify it.
   * 
   * @param owner the node that is the parent of each of the elements in the list
   * @return the list that was created
   */
  public static <E extends ASTNode> NodeList<E> create(ASTNode owner) {
    return new NodeList<E>(owner);
  }

  /**
   * The node that is the parent of each of the elements in the list.
   */
  private final ASTNode owner;

  /**
   * The elements of the list, or {@code null} if the list is empty.
   */
  private ASTNode[] elements = null;

  /**
   * Initialize a newly created list of nodes to be empty.
   * 
   * @param owner the node that is the parent of each of the elements in the list
   */
  public NodeList(ASTNode owner) {
    this.owner = owner;
  }

  /**
   * Use the given visitor to visit each of the nodes in this list.
   * 
   * @param visitor the visitor to be used to visit the elements of this list
   */
  public void accept(ASTVisitor<?> visitor) {
    if (elements == null) {
      return;
    }
    for (ASTNode element : elements) {
      element.accept(visitor);
    }
  }

  @Override
  public void add(int index, E node) {
    if (elements == null) {
      if (index != 0) {
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
      }
      owner.becomeParentOf(node);
      elements = new ASTNode[] {node};
      return;
    }
    int length = elements.length;
    if (index < 0 || index > length) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
    }
    ASTNode[] newElements = new ASTNode[length + 1];
    owner.becomeParentOf(node);
    System.arraycopy(elements, 0, newElements, 0, index);
    newElements[index] = node;
    System.arraycopy(elements, index, newElements, index + 1, length - index);
    elements = newElements;
  }

  @Override
  public boolean addAll(Collection<? extends E> nodes) {
    if (nodes != null && !nodes.isEmpty()) {
      if (elements == null) {
        elements = new ASTNode[nodes.size()];
        int index = 0;
        for (E node : nodes) {
          owner.becomeParentOf(node);
          elements[index++] = node;
        }
        return true;
      }
      int oldCount = elements.length;
      int newCount = nodes.size();
      ASTNode[] newElements = new ASTNode[oldCount + newCount];
      System.arraycopy(elements, 0, newElements, 0, oldCount);
      int index = oldCount;
      for (E node : nodes) {
        owner.becomeParentOf(node);
        newElements[index++] = node;
      }
      elements = newElements;
      return true;
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public E get(int index) {
    if (elements == null) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    } else if (index < 0 || index >= elements.length) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
    }
    return (E) elements[index];
  }

  /**
   * Return the first token included in this node's source range.
   * 
   * @return the first token included in this node's source range
   */
  public Token getBeginToken() {
    if (elements == null) {
      return null;
    }
    return elements[0].getBeginToken();
  }

  /**
   * Return the last token included in this node list's source range.
   * 
   * @return the last token included in this node list's source range
   */
  public Token getEndToken() {
    if (elements == null) {
      return null;
    }
    return elements[elements.length - 1].getEndToken();
  }

  /**
   * Return the node that is the parent of each of the elements in the list.
   * 
   * @return the node that is the parent of each of the elements in the list
   */
  public ASTNode getOwner() {
    return owner;
  }

  @Override
  @SuppressWarnings("unchecked")
  public E remove(int index) {
    if (elements == null) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    } else if (index < 0 || index >= elements.length) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
    }
    E removedNode = (E) elements[index];
    int length = elements.length;
    if (length == 1) {
      elements = null;
      return removedNode;
    }
    ASTNode[] newElements = new ASTNode[length - 1];
    System.arraycopy(elements, 0, newElements, 0, index);
    System.arraycopy(elements, index + 1, newElements, index, length - index - 1);
    elements = newElements;
    return removedNode;
  }

  @Override
  @SuppressWarnings("unchecked")
  public E set(int index, E node) {
    if (elements == null) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    } else if (index < 0 || index >= elements.length) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
    }
    E replacedNode = (E) elements[index];
    owner.becomeParentOf(node);
    elements[index] = node;
    return replacedNode;
  }

  @Override
  public int size() {
    if (elements == null) {
      return 0;
    }
    return elements.length;
  }
}
