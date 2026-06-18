package com.example.demo.sentiment_analysis.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisService redisService;

    @Test
    void getReturnsMappedValueWhenKeyExists() throws Exception {
        TypeReference<Map<String, String>> typeReference = new TypeReference<>() {};
        Map<String, String> expected = Map.of("name", "codex");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn("{\"name\":\"codex\"}");
        when(objectMapper.readValue("{\"name\":\"codex\"}", typeReference)).thenReturn(expected);

        Map<String, String> result = redisService.get("key", typeReference);

        assertEquals(expected, result);
    }

    @Test
    void getReturnsNullWhenKeyIsMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("missing")).thenReturn(null);

        assertNull(redisService.get("missing", new TypeReference<Map<String, String>>() {}));
    }

    @Test
    void getReturnsNullWhenRedisThrowsException() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertNull(redisService.get("key", new TypeReference<Map<String, String>>() {}));
    }

    @Test
    void setWritesJsonWithoutTtlWhenTtlIsNull() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(Map.of("count", 1))).thenReturn("{\"count\":1}");

        redisService.set("key", Map.of("count", 1), null);

        verify(valueOperations).set("key", "{\"count\":1}");
    }

    @Test
    void setWritesJsonWithTtlWhenTtlIsProvided() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString("value")).thenReturn("\"value\"");

        redisService.set("key", "value", 30L);

        verify(valueOperations).set("key", "\"value\"", 30L, TimeUnit.SECONDS);
    }

    @Test
    void setSwallowsSerializationException() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("bad json"));

        redisService.set("key", "value", 30L);
    }

    @Test
    void incrementReturnsRedisValueAndNullOnException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("count")).thenReturn(2L);

        assertEquals(2L, redisService.increment("count"));

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        assertNull(redisService.increment("count"));
    }

    @Test
    void decrementReturnsRedisValueAndNullOnException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement("count")).thenReturn(1L);

        assertEquals(1L, redisService.decrement("count"));

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        assertNull(redisService.decrement("count"));
    }

    @Test
    void getLongReturnsParsedValueNullForMissingAndNullForException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("count")).thenReturn("10", null, "bad");

        assertEquals(10L, redisService.getLong("count"));
        assertNull(redisService.getLong("count"));
        assertNull(redisService.getLong("count"));
    }

    @Test
    void setLongWritesWithAndWithoutTtlAndSwallowsException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.setLong("count", 10L, null);
        redisService.setLong("count", 11L, 60L);
        doThrow(new RuntimeException("redis down")).when(valueOperations).set("bad", "12");
        redisService.setLong("bad", 12L, null);

        verify(valueOperations).set("count", "10");
        verify(valueOperations).set("count", "11", 60L, TimeUnit.SECONDS);
    }
}
