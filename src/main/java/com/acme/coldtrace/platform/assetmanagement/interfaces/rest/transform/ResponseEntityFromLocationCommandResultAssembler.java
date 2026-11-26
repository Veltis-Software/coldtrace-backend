package com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.LocationCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Location;
import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementFailure;
import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementProblemProperties;
import com.acme.coldtrace.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * Interface layer translator converting location command results to HTTP responses.
 *
 * @since 1.0
 */
public class ResponseEntityFromLocationCommandResultAssembler {
    /**
     * Converts a location creation result into an HTTP response.
     *
     * @param result location command result
     * @param messageSource message source for localized failure details
     * @return 201 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromCreateResult(
            Result<Location, LocationCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                location -> new ResponseEntity<>(
                        LocationResourceFromEntityAssembler.toResourceFromEntity(location),
                        CREATED
                ),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts a location update result into an HTTP response.
     *
     * @param result location command result
     * @param messageSource message source for localized failure details
     * @return 200 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromUpdateResult(
            Result<Location, LocationCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                location -> ResponseEntity.ok(LocationResourceFromEntityAssembler.toResourceFromEntity(location)),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts a location deletion result into an HTTP response.
     *
     * @param result location deletion command result
     * @param messageSource message source for localized failure details
     * @return 204 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromDeleteResult(
            Result<?, LocationCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    private static ResponseEntity<?> toFailureResponse(
            LocationCommandFailure failure,
            MessageSource messageSource
    ) {
        var status = statusFromFailure(failure);
        var problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                localizeMessage(messageSource, failure)
        );
        appendPlanEntitlementProperties(problemDetail, failure);
        return ResponseEntity.status(status).body(problemDetail);
    }

    private static HttpStatus statusFromFailure(LocationCommandFailure failure) {
        if (failure instanceof LocationCommandFailure.DuplicateName ||
                failure instanceof LocationCommandFailure.DeleteBlocked ||
                failure instanceof LocationCommandFailure.PlanLimitExceeded) {
            return HttpStatus.CONFLICT;
        }
        if (failure instanceof LocationCommandFailure.OrganizationNotFound ||
                failure instanceof LocationCommandFailure.LocationNotFound) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private static String localizeMessage(MessageSource messageSource, LocationCommandFailure failure) {
        return messageSource.getMessage(
                failure.messageKey(),
                failure.args(),
                failure.messageKey(),
                LocaleContextHolder.getLocale()
        );
    }

    private static void appendPlanEntitlementProperties(ProblemDetail problemDetail, LocationCommandFailure failure) {
        if (failure instanceof PlanEntitlementFailure planFailure) {
            PlanEntitlementProblemProperties.from(planFailure.entitlement())
                    .forEach(problemDetail::setProperty);
        }
    }
}
