package ict.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClinicService implements Serializable {

    private Integer clinicId;
    private String clinicName;
    private String district;
    private String address;
    private String phone;
    private boolean active;
    private List<String> services = new ArrayList<>();

    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getServices() {
        return Collections.unmodifiableList(services);
    }

    public void setServices(List<String> services) {
        this.services = services == null ? new ArrayList<>() : new ArrayList<>(services);
    }

    public void addService(String serviceName) {
        if (serviceName == null) {
            return;
        }

        String trimmed = serviceName.trim();
        if (!trimmed.isEmpty()) {
            services.add(trimmed);
        }
    }

    public String getServicesLabel() {
        return services.isEmpty() ? "No services listed" : String.join(", ", services);
    }
}
