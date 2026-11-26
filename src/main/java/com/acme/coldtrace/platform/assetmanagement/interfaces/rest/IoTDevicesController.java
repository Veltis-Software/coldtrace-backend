package com.acme.coldtrace.platform.assetmanagement.interfaces.rest;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.IoTDeviceCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.application.queryservices.IoTDeviceQueryService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetIoTDeviceByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetIoTDevicesByOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.CreateIoTDeviceResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.IoTDeviceResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.UpdateIoTDeviceResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.CreateIoTDeviceCommandFromResourceAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromIoTDeviceCommandResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromIoTDeviceQueryResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.UpdateIoTDeviceCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing IoT device endpoints.
 * <p>
 * Device operations are scoped by organization. The controller translates HTTP
 * requests into command/query objects while application services validate
 * gateway connectivity, optional asset assignment and location compatibility.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/iot-devices", produces = APPLICATION_JSON_VALUE)
@Tag(name = "IoT Devices", description = "Endpoints for IoT devices connected to gateways")
public class IoTDevicesController {
    private final IoTDeviceCommandService iotDeviceCommandService;
    private final IoTDeviceQueryService iotDeviceQueryService;
    private final MessageSource messageSource;

    public IoTDevicesController(
            IoTDeviceCommandService iotDeviceCommandService,
            IoTDeviceQueryService iotDeviceQueryService,
            MessageSource messageSource
    ) {
        this.iotDeviceCommandService = iotDeviceCommandService;
        this.iotDeviceQueryService = iotDeviceQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets IoT devices that belong to an organization.
     *
     * @param organizationId organization identifier used to filter devices
     * @return response entity containing device resources
     */
    @Operation(summary = "Get IoT devices by organization",
            description = "Gets IoT devices connected to gateways owned by the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devices found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = IoTDeviceResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getIoTDevicesByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/iot-devices", organizationId);
        var devices = iotDeviceQueryService.handle(new GetIoTDevicesByOrganizationIdQuery(organizationId));
        return ResponseEntityFromIoTDeviceQueryResultAssembler.toResponseEntityFromList(devices);
    }

    /**
     * Gets one IoT device by id.
     *
     * @param organizationId organization identifier
     * @param iotDeviceId IoT device identifier
     * @return response entity containing one device resource
     */
    @Operation(summary = "Get IoT device by id",
            description = "Gets one IoT device that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device found",
                    content = @Content(schema = @Schema(implementation = IoTDeviceResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or device not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{iotDeviceId}")
    public ResponseEntity<?> getIoTDeviceById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "iotDeviceId", description = "IoT device identifier", required = true)
            @PathVariable Long iotDeviceId) {
        log.debug("GET /organizations/{}/iot-devices/{}", organizationId, iotDeviceId);
        var device = iotDeviceQueryService.handle(
                new GetIoTDeviceByIdAndOrganizationIdQuery(organizationId, iotDeviceId)
        );
        if (device.isEmpty()) {
            return ResponseEntityFromIoTDeviceQueryResultAssembler.notFound(
                    messageSource,
                    "asset-management.iot-device.error.iot-device-not-found"
            );
        }
        return ResponseEntityFromIoTDeviceQueryResultAssembler.toResponseEntityFromIoTDevice(device.orElseThrow());
    }

    /**
     * Creates an IoT device.
     *
     * @param organizationId organization identifier
     * @param resource device creation request resource
     * @return response entity containing the created device resource
     */
    @Operation(
            summary = "Create an IoT device",
            description = "Creates an IoT device connected to an organization gateway",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "IoT device creation request",
                    content = @Content(schema = @Schema(implementation = CreateIoTDeviceResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device created",
                    content = @Content(schema = @Schema(implementation = IoTDeviceResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, gateway or asset not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - device uuid already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createIoTDevice(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateIoTDeviceResource resource) {
        log.debug("POST /organizations/{}/iot-devices - uuid={}", organizationId, resource.uuid());
        var command = CreateIoTDeviceCommandFromResourceAssembler.toCommandFromResource(resource, organizationId);
        var device = iotDeviceCommandService.handle(command);
        return ResponseEntityFromIoTDeviceCommandResultAssembler.toResponseEntityFromCreateResult(
                device,
                messageSource
        );
    }

    /**
     * Updates an IoT device.
     *
     * @param organizationId organization identifier
     * @param iotDeviceId IoT device identifier
     * @param resource device update request resource
     * @return response entity containing the updated device resource
     */
    @Operation(
            summary = "Update an IoT device",
            description = "Updates an IoT device connection, assignment and calibration data",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "IoT device update request",
                    content = @Content(schema = @Schema(implementation = UpdateIoTDeviceResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device updated",
                    content = @Content(schema = @Schema(implementation = IoTDeviceResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, gateway, asset or device not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - device uuid already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{iotDeviceId}")
    public ResponseEntity<?> updateIoTDevice(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "iotDeviceId", description = "IoT device identifier", required = true)
            @PathVariable Long iotDeviceId,
            @Valid @RequestBody UpdateIoTDeviceResource resource) {
        log.debug("PUT /organizations/{}/iot-devices/{} - uuid={}",
                organizationId, iotDeviceId, resource.uuid());
        var command = UpdateIoTDeviceCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                iotDeviceId
        );
        var device = iotDeviceCommandService.handle(command);
        return ResponseEntityFromIoTDeviceCommandResultAssembler.toResponseEntityFromUpdateResult(
                device,
                messageSource
        );
    }

    /**
     * Deletes an IoT device.
     *
     * @param organizationId organization identifier
     * @param iotDeviceId IoT device identifier
     * @return empty response on success or failure detail
     */
    @Operation(summary = "Delete an IoT device",
            description = "Deletes one IoT device that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "IoT device deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or IoT device not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "IoT device cannot be deleted because related data exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{iotDeviceId}")
    public ResponseEntity<?> deleteIoTDevice(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "iotDeviceId", description = "IoT device identifier", required = true)
            @PathVariable Long iotDeviceId) {
        log.debug("DELETE /organizations/{}/iot-devices/{}", organizationId, iotDeviceId);
        var result = iotDeviceCommandService.handle(new DeleteIoTDeviceCommand(organizationId, iotDeviceId));
        return ResponseEntityFromIoTDeviceCommandResultAssembler.toResponseEntityFromDeleteResult(
                result,
                messageSource
        );
    }
}
