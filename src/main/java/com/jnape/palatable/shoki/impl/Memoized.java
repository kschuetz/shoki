package com.jnape.palatable.shoki.impl;

import com.jnape.palatable.lambda.functions.Fn0;

final class Memoized<A> {

    private final Fn0<A> computation;

    private volatile A       a;
    private volatile boolean computed;

    private Memoized(Fn0<A> computation) {
        this.computation = computation;
    }

    public A getOrCompute() {
        if (!computed)
            synchronized (this) {
                if (!computed) {
                    a        = computation.apply();
                    computed = true;
                }
            }

        return a;
    }

    public static <A> Memoized<A> memoized(Fn0<A> computation) {
        return new Memoized<>(computation);
    }
}
