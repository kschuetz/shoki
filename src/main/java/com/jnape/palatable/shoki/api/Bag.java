package com.jnape.palatable.shoki.api;

import com.jnape.palatable.lambda.adt.hlist.Tuple2;

/**
 * A {@link Bag} is a {@link Collection} of elements offering {@link RandomAccess} lookup for the number of occurrences
 * of a given element.
 *
 * @param <Size> the known size {@link Number} type
 * @param <A>    the element type
 */
public interface Bag<Size extends Number, A> extends Collection<Size, Tuple2<A, Size>>, RandomAccess<A, Size> {

    Bag<Size, A> add(A a);

    Bag<Size, A> add(A a, Size count);

    Bag<Size, A> remove(A a);

    Bag<Size, A> remove(A a, Size count);

    default Bag<Size, A> removeAll(A a) {
        return remove(a, get(a));
    }

    @Override
    Bag<Size, A> tail();
}
