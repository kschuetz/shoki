package com.jnape.palatable.shoki;

import com.jnape.palatable.shoki.testsupport.StubbedHashingAlgorithm;
import org.junit.Test;

import java.util.HashMap;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.Maybe.nothing;
import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.shoki.HashArrayMapTrie.empty;
import static com.jnape.palatable.shoki.SizeInfo.known;
import static com.jnape.palatable.shoki.api.EquivalenceRelation.objectEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static testsupport.matchers.IterableMatcher.isEmpty;
import static testsupport.matchers.IterableMatcher.iterates;

public class HashArrayMapTrieTest {

    @Test
    public void getMissingKey() {
        assertEquals(nothing(), HashArrayMapTrie.<Integer, Boolean>empty().get(0));
    }

    @Test
    public void getPresentKey() {
        assertEquals(just(true), HashArrayMapTrie.<Integer, Boolean>empty().put(0, true).get(0));
    }

    @Test
    public void putReplacementKey() {
        assertEquals(just(false), HashArrayMapTrie.<Integer, Boolean>empty()
                .put(0, true)
                .put(0, false)
                .get(0));
    }

    @Test
    public void immutability() {
        HashArrayMapTrie<Integer, Boolean> empty = empty();
        assertNotSame(empty, empty.put(0, true));
        assertEquals(nothing(), empty.get(0));
    }

    @Test
    public void keysWithPartialHashCollisionPropagateDownwards() {
        HashArrayMapTrie<String, Integer> nested = HashArrayMapTrie.<String, Integer>empty(
                objectEquals(), StubbedHashingAlgorithm.<String>stubbedHashingAlgorithm()
                        .stub("foo", 0b00_00000_00000_00000_00000_00000_00000)
                        .stub("bar", 0b00_00000_00000_00000_00000_00001_00000)
                        .stub("baz", 0b00_00000_00000_00000_00001_00001_00000)
                        .stub("qux", 0b00_00000_00000_00000_00001_00000_00000)
                        .stub("zux", 0b01_00000_00000_00000_00001_00000_00000))
                .put("foo", 1)
                .put("bar", 2)
                .put("baz", 3)
                .put("qux", 4)
                .put("zux", 5);

        assertEquals(just(1), nested.get("foo"));
        assertEquals(just(2), nested.get("bar"));
        assertEquals(just(3), nested.get("baz"));
        assertEquals(just(4), nested.get("qux"));
        assertEquals(just(5), nested.get("zux"));
    }

    @Test
    public void keysWithFullCollisionsAreStoredAdjacently() {
        HashArrayMapTrie<String, Integer> collision = HashArrayMapTrie.<String, Integer>empty(
                objectEquals(), StubbedHashingAlgorithm.<String>stubbedHashingAlgorithm()
                        .stub("foo", 0b00_00000_00000_00000_00000_00000_00000)
                        .stub("bar", 0b00_00000_00000_00000_00000_00000_00000))
                .put("foo", 0)
                .put("bar", 1);

        assertEquals(just(0), collision.get("foo"));
        assertEquals(just(1), collision.get("bar"));
    }

    @Test
    public void overridingAsPartOfCollision() {
        HashArrayMapTrie<String, Integer> collision = HashArrayMapTrie.<String, Integer>empty(
                objectEquals(), StubbedHashingAlgorithm.<String>stubbedHashingAlgorithm()
                        .stub("foo", 0b00_00000_00000_00000_00000_00000_00000)
                        .stub("bar", 0b00_00000_00000_00000_00000_00000_00000))
                .put("foo", 0)
                .put("bar", 1)
                .put("bar", 2);

        assertEquals(just(0), collision.get("foo"));
        assertEquals(just(2), collision.get("bar"));
    }

    @Test
    public void contains() {
        HashArrayMapTrie<Integer, String> empty = empty();
        assertFalse(empty.contains(0));
        assertTrue(empty.put(0, "foo").contains(0));
    }

    @Test
    public void remove() {
        HashArrayMapTrie<Integer, Boolean> empty = empty();
        assertEquals(empty, empty.remove(0));
        assertEquals(nothing(), empty.put(0, true).remove(0).get(0));
    }

    @Test
    public void removeNested() {
        HashArrayMapTrie<String, Integer> nested = HashArrayMapTrie.<String, Integer>empty(
                objectEquals(), StubbedHashingAlgorithm.<String>stubbedHashingAlgorithm()
                        .stub("foo", 0b00_00000_00000_00000_00000_00000_00000)
                        .stub("bar", 0b00_00000_00000_00000_00000_00001_00000))
                .put("foo", 0)
                .put("bar", 1)
                .remove("foo");
        assertEquals(just(1), nested.get("bar"));
        assertEquals(nothing(), nested.get("foo"));
    }

    @Test
    public void removeFromCollision() {
        HashArrayMapTrie<String, Integer> collision = HashArrayMapTrie.<String, Integer>empty(
                objectEquals(), StubbedHashingAlgorithm.<String>stubbedHashingAlgorithm()
                        .stub("foo", 0b00_00000_00000_00000_00000_00000_00000)
                        .stub("bar", 0b00_00000_00000_00000_00000_00000_00000))
                .put("foo", 0)
                .put("bar", 1)
                .remove("foo");

        assertEquals(nothing(), collision.get("foo"));
        assertEquals(just(1), collision.get("bar"));
    }

    @Test
    public void buildingFromJavaMap() {
        HashArrayMapTrie<Integer, Boolean> hashArrayMapTrie = HashArrayMapTrie.fromJavaMap(new HashMap<Integer,
                Boolean>() {{
            put(0, true);
            put(1, false);
            put(2, true);
        }});

        assertEquals(just(true), hashArrayMapTrie.get(0));
        assertEquals(just(false), hashArrayMapTrie.get(1));
        assertEquals(just(true), hashArrayMapTrie.get(2));
        assertEquals(nothing(), hashArrayMapTrie.get(3));
    }

    @Test
    public void sizeInfo() {
        assertEquals(known(0), empty().sizeInfo());
        assertEquals(known(1), HashArrayMapTrie.empty().put(1, 1).sizeInfo());
        HashArrayMapTrie<Integer, Boolean> collisionsAndNesting =
                HashArrayMapTrie.<Integer, Boolean>empty(objectEquals(),
                                                         StubbedHashingAlgorithm.<Integer>stubbedHashingAlgorithm()
                                                                 .stub(0, 0b00_00000_00000_00000_00000_00000_00000)
                                                                 .stub(1, 0b00_00000_00000_00000_00000_00001_00000)
                                                                 .stub(2, 0b00_00000_00000_00000_00000_00001_00000))
                        .put(0, true)
                        .put(1, false)
                        .put(2, true);
        assertEquals(known(3), collisionsAndNesting.sizeInfo());
        assertEquals(known(0), empty().put(1, 1).remove(1).sizeInfo());
    }

    @Test
    public void emptyDetection() {
        assertTrue(empty().isEmpty());
        assertFalse(empty().put(1, 1).isEmpty());
        assertTrue(empty().put(1, 1).remove(1).isEmpty());
    }

    @Test
    public void toStringIsUseful() {
        assertEquals("ImmutableHashMap[]", empty().toString());
        assertEquals("ImmutableHashMap[(key=value)]", empty().put("key", "value").toString());
        assertEquals("ImmutableHashMap[(foo=foo value)|(baz=baz value)|(bar=bar value)]",
                     empty(objectEquals(),
                           StubbedHashingAlgorithm.<String>stubbedHashingAlgorithm()
                                   .stub("foo", 0b00_00000_00000_00000_00000_00000_00000)
                                   .stub("bar", 0b00_00000_00000_00000_00000_00001_00000)
                                   .stub("baz", 0b00_00000_00000_00000_00000_00001_00000))
                             .put("foo", "foo value")
                             .put("bar", "bar value")
                             .put("baz", "baz value")
                             .toString());
    }

    @Test
    public void iterableOverEntries() {
        assertThat(empty(), isEmpty());
        assertThat(empty().put(0, true), iterates(tuple(0, true)));
        assertThat(empty(objectEquals(),
                         StubbedHashingAlgorithm.<String>stubbedHashingAlgorithm()
                                 .stub("foo", 0b00_00000_00000_00000_00000_00000_00000)
                                 .stub("bar", 0b00_00000_00000_00000_00000_00001_00000)
                                 .stub("baz", 0b00_00000_00000_00000_00000_00001_00000))
                           .put("foo", 1)
                           .put("bar", 2)
                           .put("baz", 3),
                   iterates(tuple("foo", 1), tuple("baz", 3), tuple("bar", 2)));
    }

    @Test
    public void headEntry() {
        assertEquals(nothing(), empty().head());
        assertEquals(just(tuple("foo", 1)), empty().put("foo", 1).head());
        assertEquals(just(tuple("foo", 1)), empty().put("foo", 1).put("bar", 32).head());
        assertEquals(just(tuple("foo", 1)), empty().put("bar", 32).put("foo", 1).head());
    }

    @Test
    public void tail() {
        assertEquals(empty(), empty().tail());
        assertTrue(empty().put("foo", 1).tail().isEmpty());

        HashArrayMapTrie<Integer, Boolean> tail = HashArrayMapTrie.<Integer, Boolean>empty()
                .put(0, true)
                .put(32, false)
                .tail();
        assertEquals(nothing(), tail.get(0));
        assertEquals(just(false), tail.get(32));
    }

    @Test
    public void sameEntries() {
        assertTrue(empty().sameEntries(empty()));
        assertTrue(empty().put(1, 1).sameEntries(empty().put(1, 1)));
        assertTrue(empty().put(1, 1).put(2, 2).remove(2).sameEntries(empty().put(1, 1)));

        assertTrue(HashArrayMapTrie.<Integer, Boolean>empty()
                           .put(1, true)
                           .put(2, false)
                           .sameEntries(HashArrayMapTrie.<Integer, Boolean>empty()
                                                .put(1, true)
                                                .put(2, true),
                                        (v1, v2) -> true));

        assertTrue(HashArrayMapTrie.<Integer, Boolean>empty((k, k2) -> k % 2 == k2 % 2, k -> k.hashCode() % 2)
                           .put(1, true)
                           .put(2, false)
                           .put(3, false)
                           .put(4, true).sameEntries(HashArrayMapTrie.<Integer, Boolean>empty()
                                                             .put(3, false)
                                                             .put(4, true)));

        assertTrue(HashArrayMapTrie.<Integer, Boolean>empty()
                           .put(3, false)
                           .put(4, true).sameEntries(HashArrayMapTrie.<Integer, Boolean>empty((k, k2) -> k % 2 == k2 % 2,
                                                                                              k -> k.hashCode() % 2)
                                                             .put(1, true)
                                                             .put(2, false)
                                                             .put(3, false)
                                                             .put(4, true)));

        assertFalse(HashArrayMapTrie.<Integer, Boolean>empty((k, k2) -> k % 2 == k2 % 2, k -> k.hashCode() % 2)
                            .put(4, true)
                            .put(3, false)
                            .put(2, false)
                            .put(1, true)
                            .sameEntries(HashArrayMapTrie.<Integer, Boolean>empty()
                                                 .put(3, false)
                                                 .put(4, true)));

        assertFalse(HashArrayMapTrie.<Integer, Boolean>empty()
                            .put(3, false)
                            .put(4, true)
                            .sameEntries(HashArrayMapTrie.<Integer, Boolean>empty((k, k2) -> k % 2 == k2 % 2,
                                                                                  k -> k.hashCode() % 2)
                                                 .put(4, true)
                                                 .put(3, false)
                                                 .put(2, false)
                                                 .put(1, true)));
    }

    @Test
    public void equalsUsesSameEntriesWithObjectEqualsForValues() {
        assertEquals(empty(), empty());
        assertEquals(empty().put(1, 1), empty().put(1, 1));
        assertEquals(empty().put(1, 1).put(2, 2).remove(2), empty().put(1, 1));

        assertNotEquals(empty(), empty().put(1, 1));
        assertNotEquals(empty().put(1, 1), empty().put(1, 2));
        assertNotEquals(empty().put(1, 1), empty().put(2, 1));

        assertEquals(HashArrayMapTrie.<Integer, Boolean>empty()
                             .put(0, true)
                             .put(32, false),
                     HashArrayMapTrie.<Integer, Boolean>empty()
                             .put(0, true)
                             .put(32, false)
                             .put(64, true)
                             .remove(64));
    }

    @Test
    public void hashCodeUsesKeyHashingAlgorithmForKeysAndObjectHashForValuesForEqualsSymmetry() {
        assertEquals(empty().hashCode(), empty().hashCode());
        assertEquals(empty().put(1, 1).hashCode(), empty().put(1, 1).hashCode());

        assertNotEquals(empty().hashCode(), empty().put(1, 1).hashCode());
        assertNotEquals(empty().put(1, 1).hashCode(), empty().put(1, 2).hashCode());

        assertEquals(empty().put(0, 1).put(32, 2).remove(32).hashCode(), empty().put(0, 1).hashCode());
    }

    @Test
    public void keys() {
        assertEquals(ImmutableHashSet.empty(), HashArrayMapTrie.empty().keys());
        assertEquals(ImmutableHashSet.of("foo", "bar", "baz"),
                     HashArrayMapTrie.fromJavaMap(new HashMap<String, Integer>() {{
                         put("foo", 1);
                         put("bar", 2);
                         put("baz", 3);
                     }}).keys());
    }

    @Test(timeout = 1000)
    public void insertionSanityBenchmark() {
        int                                n                = 1_000_000;
        HashArrayMapTrie<Integer, Boolean> hashArrayMapTrie = empty();
        for (int i = 0; i < n; i++) {
            hashArrayMapTrie = hashArrayMapTrie.put(i, true);
        }
    }
}
