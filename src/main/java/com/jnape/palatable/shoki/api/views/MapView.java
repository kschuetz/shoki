package com.jnape.palatable.shoki.api.views;

import com.jnape.palatable.shoki.api.Map;

public interface MapView<Size extends Number, K, V> {

    Map<Size, K, V> asMap();
}
