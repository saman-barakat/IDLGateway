package es.us.isa.idl.idlgateway.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RequestValidation {
    private Boolean valid;
    private String message;

    public RequestValidation(Boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
}
