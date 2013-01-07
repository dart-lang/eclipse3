/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ElementProxy;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.source.Source;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemoryIndexStoreImplTest extends EngineTestCase {
  private MemoryIndexStoreImpl store = new MemoryIndexStoreImpl();
  private ElementLocation elementLocationA = mock(ElementLocation.class);
  private ElementLocation elementLocationB = mock(ElementLocation.class);
  private ElementLocation elementLocationC = mock(ElementLocation.class);
  private ElementProxy elementA = mock(ElementProxy.class);
  private ElementProxy elementB = mock(ElementProxy.class);
  private ElementProxy elementC = mock(ElementProxy.class);
  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);
  private Source sourceC = mock(Source.class);
  private Relationship relationship = Relationship.getRelationship("test-relationship");
  private Location location = mock(Location.class);

  public void test_clear() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    assertEquals(1, store.getElementCount());
    assertEquals(1, store.getRelationshipCount());
    // clear
    store.clear();
    assertEquals(0, store.getElementCount());
    assertEquals(0, store.getRelationshipCount());
  }

  public void test_getElementCount() throws Exception {
    Relationship relationshipA = Relationship.getRelationship("test-A");
    Relationship relationshipB = Relationship.getRelationship("test-B");
    assertEquals(0, store.getElementCount());
    // add for A
    store.recordRelationship(elementA, relationshipA, location);
    assertEquals(1, store.getElementCount());
    // one more for A, still 1 element
    store.recordRelationship(elementA, relationshipB, location);
    assertEquals(1, store.getElementCount());
    // add for B, now 2 elements
    store.recordRelationship(elementB, relationshipA, location);
    assertEquals(2, store.getElementCount());
  }

  public void test_getRelationships_hasOne() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).containsOnly(location);
  }

  public void test_getRelationships_hasTwo() throws Exception {
    Location locationA = mock(Location.class);
    Location locationB = mock(Location.class);
    when(locationA.getElement()).thenReturn(elementA);
    when(locationB.getElement()).thenReturn(elementB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementA, relationship, locationB);
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).containsOnly(locationA, locationB);
  }

  public void test_getRelationships_noRelations() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    Location[] locations = store.getRelationships(
        elementA,
        Relationship.getRelationship("no-such-relationship"));
    assertThat(locations).isEmpty();
  }

  public void test_getSourceCount() throws Exception {
    Source sourceA = mock(Source.class);
    Source sourceB = mock(Source.class);
    Source sourceC = mock(Source.class);
    // no relationships
    assertEquals(0, store.getSourceCount());
    // add relationship in "sourceA"
    {
      when(elementA.getSource()).thenReturn(sourceA);
      Location location = mock(Location.class);
      when(location.getElement()).thenReturn(elementA);
      store.recordRelationship(elementA, relationship, location);
      assertEquals(1, store.getSourceCount());
    }
    // add relationship in "sourceB" to "sourceC"
    {
      when(elementB.getSource()).thenReturn(sourceB);
      when(elementC.getSource()).thenReturn(sourceC);
      Location location = mock(Location.class);
      when(location.getElement()).thenReturn(elementC);
      store.recordRelationship(elementB, relationship, location);
      assertEquals(3, store.getSourceCount());
    }
  }

  public void test_recordRelationship() throws Exception {
    // no relationships initially
    assertEquals(0, store.getRelationshipCount());
    // record relationship
    store.recordRelationship(elementA, relationship, location);
    assertEquals(1, store.getRelationshipCount());
  }

  public void test_recordRelationship_noElement() throws Exception {
    store.recordRelationship(null, relationship, location);
    assertEquals(0, store.getRelationshipCount());
  }

  public void test_recordRelationship_noLocation() throws Exception {
    store.recordRelationship(elementA, relationship, null);
    assertEquals(0, store.getRelationshipCount());
  }

  public void test_removeSource_withDeclaration() throws Exception {
    when(elementA.getSource()).thenReturn(sourceA);
    when(elementB.getSource()).thenReturn(sourceB);
    when(elementC.getSource()).thenReturn(sourceC);
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove A, no relations and locations
    store.removeSource(sourceA);
    assertEquals(0, store.getRelationshipCount());
    assertEquals(0, store.getLocationCount());
  }

  public void test_removeSource_withRelationship() throws Exception {
    when(elementA.getSource()).thenReturn(sourceA);
    when(elementB.getSource()).thenReturn(sourceB);
    when(elementC.getSource()).thenReturn(sourceC);
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove B, 1 relation and 1 location left
    store.removeSource(sourceB);
    assertEquals(1, store.getRelationshipCount());
    assertEquals(1, store.getLocationCount());
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).containsOnly(locationC);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(elementA.getLocation()).thenReturn(elementLocationA);
    when(elementB.getLocation()).thenReturn(elementLocationB);
    when(elementC.getLocation()).thenReturn(elementLocationC);
    when(location.getElement()).thenReturn(elementC);
  }
}
