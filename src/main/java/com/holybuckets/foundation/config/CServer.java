package com.holybuckets.foundation.config;


public class CServer extends ConfigBase {

	public final CFoundation foundation = nested(0, CFoundation::new, Comments.foundation);

	@Override
	public String getName() {
		return "server";
	}

	private static class Comments {
		static String foundation = "Configs for the Holy Buckets Foundational Utilities library mod";
	}

}
