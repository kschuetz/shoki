package com.jnape.palatable.shoki;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.shoki.api.Bag;
import com.jnape.palatable.shoki.api.Collection;
import com.jnape.palatable.shoki.api.EquivalenceRelation;
import com.jnape.palatable.shoki.api.HashingAlgorithm;
import com.jnape.palatable.shoki.api.views.MapView;
import com.jnape.palatable.shoki.api.views.SetView;

import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Eq.eq;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Into.into;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Map.map;
import static com.jnape.palatable.lambda.functions.builtin.fn3.FoldLeft.foldLeft;
import static com.jnape.palatable.lambda.functions.builtin.fn4.IfThenElse.ifThenElse;
import static com.jnape.palatable.shoki.api.EquivalenceRelation.objectEquals;
import static com.jnape.palatable.shoki.api.HashingAlgorithm.objectHashCode;

public final class ImmutableHashBag<A> implements Bag<Integer, A>, SetView<Integer, A>, MapView<Integer, A, Integer> {

    private static final ImmutableHashBag<?> DEFAULT_EMPTY = new ImmutableHashBag<>(HashArrayMapTrie.empty());

    private final HashArrayMapTrie<A, Integer> table;

    private ImmutableHashBag(HashArrayMapTrie<A, Integer> table) {
        this.table = table;
    }

    @Override
    public ImmutableHashSet<A> asSet() {
        return table.keys();
    }

    @Override
    public HashArrayMapTrie<A, Integer> asMap() {
        return table;
    }

    @Override
    public ImmutableHashBag<A> add(A a) {
        return new ImmutableHashBag<>(table.putOrUpdate(a, 1, Integer::sum));
    }

    @Override
    public ImmutableHashBag<A> remove(A a) {
        return table.get(a)
                .match(constantly(this),
                       ifThenElse(eq(1),
                                  __ -> new ImmutableHashBag<>(table.remove(a)),
                                  count -> new ImmutableHashBag<>(table.put(a, count - 1))));
    }

    @Override
    public ImmutableHashBag<A> tail() {
        return new ImmutableHashBag<>(table.tail());
    }

    @Override
    public SizeInfo.Known<Integer> sizeInfo() {
        return table.sizeInfo();
    }

    @Override
    public Integer get(A a) {
        return table.get(a).orElse(0);
    }

    @Override
    public boolean contains(A a) {
        return table.contains(a);
    }

    @Override
    public Maybe<Tuple2<A, Integer>> head() {
        return table.head();
    }

    @Override
    public String toString() {
        return "ImmutableHashBag[" +
                String.join(", ", map(into((e, c) -> String.format("(%s=%d)", e, c)), table)) +
                ']';
    }

    public static <A> ImmutableHashBag<A> empty(EquivalenceRelation<A> equivalenceRelation,
                                                HashingAlgorithm<A> hashingAlgorithm) {
        return new ImmutableHashBag<>(HashArrayMapTrie.empty(equivalenceRelation, hashingAlgorithm));
    }

    @SuppressWarnings("unchecked")
    public static <A> ImmutableHashBag<A> empty() {
        return (ImmutableHashBag<A>) DEFAULT_EMPTY;
    }

    public static <A> ImmutableHashBag<A> fromCollection(Collection<Integer, A> as,
                                                         EquivalenceRelation<A> equivalenceRelation,
                                                         HashingAlgorithm<A> hashingAlgorithm) {
        return foldLeft(ImmutableHashBag::add, empty(equivalenceRelation, hashingAlgorithm), as);
    }

    public static <A> ImmutableHashBag<A> fromCollection(Collection<Integer, A> as) {
        return fromCollection(as, objectEquals(), objectHashCode());
    }
}
