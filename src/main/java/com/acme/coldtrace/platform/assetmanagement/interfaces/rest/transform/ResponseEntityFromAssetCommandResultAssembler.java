package com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Asset;
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
 * Interface layer translator converting asset command results to HTTP
 * responses.
 * <p>
 * Command services return typed results instead of HTTP concerns. This
 * assembler is the single place where those results become REST success
 * resources or localized {@link ProblemDetail} errors.
 *
 * @since 1.0
 */
public class ResponseEntityFromAssetCommandResultAssembler {
    /**
     * Converts an asset creation result into an HTTP response.
     *
     * @param result asset command result
     * @param messageSource message source for localized failure details
     * @return 201 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromCreateResult(
            Result<Asset, AssetCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                asset -> new ResponseEntity<>(
                        AssetResourceFromEntityAssembler.toResourceFromEntity(asset),
                        CREATED
                ),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts an asset update result into an HTTP response.
     *
     * @param result asset command result
     * @param messageSource message source for localized failure details
     * @return 200 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromUpdateResult(
            Result<Asset, AssetCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                asset -> ResponseEntity.ok(AssetResourceFromEntityAssembler.toResourceFromEntity(asset)),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts an asset deletion result into an HTTP response.
     *
     * @param result asset deletion command result
     * @param messageSource message source for localized failure details
     * @return 204 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromDeleteResult(
            Result<?, AssetCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    private static ResponseEntity<?> toFailureResponse(
            AssetCommandFailure failure,
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

    private static HttpStatus statusFromFailure(AssetCommandFailure failure) {
        if (failure instanceof AssetCommandFailure.DuplicateUuid ||
                failure instanceof AssetCommandFailure.DeleteBlocked ||
                failure instanceof AssetCommandFailure.PlanLimitExceeded) {
            return HttpStatus.CONFLICT;
        }
        if (failure instanceof AssetCommandFailure.OrganizationNotFound ||
                failure instanceof AssetCommandFailure.LocationNotFound ||
                failure instanceof AssetCommandFailure.AssetNotFound) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private static String localizeMessage(MessageSource messageSource, AssetCommandFailure failure) {
        return messageSource.getMessage(
                failure.messageKey(),
                failure.args(),
                failure.messageKey(),
                LocaleContextHolder.getLocale()
        );
    }

    private static void appendPlanEntitlementProperties(ProblemDetail problemDetail, AssetCommandFailure failure) {
        if (failure instanceof PlanEntitlementFailure planFailure) {
            PlanEntitlementProblemProperties.from(planFailure.entitlement())
                    .forEach(problemDetail::setProperty);
        }
    }
}
