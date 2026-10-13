package com.systemguard.scan.dto;

import com.systemguard.common.model.enums.ScannerType;
import java.util.List;

public class TriggerScanRequest {
    private String branch;
    private List<ScannerType> scanners;

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public List<ScannerType> getScanners() { return scanners; }
    public void setScanners(List<ScannerType> scanners) { this.scanners = scanners; }
}
