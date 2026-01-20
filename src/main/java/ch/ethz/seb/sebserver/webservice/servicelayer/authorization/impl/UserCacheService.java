package ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl;

import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class UserCacheService {

    private static final Logger log = LoggerFactory.getLogger(UserCacheService.class);

    public static final String SERVER_USER_CACHE = "SERVER_USER_CACHE";

    private final UserDAO userDAO;

    public UserCacheService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Cacheable(
            cacheNames = SERVER_USER_CACHE,
            key = "#name",
            unless = "#result == null")
    public SEBServerUser serverUserByName(String name) {
        System.out.println("*************** load server user: " + name);
        return userDAO.sebServerUserByUsername(name)
                .onError(error -> log.error("Failed to get SEBServerUser by name: {} cause: {}", name, error.getMessage()))
                .getOr(null);
    }

    @CacheEvict(
            cacheNames = SERVER_USER_CACHE,
            key = "#name")
    public void evictServerUserByName(String name) {
        if (log.isTraceEnabled()) {
            log.trace("Eviction of ServerUser from cache, name: {}", name);
        }
    }
}
