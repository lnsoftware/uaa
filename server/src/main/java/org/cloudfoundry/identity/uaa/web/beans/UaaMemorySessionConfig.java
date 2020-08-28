package org.cloudfoundry.identity.uaa.web.beans;

import org.cloudfoundry.identity.uaa.util.SessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@Configuration
@Conditional(UaaMemorySessionConfig.MemoryConfigured.class)
@EnableSpringHttpSession
@EnableScheduling
public class UaaMemorySessionConfig extends UaaSessionConfig {

    private final static Logger logger = LoggerFactory.getLogger(UaaMemorySessionConfig.class);

    public static class MemoryConfigured implements Condition {
        @Override
        public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            String sessionStore = getSessionStore(context.getEnvironment());
            validateSessionStore(sessionStore);
            return MEMORY_SESSION_STORE_TYPE.equals(sessionStore);
        }
    }

    @Bean(destroyMethod = "destroy")
    public MapSessionRepository sessionRepository(
            final @Value("${servlet.idle-timeout:1800}") int idleTimeout,
            @Autowired PurgeableSessionMap purgeableSessionMap
    ) {
        MapSessionRepository sessionRepository = new MapSessionRepository(purgeableSessionMap) {
            @Override
            public void save(MapSession session) {
                super.save(session);
                if (session.getAttribute(SessionUtils.SPRING_SECURITY_CONTEXT) != null) {
                    logger.info("********we have a spring sec context");
                }
            }

            public void destroy() {
                logger.info("****************** destroyed");
            }
        };
        sessionRepository.setDefaultMaxInactiveInterval(idleTimeout);
        return sessionRepository;
    }

    @Autowired
    void log() {
        logger.info("Using memory session configuration");
    }
}