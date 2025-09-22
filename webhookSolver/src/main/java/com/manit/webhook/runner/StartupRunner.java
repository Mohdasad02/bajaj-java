package com.hiring.webhook.runner;

import com.hiring.webhook.config.AppProperties;
import com.hiring.webhook.model.Solution;
import com.hiring.webhook.repo.SolutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StartupRunner implements ApplicationRunner {
    private final Logger log = LoggerFactory.getLogger(StartupRunner.class);
    private final WebClient webClient;
    private final AppProperties props;
    private final SolutionRepository repo;

    public StartupRunner(WebClient webClient, AppProperties props, SolutionRepository repo) {
        this.webClient = webClient;
        this.props = props;
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("StartupRunner started - preparing to call generateWebhook...");
        Map<String,String> body = new HashMap<>();
        body.put("name", props.getName());
        body.put("regNo", props.getRegNo());
        body.put("email", props.getEmail());

        // 1) call generateWebhook
        GenerateResponse gen = webClient.post()
                .uri(props.getGenerateWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GenerateResponse.class)
                .block();

        if (gen == null || gen.getWebhook() == null || gen.getAccessToken() == null) {
            log.error("generateWebhook returned empty response, aborting.");
            return;
        }
        log.info("Received webhook={} accessTokenPresent={}", gen.getWebhook(), gen.getAccessToken() != null);

        // 2) determine odd/even based on last two numeric digits of regNo
        boolean isOdd = determineOddFromRegNo(props.getRegNo());
        log.info("RegNo '{}' -> isOdd = {}", props.getRegNo(), isOdd);

        String finalQuery;
        if (isOdd) finalQuery = props.getFinalQueryOdd();
        else finalQuery = props.getFinalQueryEven();

        if (finalQuery == null || finalQuery.trim().isEmpty() || finalQuery.contains("PUT YOUR")) {
            log.error("Final query for selected parity is not set. Please set app.finalQueryOdd or app.finalQueryEven in application.yml");
            return;
        }

        // 3) Save to DB
        Solution sol = new Solution(props.getRegNo(), gen.getWebhook(), finalQuery, Instant.now());
        repo.save(sol);
        log.info("Saved solution to DB id={}", sol.getId());

        // 4) send finalQuery to returned webhook
        Map<String,String> submitBody = Map.of("finalQuery", finalQuery);
        String accessToken = gen.getAccessToken();

        try {
            SubmitResponse resp = webClient.post()
                    .uri(props.getSubmitWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .bodyValue(submitBody)
                    .retrieve()
                    .bodyToMono(SubmitResponse.class)
                    .block();

            log.info("Submission response: {}", resp == null ? "null" : resp);
        } catch (Exception ex) {
            log.error("Error submitting final query to webhook", ex);
        }

        log.info("StartupRunner finished.");
    }

    private boolean determineOddFromRegNo(String regNo) {
        if (regNo == null) return true;
        // extract digits, take last two digits if exist
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(regNo);
        String allDigits = "";
        while (m.find()) allDigits += m.group();
        if (allDigits.length() == 0) return true;
        String lastTwo = allDigits.length() >= 2 ?
                allDigits.substring(allDigits.length() - 2) : allDigits;
        try {
            int v = Integer.parseInt(lastTwo);
            return (v % 2) == 1;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    // simple DTOs for responses:
    public static class GenerateResponse {
        private String webhook;
        private String accessToken;
        public GenerateResponse() {}
        public String getWebhook() { return webhook; }
        public void setWebhook(String webhook) { this.webhook = webhook; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }

    public static class SubmitResponse {
        // the webhook's response structure is unknown; we keep it generic
        private Map<String,Object> raw;
        public SubmitResponse(){}
        public Map<String,Object> getRaw(){ return raw;}
        public void setRaw(Map<String,Object> raw){ this.raw = raw; }
        @Override public String toString(){ return raw == null ? "null" : raw.toString(); }
    }
}
