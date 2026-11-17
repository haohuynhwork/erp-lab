package com.erplab.masterdata.domain;

public enum ProductType {

	/** Bought and consumed in production. */
	RAW,
	/** Made in production, used in other products. */
	SEMI_FINISHED,
	/** Made in production and sold. */
	FINISHED,
	/** Bought and sold as is. */
	GOODS,
	/** Not stocked. */
	SERVICE;

	public boolean stocked() {
		return this != SERVICE;
	}

	public boolean manufactured() {
		return this == SEMI_FINISHED || this == FINISHED;
	}

}
