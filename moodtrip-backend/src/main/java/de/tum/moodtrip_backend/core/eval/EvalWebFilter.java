package de.tum.moodtrip_backend.core.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-100)
public class EvalWebFilter implements WebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvalWebFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        EvalRun run = new EvalRun();

        // ✅ compatible across Spring versions
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : "UNKNOWN";
        run.setHttp(method, path);

        long t0 = System.nanoTime();

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(EvalRun.CTX_KEY, run))
                .doFinally(signal -> {
                    long totalMs = (System.nanoTime() - t0) / 1_000_000;
                    run.finish(totalMs);
                    LOGGER.info("EVAL_RUN_JSON {}", run.toJsonLine(objectMapper));
                });
    }
}
