package org.acme;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.drools.demo.demo20230606_datamodel.Fact;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusIntegrationTest
public class DroolsResourceIT {
    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void testEndpoint() throws Exception {
        final var JSON = Files.readString(Paths.get(DroolsResourceIT.class.getResource("/100-line-items.json").toURI()));

        final var OUTPUT_JSON = given()
          .when()
          .body(JSON)
          .contentType(ContentType.JSON)
          .post("/drools")
          .then()
             .statusCode(200)
             .extract()
             .asString()
             ;
        List<Fact> unmarshalOutput = new ObjectMapper()
                .readerFor(new TypeReference<List<Fact>>() {})
                .readValue(OUTPUT_JSON);
        assertThat(unmarshalOutput)
                .as("202 of '100 items' (1x Account, 1x Cart, 100x CartItem, 100x Product) from original file, + 100x Discount from rules")
                .hasSize(302);
                
        List<Fact> priceAdjustments = unmarshalOutput.stream().filter(t -> t.getObjectType().equals("PriceAdjustment")).collect(Collectors.toList());
        assertThat(priceAdjustments)
                .hasSize(100)
                .first()
                .hasFieldOrPropertyWithValue("fields.Source", "Promotion")
                ;
    }
}
