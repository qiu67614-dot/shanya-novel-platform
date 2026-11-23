package com.sdjz.novel.pojo.dto.reader;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WithdrawalDTO {
    @Size(max = 500)
    private String reason;
}
