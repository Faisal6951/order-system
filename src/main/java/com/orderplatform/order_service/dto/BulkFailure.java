package com.orderplatform.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class BulkFailure {
    private String rowTitle;

    private String reason;
}
