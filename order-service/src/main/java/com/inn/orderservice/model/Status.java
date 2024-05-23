package com.inn.orderservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Status {

    @JsonProperty("INITIATED")
    INITIATED("INITIATED"),
    @JsonProperty("CREATED")
    CREATED("CREATED"),
    @JsonProperty("REJECTED")
    REJECTED("REJECTED"),
    @JsonProperty("SENT")
    SENT("SENT");

    private final String status;

    Status(String status){
        this.status=status;
    }

    @JsonValue
    public String getStatus(){
        return status;
    }

    public static Status fromString(String statusDto)  {

            switch (statusDto) {
                case "INITIATED":
                    return Status.INITIATED;
                case "CREATED":
                    return Status.CREATED;
                case "REJECTED":
                    return Status.REJECTED;
                case "SENT":
                    return Status.SENT;
                default:
                    throw new IllegalArgumentException("Unknown status: " + statusDto);
            }
        }
}
