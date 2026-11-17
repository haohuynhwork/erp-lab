/**
 * Shared kernel: base entity, response envelope, error handling, security and web config.
 * Every context may use it; it never depends on a context.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.erplab.common;
