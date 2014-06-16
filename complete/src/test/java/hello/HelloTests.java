
package hello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class HelloTests {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private HttpMessageConverters converters;

	@Test
	public void envEndpointNotHidden() {
		TestRestTemplate template = new TestRestTemplate();
		template.setMessageConverters(converters.getConverters());
		ResponseEntity<Greeting> response = template.exchange("http://localhost:"
				+ this.port + "/greeting", HttpMethod.GET, new HttpEntity<Void>(
				(Void) null), Greeting.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody().getContent());
	}
}