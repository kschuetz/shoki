package com.jnape.palatable.shoki.impl;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.shoki.api.Collection;
import com.jnape.palatable.shoki.api.HashingAlgorithm;
import com.jnape.palatable.shoki.api.Natural;
import com.jnape.palatable.shoki.api.SizeInfo;
import com.jnape.palatable.shoki.api.SizeInfo.Known;
import com.jnape.palatable.shoki.api.Stack;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.Maybe.nothing;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Downcast.downcast;
import static com.jnape.palatable.lambda.functions.builtin.fn3.FoldLeft.foldLeft;
import static com.jnape.palatable.shoki.api.EquivalenceRelation.equivalent;
import static com.jnape.palatable.shoki.api.EquivalenceRelation.objectEquals;
import static com.jnape.palatable.shoki.api.Natural.zero;
import static com.jnape.palatable.shoki.api.OrderedCollection.EquivalenceRelations.sameElementsSameOrder;
import static com.jnape.palatable.shoki.api.OrderedCollection.HashingAlgorithms.elementsInOrder;
import static com.jnape.palatable.shoki.api.SizeInfo.known;
import static com.jnape.palatable.shoki.impl.Memoized.memoized;

/**
 * A strictly-evaluated {@link Stack}.
 *
 * @param <A> the element type
 * @see StrictQueue
 */
public abstract class StrictStack<A> implements Stack<Natural, A> {

    private StrictStack() {
    }

    /**
     * Produce a new {@link StrictStack} instance with <code>a</code> added to the front. <code>O(1)</code>.
     *
     * @param a the element
     * @return the new {@link StrictStack}
     */
    @Override
    public final StrictStack<A> cons(A a) {
        return new Head<>(a, this);
    }

    /**
     * The remaining elements after removing the head of this {@link StrictStack}, or an empty {@link StrictStack} if
     * there is no head. <code>O(1)</code>.
     *
     * @return the tail of this {@link StrictStack}
     */
    @Override
    public abstract StrictStack<A> tail();

    /**
     * Reverse this {@link StrictStack}. <code>O(n)</code>.
     *
     * @return this {@link StrictStack}, reversed
     */
    @Override
    public StrictStack<A> reverse() {
        return foldLeft(StrictStack::cons, strictStack(), this);
    }

    /**
     * If this {@link StrictStack} is not empty, return the head element wrapped in {@link Maybe}. Otherwise, return
     * {@link Maybe#nothing()}. <code>O(1)</code>.
     *
     * @return {@link Maybe} the head element of this {@link StrictStack}
     */
    @Override
    public abstract Maybe<A> head();

    /**
     * The {@link SizeInfo} of this {@link StrictStack}. Amortized <code>O(1)</code>.
     */
    @Override
    public abstract Known<Natural> sizeInfo();

    /**
     * Returns true if this {@link StrictStack} is empty; otherwise, returns false. <code>O(1)</code>.
     *
     * @return whether or not this {@link StrictStack} is empty
     */
    @Override
    public abstract boolean isEmpty();

    /**
     * {@inheritDoc}
     * <code>O(o)</code>.
     */
    @Override
    public StrictStack<A> consAll(Collection<Natural, A> other) {
        return (StrictStack<A>) Stack.super.consAll(other);
    }

    /**
     * Returns true if <code>other</code> is an {@link StrictStack} with exactly the same elements in the same order
     * as this {@link StrictStack}; otherwise, returns false. <code>O(n)</code>.
     *
     * @param other the reference object with which to compare
     * @return true if the compared to a value-equal {@link StrictStack}
     */
    @Override
    public final boolean equals(Object other) {
        return other instanceof StrictStack<?> &&
                equivalent(this, downcast(other), sameElementsSameOrder(objectEquals()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Iterator<A> iterator() {
        return new Iterator<A>() {
            StrictStack<A> rest = StrictStack.this;

            @Override
            public boolean hasNext() {
                return rest instanceof Head<?>;
            }

            @Override
            public A next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                Head<A> head = (Head<A>) this.rest;
                A       next = head.head;
                rest = head.tail;
                return next;
            }
        };
    }

    /**
     * {@inheritDoc}
     * Amortized <code>O(1)</code>.
     *
     * @return the hash code
     */
    @Override
    public abstract int hashCode();

    /**
     * Provide a debug-friendly string representation of this {@link StrictStack}. <code>O(n)</code>
     *
     * @return the string representation of this {@link StrictStack}
     */
    @Override
    public final String toString() {
        StringBuilder body = new StringBuilder("StrictStack[");

        StrictStack<A> next = this;
        while (next != Empty.INSTANCE) {
            Head<A> head = (Head<A>) next;
            body.append(head.head);
            next = head.tail;
            if (next != Empty.INSTANCE)
                body.append(", ");
        }

        return body.append("]").toString();
    }

    /**
     * Create a {@link StrictStack} of zero or more elements, with the elements queued for removal from left to right.
     * <code>O(n)</code>.
     *
     * @param as  the elements to {@link StrictStack#cons(Object) cons} from back to front
     * @param <A> the element type
     * @return the {@link StrictStack}
     */
    @SafeVarargs
    public static <A> StrictStack<A> strictStack(A... as) {
        @SuppressWarnings("unchecked")
        StrictStack<A> result = (StrictStack<A>) Empty.INSTANCE;
        for (int i = as.length - 1; i >= 0; i--)
             result = result.cons(as[i]);
        return result;
    }

    private static final class Head<A> extends StrictStack<A> {
        private final A                 head;
        private final StrictStack<A>    tail;
        private final Memoized<Natural> size;
        private final Memoized<Integer> hashCode;

        private Head(A head, StrictStack<A> tail) {
            this.head = head;
            this.tail = tail;
            size      = memoized(foldLeft((Natural s, A __) -> s.inc(), zero()).thunk(this));
            hashCode  = memoized(elementsInOrder(HashingAlgorithm.<A>objectHashCode()).thunk(this));
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Maybe<A> head() {
            return just(head);
        }

        @Override
        public StrictStack<A> tail() {
            return tail;
        }

        @Override
        public Known<Natural> sizeInfo() {
            return known(size.getOrCompute());
        }

        @Override
        public int hashCode() {
            return hashCode.getOrCompute();
        }
    }

    private static final class Empty<A> extends StrictStack<A> {
        private static final Empty<?> INSTANCE = new Empty<>();

        private Empty() {
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Maybe<A> head() {
            return nothing();
        }

        @Override
        public StrictStack<A> tail() {
            return this;
        }

        @Override
        public Known<Natural> sizeInfo() {
            return known(zero());
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
