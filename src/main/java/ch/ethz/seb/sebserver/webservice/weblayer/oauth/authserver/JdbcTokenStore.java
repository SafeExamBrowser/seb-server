package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlBinaryValue;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;


public class JdbcTokenStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcTokenStore.class);

	private final String insertAccessTokenSql = "insert into oauth_access_token (token_id, token, authentication_id, user_name, client_id, authentication, refresh_token) values (?, ?, ?, ?, ?, ?, ?)";;
	//private final String selectAccessTokenSql = "select token_id, token from oauth_access_token where token_id = ?";

    private final String selectAllAccessTokens = "select token_id, token from oauth_access_token";
    private final String selectAllRefreshTokens = "select token_id, token from oauth_refresh_token";
    private final String selectAccessTokenAuthenticationSql = "select token_id, authentication from oauth_access_token where token_id = ?";
    private final String selectAuthenticationByIdSql = "select authentication from oauth_access_token where authentication_id = ?";;
	private final String deleteAccessTokenSql = "delete from oauth_access_token where token_id = ?";
	private final String insertRefreshTokenSql = "insert into oauth_refresh_token (token_id, token, authentication) values (?, ?, ?)";
	private final String selectRefreshTokenSql = "delete from oauth_access_token where refresh_token = ?";
	private final String selectRefreshTokenAuthenticationSql = "select token_id, authentication from oauth_refresh_token where token_id = ?";
	private final String deleteRefreshTokenSql = "delete from oauth_refresh_token where token_id = ?";
	private final String deleteAccessTokenFromRefreshTokenSql = "delete from oauth_access_token where refresh_token = ?";

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

//	public OAuth2AccessToken readAccessToken(String tokenValue) {
//		OAuth2AccessToken accessToken = null;
//
//		try {
//			accessToken = jdbcTemplate.queryForObject(selectAccessTokenSql, new RowMapper<OAuth2AccessToken>() {
//				public OAuth2AccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
//					return deserializeAccessToken(rs.getBytes(2));
//				}
//			}, extractTokenKey(tokenValue));
//		}
//		catch (EmptyResultDataAccessException e) {
//			if (log.isInfoEnabled()) {
//				log.info("Failed to find access token");
//			}
//		}
//		catch (IllegalArgumentException e) {
//			log.warn("Failed to deserialize access token", e);
//			removeAccessToken(tokenValue);
//		}
//
//		return accessToken;
//	}

	public void removeAccessToken(OAuth2AccessToken token) {
		removeAccessToken(token.getTokenValue());
	}

	public void removeAccessToken(String tokenValue) {
		jdbcTemplate.update(deleteAccessTokenSql, extractTokenKey(tokenValue));
	}

//	public OAuth2Authorization readAuthorization(OAuth2AccessToken token) {
//		return readAuthorization(token.getTokenValue());
//	}

    public OAuth2Authorization tryReadAuthorizationById(String id) {
        try {
            return jdbcTemplate.queryForObject(
                    selectAuthenticationByIdSql,
                    (rs, rowNum) -> deserializeAuthorization(rs.getBytes(1)), id);
        } catch (Exception e) {
            return null;
        }
    }

    public OAuth2Authorization readAuthorizationById(String id) {
        OAuth2Authorization authentication = null;

        try {

            authentication = jdbcTemplate.queryForObject(
                    selectAuthenticationByIdSql,
                    (rs, rowNum) -> deserializeAuthorization(rs.getBytes(1)), id);

        } catch (EmptyResultDataAccessException e) {
            if (log.isInfoEnabled()) {
                log.info("Failed to find Authorization: {}", id);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to deserialize Authorization", e);
        }

        return authentication;
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

	public OAuth2RefreshToken readRefreshToken(String token) {
		OAuth2RefreshToken refreshToken = null;

		try {
			refreshToken = jdbcTemplate.queryForObject(selectRefreshTokenSql, new RowMapper<OAuth2RefreshToken>() {
				public OAuth2RefreshToken mapRow(ResultSet rs, int rowNum) throws SQLException {
					return deserializeRefreshToken(rs.getBytes(2));
				}
			}, extractTokenKey(token));
		}
		catch (EmptyResultDataAccessException e) {
			if (log.isInfoEnabled()) {
				log.info("Failed to find refresh token: {}", token);
			}
		}
		catch (IllegalArgumentException e) {
			log.warn("Failed to deserialize refresh token: {} cause: {}", token, e.getMessage());
			removeRefreshToken(token);
		}

		return refreshToken;
	}

	public void removeRefreshToken(OAuth2RefreshToken token) {
		removeRefreshToken(token.getTokenValue());
	}

	public void removeRefreshToken(String token) {

        log.info("Remove refresh token: {}", token);

		jdbcTemplate.update(deleteRefreshTokenSql, extractTokenKey(token));
	}

	public OAuth2Authorization readAAuthorizationForRefreshToken(OAuth2RefreshToken token) {
		return readAuthorizationForRefreshToken(token.getTokenValue());
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

	public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
		removeAccessTokenUsingRefreshToken(refreshToken.getTokenValue());
	}

	public void removeAccessTokenUsingRefreshToken(final String refreshToken) {
        log.info("Remove access token by refresh token: {} ", refreshToken);

        int update = jdbcTemplate.update(
                deleteAccessTokenFromRefreshTokenSql,
                new Object[]{extractTokenKey(refreshToken)},
                new int[]{Types.VARCHAR});

        if (update == 0) {
            log.warn("failed to delete access token for refresh token: {}: ", refreshToken);
        }
    }

    public Collection<OAuth2AccessToken> getAllAccessTokens() {
        List<OAuth2AccessToken> result = jdbcTemplate.query(
                selectAllAccessTokens,
                (rs, rowNum) -> deserializeAccessToken(rs.getBytes(2)));
        return result;
    }

    public Collection<OAuth2RefreshToken> getAllRefreshTokens() {
        return jdbcTemplate.query(
                selectAllRefreshTokens,
                (rs, rowNum) -> deserializeRefreshToken(rs.getBytes(2)));
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

	private final class SafeAccessTokenRowMapper implements RowMapper<OAuth2AccessToken> {
		public OAuth2AccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				return deserializeAccessToken(rs.getBytes(2));
			}
			catch (IllegalArgumentException e) {
				String token = rs.getString(1);
				jdbcTemplate.update(deleteAccessTokenSql, token);
				return null;
			}
		}
	}

	protected byte[] serializeAccessToken(OAuth2AccessToken token) {
		return SerializationUtils.serialize(token);
	}

	protected byte[] serializeRefreshToken(OAuth2RefreshToken token) {
		return SerializationUtils.serialize(token);
	}

	protected byte[] serializeAuthorization(OAuth2Authorization authorization) {
		return SerializationUtils.serialize(authorization);
	}

	protected OAuth2AccessToken deserializeAccessToken(byte[] token) {
		return (OAuth2AccessToken) SerializationUtils.deserialize(token);
	}

	protected OAuth2RefreshToken deserializeRefreshToken(byte[] token) {
		return (OAuth2RefreshToken) SerializationUtils.deserialize(token);
	}

	protected OAuth2Authorization deserializeAuthorization(byte[] authentication) {
		return (OAuth2Authorization) SerializationUtils.deserialize(authentication);
	}

}
