/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written by Forrest Rae <forrest.rae@fidelissd.com>, December, 2017                     *
 *  *****************************************************************************************
 */

package com.simbaquartz.xcommon.util;

import io.jsonwebtoken.*;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ofbiz.base.util.UtilValidate;

import java.util.Date;
import java.util.Map;

public class JWTUtils {

    private static final String SECRET_KEY = "fsd_secret@123"; //TODO: Move to properties

    public static String generateJwt(Map data){
        return generateJwt(data, null);
    }

    public static String generateJwt(Map data, Date expiration){
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS512, SECRET_KEY);
        jwtBuilder.setClaims(data);

        if(UtilValidate.isEmpty(expiration)) {
            // Set to 24 hours by default
            expiration = DateUtils.addDays(new Date(),1);
        }
        jwtBuilder.setExpiration(expiration);
        return jwtBuilder.compact();
    }

    public static Map parseJwt(String jwtToken) throws InvalidTokenException {
        Map output = null;
        try {
            JwtParser jwtParser = Jwts.parser();
            jwtParser.setSigningKey(SECRET_KEY);

            Jwt jwt = jwtParser.parse(jwtToken);
            Object obj = jwt.getBody();
            output = (Map)obj;
        } catch (Exception ex) {
            throw new InvalidTokenException("Token Expired or is Invalid.");
        }

        return output;
    }
}

