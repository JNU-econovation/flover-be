package com.plover.plover_be.plogging.service;

import com.plover.plover_be.plogging.exception.PloggingErrorCode;
import com.plover.plover_be.plogging.exception.PloggingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PloggingImageValidator {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    public void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw imageValidationException(PloggingErrorCode.IMAGE_FILE_REQUIRED);
        }

        if (image.getSize() > MAX_FILE_SIZE_BYTES) {
            throw imageValidationException(PloggingErrorCode.IMAGE_SIZE_EXCEEDED);
        }

        ImageFormat contentTypeFormat = ImageFormat.fromContentType(image.getContentType());
        ImageFormat extensionFormat = ImageFormat.fromFilename(image.getOriginalFilename());
        if (contentTypeFormat == null || extensionFormat == null || contentTypeFormat != extensionFormat) {
            throw imageValidationException(PloggingErrorCode.INVALID_IMAGE_FORMAT);
        }

        validateImageContent(image, contentTypeFormat);
    }

    private void validateImageContent(MultipartFile image, ImageFormat expectedFormat) {
        try (InputStream inputStream = image.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageInputStream == null) {
                throw imageValidationException(PloggingErrorCode.INVALID_IMAGE_FORMAT);
            }

            ImageFormat magicNumberFormat = readMagicNumberFormat(imageInputStream);
            if (magicNumberFormat == null || magicNumberFormat != expectedFormat) {
                throw imageValidationException(PloggingErrorCode.INVALID_IMAGE_FORMAT);
            }

            validateImageHeader(imageInputStream, expectedFormat);
        } catch (PloggingException e) {
            throw e;
        } catch (IOException e) {
            throw new PloggingException(PloggingErrorCode.IMAGE_PROCESSING_FAILED);
        }
    }

    private ImageFormat readMagicNumberFormat(ImageInputStream imageInputStream) {
        try {
            byte[] header = new byte[8];
            int bytesRead = imageInputStream.read(header);
            imageInputStream.seek(0);
            return ImageFormat.fromMagicNumber(header, Math.max(bytesRead, 0));
        } catch (IOException e) {
            throw imageValidationException(PloggingErrorCode.INVALID_IMAGE_FORMAT);
        }
    }

    private void validateImageHeader(ImageInputStream imageInputStream, ImageFormat expectedFormat) {
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw imageValidationException(PloggingErrorCode.INVALID_IMAGE_FORMAT);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                ImageFormat actualFormat = ImageFormat.fromReaderFormatName(reader.getFormatName());
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (actualFormat != expectedFormat || width <= 0 || height <= 0) {
                    throw imageValidationException(PloggingErrorCode.INVALID_IMAGE_FORMAT);
                }

                if ((long) width * height > MAX_IMAGE_PIXELS) {
                    throw imageValidationException(PloggingErrorCode.IMAGE_PIXEL_LIMIT_EXCEEDED);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw imageValidationException(PloggingErrorCode.INVALID_IMAGE_FORMAT);
        }
    }

    private PloggingException imageValidationException(PloggingErrorCode errorCode) {
        return new PloggingException(errorCode);
    }

    private enum ImageFormat {
        JPEG(Set.of("image/jpeg"), Set.of("jpg", "jpeg")),
        PNG(Set.of("image/png"), Set.of("png"));

        private final Set<String> contentTypes;
        private final Set<String> extensions;

        ImageFormat(Set<String> contentTypes, Set<String> extensions) {
            this.contentTypes = contentTypes;
            this.extensions = extensions;
        }

        private static ImageFormat fromContentType(String contentType) {
            if (contentType == null || contentType.isBlank()) {
                return null;
            }

            String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
            for (ImageFormat format : values()) {
                if (format.contentTypes.contains(normalizedContentType)) {
                    return format;
                }
            }
            return null;
        }

        private static ImageFormat fromFilename(String filename) {
            if (filename == null || filename.isBlank()) {
                return null;
            }

            int extensionStart = filename.lastIndexOf('.');
            if (extensionStart < 0 || extensionStart == filename.length() - 1) {
                return null;
            }

            String extension = filename.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
            for (ImageFormat format : values()) {
                if (format.extensions.contains(extension)) {
                    return format;
                }
            }
            return null;
        }

        private static ImageFormat fromMagicNumber(byte[] header, int bytesRead) {
            if (bytesRead >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return JPEG;
            }

            if (bytesRead >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A) {
                return PNG;
            }

            return null;
        }

        private static ImageFormat fromReaderFormatName(String formatName) {
            if (formatName == null) {
                return null;
            }

            return switch (formatName.toLowerCase(Locale.ROOT)) {
                case "jpeg", "jpg" -> JPEG;
                case "png" -> PNG;
                default -> null;
            };
        }
    }
}
