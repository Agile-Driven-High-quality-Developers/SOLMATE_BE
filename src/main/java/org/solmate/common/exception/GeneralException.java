package org.solmate.common.exception;


import org.solmate.common.base.BaseStatus;

import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {
    private final BaseStatus errorStatus;

    public GeneralException(BaseStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}
