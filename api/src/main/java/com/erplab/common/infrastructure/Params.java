package com.erplab.common.infrastructure;

import java.util.HashMap;
import java.util.Map;

/** Named SQL parameters that may be null ({@code Map.of} rejects nulls). */
public final class Params {

	private Params() {
	}

	public static Map<String, Object> of(Object... keysAndValues) {
		var map = new HashMap<String, Object>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			map.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return map;
	}

}
