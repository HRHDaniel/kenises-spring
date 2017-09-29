package com.amazonaws.lambda.testdrh;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

@SpringBootApplication
public class LambdaSpringBootstrap implements RequestHandler<KinesisEvent, Integer> {
	
	private static ApplicationContext appContext = null;
	
	private static ApplicationContext getAppContext() {
		if ( appContext == null ) {
			appContext = new SpringApplicationBuilder()
	                .sources(LambdaSpringBootstrap.class)
	                .bannerMode(Banner.Mode.OFF)
	                .web(false)
	                .build()
	                .run();
		}
		return appContext;
	}
	
	public static void main(String[] args) {
		// TODO
	}

	@Override
	public Integer handleRequest(KinesisEvent event, Context context) {
		return getAppContext().getBean(LambdaFunctionHandler.class).handleRequest(event,context);
	}

}
