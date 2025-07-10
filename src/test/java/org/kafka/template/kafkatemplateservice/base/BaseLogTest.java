// BaseLogTest.java
package org.kafka.template.kafkatemplateservice.base;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseLogTest {

    protected ListAppender<ILoggingEvent> listAppender;
    protected Logger logger;

    protected void setUpLogger(Class<?> clazz) {
        logger = (Logger) LoggerFactory.getLogger(clazz);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    protected void tearDownLogger() {
        if (logger != null && listAppender != null) {
            logger.detachAppender(listAppender);
        }
    }

    protected void assertLog(Level level, String message) {
        List<ILoggingEvent> logsList = listAppender.list;
        boolean found = logsList.stream()
                .anyMatch(event -> event.getLevel().equals(level) &&
                        event.getFormattedMessage().contains(message));
        assertTrue(found, "Expected log with level " + level + " and message containing: " + message);
    }
}
