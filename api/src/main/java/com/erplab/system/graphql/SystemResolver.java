package com.erplab.system.graphql;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class SystemResolver {

	@QueryMapping
	String ping() {
		return "pong";
	}

}
