package anubis.netsupport_school.backend.api.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import tools.jackson.databind.ObjectMapper;


@Configuration @EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
    private final MessageRouter router;
    private final ObjectMapper mapper;

    public WebSocketConfiguration(MessageRouter router, ObjectMapper mapper) {
        this.router = router;
        this.mapper = mapper;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler(), "/websocket").setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler handler(){
        return new WebsocketConnectionHandler(mapper, router);
    }
}
