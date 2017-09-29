package com.amazonaws.lambda.testdrh;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.Base64;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
@RunWith(MockitoJUnitRunner.class)
public class LambdaFunctionHandlerTest {

	private KinesisEvent input;

	@Mock
	private TestBean testBean;

	@InjectMocks
	private LambdaFunctionHandler handler;
	
	private static byte[] adtFile;
	
	@BeforeClass 
	public static void readInputFile() throws IOException {
		adtFile = FileUtils.readFileToByteArray(new File("src/test/resources/adt03.xml"));
	}

	@Before
	public void createInput() throws IOException {
		
		input = TestUtils.parse("/kinesis-event.json", KinesisEvent.class);
		input.getRecords().get(0).getKinesis().setData(ByteBuffer.wrap(adtFile));
		ReflectionTestUtils.setField(handler, "bpm", "src/main/resources/sample.bpmn");
	}

	private Context createContext() {
		TestContext ctx = new TestContext();

		// TODO: customize your context here if needed.
		ctx.setFunctionName("Your Function Name");

		return ctx;
	}

	@Test
	public void testLambdaFunctionHandler() {
		String encoded = Base64.getEncoder().encodeToString("Hello, this is a test 123.".getBytes());
		System.err.println(encoded);
		Context ctx = createContext();

		Integer output = handler.handleRequest(input, ctx);

		// TODO: validate output here if needed.
		Assert.assertEquals(1, output.intValue());
	}
}
