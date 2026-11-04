package com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates;

import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.IoTDeviceUuid;
import com.acme.coldtrace.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * IoT device aggregate for the asset management bounded context.
 * <p>
 * An IoT device represents a physical sensor or capture device connected to an
 * edge gateway. It belongs to one organization, always references the gateway
 * that transports its telemetry, and may optionally be assigned to a monitored
 * asset when both resources share a compatible operational location.
 * <p>
 * The aggregate stores identifiers instead of object references. Application
 * services validate organization ownership, gateway existence, asset existence
 * and location compatibility before the aggregate is persisted.
 *
 * @since 1.0
 */
@Getter
public class IoTDevice extends AbstractDomainAggregateRoot<IoTDevice> {
    /**
     * Unique constraint name shared with the infrastructure persistence layer.
     */
    public static final String ORGANIZATION_ID_UUID_UNIQUE_CONSTRAINT = "uk_iot_device_organization_id_uuid";

    private Long id;
    private Long organizationId;
    private Long gatewayId;
    private IoTDeviceUuid uuid;
    private String deviceType;
    private String model;
    private String measurementType;
    private List<String> measurementParameters;
    private Integer readingFrequencySeconds;
    private Long assetId;
    private String status;
    private String calibrationStatus;
    private LocalDate lastCalibrationDate;
    private LocalDate nextCalibrationDate;

    protected IoTDevice() {
    }

    /**
     * Creates an IoT device aggregate from a creation command.
     *
     * @param command command containing validated and normalized device data
     * @see CreateIoTDeviceCommand
     */
    public IoTDevice(CreateIoTDeviceCommand command) {
        this.organizationId = command.organizationId();
        this.gatewayId = command.gatewayId();
        this.uuid = new IoTDeviceUuid(command.uuid());
        this.deviceType = command.deviceType();
        this.model = command.model();
        this.measurementType = command.measurementType();
        this.measurementParameters = List.copyOf(command.measurementParameters());
        this.readingFrequencySeconds = command.readingFrequencySeconds();
        this.assetId = command.assetId();
        this.status = command.status();
        this.calibrationStatus = command.calibrationStatus();
        this.lastCalibrationDate = command.lastCalibrationDate();
        this.nextCalibrationDate = command.nextCalibrationDate();
    }

    /**
     * Rebuilds an IoT device aggregate from persisted state.
     *
     * @param id persistence identifier
     * @param organizationId organization that owns the device
     * @param gatewayId connected gateway identifier
     * @param uuid organization-scoped business identifier
     * @param deviceType supported device type
     * @param model manufacturer or model label
     * @param measurementType human-readable measurement type label
     * @param measurementParameters telemetry parameters produced by the device
     * @param readingFrequencySeconds expected reading cadence in seconds
     * @param assetId optional assigned asset identifier
     * @param status operational status
     * @param calibrationStatus calibration status
     * @param lastCalibrationDate last calibration date
     * @param nextCalibrationDate next expected calibration date
     */
    public IoTDevice(
            Long id,
            Long organizationId,
            Long gatewayId,
            IoTDeviceUuid uuid,
            String deviceType,
            String model,
            String measurementType,
            List<String> measurementParameters,
            Integer readingFrequencySeconds,
            Long assetId,
            String status,
            String calibrationStatus,
            LocalDate lastCalibrationDate,
            LocalDate nextCalibrationDate
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.gatewayId = gatewayId;
        this.uuid = uuid;
        this.deviceType = deviceType;
        this.model = model;
        this.measurementType = measurementType;
        this.measurementParameters = List.copyOf(measurementParameters);
        this.readingFrequencySeconds = readingFrequencySeconds;
        this.assetId = assetId;
        this.status = status;
        this.calibrationStatus = calibrationStatus;
        this.lastCalibrationDate = lastCalibrationDate;
        this.nextCalibrationDate = nextCalibrationDate;
    }

    /**
     * Updates mutable IoT device fields with data from the update command.
     *
     * @param command command containing validated and normalized device state
     * @see UpdateIoTDeviceCommand
     */
    public void update(UpdateIoTDeviceCommand command) {
        this.gatewayId = command.gatewayId();
        this.uuid = new IoTDeviceUuid(command.uuid());
        this.deviceType = command.deviceType();
        this.model = command.model();
        this.measurementType = command.measurementType();
        this.measurementParameters = List.copyOf(command.measurementParameters());
        this.readingFrequencySeconds = command.readingFrequencySeconds();
        this.assetId = command.assetId();
        this.status = command.status();
        this.calibrationStatus = command.calibrationStatus();
        this.lastCalibrationDate = command.lastCalibrationDate();
        this.nextCalibrationDate = command.nextCalibrationDate();
    }

    /**
     * Returns the device uuid as a string for application and REST consumers.
     *
     * @return IoT device business uuid
     */
    public String getUuid() {
        return this.uuid.value();
    }

    /** Assigns an asset through the versioned source configuration use case. */
    public void associateSourceAsset(Long assetId) {
        if (assetId == null || assetId <= 0) throw new IllegalArgumentException("Invalid asset");
        this.assetId = assetId;
    }

    /**
     * Returns the strongly typed device uuid value object.
     *
     * @return IoT device uuid value object
     */
    public IoTDeviceUuid getUuidValue() {
        return this.uuid;
    }
}
