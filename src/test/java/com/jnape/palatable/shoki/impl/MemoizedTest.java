package com.jnape.palatable.shoki.impl;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jnape.palatable.lambda.functions.specialized.SideEffect.sideEffect;
import static com.jnape.palatable.shoki.impl.Memoized.memoized;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MemoizedTest {

    @Test
    public void yieldsResultOfComputation() {
        Memoized<Integer> memoizedInt = memoized(() -> 42);
        assertEquals((Integer) 42, memoizedInt.getOrCompute());
    }

    @Test
    public void isComputedOnlyOnce() {
        AtomicInteger     invocations = new AtomicInteger(0);
        Memoized<Integer> memoizedInt = memoized(invocations::incrementAndGet);
        assertEquals((Integer) 1, memoizedInt.getOrCompute());
        assertEquals((Integer) 1, memoizedInt.getOrCompute());
        assertEquals(1, invocations.get());
    }

    @Test
    public void threadSafe() {
        CountDownLatch latch = new CountDownLatch(2);
        Memoized<Integer> memoized = memoized(() -> {
            latch.countDown();
            if (latch.await(100, MILLISECONDS))
                throw new IllegalStateException("Expected mutual exclusion, but computations interleaved");
            return 42;
        });

        new Thread(sideEffect(memoized::getOrCompute).toRunnable()) {{
            start();
        }};
        assertEquals((Integer) 42, memoized.getOrCompute());
    }

    @Test
    public void doesNotMemoizeException() {
        AtomicInteger invocations = new AtomicInteger(0);
        Memoized<Integer> memoized = memoized(() -> {
            invocations.incrementAndGet();
            throw new IllegalStateException();
        });
        try {
            memoized.getOrCompute();
            fail("Expected exception to have been thrown");
        } catch (IllegalStateException expected) {
        }

        try {
            memoized.getOrCompute();
            fail("Expected exception to have been thrown");
        } catch (IllegalStateException expected) {
        }

        assertEquals(2, invocations.get());
    }
}