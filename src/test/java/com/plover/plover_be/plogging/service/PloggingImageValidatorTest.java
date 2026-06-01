package com.plover.plover_be.plogging.service;

import com.plover.plover_be.plogging.exception.PloggingErrorCode;
import com.plover.plover_be.plogging.exception.PloggingException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PloggingImageValidatorTest {

    private static final String FILE_PART_NAME = "image";
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L;

    private final PloggingImageValidator validator = new PloggingImageValidator();

    @DisplayName("정상 JPEG 이미지는 검증을 통과한다")
    @Test
    void validate_정상_jpeg_통과() throws IOException {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.jpg", "image/jpeg", createImageBytes("jpg")
        );

        // when & then
        assertThatCode(() -> validator.validate(image))
                .doesNotThrowAnyException();
    }

    @DisplayName("정상 PNG 이미지는 검증을 통과한다")
    @Test
    void validate_정상_png_통과() throws IOException {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.png", "image/png", createImageBytes("png")
        );

        // when & then
        assertThatCode(() -> validator.validate(image))
                .doesNotThrowAnyException();
    }

    @DisplayName("빈 파일이면 예외가 발생한다")
    @Test
    void validate_빈파일_예외() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.jpg", "image/jpeg", new byte[0]
        );

        // when & then
        assertImageValidationFails(image, PloggingErrorCode.IMAGE_FILE_REQUIRED);
    }

    @DisplayName("허용 크기를 초과하면 예외가 발생한다")
    @Test
    void validate_크기초과_예외() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.jpg", "image/jpeg", new byte[(int) MAX_FILE_SIZE_BYTES + 1]
        );

        // when & then
        assertImageValidationFails(image, PloggingErrorCode.IMAGE_SIZE_EXCEEDED);
    }

    @DisplayName("허용되지 않은 MIME 타입이면 예외가 발생한다")
    @Test
    void validate_mime_타입_예외() throws IOException {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.jpg", "application/octet-stream", createImageBytes("jpg")
        );

        // when & then
        assertImageValidationFails(image, PloggingErrorCode.INVALID_IMAGE_FORMAT);
    }

    @DisplayName("허용되지 않은 확장자이면 예외가 발생한다")
    @Test
    void validate_확장자_예외() throws IOException {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.gif", "image/jpeg", createImageBytes("jpg")
        );

        // when & then
        assertImageValidationFails(image, PloggingErrorCode.INVALID_IMAGE_FORMAT);
    }

    @DisplayName("파일 시그니처가 이미지가 아니면 예외가 발생한다")
    @Test
    void validate_magic_number_예외() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.jpg", "image/jpeg", "not-image".getBytes()
        );

        // when & then
        assertImageValidationFails(image, PloggingErrorCode.INVALID_IMAGE_FORMAT);
    }

    @DisplayName("MIME 타입과 실제 이미지 형식이 다르면 예외가 발생한다")
    @Test
    void validate_이미지_형식_불일치_예외() throws IOException {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.jpg", "image/jpeg", createImageBytes("png")
        );

        // when & then
        assertImageValidationFails(image, PloggingErrorCode.INVALID_IMAGE_FORMAT);
    }

    @DisplayName("이미지 픽셀 수가 제한을 초과하면 예외가 발생한다")
    @Test
    void validate_픽셀수_초과_예외() throws IOException {
        // given
        MockMultipartFile image = new MockMultipartFile(
                FILE_PART_NAME, "trash.png", "image/png", createImageBytes("png", 5000, 4001, BufferedImage.TYPE_BYTE_BINARY)
        );

        // when & then
        assertImageValidationFails(image, PloggingErrorCode.IMAGE_PIXEL_LIMIT_EXCEEDED);
    }

    private void assertImageValidationFails(MockMultipartFile image, PloggingErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(image))
                .isInstanceOf(PloggingException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    private byte[] createImageBytes(String formatName) throws IOException {
        return createImageBytes(formatName, 10, 10, BufferedImage.TYPE_INT_RGB);
    }

    private byte[] createImageBytes(String formatName, int width, int height, int imageType) throws IOException {
        BufferedImage image = new BufferedImage(width, height, imageType);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, outputStream);
        return outputStream.toByteArray();
    }
}
