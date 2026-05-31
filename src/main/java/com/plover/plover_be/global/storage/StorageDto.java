package com.plover.plover_be.global.storage;

public class StorageDto {

    public record PresignedUploadUrlResponse(String uploadUrl, String objectUrl) {}
}
