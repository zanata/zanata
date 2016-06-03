package org.zanata.limits;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.deltaspike.jpa.api.transaction.Transactional;
import org.zanata.ApplicationConfiguration;
import org.zanata.async.Async;
import org.zanata.events.ConfigurationChanged;
import org.zanata.util.Introspectable;
import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zanata.util.ServiceLocator;

import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Named("rateLimitManager")
@javax.enterprise.context.ApplicationScoped

@Slf4j
public class RateLimitManager implements Introspectable {

    public static final String INTROSPECTABLE_FIELD_RATE_LIMITERS =
            "RateLimiters";
    private final Cache<RateLimiterToken, RestCallLimiter> activeCallers = CacheBuilder
            .newBuilder().maximumSize(100).build();

    @Getter(AccessLevel.PROTECTED)
    @VisibleForTesting
    private int maxConcurrent;
    @Getter(AccessLevel.PROTECTED)
    @VisibleForTesting
    private int maxActive;

    @Inject
    private ApplicationConfiguration appConfig;

    public static RateLimitManager getInstance() {
        return ServiceLocator.instance().getInstance(RateLimitManager.class);
    }

    @PostConstruct
    public void loadConfig() {
        readRateLimitState();
    }

    private void readRateLimitState() {
        maxConcurrent = appConfig.getMaxConcurrentRequestsPerApiKey();
        maxActive = appConfig.getMaxActiveRequestsPerApiKey();
    }

    @Async
    @Transactional
    public void configurationChanged(
            @Observes(during = TransactionPhase.AFTER_SUCCESS)
            ConfigurationChanged payload) {
        int oldConcurrent = maxConcurrent;
        int oldActive = maxActive;
        boolean changed = false;
        readRateLimitState();
        if (oldConcurrent != maxConcurrent) {
            log.info(
                    "application configuration changed. Old concurrent: {}, New concurrent: {}",
                    oldConcurrent, maxConcurrent);
            changed = true;
        }
        if (oldActive != maxActive) {
            log.info(
                    "application configuration changed. Old active: {}, New active: {}",
                    oldActive, maxActive);
            changed = true;
        }
        if (changed) {
            for (RestCallLimiter restCallLimiter : activeCallers.asMap()
                    .values()) {
                restCallLimiter.changeConfig(maxConcurrent, maxActive);
            }
        }
    }

    // below are all monitoring stuff
    @Override
    public String getIntrospectableId() {
        return getClass().getCanonicalName();
    }

    @Override
    public Collection<String> getIntrospectableFieldNames() {
        return Lists.newArrayList(INTROSPECTABLE_FIELD_RATE_LIMITERS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getFieldValueAsString(String fieldName) {
        if (INTROSPECTABLE_FIELD_RATE_LIMITERS.equals(fieldName)) {
            return Iterables.toString(peekCurrentBuckets());
        }
        throw new IllegalArgumentException("unknown field:" + fieldName);
    }

    private Iterable<String> peekCurrentBuckets() {
        ConcurrentMap<RateLimiterToken, RestCallLimiter> map = activeCallers.asMap();
        return Iterables.transform(map.entrySet(),
                new Function<Map.Entry<RateLimiterToken, RestCallLimiter>, String>() {

                    @Override
                    public String
                            apply(Map.Entry<RateLimiterToken, RestCallLimiter> input) {

                        RestCallLimiter rateLimiter = input.getValue();
                        return input.getKey() + ":" + rateLimiter;
                    }
                });
    }

    /**
     * @param key - {@link RateLimiterToken.TYPE )
     */
    public RestCallLimiter getLimiter(final RateLimiterToken key) {

        if (getMaxConcurrent() == 0 && getMaxActive() == 0) {
            if (activeCallers.size() > 0) {
                activeCallers.invalidateAll();
            }
            // short circuit if we don't want limiting
            return NoLimitLimiter.INSTANCE;
        }
        try {
            return activeCallers.get(key, new Callable<RestCallLimiter>() {
                @Override
                public RestCallLimiter call() throws Exception {
                    log.debug("creating rate limiter for key: {}", key);
                    return new RestCallLimiter(getMaxConcurrent(),
                            getMaxActive());
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class NoLimitLimiter extends RestCallLimiter {
        private static final NoLimitLimiter INSTANCE = new NoLimitLimiter();

        private NoLimitLimiter() {
            super(0, 0);
        }

    }

}
