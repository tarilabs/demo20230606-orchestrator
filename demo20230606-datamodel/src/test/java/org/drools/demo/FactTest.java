package org.drools.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.drools.demo.demo20230606.datamodel.Fact;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FactTest {

    @Test
    public void basicUnmarshalling() throws Exception {
        String json = """
                {
                    "objectType": "Cart",
                    "id": "abc-123",
                    "fields": {
                        "a":1, "b": 47
                    }
                }
                """;

        Fact bean = new ObjectMapper()
            .readerFor(Fact.class)
            .readValue(json);
        System.out.println(bean);
        
        assertThat(bean.getObjectType()).isEqualTo("Cart");
        assertThat(bean.getId()).isEqualTo("abc-123");
        assertThat(bean.getFieldValue("b")).isEqualTo(47);
    }

    @Test
    public void settingPerRHSIbelieve() throws Exception {
        Fact discount = new Fact( "PriceAdjustment", "abc-123" );
        discount.setFieldValue( "Source", "Promotion" );
        System.out.println(discount);

        assertThat(discount.getObjectType()).isEqualTo("PriceAdjustment");
        assertThat(discount.getId()).isEqualTo("abc-123");
        assertThat(discount.getFieldValue("Source")).isEqualTo("Promotion");
    }
}
