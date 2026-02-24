package com.example.platformservice;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import feign.Response;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.lang.reflect.Type;

@Configuration
public class FeignXmlConfig {

    @Bean
    public XmlFeignDecoder xmlFeignDecoder() {
        return new XmlFeignDecoder();
    }

    public static class XmlFeignDecoder implements Decoder {

        private final XmlMapper xmlMapper = new XmlMapper();

        @Override
        public Object decode(Response response, Type type) throws IOException {
            if (response.body() == null) {
                return null;
            }
            return xmlMapper.readValue(
                    response.body().asInputStream(),
                    xmlMapper.constructType(type)
            );
        }
    }
}
