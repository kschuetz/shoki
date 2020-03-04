package com.jnape.palatable.shoki.api.views;

import com.jnape.palatable.shoki.api.Set;

public interface SetView<Size extends Number, A> {

    Set<Size, A> asSet();
}
