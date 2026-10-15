package net.java21.library.external.opennaru.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 테스트용 도서관정보나루 API 키 조회
 *
 * <p>환경 변수 LIBRARY_API_KEY → 프로젝트 루트 secrets.properties → "test-key" 순으로 찾습니다.
 * 실제 API를 호출하는 테스트는 유효한 키가 있어야 통과합니다.</p>
 */
final class TestApiKeys {

    private static final String ENV_NAME = "LIBRARY_API_KEY";

    private TestApiKeys() {
    }

    static String libraryApiKey() {
        String fromEnv = System.getenv(ENV_NAME);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        Path secrets = Path.of("secrets.properties");
        if (Files.exists(secrets)) {
            Properties props = new Properties();
            try (Reader reader = Files.newBufferedReader(secrets)) {
                props.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("secrets.properties를 읽을 수 없습니다.", e);
            }
            String fromFile = props.getProperty(ENV_NAME);
            if (fromFile != null && !fromFile.isBlank()) {
                return fromFile;
            }
        }
        return "test-key";
    }
}
