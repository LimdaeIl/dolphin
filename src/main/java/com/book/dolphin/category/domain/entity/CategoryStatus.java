package com.book.dolphin.category.domain.entity;


/**
 * 카테고리 노출/운영 상태.
 *
 * <p>목적:
 * <ul>
 *   <li><b>ACTIVE</b>: 사용자/검색에 노출되는 정상 운영 상태</li>
 *   <li><b>READY</b>: 작성/검수 중이며 기본적으로 비노출(내부 관리만)</li>
 *   <li><b>DISABLED</b>: 운영 중단(비노출), 데이터는 유지</li>
 * </ul>
 * </p>
 *
 * <h3>상태 전이 가이드(권장)</h3>
 * <pre>
 * READY  →  ACTIVE     # 최초 공개
 * ACTIVE →  DISABLED   # 임시 중단/단종 등
 * DISABLED → ACTIVE    # 재개
 * </pre>
 *
 * @apiNote 클라이언트 노출 필터는 보통 {@code status == ACTIVE}만 허용합니다.
 * @implNote Enum은 DB에 {@code EnumType.STRING}으로 저장하는 것을 권장합니다
 *           (순서 변경에 안전).
 * @since 1.0
 */
public enum CategoryStatus {
    ACTIVE,
    READY,
    DISABLED
}
