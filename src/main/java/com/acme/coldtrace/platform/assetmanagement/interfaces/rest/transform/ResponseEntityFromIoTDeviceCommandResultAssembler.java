package com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.IoTDeviceCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.IoTDevice;
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
 * Interface layer translator converting IoT device command results to HTTP responses.
 *
 * @since 1.0
 */
public class ResponseEntityFromIoTDeviceCommandResultAssembler {
    /**
     * Converts an IoT device creation result into an HTTP response.
     *
     * @param result IoT device command result
     * @param messageSource message source for localized failure details
     * @return 201 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromCreateResult(
            Result<IoTDevice, IoTDeviceCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                device -> new ResponseEntity<>(
                        IoTDeviceResourceFromEntityAssembler.toResourceFromEntity(device),
                        CREATED
                ),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts an IoT device update result into an HTTP response.
     *
     * @param result IoT device command result
     * @param messageSource message source for localized failure details
     * @return 200 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromUpdateResult(
            Result<IoTDevice, IoTDeviceCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                device -> ResponseEntity.ok(IoTDeviceResourceFromEntityAssembler.toResourceFromEntity(device)),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    /**
     * Converts an IoT device deletion result into an HTTP response.
     *
     * @param result IoT device deletion command result
     * @param messageSource message source for localized failure details
     * @return 204 response on success or error response on failure
     */
    public static ResponseEntity<?> toResponseEntityFromDeleteResult(
            Result<?, IoTDeviceCommandFailure> result,
            MessageSource messageSource
    ) {
        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                failure -> toFailureResponse(failure, messageSource)
        );
    }

    private static ResponseEntity<?> toFailureResponse(
            IoTDeviceCommandFailure failure,
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

    private static HttpStatus statusFromFailure(IoTDeviceCommandFailure failure) {
        if (failure instanceof IoTDeviceCommandFailure.ManagedSourceConfiguration ||
                failure instanceof IoTDeviceCommandFailure.DuplicateUuid ||
                failure instanceof IoTDeviceCommandFailure.DeleteBlocked ||
                failure instanceof IoTDeviceCommandFailure.PlanLimitExceeded) {
            return HttpStatus.CONFLICT;
        }
        if (failure instanceof IoTDeviceCommandFailure.IncompatibleAssetLocation) {
            return HttpStatus.BAD_REQUEST;
        }
        if (failure instanceof IoTDeviceCommandFailure.OrganizationNotFound ||
                failure instanceof IoTDeviceCommandFailure.GatewayNotFound ||
                failure instanceof IoTDeviceCommandFailure.AssetNotFound ||
                failure instanceof IoTDeviceCommandFailure.IoTDeviceNotFound) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private static String localizeMessage(MessageSource messageSource, IoTDeviceCommandFailure failure) {
        return messageSource.getMessage(
                failure.messageKey(),
                failure.args(),
                failure.messageKey(),
                LocaleContextHolder.getLocale()
        );
    }

    private static void appendPlanEntitlementProperties(ProblemDetail problemDetail, IoTDeviceCommandFailure failure) {
        if (failure instanceof PlanEntitlementFailure planFailure) {
            PlanEntitlementProblemProperties.from(planFailure.entitlement())
                    .forEach(problemDetail::setProperty);
        }
    }
}
