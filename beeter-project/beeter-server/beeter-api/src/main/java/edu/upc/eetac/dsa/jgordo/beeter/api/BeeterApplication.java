package edu.upc.eetac.dsa.jgordo.beeter.api;

import org.glassfish.jersey.linking.DeclarativeLinkingFeature;
import org.glassfish.jersey.server.ResourceConfig;
 
public class BeeterApplication extends ResourceConfig {
	public BeeterApplication() {
		super();
		System.out.println("prueva");
		register(DeclarativeLinkingFeature.class);
	}
}