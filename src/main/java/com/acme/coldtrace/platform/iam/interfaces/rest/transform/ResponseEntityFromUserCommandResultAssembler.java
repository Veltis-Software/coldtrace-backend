package com.acme.coldtrace.platform.iam.interfaces.rest.transform;

import com.acme.coldtrace.platform.iam.application.commandservices.UserCommandFailure;
import com.acme.coldtrace.platform.iam.domain.model.aggregates.User;
import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementFailure;
import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementProblemProperties;
import com.acme.coldtrace.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Interface layer translator converting user command results to HTTP responses.
 *
 * @since 1.0
 */
public class ResponseEntityFromUserCommandResultAssembler {
    /**
     * Converts a user command result into an HTTP response.
     *
     * @param result user command result
     * @param messageSource message source for localized failure details
     * @return 201 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromResult(
            Result<User, UserCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                user -> new ResponseEntity<>(UserResourceFromEntityAssembler.toResourceFromEntity(user), CREATED),
                failure -> {
                    var status = statusFromFailure(failure);
                    var problemDetail = ProblemDetail.forStatusAndDetail(
                            status,
                            localizeMessage(messageSource, failure)
                    );
                    appendPlanEntitlementProperties(problemDetail, failure);
                    return ResponseEntity.status(status).body(problemDetail);
                }
        );
    }

    /**
     * Converts a user role assignment result into an HTTP response.
     *
     * @param result user command result
     * @param messageSource message source for localized failure details
     * @return 200 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromRoleAssignmentResult(
            Result<User, UserCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                user -> new ResponseEntity<>(UserResourceFromEntityAssembler.toResourceFromEntity(user), OK),
                failure -> {
                    var status = statusFromFailure(failure);
                    var problemDetail = ProblemDetail.forStatusAndDetail(
                            status,
                            localizeMessage(messageSource, failure)
                    );
                    appendPlanEntitlementProperties(problemDetail, failure);
                    return ResponseEntity.status(status).body(problemDetail);
                }
        );
    }

    /**
     * Converts a user deletion result into an HTTP response.
     *
     * @param result user deletion command result
     * @param messageSource message source for localized failure details
     * @return 204 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromDeleteResult(
            Result<?, UserCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                failure -> {
                    var status = statusFromFailure(failure);
                    var problemDetail = ProblemDetail.forStatusAndDetail(
                            status,
                            localizeMessage(messageSource, failure)
                    );
                    appendPlanEntitlementProperties(problemDetail, failure);
                    return ResponseEntity.status(status).body(problemDetail);
                }
        );
    }

    /**
     * Maps a user command failure to an HTTP status.
     *
     * @param failure user command failure
     * @return HTTP status for the failure
     */
    private static HttpStatus statusFromFailure(UserCommandFailure failure) {
        if (failure instanceof UserCommandFailure.DuplicateEmail ||
                failure instanceof UserCommandFailure.DeleteBlocked ||
                failure instanceof UserCommandFailure.PlanLimitExceeded) {
            return HttpStatus.CONFLICT;
        }
        if (failure instanceof UserCommandFailure.OrganizationNotFound ||
                failure instanceof UserCommandFailure.UserNotFound ||
                failure instanceof UserCommandFailure.RoleNotFound) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * Resolves the localized message for a user command failure.
     *
     * @param messageSource message source for i18n
     * @param failure user command failure
     * @return localized message or message key fallback
     */
    private static String localizeMessage(MessageSource messageSource, UserCommandFailure failure) {
        return messageSource.getMessage(
                failure.messageKey(),
                failure.args(),
                failure.messageKey(),
                LocaleContextHolder.getLocale()
        );
    }

    private static void appendPlanEntitlementProperties(ProblemDetail problemDetail, UserCommandFailure failure) {
        if (failure instanceof PlanEntitlementFailure planFailure) {
            PlanEntitlementProblemProperties.from(planFailure.entitlement())
                    .forEach(problemDetail::setProperty);
        }
    }
}
