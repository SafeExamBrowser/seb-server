package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Types;
import java.util.Collection;

import javax.sql.DataSource;

import ch.ethz.seb.sebserver.gbl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.SqlBinaryValue;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;


public class JdbcTokenStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcTokenStore.class);

	private final String insertAccessTokenSql = "insert into oauth_access_token (token_id, token, authentication_id, user_name, client_id, authentication, refresh_token) values (?, ?, ?, ?, ?, ?, ?)";;
    private final String selectAllAccessTokens = "select token_id, token from oauth_access_token";
    private final String selectAllRefreshTokens = "select token_id, token from oauth_refresh_token";
    private final String selectAccessTokenAuthenticationSql = "select token_id, authentication from oauth_access_token where token_id = ?";
    private final String selectAuthenticationByIdSql = "select authentication from oauth_access_token where authentication_id = ?";;
	private final String deleteAccessTokenSql = "delete from oauth_access_token where token_id = ?";
	private final String insertRefreshTokenSql = "insert into oauth_refresh_token (token_id, token, authentication) values (?, ?, ?)";
	private final String selectRefreshTokenAuthenticationSql = "select token_id, authentication from oauth_refresh_token where token_id = ?";
	private final String deleteRefreshTokenSql = "delete from oauth_refresh_token where token_id = ?";

	private final JdbcTemplate jdbcTemplate;

	public JdbcTokenStore(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource required");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void storeAccessToken(OAuth2AccessToken token, OAuth2Authorization authorization) {
		String refreshToken = null;
		if (authorization.getRefreshToken() != null) {
			refreshToken = authorization.getRefreshToken().getToken().getTokenValue();
		}

        int update = jdbcTemplate.update(insertAccessTokenSql, new Object[]{extractTokenKey(token.getTokenValue()),
                new SqlBinaryValue(serializeAccessToken(token)), authorization.getId(),
                authorization.getPrincipalName(),
                authorization.getRegisteredClientId(),
                new SqlBinaryValue(serializeAuthorization(authorization)), extractTokenKey(refreshToken)}, new int[]{
                Types.VARCHAR, Types.BLOB, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BLOB, Types.VARCHAR});

        if (log.isDebugEnabled()) {
            log.debug("Stored new access token for: {}", authorization);
        }
    }

	public void removeAccessToken(OAuth2AccessToken token) {
		removeAccessToken(token.getTokenValue());
	}

	public void removeAccessToken(String tokenValue) {
        log.debug("Remove access token: {}", tokenValue);
		jdbcTemplate.update(deleteAccessTokenSql, extractTokenKey(tokenValue));
	}

    public void deleteAccessTokenById(final String tokenId) {
        jdbcTemplate.update(deleteAccessTokenSql, tokenId);
    }

    public void removeRefreshToken(OAuth2RefreshToken token) {
        removeRefreshToken(token.getTokenValue());
    }

    public void removeRefreshToken(String token) {
        log.debug("Remove refresh token: {}", token);
        jdbcTemplate.update(deleteRefreshTokenSql, extractTokenKey(token));
    }

    public void deleteRefreshTokenById(String tokenId) {
        jdbcTemplate.update(deleteRefreshTokenSql, tokenId);
    }

    public OAuth2Authorization tryReadAuthorizationById(String id) {
        try {
            return jdbcTemplate.queryForObject(
                    selectAuthenticationByIdSql,
                    (rs, rowNum) -> deserializeAuthorization(rs.getBytes(1)), id);
        } catch (Exception e) {
            return null;
        }
    }

	public OAuth2Authorization readAuthorization(String token) {
        OAuth2Authorization authentication = null;

		try {

			authentication = jdbcTemplate.queryForObject(
                    selectAccessTokenAuthenticationSql,
                    (rs, rowNum) -> deserializeAuthorization(rs.getBytes(2)), extractTokenKey(token));

		} catch (EmptyResultDataAccessException e) {
			if (log.isInfoEnabled()) {
				log.info("Failed to find access token: {}", token);
			}
		} catch (IllegalArgumentException e) {
			log.warn("Failed to deserialize authentication", e);
			removeAccessToken(token);
		}

		return authentication;
	}

	public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authorization authorization) {

        log.info("Store refresh token: {} for auth: {}", refreshToken.getTokenValue(), authorization.getId());

        int update = jdbcTemplate.update(insertRefreshTokenSql, new Object[]{extractTokenKey(refreshToken.getTokenValue()),
                new SqlBinaryValue(serializeRefreshToken(refreshToken)),
                new SqlBinaryValue(serializeAuthorization(authorization))}, new int[]{Types.VARCHAR, Types.BLOB,
                Types.BLOB});

        if (update == 0) {
            log.warn("Failed to store refreshToken: {}", refreshToken);
        }
    }

	public OAuth2Authorization readAuthorizationForRefreshToken(String value) {
        OAuth2Authorization authentication = null;

		try {
			authentication = jdbcTemplate
                    .queryForObject(selectRefreshTokenAuthenticationSql,
                    (rs, rowNum) -> deserializeAuthorization(rs.getBytes(2)), extractTokenKey(value));
		}
		catch (EmptyResultDataAccessException e) {
			if (log.isInfoEnabled()) {
				log.info("Failed to find by refresh token token: {}", value);
			}
		}
		catch (IllegalArgumentException e) {
			log.warn("Failed to deserialize authorization token", e);
			removeRefreshToken(value);
		}

		return authentication;
	}

    public Collection<Pair<String, byte[]>> getAllAccessTokens() {
        return jdbcTemplate.query(
                selectAllAccessTokens,
                (rs, rowNum) -> new Pair<>(rs.getString(1), rs.getBytes(2)));
    }

    public Collection<Pair<String, byte[]>> getAllRefreshTokens() {
        return jdbcTemplate.query(
                selectAllRefreshTokens,
                (rs, rowNum) ->  new Pair<>(rs.getString(1), rs.getBytes(2)));
    }

	protected String extractTokenKey(String value) {
		if (value == null) {
			return null;
		}
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
		}

        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return String.format("%032x", new BigInteger(1, bytes));
    }

	byte[] serializeAccessToken(OAuth2AccessToken token) {
		return SerializationUtils.serialize(token);
	}

	byte[] serializeRefreshToken(OAuth2RefreshToken token) {
		return SerializationUtils.serialize(token);
	}

	byte[] serializeAuthorization(OAuth2Authorization authorization) {
		return SerializationUtils.serialize(authorization);
	}

	OAuth2AccessToken deserializeAccessToken(byte[] token) {
		return (OAuth2AccessToken) SerializationUtils.deserialize(token);
	}

	OAuth2RefreshToken deserializeRefreshToken(byte[] token) {
		return (OAuth2RefreshToken) SerializationUtils.deserialize(token);
	}

	OAuth2Authorization deserializeAuthorization(byte[] authentication) {
		return (OAuth2Authorization) SerializationUtils.deserialize(authentication);
	}

}
