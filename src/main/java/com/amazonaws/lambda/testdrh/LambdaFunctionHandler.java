package com.amazonaws.lambda.testdrh;

import java.util.Base64;
import java.util.HashMap;

import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;

@Component
public class LambdaFunctionHandler {

	@Autowired
	private TestBean testDependency;
	
	@Value("${bpm:resources/sample.bpmn}")
	private String bpm;

	public Integer handleRequest(KinesisEvent event, Context context) {
		processKinesisEvent(event, context);

//		runBpm();

		return event.getRecords().size();
	}

	private void runBpm(String input) {
		KieHelper kieHelper = new KieHelper();
		
		KieBase kbase = kieHelper.addResource(ResourceFactory.newFileResource(bpm)).build();
		KieSession ksession = kbase.newKieSession();
		try {
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put("input", input);
			ProcessInstance processInstance = ksession.startProcess("com.sample.bpmn.hello", parameters);
		} finally {
			ksession.dispose();
		}
	}

	private void processKinesisEvent(KinesisEvent event, Context context) {
		context.getLogger().log("Input: " + event);
		context.getLogger().log("Using dependency: " + testDependency.getName());

		for (KinesisEventRecord record : event.getRecords()) {
			String payload = new String(record.getKinesis().getData().array());
//			String payload = new String(Base64.getDecoder().decode(record.getKinesis().getData().array()));
			context.getLogger().log("Payload: " + payload);
			runBpm(payload);
		}
	}

	private void runBPM(KinesisEvent event, Context context) {
		
//		KieServices kieServices = KieServices.Factory.get();
//		
//		KieModuleModel kieModuleModel = kieServices.newKieModuleModel();
//
//
//		KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel( "kbase")
//		        .setDefault(true);
////		        .setEqualsBehavior( EqualityBehaviorOption.EQUALITY )
////		        .setEventProcessingMode( EventProcessingOption.STREAM );
//
//		KieSessionModel ksessionModel = kieBaseModel.newKieSessionModel( "ksession" )
//		        .setDefault(true);
////		        .setType( KieSessionModel.KieSessionType.STATELESS )
////		        .setClockType( ClockTypeOption.get("realtime") );
//
//		KieFileSystem kfs = kieServices.newKieFileSystem();
//		kieServices.newKieBuilder(kfs).buildAll();
//		KieContainer container = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
		

//		
//		KieServices kieServices = KieServices.Factory.get();
//		KieContainer container = kieServices.newKieClasspathContainer();
//		KieBase kbase = container.getKieBase();
//		KieSession ksession = kbase.newKieSession();
//		try {
//			ksession.startProcess("com.sample.bpmn.hello");
//		} finally {
//			ksession.dispose();
//		}
	}
}
