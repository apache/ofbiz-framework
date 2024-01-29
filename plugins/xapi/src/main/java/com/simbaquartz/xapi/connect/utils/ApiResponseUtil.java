/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@fidelissd.com>,  May, 2017                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xapi.connect.utils;

import com.simbaquartz.xapi.connect.models.errors.Error;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;

/** Created by mande on 5/20/2017. */
public class ApiResponseUtil {
  private static final String module = ApiResponseUtil.class.getName();

  public static Response notImplemented() {
    return prepareDefaultResponse(Response.Status.NOT_IMPLEMENTED, null);
  }

  public static Response prepareDefaultResponse(Response.Status statusCode) {
    return prepareDefaultResponse(statusCode, null);
  }

  public static Response notFoundResponse(String message) {
    return prepareDefaultResponse(Response.Status.NOT_FOUND, message);
  }

  public static Response badRequestResponse(String message) {
    return prepareDefaultResponse(Response.Status.BAD_REQUEST, message);
  }

  public static Response badRequestResponse(String message, String field) {
    return prepareDefaultResponse(Response.Status.BAD_REQUEST, message, field);
  }

  public static Response serverErrorResponse() {
    return prepareDefaultResponse(
        Response.Status.INTERNAL_SERVER_ERROR,
        "An error occurred while trying to process your request, support team has been notified, please try again or contact support.");
  }

  public static Response serverErrorResponse(String message) {
    return prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
  }

  public static Response prepareDefaultResponse(Response.Status statusCode, String message) {
    return prepareDefaultResponse(statusCode, message, null, null);
  }

  public static Response prepareDefaultResponse(
      Response.Status statusCode, String message, String field) {
    return prepareDefaultResponse(statusCode, message, field, null);
  }

  public static Response prepareDefaultResponse(
      Response.Status statusCode, String message, String field, Error.CodeEnum errorCode) {
    Response response = Response.status(Response.Status.OK).build();

    switch (statusCode) {
      case UNAUTHORIZED:
        if (UtilValidate.isEmpty(message)) {
          message = "A valid authorization header must be provided. Please check api docs.";
        }

        if (UtilValidate.isEmpty(errorCode)) {
          errorCode = Error.CodeEnum.UNAUTHORIZED;
        }

        Error notAuthroizedError =
            new Error(errorCode, Error.CategoryEnum.AUTHENTICATION_ERROR, message, field);
        response =
            Response.status(statusCode)
                .header("WWW-Authenticate", "Authorization Header must be provided.")
                .entity(notAuthroizedError)
                .build();
        break;
      case INTERNAL_SERVER_ERROR:
        if (UtilValidate.isEmpty(message)) {
          message =
              "An error occurred while trying to process your request. Please try again later, if you continue receiving this error, please contact support.";
        }
        Error serverError =
            new Error(
                Error.CodeEnum.INTERNAL_SERVER_ERROR, Error.CategoryEnum.API_ERROR, message, field);
        response = Response.status(statusCode).entity(serverError).build();
        break;
      case NOT_FOUND:
        if (UtilValidate.isEmpty(message)) {
          message =
              "Not found. The resource you are looking for couldn't be found, please validate your resource identifier and try again.";
        }

        Error notFoundError =
            new Error(
                Error.CodeEnum.NOT_FOUND, Error.CategoryEnum.INVALID_REQUEST_ERROR, message, field);
        response = Response.status(statusCode).entity(notFoundError).build();
        break;
      case FORBIDDEN:
        if (UtilValidate.isEmpty(message)) {
          message =
              "Access Denied. You do not have access to the requested resource. Please validate your resource identifier and try again.";
        }

        if (UtilValidate.isEmpty(errorCode)) {
          errorCode = Error.CodeEnum.FORBIDDEN;
        }

        Error forbiddenError =
            new Error(errorCode, Error.CategoryEnum.INVALID_REQUEST_ERROR, message, field);
        response = Response.status(statusCode).entity(forbiddenError).build();
        break;
      case NOT_IMPLEMENTED:
        if (UtilValidate.isEmpty(message)) {
          message =
              "Not implemented, please request a feature if you'd like to see this implemented.";
        }

        if (UtilValidate.isEmpty(errorCode)) {
          errorCode = Error.CodeEnum.NOT_IMPLEMENTED;
        }

        Error notImplementedError =
            new Error(errorCode, Error.CategoryEnum.INVALID_REQUEST_ERROR, message, field);
        response = Response.status(statusCode).entity(notImplementedError).build();
        break;
      case BAD_REQUEST:
        if (UtilValidate.isEmpty(message)) {
          message = "Invalid request, please check your input and try again.";
        }

        if (UtilValidate.isEmpty(errorCode)) {
          errorCode = Error.CodeEnum.BAD_REQUEST;
        }

        Error badRequestError =
            new Error(errorCode, Error.CategoryEnum.INVALID_REQUEST_ERROR, message, field);
        response = Response.status(statusCode).entity(badRequestError).build();
        break;
      default:
        break;
    }

    return response;
  }

  public static Response prepareStandardResponse(Response.Status statusCode, List<Error> errors) {
    Response response;

    Map standardResponse = FastMap.newInstance();

    standardResponse.put("errors", errors);
    switch (statusCode) {
      case UNAUTHORIZED:
        response =
            Response.status(statusCode)
                .header("WWW-Authenticate", "Authorization Header must be provided.")
                .entity(standardResponse)
                .build();
        break;
      default:
        response = Response.status(statusCode).entity(standardResponse).build();
        break;
    }

    return response;
  }

  /**
   * Prepares 401 UNAUTHORIZED  response and returns with the input message.
   *
   * @param message Error message explaining the user what went wrong.
   * @return
   */
  public static Response prepareUnauthorizedResponse(String message) {
    return Response.status(Response.Status.UNAUTHORIZED).entity(message).build();
  }

  /**
   * Prepares 200 OK response and serializes input object as JSON to the response stream.
   *
   * @param objectToWriteAsJson Object that needs to be serialized as JSON to the response.
   * @return
   */
  public static Response prepareOkResponse(Object objectToWriteAsJson) {
    return Response.status(Response.Status.OK).entity(objectToWriteAsJson).build();
  }

  /**
   * Prepares 201 Created response and serializes input object as JSON to the response stream.
   *
   * @param objectToWriteAsJson Object that needs to be serialized as JSON to the response.
   * @return
   */
  public static Response prepareResourceCreatedResponse(Object objectToWriteAsJson) {
    return Response.status(Response.Status.CREATED).entity(objectToWriteAsJson).build();
  }

  public static Response prepareErrorResponse(
      Response.Status statusCode, Object objectToWriteAsJson) {
    return Response.status(statusCode).entity(objectToWriteAsJson).build();
  }

  /**
   * Use this to set up a redirect, example a file download request being redirected to a CDN/cloud
   * storage url.
   *
   * @param urlToRedirectTo
   * @return
   * @throws URISyntaxException
   */
  public static Response prepareRedirectResponse(String urlToRedirectTo) {
    ResponseBuilder responseBuilder;
    try {
      responseBuilder = Response.status(Status.SEE_OTHER).location(new URI(urlToRedirectTo));
    } catch (URISyntaxException e) {
      Debug.logError(e, module);
      return serverErrorResponse(
          "Unable to create redirect response, please see logs for more details.");
    }

    return responseBuilder.build();
  }
}
