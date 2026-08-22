package com.sgv.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WarehouseTransferDto(
    Long id,
    String series,
    Long documentNumber,
    Integer documentYear,
    String warehouseName,
    String branchName,
    String status,
    String requestedByName,
    LocalDateTime createdAt,
    int totalItemsCount
) {}
