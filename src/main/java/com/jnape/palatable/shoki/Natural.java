package com.jnape.palatable.shoki;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.adt.coproduct.CoProduct2;
import com.jnape.palatable.lambda.functions.Fn1;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Objects;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Eq.eq;
import static com.jnape.palatable.lambda.functions.builtin.fn2.GTE.gte;
import static com.jnape.palatable.lambda.functions.builtin.fn4.IfThenElse.ifThenElse;
import static java.math.BigInteger.ZERO;

public abstract class Natural extends Number implements
        CoProduct2<Natural.Zero, Natural.NonZero, Natural>,
        Comparable<Natural> {

    private Natural() {
    }

    public final BigInteger value() {
        return match(constantly(BigInteger.ZERO), nz -> nz.value);
    }

    @Override
    public int compareTo(Natural other) {
        return Comparator.<BigInteger>naturalOrder().compare(value(), other.value());
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof Natural && Objects.equals(value(), ((Natural) other).value());
    }

    @Override
    public int hashCode() {
        return value().hashCode();
    }

    public abstract Natural add(Zero zero);

    public abstract NonZero add(NonZero nonZero);

    public abstract Natural add(Natural addend);

    @Override
    public final int intValue() {
        return match(constantly(0), nz -> nz.value().intValue());
    }

    @Override
    public final long longValue() {
        return match(constantly(0L), nz -> nz.value().longValue());
    }

    @Override
    public final float floatValue() {
        return match(constantly(0F), nz -> nz.value().floatValue());
    }

    @Override
    public final double doubleValue() {
        return match(constantly(0D), nz -> nz.value().doubleValue());
    }

    public static Maybe<Natural> natural(BigInteger value) {
        return just(value)
                .filter(gte(ZERO))
                .fmap(ifThenElse(eq(ZERO), constantly(Zero.INSTANCE), NonZero::new));
    }

    public static Zero zero() {
        return Zero.INSTANCE;
    }

    public static Natural clampZero(BigInteger value) {
        return natural(value).orElse(Zero.INSTANCE);
    }

    public static final class Zero extends Natural {
        private static final Zero INSTANCE = new Zero();

        private Zero() {
        }

        @Override
        public Natural add(Natural addend) {
            return addend;
        }

        @Override
        public Zero add(Zero zero) {
            return this;
        }

        @Override
        public NonZero add(NonZero nonZero) {
            return nonZero;
        }

        @Override
        public <R> R match(Fn1<? super Zero, ? extends R> aFn, Fn1<? super NonZero, ? extends R> bFn) {
            return aFn.apply(this);
        }
    }

    public static final class NonZero extends Natural {
        private final BigInteger value;

        private NonZero(BigInteger value) {
            this.value = value;
        }

        @Override
        public NonZero add(Zero zero) {
            return this;
        }

        @Override
        public NonZero add(NonZero nonZero) {
            return add((Natural) nonZero);
        }

        @Override
        public NonZero add(Natural addend) {
            return new NonZero(value.add(addend.value()));
        }

        @Override
        public <R> R match(Fn1<? super Zero, ? extends R> aFn, Fn1<? super NonZero, ? extends R> bFn) {
            return bFn.apply(this);
        }
    }
}
