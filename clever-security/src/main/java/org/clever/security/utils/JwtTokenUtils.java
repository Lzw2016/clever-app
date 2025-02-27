package org.clever.security.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.DateUtils;
import org.clever.core.codec.EncodeDecodeUtils;
import org.clever.core.id.IDCreateUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.tuples.TupleTwo;
import org.clever.security.SecurityDataSource;
import org.clever.security.config.TokenConfig;
import org.clever.security.exception.LoginInnerException;
import org.clever.security.exception.ParserJwtTokenException;
import org.clever.security.login.AddJwtTokenExtData;
import org.clever.security.model.UserInfo;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.clever.security.impl.model.query.QSysJwtToken.sysJwtToken;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/02 21:32 <br/>
 */
@Slf4j
public class JwtTokenUtils {
    public static String SECRET_KEY_SUFFIX = "0123456789012345678901234567890123456789012345678901234567890123";
    private static final Class<? extends Claims> CLAIMS_CLASS = Jwts.claims().getClass();

    /**
     * 创建JWT-Token
     *
     * @param request                请求对象
     * @param tokenConfig            Token配置
     * @param userInfo               用户信息
     * @param addJwtTokenExtDataList 用户扩展JWT-Token实现
     */
    public static TupleTwo<String, Claims> createJwtToken(HttpServletRequest request, TokenConfig tokenConfig, UserInfo userInfo, List<AddJwtTokenExtData> addJwtTokenExtDataList) {
        //创建Token令牌 - iss（签发者）, aud（接收方）, sub（面向的用户）,exp（过期时间戳）, iat（签发时间）, jti（JWT ID）
        Claims claims = Jwts.claims();
        claims.setIssuer(tokenConfig.getIssuer());
        claims.setAudience(tokenConfig.getAudience());
        claims.setSubject(userInfo.strUserId());
        // 加入自定义信息
        if (addJwtTokenExtDataList != null && !addJwtTokenExtDataList.isEmpty()) {
            // addJwtTokenExtDataList = ListSortUtils.sort(addJwtTokenExtDataList);
            Map<String, Object> extData = new HashMap<>();
            for (AddJwtTokenExtData addJwtTokenExtData : addJwtTokenExtDataList) {
                extData = addJwtTokenExtData.addExtData(request, tokenConfig, userInfo, extData);
            }
            if (extData != null) {
                extData.forEach((key, value) -> {
                    if (claims.containsKey(key)) {
                        throw new LoginInnerException("设置Token扩展信息失败，key=[" + key + "]重复(不能覆盖内置数据)");
                    }
                    claims.put(key, value);
                });
            }
        }
        String jwtToken = createJwtToken(tokenConfig, claims);
        return TupleTwo.creat(jwtToken, claims);
    }

    /**
     * 创建JWT-Token(会更新claims对象属性)
     *
     * @param tokenConfig Token配置
     * @param claims      Token内容Claims(会更新属性值)
     */
    public static String createJwtToken(TokenConfig tokenConfig, Claims claims) {
        // 获取当前时间戳
        long now = System.currentTimeMillis();
        // Token过期时间
        Date expiration = new Date(now + tokenConfig.getTokenValidity().toMillis());
        // 优化过期时间
        if (StringUtils.isNotBlank(tokenConfig.getHoursInDay())) {
            String date = DateUtils.formatToString(expiration, DateUtils.yyyy_MM_dd);
            try {
                expiration = DateUtils.parseDate(date + " " + StringUtils.trim(tokenConfig.getHoursInDay()), DateUtils.yyyy_MM_dd_HH_mm_ss);
            } catch (Throwable e) {
                log.warn("### token.hoursInDay配置错误", e);
            }
            if (expiration.getTime() <= now) {
                expiration = DateUtils.addDays(expiration, 1);
            }
        }
        // 更新claims信息
        claims.setExpiration(expiration);
        claims.setIssuedAt(new Date());
        // claims.setId(String.valueOf(SnowFlake.SNOW_FLAKE.nextId()));
        claims.setId(String.valueOf(SecurityDataSource.getJdbc().nextId(sysJwtToken.getTableName())));
        // 签名私钥
        Key key = getHmacShaKey(tokenConfig.getSecretKey(), claims.getSubject());
        return Jwts.builder()
            .setClaims(claims)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * 创建刷新Token
     *
     * @param userId 用户id
     */
    public static String createRefreshToken(Long userId) {
        return userId + ":" + IDCreateUtils.shortUuid();
    }

    /**
     * 解析验证JWT-Token
     *
     * @param tokenConfig Token配置
     * @param jwtToken    JWT-Token字符串
     */
    public static Claims parserJwtToken(TokenConfig tokenConfig, String jwtToken) {
        Claims claims = readClaims(jwtToken);
        String userId = claims.getSubject();
        Key key = getHmacShaKey(tokenConfig.getSecretKey(), userId);
        try {
            // 通过密钥验证Token
            return Jwts.parserBuilder()
                // .requireIssuer(tokenConfig.getIssuer())
                // .requireAudience(tokenConfig.getAudience())
                // .requireSubject(uid)
                .setAllowedClockSkewSeconds(60)
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwtToken).getBody();
        } catch (SignatureException e) {
            // 签名异常
            throw new ParserJwtTokenException("Token签名异常", e);
        } catch (MalformedJwtException e) {
            // JWT格式错误
            throw new ParserJwtTokenException("Token格式错误", e);
        } catch (ExpiredJwtException e) {
            // JWT过期
            throw new ParserJwtTokenException("TokenJWT过期", e);
        } catch (UnsupportedJwtException e) {
            // 不支持该JWT
            throw new ParserJwtTokenException("不支持该Token", e);
        } catch (JwtException e) {
            throw new ParserJwtTokenException("Token验证失败", e);
        } catch (IllegalArgumentException e) {
            // 参数错误异常
            throw new ParserJwtTokenException("Token参数错误异常", e);
        }
    }

    /**
     * 从JWT-Token中读取Claims(不校验Token)
     */
    public static Claims readClaims(String jwtToken) {
        String[] strArray = jwtToken.split("\\.");
        if (strArray.length != 3) {
            throw new ParserJwtTokenException("Token格式不正确");
        }
        // 解析获得签名私钥
        String payload = strArray[1];
        payload = new String(EncodeDecodeUtils.decodeBase64(payload));
        return JacksonMapper.getInstance().fromJson(payload, CLAIMS_CLASS);
    }

    /**
     * 获取JWT-Token签名
     *
     * @param secretKey 签名配置
     * @param userId    用户UID
     */
    private static Key getHmacShaKey(String secretKey, String userId) {
        secretKey = secretKey + SECRET_KEY_SUFFIX;
        byte[] bytes = (StringUtils.left(userId, 8) + secretKey).getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
