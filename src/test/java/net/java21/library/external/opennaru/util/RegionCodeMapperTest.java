package net.java21.library.external.opennaru.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RegionCodeMapper 단위 테스트
 */
class RegionCodeMapperTest {

    @Test
    void testGwangjuNamguMapping() {
        // "광주 남구"가 24030으로 매핑되는지 확인
        String code = RegionCodeMapper.toRegionCode("광주 남구");
        assertThat(code).isEqualTo("24030");
    }

    @Test
    void testBusanNamguMapping() {
        String code = RegionCodeMapper.toRegionCode("부산 남구");
        assertThat(code).isEqualTo("21070");
    }

    @Test
    void testDaeguNamguMapping() {
        String code = RegionCodeMapper.toRegionCode("대구 남구");
        assertThat(code).isEqualTo("22040");
    }

    @Test
    void testUlsanNamguMapping() {
        String code = RegionCodeMapper.toRegionCode("울산 남구");
        assertThat(code).isEqualTo("26020");
    }

    @Test
    void testGangnamGuMapping() {
        String code = RegionCodeMapper.toRegionCode("강남구");
        assertThat(code).isEqualTo("11230");
    }

    @Test
    void testSeoulGangnamGuMapping() {
        String code = RegionCodeMapper.toRegionCode("서울 강남구");
        assertThat(code).isEqualTo("11230");
    }

    @Test
    void testGwangjuCityMapping() {
        String code = RegionCodeMapper.toRegionCode("광주");
        assertThat(code).isEqualTo("24");
    }

    @Test
    void testJongnoGuMapping() {
        String code = RegionCodeMapper.toRegionCode("종로구");
        assertThat(code).isEqualTo("11010");
    }

    @Test
    void testGwangsanGuMapping() {
        String code = RegionCodeMapper.toRegionCode("광산구");
        assertThat(code).isEqualTo("24050");
    }

    @Test
    void testIsRegionCode() {
        assertThat(RegionCodeMapper.isRegionCode("11")).isTrue();
        assertThat(RegionCodeMapper.isRegionCode("24030")).isTrue();
        assertThat(RegionCodeMapper.isRegionCode("11010")).isTrue();
        assertThat(RegionCodeMapper.isRegionCode("680")).isFalse();  // 강남구 옛 코드 (현재 사용하지 않음)
        assertThat(RegionCodeMapper.isRegionCode("abc")).isFalse();
        assertThat(RegionCodeMapper.isRegionCode("123456")).isFalse();
    }

    @Test
    void testNullOrBlankInput() {
        assertThat(RegionCodeMapper.toRegionCode(null)).isNull();
        assertThat(RegionCodeMapper.toRegionCode("")).isNull();
        assertThat(RegionCodeMapper.toRegionCode("   ")).isNull();
    }
}
