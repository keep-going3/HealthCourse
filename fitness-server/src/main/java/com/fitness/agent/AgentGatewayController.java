package com.fitness.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.module.body.service.BodyService;
import com.fitness.module.user.entity.User;
import com.fitness.module.user.mapper.UserMapper;
import com.fitness.module.workout.dto.WorkoutSaveRequest;
import com.fitness.module.workout.service.WorkoutService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AgentGatewayController {

    private final WebClient agentWebClient;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final BodyService bodyService;
    private final WorkoutService workoutService;
    private final CircuitBreakerRegistry cbRegistry;

    @Value("${agent.base-url}")
    private String agentBaseUrl;

    @Value("${agent.connect-timeout}")
    private int connectTimeout;

    @Value("${agent.read-timeout}")
    private int readTimeout;

    @PostMapping("/api/agent/chat/stream")
    public SseEmitter chatStream(@RequestAttribute("userId") Long userId,
                                 @RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        SseEmitter emitter = new SseEmitter(300_000L);

        CircuitBreaker cb = cbRegistry.circuitBreaker("agent-chat");

        CompletableFuture.runAsync(() -> {
            try {
                // buildContext 可能在 DB 查询上耗时，先执行再进入熔断保护
                Map<String, Object> context = buildContext(userId);
                Decorators.ofRunnable(() -> proxyAgentChat(emitter, userId, message, context))
                        .withCircuitBreaker(cb)
                        .run();
            } catch (Exception e) {
                log.warn("agent proxy failed, fallback", e);
                fallbackResponse(emitter, message);
            }
        });

        return emitter;
    }

    @PostMapping("/api/agent/parse-workout")
    public Result<Map> parseWorkout(@RequestAttribute("userId") Long userId,
                                    @RequestBody Map<String, String> body) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("text", body.getOrDefault("text", ""));

        CircuitBreaker cb = cbRegistry.circuitBreaker("agent-parse");

        try {
            Map result = Decorators.ofSupplier(() ->
                            agentWebClient.post()
                                    .uri("/agent/parse-workout")
                                    .bodyValue(request)
                                    .retrieve()
                                    .bodyToMono(Map.class)
                                    .block())
                    .withCircuitBreaker(cb)
                    .decorate()
                    .get();
            return Result.success(result);
        } catch (Exception e) {
            log.error("parse-workout proxy error", e);
            return Result.error(503, "AI 解析服务暂时不可用");
        }
    }

    @GetMapping("/api/health")
    public Result<Map> health() {
        try {
            Map agentHealth = agentWebClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return Result.success(agentHealth);
        } catch (Exception e) {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "degraded");
            status.put("agent", "unreachable");
            return Result.success(status);
        }
    }

    // ---- 私有方法 ----

    private void proxyAgentChat(SseEmitter emitter, Long userId, String message,
                                Map<String, Object> context) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("message", message);
        request.put("context", context);

        HttpURLConnection conn = null;
        try {
            URL url = URI.create(agentBaseUrl + "/agent/chat/stream").toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(0); // SSE 是长连接，不做读取超时

            objectMapper.writeValue(conn.getOutputStream(), request);

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("agent returned HTTP {}", status);
                fallbackResponse(emitter, message);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    if (!trimmed.startsWith("data: ")) continue;

                    String json = trimmed.substring(6);

                    // 检测 save_workout 事件，后端代为保存训练
                    String eventType = null;
                    JsonNode workoutDataNode = null;
                    try {
                        JsonNode eventNode = objectMapper.readTree(json);
                        if (eventNode.has("type")) {
                            eventType = eventNode.get("type").asText();
                        }
                        if ("save_workout".equals(eventType)) {
                            workoutDataNode = eventNode.get("workoutData");
                        }
                    } catch (Exception e) {
                        // 非 JSON 或格式异常，当作普通 chunk 转发
                    }

                    if ("save_workout".equals(eventType) && workoutDataNode != null) {
                        try {
                            WorkoutSaveRequest req = objectMapper.treeToValue(workoutDataNode, WorkoutSaveRequest.class);
                            try {
                                workoutService.save(userId, req);
                                json = "{\"type\":\"chunk\",\"content\":\"✅ 训练已自动保存至记录！\"}";
                            } catch (BusinessException be) {
                                if (be.getCode() == 409) {
                                    workoutService.update(userId, req);
                                    json = "{\"type\":\"chunk\",\"content\":\"✅ 训练记录已更新！\"}";
                                } else {
                                    throw be;
                                }
                            }
                        } catch (Exception e) {
                            log.error("save workout error", e);
                            json = "{\"type\":\"chunk\",\"content\":\"⚠️ 训练自动保存失败\"}";
                        }
                        try {
                            emitter.send(SseEmitter.event().data(json, MediaType.APPLICATION_JSON));
                        } catch (IOException e) {
                            log.info("client disconnected, stopping agent stream");
                            break;
                        }
                    } else {
                        try {
                            emitter.send(SseEmitter.event().data(json, MediaType.APPLICATION_JSON));
                        } catch (IOException e) {
                            log.info("client disconnected, stopping agent stream");
                            break;
                        }
                    }
                }
            }
            emitter.complete();
        } catch (Exception e) {
            log.error("agent proxy error", e);
            fallbackResponse(emitter, message);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private Map<String, Object> buildContext(Long userId) {
        Map<String, Object> ctx = new HashMap<>();

        // userInfo
        User user = userMapper.selectById(userId);
        if (user != null) {
            Map<String, Object> ui = new HashMap<>();
            ui.put("goal", user.getGoal());
            ui.put("height", user.getHeightCm());
            ui.put("targetWeight", user.getTargetWeightKg());
            ctx.put("userInfo", ui);
        }

        // 最近训练（遍历最近 30 天）
        try {
            List<Map<String, Object>> recentWorkouts = new ArrayList<>();
            var end = java.time.LocalDate.now();
            var start = end.minusDays(30);
            for (var d = start; !d.isAfter(end); d = d.plusDays(1)) {
                var detail = workoutService.getDetail(userId, d.toString());
                if (detail != null) {
                    Map<String, Object> w = new HashMap<>();
                    w.put("sessionDate", detail.getSessionDate().toString());
                    w.put("bodyParts", detail.getBodyParts());
                    w.put("exercises", detail.getExercises().stream().map(ex -> {
                        Map<String, Object> em = new HashMap<>();
                        em.put("exerciseName", ex.getExerciseName());
                        em.put("setNumber", ex.getSetNumber());
                        em.put("weightKg", ex.getWeightKg());
                        em.put("reps", ex.getReps());
                        em.put("rpe", ex.getRpe());
                        return em;
                    }).toList());
                    recentWorkouts.add(w);
                }
            }
            ctx.put("recentWorkouts", recentWorkouts);
        } catch (Exception e) {
            log.warn("fetch recent workouts failed", e);
            ctx.put("recentWorkouts", Collections.emptyList());
        }

        // 最近 3 个月体重
        try {
            ctx.put("weightRecords", bodyService.getWeightList(userId, 3));
        } catch (Exception e) {
            log.warn("fetch weight records failed", e);
            ctx.put("weightRecords", Collections.emptyList());
        }

        ctx.put("chatHistory", Collections.emptyList());
        return ctx;
    }

    private void fallbackResponse(SseEmitter emitter, String message) {
        try {
            String msgId = UUID.randomUUID().toString();
            emitter.send(SseEmitter.event().data(
                    "{\"type\":\"start\",\"messageId\":\"" + msgId + "\"}", MediaType.APPLICATION_JSON));
            emitter.send(SseEmitter.event().data(
                    "{\"type\":\"chunk\",\"content\":\"" + escapeJson(fallbackText(message)) + "\"}",
                    MediaType.APPLICATION_JSON));
            emitter.send(SseEmitter.event().data(
                    "{\"type\":\"end\",\"messageId\":\"" + msgId + "\"}", MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private String fallbackText(String message) {
        if (message.contains("分析") || message.contains("统计") || message.contains("数据"))
            return "AI 服务暂时不可用。请前往「身体数据」页面查看训练数据和体重趋势。";
        if (message.contains("推荐") || message.contains("计划") || message.contains("建议"))
            return "AI 服务暂时不可用。请前往「训练记录」页面参考动作库自行制定计划。";
        return "AI 教练正在维护升级，请稍后再试。";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
