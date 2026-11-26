package com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.GatewayCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Gateway;
import com.acme.coldtrace.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * Interface layer translator converting gateway command results to HTTP responses.
 *
 * @since 1.0
 */
public class ResponseEntityFromGatewayCommandResultAssembler {
    /**
     * Converts a gateway creation result into an HTTP response.
     *
     * @param result gateway command result
     * @param messageSource message source for localized failure details
     * @return 201 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromCreateResult(
            Result<Gateway, GatewayCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                gateway -> new ResponseEntity<>(
                        GatewayResourceFromEntityAssembler.toResourceFromEntity(gateway),
                        CREATED
                ),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts a gateway update result into an HTTP response.
     *
     * @param result gateway command result
     * @param messageSource message source for localized failure details
     * @return 200 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromUpdateResult(
            Result<Gateway, GatewayCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                gateway -> ResponseEntity.ok(GatewayResourceFromEntityAssembler.toResourceFromEntity(gateway)),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts a gateway deletion result into an HTTP response.
     *
     * @param result gateway deletion command result
     * @param messageSource message source for localized failure details
     * @return 204 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromDeleteResult(
            Result<?, GatewayCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    private static ResponseEntity<?> toFailureResponse(
            GatewayCommandFailure failure,
            MessageSource messageSource
    ) {
        var status = statusFromFailure(failure);
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(
                status,
                localizeMessage(messageSource, failure)
        ));
    }

    private static HttpStatus statusFromFailure(GatewayCommandFailure failure) {
        if (failure instanceof GatewayCommandFailure.DuplicateUuid ||
                failure instanceof GatewayCommandFailure.DeleteBlocked) {
            return HttpStatus.CONFLICT;
        }
        if (failure instanceof GatewayCommandFailure.OrganizationNotFound ||
                failure instanceof GatewayCommandFailure.LocationNotFound ||
                failure instanceof GatewayCommandFailure.GatewayNotFound) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private static String localizeMessage(MessageSource messageSource, GatewayCommandFailure failure) {
        return messageSource.getMessage(
                failure.messageKey(),
                failure.args(),
                failure.messageKey(),
                LocaleContextHolder.getLocale()
        );
    }
}
