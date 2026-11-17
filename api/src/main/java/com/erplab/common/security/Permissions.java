package com.erplab.common.security;

/** Authorities carried in the access token ({@code perms} claim), used in {@code @PreAuthorize}. */
public final class Permissions {

	private Permissions() {
	}

	public static final String USERS_MANAGE = "users:manage";
	public static final String MASTERDATA_READ = "masterdata:read";
	public static final String MASTERDATA_WRITE = "masterdata:write";
	public static final String INVENTORY_READ = "inventory:read";
	public static final String INVENTORY_WRITE = "inventory:write";
	public static final String PURCHASING_READ = "purchasing:read";
	public static final String PURCHASING_WRITE = "purchasing:write";
	public static final String SALES_READ = "sales:read";
	public static final String SALES_WRITE = "sales:write";
	public static final String ACCOUNTING_READ = "accounting:read";
	public static final String ACCOUNTING_WRITE = "accounting:write";
	public static final String REPORTS_READ = "reports:read";

}
