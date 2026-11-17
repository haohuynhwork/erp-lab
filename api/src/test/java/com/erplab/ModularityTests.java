package com.erplab;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails when a context reaches into another context's internals. */
class ModularityTests {

	@Test
	void verifiesModuleBoundaries() {
		ApplicationModules.of(ApiApplication.class).verify();
	}

}
