package com.jnape.palatable.shoki.api;

import com.jnape.palatable.lambda.adt.hlist.Tuple2;

public interface Bag<Size extends Number, A> extends Collection<Size, Tuple2<A, Integer>>, RandomAccess<A, Size> {

    Bag<Size, A> add(A a);

    Bag<Size, A> remove(A a);

    Set<Size, A> asSet();

    Map<Size, A, Integer> asMap();

    @Override
    Bag<Size, A> tail();
}
