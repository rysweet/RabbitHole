package org.alice.ide.projecturi;

import org.junit.Test;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.net.URI;

import static org.junit.Assert.*;

public class ProjectSnapshotTest {

  @Test
  public void getUri_returnsConstructedUri() {
    URI uri = URI.create("file:///virtual/test.a3p");
    ProjectSnapshot snapshot = new ProjectSnapshot(uri, "test", new ImageIcon());

    assertEquals(uri, snapshot.getUri());
  }

  @Test
  public void getText_returnsConstructedText() {
    ProjectSnapshot snapshot = new ProjectSnapshot(URI.create("file:///virtual/test.a3p"), "Test Project", new ImageIcon());

    assertEquals("Test Project", snapshot.getText());
  }

  @Test
  public void getIcon_returnsConstructedIcon() {
    Icon icon = new ImageIcon();
    ProjectSnapshot snapshot = new ProjectSnapshot(URI.create("file:///virtual/test.a3p"), "Test Project", icon);

    assertSame(icon, snapshot.getIcon());
  }

  @Test
  public void getThumbnail_null_byDefault() {
    ProjectSnapshot snapshot = new ProjectSnapshot(URI.create("file:///virtual/test.a3p"), "Test Project", new ImageIcon());

    assertNull(snapshot.getThumbnail());
  }

  @Test
  public void isVrProject_windowCamera_false() {
    ProjectSnapshot snapshot = new ProjectSnapshot(URI.create("file:///virtual/test.a3p"), "Test Project", new ImageIcon());

    assertFalse(snapshot.isVrProject());
  }

  @Test
  public void hasUri_nonNull_true() {
    ProjectSnapshot snapshot = new ProjectSnapshot(URI.create("file:///virtual/test.a3p"), "Test Project", new ImageIcon());

    assertTrue(snapshot.hasUri());
  }

  @Test
  public void hasUri_null_false() {
    ProjectSnapshot snapshot = new ProjectSnapshot(null, "test", null);

    assertFalse(snapshot.hasUri());
  }

  @Test
  public void equals_sameUri_true() {
    URI uri = URI.create("file:///virtual/test.a3p");

    assertEquals(new ProjectSnapshot(uri, "one", new ImageIcon()), new ProjectSnapshot(uri, "two", new ImageIcon()));
  }

  @Test
  public void equals_differentUri_false() {
    ProjectSnapshot first = new ProjectSnapshot(URI.create("file:///virtual/one.a3p"), "one", new ImageIcon());
    ProjectSnapshot second = new ProjectSnapshot(URI.create("file:///virtual/two.a3p"), "two", new ImageIcon());

    assertNotEquals(first, second);
  }

  @Test
  public void equals_sameObject_true() {
    ProjectSnapshot snapshot = new ProjectSnapshot(URI.create("file:///virtual/test.a3p"), "Test Project", new ImageIcon());

    assertEquals(snapshot, snapshot);
  }

  @Test
  public void equals_differentType_false() {
    ProjectSnapshot snapshot = new ProjectSnapshot(URI.create("file:///virtual/test.a3p"), "Test Project", new ImageIcon());

    assertNotEquals(snapshot, "not a snapshot");
  }

  @Test
  public void hashCode_sameUri_sameHash() {
    URI uri = URI.create("file:///virtual/test.a3p");

    assertEquals(new ProjectSnapshot(uri, "one", new ImageIcon()).hashCode(),
        new ProjectSnapshot(uri, "two", new ImageIcon()).hashCode());
  }

  @Test
  public void hashCode_differentUri_differentHash() {
    ProjectSnapshot first = new ProjectSnapshot(URI.create("file:///virtual/one.a3p"), "one", new ImageIcon());
    ProjectSnapshot second = new ProjectSnapshot(URI.create("file:///virtual/two.a3p"), "two", new ImageIcon());

    assertNotEquals(first.hashCode(), second.hashCode());
  }
}
